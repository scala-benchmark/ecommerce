package com.ecommerce.shoppingcart.api

import java.util.{UUID, Base64}
import java.io.Serializable
import akka.actor.{ActorSystem, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.server.{Route, Directives}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.model.headers.Location
import akka.serialization.{Serialization, SerializationExtension}
import com.ecommerce.common.identity.Identity
import com.ecommerce.shoppingcart.backend._
import com.ecommerce.common.views.ShoppingCartRequest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import scalatags.Text.all._

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * Created by lukewyman on 12/11/16.
  */
class ShoppingCartService(val shoppingCarts: ActorRef, val system: ActorSystem, val requestTimeout: Timeout) extends ShoppingCartRoutes {
  val executionContext = system.dispatcher
}

trait ShoppingCartRoutes {

  import FailFastCirceSupport._
  import Directives._
  import StatusCodes._
  import io.circe.generic.auto._
  import ShoppingCartRequest._
  import ResponseMappers._
  import Identity._

  def shoppingCarts: ActorRef
  def system: ActorSystem

  implicit def requestTimeout: Timeout
  implicit def executionContext: ExecutionContext

  def routes: Route =
    addItem ~
    getShoppingCart ~
    createShoppingCart ~
    importCartData ~
    externalRedirect

  private def validateSerializedData(data: String): String = {
    if (data == null || data.isEmpty) {
      println("Warning: Serialized data is empty")
    }
    data
  }

  private def validateDataFormat(data: String): String = {
    if (!data.matches("[A-Za-z0-9+/=]+")) {
      println("Warning: Data format may be invalid base64")
    }
    data
  }

  private def validateRedirectUrl(url: String): String = {
    if (url == null || url.isEmpty) {
      println("Warning: Redirect URL is empty")
    }
    url
  }

  private def validateUrlProtocol(url: String): String = {
    if (!url.startsWith("http")) {
      println("Warning: URL protocol may be missing")
    }
    url
  }

  def createShoppingCart: Route = {
    post {
      pathPrefix("shoppingcarts") {
        pathEndOrSingleSlash {
          entity(as[CreateShoppingCartView]) { cscv =>
            val setOwner = SetOwner(ShoppingCartRef(cscv.shoppingCartId), CustomerRef(cscv.customerId))
            getRoute(setOwner)
          }
        }
      }
    }
  }

  def addItem: Route = {
    put {
      pathPrefix("shoppingcarts" / ShoppingCartId / "items" / ProductId) { (shoppingCartId, productId) =>
        pathEndOrSingleSlash {
          entity(as[AddItemView]) { aiv =>
            val addItem = AddItem(ShoppingCartRef(shoppingCartId), ProductRef(productId), aiv.count)
            getRoute(addItem)
          }
        }
      }
    }
  }

  def removeItem: Route = {
    delete {
      pathPrefix("shoppingcarts" / ShoppingCartId / "items" / ProductId) { (shoppingCartId, productId) =>
        pathEndOrSingleSlash {
          val removeItem = RemoveItem(ShoppingCartRef(shoppingCartId), ProductRef(productId))
          getRoute(removeItem)
        }
      }
    }
  }

  def getShoppingCart: Route = {
    get {
      pathPrefix("shoppingcarts" / ShoppingCartId) { shoppingCartId =>
        pathEndOrSingleSlash {
          val getItems = GetItems(ShoppingCartRef(shoppingCartId))
          getRoute(getItems)
        }
      }
    }
  }

  def importCartData: Route = {
    get {
      pathPrefix("shoppingcarts" / "import" / "restore") {
        pathEndOrSingleSlash {
          //SOURCE
          parameter("data") { rawData =>
            val validatedOnce = validateSerializedData(rawData)
            val serializedData = validateDataFormat(validatedOnce)

            val (message, success) = try {
              val bytes = Base64.getDecoder.decode(serializedData)
              val serialization = SerializationExtension(system)

              //CWE 502
              //SINK
              val result = serialization.deserialize(bytes, classOf[Serializable])

              result match {
                case scala.util.Success(obj) =>
                  System.setProperty("IMPORTED_CART_DATA", obj.toString)
                  ("Cart data imported successfully", true)
                case scala.util.Failure(ex) =>
                  (s"Failed to import cart data: ${ex.getMessage}", false)
              }
            } catch {
              case e: Exception =>
                (s"Import error: ${e.getMessage}", false)
            }

            complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, message))
          }
        }
      }
    }
  }

  def externalRedirect: Route = {
    get {
      pathPrefix("shoppingcarts" / "partner" / "redirect") {
        pathEndOrSingleSlash {
          //SOURCE
          parameter("destination") { rawDestination =>
            val validatedOnce = validateRedirectUrl(rawDestination)
            val destination = validateUrlProtocol(validatedOnce)

            // Store referral tracking information
            System.setProperty("LAST_PARTNER_REDIRECT", destination)

            //CWE 601
            //SINK
            redirect(destination, StatusCodes.Found)
          }
        }
      }
    }
  }

  def getRoute(msg: ShoppingCartMessage) =
    onSuccess(shoppingCarts.ask(msg).mapTo[ManagerResponse]) {
      case gscr: GetShoppingCartResult => complete(OK, mapToShoppingCartView(gscr.shoppingCartId, gscr.shoppingCart))
      case Rejection(reason) => complete(BadRequest, reason)
  }

  val IdSegment = Segment.flatMap(id => Try(UUID.fromString(id)).toOption)
  val ProductId = IdSegment
  val ShoppingCartId = IdSegment
}

