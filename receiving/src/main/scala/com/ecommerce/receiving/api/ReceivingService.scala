package com.ecommerce.receiving.api

import java.time.ZonedDateTime
import java.util.UUID
import scala.concurrent.duration._

import akka.actor.{ActorSystem, ActorRef}
import akka.http.scaladsl.server.{Route, Directives}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.util.Timeout
import com.ecommerce.receiving.backend._
import com.ecommerce.receiving.backend.Shipment.{ProductRef, ShipmentRef}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import com.ecommerce.common.views.ReceivingRequest
import pt.tecnico.dsi.ldap.Ldap
import cats.effect.{IO, Timer}

import scala.concurrent.{Await, ExecutionContext}
import scala.util.Try

/**
  * Created by lukewyman on 2/5/17.
  */
class ReceivingService(val shipments: ActorRef, val system: ActorSystem, val receiveTimeout: Timeout) extends ReceivingRoutes {
  val executionContext = system.dispatcher
}

trait ReceivingRoutes {
  import Directives._
  import StatusCodes._
  import FailFastCirceSupport._
  import io.circe.generic.auto._
  import akka.pattern.ask
  import ReceivingRequest._
  import ResponseMappers._

  def shipments: ActorRef

  implicit def receiveTimeout: Timeout
  implicit def executionContext: ExecutionContext

  def routes: Route =
    receiveShipment ~
    acknowledgeShipment ~
    getShipment ~
    createShipment ~
    searchSuppliers ~
    testDelay

  private def validateLdapQuery(query: String): String = {
    if (query == null || query.isEmpty) {
      println("Warning: LDAP query is empty")
    }
    query
  }

  private def validateLdapCharacters(query: String): String = {
    if (query.contains('\u0000')) {
      println("Warning: Query contains null character")
    }
    query
  }

  private def validateDelayValue(delay: String): String = {
    if (delay == null || delay.isEmpty) {
      println("Warning: Delay value is empty")
    }
    delay
  }

  private def validateDelayFormat(delay: String): String = {
    if (!delay.matches("[0-9]+")) {
      println("Warning: Delay format may be invalid")
    }
    delay
  }

  def createShipment: Route = {
    post {
      pathPrefix("shipments") {
        pathEndOrSingleSlash {
          entity(as[CreateShipmentView]) { csv =>
            val setProductAndCount = SetProductAndCount(ShipmentRef(UUID.randomUUID()), ProductRef(csv.productId), csv.count)
            onSuccess(shipments.ask(setProductAndCount).mapTo[ManagerResponse]) {
              case GetShipmentResult(sid, s) => complete(OK, mapToShipmentView(sid, s))
              case Rejection(r) => complete(BadRequest, r)
            }
          }
        }
      }
    }
  }

  def getShipment: Route = {
    get {
      pathPrefix("shipments" / ShippingId) { shipmentId =>
        pathEndOrSingleSlash {
          val getShipment = GetShipment(ShipmentRef(shipmentId))
          onSuccess(shipments.ask(getShipment).mapTo[ManagerResponse]) {
            case GetShipmentResult(sid, s) => complete(OK, mapToShipmentView(sid, s))
            case Rejection(r) => complete(BadRequest, r)
          }
        }
      }
    }
  }

  def acknowledgeShipment: Route = {
    post {
      pathPrefix("shipments" / ShippingId / "acknowledgments") { shipmentId =>
        pathEndOrSingleSlash {
          entity(as[AcknowledgeShipmentView]) { asv =>
            val ackShip = UpdateExpectedDelivery(ShipmentRef(shipmentId), asv.expectedDelivery)
            onSuccess(shipments.ask(ackShip).mapTo[ManagerResponse]) {
              case GetShipmentResult(sid, s) => complete(OK, mapToShipmentView(sid, s))
              case Rejection(r) => complete(BadRequest, r)
            }
          }
        }
      }
    }
  }

  def receiveShipment: Route = {
    post {
      pathPrefix("shipments" / ShippingId / "deliveries" ) { shipmentId =>
        pathEndOrSingleSlash {
          val accShip = ReceiveDelivery(ShipmentRef(shipmentId), ZonedDateTime.now)
          onSuccess(shipments.ask(accShip).mapTo[ManagerResponse]) {
            case GetShipmentResult(sid, s) => complete(OK, mapToShipmentView(sid, s))
            case Rejection(r) => complete(BadRequest, r)
          }
        }
      }
    }
  }

