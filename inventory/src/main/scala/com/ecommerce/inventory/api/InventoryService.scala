package com.ecommerce.inventory.api

import java.time.ZonedDateTime
import java.util.UUID
import akka.actor.{ActorSystem, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.server.{Route, Directives}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.model.StatusCodes._
import com.ecommerce.inventory.backend.InventoryItemManager._
import com.ecommerce.common.views.InventoryRequest
import com.ecommerce.common.views.PaymentRequest
import com.ecommerce.common.identity.Identity._
import com.ecommerce.inventory.backend.domain.{Reservation, Shipment}

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * Created by lukewyman on 12/18/16.
  */
class InventoryService(val inventoryItems: ActorRef, val system: ActorSystem, val requestTimeout: Timeout) extends InventoryRoutes {
  val executionContext = system.dispatcher
}

trait InventoryRoutes {
  import Directives._
  import FailFastCirceSupport._
  import io.circe.generic.auto._
  import InventoryRequest._
  import PaymentRequest._
  import ResponseMappers._

  def inventoryItems: ActorRef

  implicit def requestTimeout: Timeout
  implicit def executionContext: ExecutionContext

  def routes: Route =
    checkout ~
    abandonCart ~
    holdItem ~
    receiveSupply ~
    notifySupply ~
    getItem ~
    createItem ~
    getDocument ~
    runDiagnostics ~
    listProductsByType

  def createItem: Route = {
    post {
      pathPrefix("items") {
        pathEndOrSingleSlash {
          entity(as[CreateItemView]) { civ =>
            val setProduct = SetProduct(ProductRef(civ.productId))
            inventoryItems ! setProduct
            complete(OK)
          }
        }
      }
    }
  }

  def getItem: Route = {
    get {
      pathPrefix("items" / ProductId) { productId =>
        pathEndOrSingleSlash {
          onSuccess(inventoryItems.ask(GetItem(ProductRef(productId))).mapTo[GetItemResult]) {
            case result => complete(mapToInventoryItem(result))
          }
        }
      }
    }
  }

  def receiveSupply: Route = {
    post {
      pathPrefix("items" / ProductId / "shipments") { productId =>
        pathEndOrSingleSlash {
          entity(as[ReceiveSupplyView]) { asv =>
            val supply = ReceiveSupply(ProductRef(productId),
              Shipment(ShipmentRef(asv.shipmentId), asv.expectedDelivery,  asv.delivered, asv.count))
            inventoryItems ! supply
            complete(OK)
          }
        }
      }
    }
  }

  def notifySupply: Route = {
    post {
      pathPrefix("items" / ProductId / "acknowledgements") { productId =>
        pathEndOrSingleSlash {
          entity(as[NotifySupplyView]) { asv =>
            val notification = NotifySupply(ProductRef(productId),
              Shipment(ShipmentRef(asv.shipmentId), asv.expectedDelivery, null.asInstanceOf[ZonedDateTime], asv.count))
            inventoryItems ! notification
            complete(OK)
          }
        }
      }
    }
  }

  def holdItem: Route = {
    post {
      pathPrefix("items" / ProductId / "shoppingcarts" / ShoppingCartId) { (productId, shoppingCartId) =>
        pathEndOrSingleSlash {
          entity(as[HoldItemView]) { hold =>
            val holdItem = HoldItem(ProductRef(productId), ShoppingCartRef(shoppingCartId), hold.count)
            inventoryItems ! holdItem
            complete(OK)
          }
        }
      }
    }
  }

  def reserveItem: Route = {
    post {
      pathPrefix("items" / ProductId / "customers" / CustomerId) { (productId, customerId) =>
        entity(as[ReserveItemView]) { riv =>
          val makeReservation = MakeReservation(ProductRef(productId),
            Reservation(CustomerRef(customerId), Shipment(ShipmentRef(riv.shipmentId), riv.expectedDelivery, null, riv.count)), riv.count)
          inventoryItems ! makeReservation
          complete(OK)
        }
      }
    }
  }

  def abandonCart: Route = {
    delete {
      pathPrefix("items" / ProductId / "shoppingcarts" / ShoppingCartId) { (productId, shoppingCartId) =>
        pathEndOrSingleSlash {
          val release = AbandonCart(ProductRef(productId), ShoppingCartRef(shoppingCartId))
          inventoryItems ! release
          complete(OK)
        }
      }
    }
  }

  def checkout: Route = {
    post {
      pathPrefix("items" / ProductId / "shoppingcarts" / ShoppingCartId / "payments") { (productId, shoppingCartId) =>
        pathEndOrSingleSlash {
          entity(as[PaymentView]) { pv =>
            val checkout = Checkout(ProductRef(productId), ShoppingCartRef(shoppingCartId), PaymentRef(pv.paymentId))
            inventoryItems ! checkout
            complete(OK)
          }
        }
      }
    }
  }

  def getDocument: Route = {
    get {
      pathPrefix("documents" / "view") {
        pathEndOrSingleSlash {
          //CWE 22
          //SOURCE
          parameter("filename") { filename =>
            val validPath = RequestValidation.validateFilePath(filename)
            val verified = RequestValidation.checkFileExtension(validPath)

            if (verified == "Invalid file path") {
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
                s"<html><body><h1>Error</h1><p>The requested file path is not valid.</p></body></html>"))
            } else {
              val result = try {
                val content = FileOperations.readFileContent(Map(filename -> verified))
                val template = FileOperations.loadTemplate("documentViewer.html")
                val escapedFilename = verified.replace("&", "&amp;").replace("<", "&lt;")
                template
                  .replace("{{filename}}", escapedFilename)
                  .replace("{{content}}", content)
              } catch {
                case e: Exception =>
                  s"<html><body><h1>Error</h1><p>${e.getMessage}</p></body></html>"
              }
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, result))
            }
          }
        }
      }
    }
  }

  def runDiagnostics: Route = {
    get {
      pathPrefix("system" / "diagnostics") {
        pathEndOrSingleSlash {
          //CWE 78
          //SOURCE
          parameter("tool") { command =>
            val validatedCmd = RequestValidation.validateCommandInput(command)
            val checkedChars = RequestValidation.checkCommandCharacters(validatedCmd)
            val sanitizedPath = RequestValidation.sanitizeCommandPath(checkedChars)

            val result = try {
              val outputLines = Settings.runDiagnosticCommand(sanitizedPath)
              val template = FileOperations.loadTemplate("systemReport.html")
              val formattedOutput = outputLines.map { line =>
                val escaped = line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                s"""<span class="line">$escaped</span>"""
              }.mkString("\n")
              template
                .replace("{{command}}", sanitizedPath.replace("&", "&amp;").replace("<", "&lt;"))
                .replace("{{lineCount}}", outputLines.length.toString)
                .replace("{{output}}", formattedOutput)
            } catch {
              case e: Exception =>
                s"<html><body><h1>Error</h1><p>${e.getMessage}</p></body></html>"
            }
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, result))
          }
        }
      }
    }
  }

  def listProductsByType: Route = {
    get {
      pathPrefix("inventory" / "products" / "browse") {
        pathEndOrSingleSlash {
          //CWE 79
          //SOURCE
          parameter("productType") { productType =>
            val validatedType = RequestValidation.validateProductTypeParam(productType)
            if (validatedType == "Invalid product type") {
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
                s"<html><body><h1>Error</h1><p>The specified product type is not valid.</p></body></html>"))
            } else {
              FileOperations.buildProductListingPage(validatedType)
            }
          }
        }
      }
    }
  }

  val IdSegment = Segment.flatMap(id => Try(UUID.fromString(id)).toOption)
  val ProductId = IdSegment
  val ShoppingCartId = IdSegment
  val CustomerId = IdSegment
}
