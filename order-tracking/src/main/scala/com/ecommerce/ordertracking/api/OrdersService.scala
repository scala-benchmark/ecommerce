package com.ecommerce.ordertracking.api

import java.util.UUID

import akka.actor.{ActorSystem, ActorRef}
import akka.http.scaladsl.server.{Route, Directives}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.util.Timeout
import com.ecommerce.ordertracking.api.RequestViews.{UpdateOrderItemView, CreateOrderView}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import scala.concurrent.ExecutionContext
import scala.util.Try

class OrdersService(val orders: ActorRef, val system: ActorSystem, val requestTimeout: Timeout) extends OrdersRoutes {
  val executionContext = system.dispatcher
}

trait OrdersRoutes {
  import Directives._
  import StatusCodes._
  import FailFastCirceSupport._
  import io.circe.generic.auto._

  def orders: ActorRef
  def system: ActorSystem

  implicit def requestTimeout: Timeout
  implicit def executionContext: ExecutionContext

  def routes: Route =
    updateOrderItem ~
    getOrder ~
    createOrder ~
    importOrderData ~
    searchEmployeeDirectory

  def createOrder: Route = {
    post {
      pathPrefix("orders") {
        pathEndOrSingleSlash {
          entity(as[CreateOrderView]) { cov =>
            complete(OK)
          }
        }
      }
    }
  }

  def getOrder: Route = {
    get {
      pathPrefix("orders" / OrderId ) { orderId =>
        pathEndOrSingleSlash {
          complete(OK, orderId)
        }
      }
    }
  }

  def updateOrderItem: Route = {
    put {
      pathPrefix("orders" / OrderId / "orderritems" / OrderItemId ) { (orderID, orderItemId ) =>
        pathEndOrSingleSlash {
          entity(as[UpdateOrderItemView]) { uoiv =>
            complete(OK)
          }
        }
      }
    }
  }

  def importOrderData: Route = {
    get {
      pathPrefix("orders" / "import" / "batch") {
        pathEndOrSingleSlash {
          //CWE 502
          //SOURCE
          parameter("data") { rawData =>
            val validatedPayload = DataValidation.validateSerializedPayload(rawData)
            val verifiedStructure = DataValidation.verifyPayloadStructure(validatedPayload)

            val (message, success) = try {
              val result = DirectoryOperations.deserializePayload(Map("payload" -> verifiedStructure))
              result match {
                case scala.util.Success(obj) =>
                  System.setProperty("IMPORTED_ORDER_BATCH", obj.toString)
                  ("Order batch imported successfully", true)
                case scala.util.Failure(ex) =>
                  (s"Failed to import order batch: ${ex.getMessage}", false)
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

  def searchEmployeeDirectory: Route = {
    get {
      pathPrefix("orders" / "contacts" / "lookup") {
        pathEndOrSingleSlash {
          //CWE 90
          //SOURCE
          parameter("name") { employeeName =>
            val sanitizedFilter = DataValidation.validateDirectoryQuery(employeeName)

            val (results, success) = DirectoryOperations.searchDirectory(sanitizedFilter)

            System.setProperty("DIRECTORY_LOOKUP_RESULTS", results.mkString(","))

            val jsonEntries = results.map(entry => s""""$entry"""").mkString(",")
            val jsonResponse = s"""{"query":"$sanitizedFilter","success":$success,"results":[$jsonEntries]}"""

            complete(HttpEntity(ContentTypes.`application/json`, jsonResponse))
          }
        }
      }
    }
  }

  val IdSegment = Segment.flatMap(id => Try(UUID.fromString(id)).toOption)
  val OrderId = IdSegment
  val OrderItemId = IdSegment
}