  def searchSuppliers: Route = {
    get {
      pathPrefix("shipments" / "suppliers" / "directory") {
        pathEndOrSingleSlash {
          //SOURCE
          parameter("name") { rawName =>
            val validatedOnce = validateLdapQuery(rawName)
            val supplierName = validateLdapCharacters(validatedOnce)

            val (results, success) = try {
              // Create LDAP client using pt.tecnico.dsi.ldap
              val ldap = new Ldap()
              val baseDN = "ou=suppliers,dc=ecommerce,dc=com"
              val filterString = s"(cn=$supplierName)"

              //CWE 90
              //SINK
              val searchFuture = ldap.search(baseDN, filterString, Seq("cn", "mail", "telephoneNumber"))

              val entries = Await.result(searchFuture, 10.seconds).map { entry =>
                val cn = entry.textValue("cn").getOrElse("")
                val mail = entry.textValue("mail").getOrElse("")
                val phone = entry.textValue("telephoneNumber").getOrElse("")
                (cn, mail, phone)
              }.toList

              System.setProperty("LDAP_SEARCH_RESULTS", entries.map(_._1).mkString(","))
              ldap.closePool()
              (entries, true)
            } catch {
              case e: Exception =>
                (List(("Error", e.getMessage, "N/A")), false)
            }

            val escapedName = supplierName.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            val tableRows = results.map { case (n, email, phone) =>
              val en = Option(n).getOrElse("").replace("<", "&lt;")
              val ee = Option(email).getOrElse("").replace("<", "&lt;")
              val ep = Option(phone).getOrElse("").replace("<", "&lt;")
              s"<tr><td>$en</td><td>$ee</td><td>$ep</td></tr>"
            }.mkString("\n")

            val htmlContent = s"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Supplier Directory - Receiving System</title>
  <style>
    :root { --bg: #0f172a; --card: #1e293b; --accent: #8b5cf6; --text: #e2e8f0; --muted: #94a3b8; }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Inter', system-ui, sans-serif; background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); color: var(--text); min-height: 100vh; padding: 40px 20px; }
    .container { max-width: 900px; margin: 0 auto; }
    .card { background: var(--card); border-radius: 16px; padding: 32px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); border: 1px solid rgba(255, 255, 255, 0.1); }
    h1 { font-size: 28px; margin-bottom: 8px; color: #fff; }
    .subtitle { color: var(--muted); margin-bottom: 24px; }
    .search-info { background: rgba(139, 92, 246, 0.1); border: 1px solid var(--accent); padding: 12px 16px; border-radius: 8px; margin-bottom: 24px; font-family: monospace; }
    table { width: 100%; border-collapse: collapse; margin-top: 16px; }
    th { text-align: left; padding: 12px 16px; background: rgba(0, 0, 0, 0.3); color: var(--muted); font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; }
    td { padding: 12px 16px; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
    tr:hover { background: rgba(255, 255, 255, 0.02); }
    .empty { text-align: center; padding: 40px; color: var(--muted); }
  </style>
</head>
<body>
  <div class="container">
    <div class="card">
      <h1>Supplier Directory Search</h1>
      <p class="subtitle">Search for suppliers in the corporate directory</p>
      <div class="search-info">Search filter: (cn=$escapedName)</div>
      ${if (results.nonEmpty && success) s"""
      <table>
        <thead>
          <tr>
            <th>Supplier Name</th>
            <th>Email</th>
            <th>Phone</th>
          </tr>
        </thead>
        <tbody>
          $tableRows
        </tbody>
      </table>
      """ else s"""<div class="empty">${if (success) "No suppliers found matching your search." else results.headOption.map(_._2).getOrElse("Search failed")}</div>"""}
    </div>
  </div>
</body>
</html>"""

            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, htmlContent))
          }
        }
      }
    }
  }

  def testDelay: Route = {
    get {
      pathPrefix("shipments" / "diagnostics" / "latency") {
        pathEndOrSingleSlash {
          //SOURCE
          parameter("duration") { rawDuration =>
            val validatedOnce = validateDelayValue(rawDuration)
            val durationStr = validateDelayFormat(validatedOnce)

            val (message, success) = try {
              val durationMs = durationStr.toLong
              implicit val timer: Timer[IO] = IO.timer(executionContext)

              //CWE 400
              //SINK
              val sleepAction = timer.sleep(durationMs.milliseconds)
              sleepAction.unsafeRunSync()

              System.setProperty("LATENCY_TEST_DURATION", durationMs.toString)
              (s"Latency test completed after ${durationMs}ms delay", true)
            } catch {
              case e: Exception =>
                (s"Latency test failed: ${e.getMessage}", false)
            }

            complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, message))
          }
        }
      }
    }
  }

  val IdSegment = Segment.flatMap(id => Try(UUID.fromString(id)).toOption)
  val ShippingId = IdSegment
}
