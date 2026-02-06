package com.ecommerce.orchestrator.backend.orchestrator

import java.time.ZonedDateTime
import java.util.UUID

import akka.actor.{ActorLogging, Actor, Props}
import akka.util.Timeout
import cats.data.EitherT
import cats.implicits._
import com.ecommerce.common.clientactors.http.HttpClient.HttpClientError
import com.ecommerce.common.identity.Identity.{ProductRef, ShipmentRef}
import com.ecommerce.orchestrator.backend.Mappers
import com.ecommerce.orchestrator.backend.ResponseViews.ReceivingSummaryView
import com.ecommerce.orchestrator.backend.clientapi.{ReceivingApi, InventoryApi}

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by lukewyman on 2/6/17.
  */
object ReceivingOrchestrator {

  val props = Props(new ReceivingOrchestrator)

  case class GetShipmentSummary(shipmentId: ShipmentRef)
  case class RequestShipment(productId: ProductRef, ordered: ZonedDateTime, count: Int)
  case class AcknowledgeShipment(productId: ProductRef, shipmentId: ShipmentRef, expectedDelivery: ZonedDateTime, count: Int)
  case class AcceptShipment(productId: ProductRef, shipmentId: ShipmentRef, delivered: ZonedDateTime, count: Int)
}

class ReceivingOrchestrator extends Actor with ActorLogging
  with ReceivingApi
  with InventoryApi {
  import Mappers._
  import ReceivingOrchestrator._
  import akka.pattern.pipe

  implicit def executionContext = context.dispatcher
  implicit def timeout: Timeout = Timeout(3 seconds)

  def receive = {
    case GetShipmentSummary(sid) =>
      val result: EitherT[Future, HttpClientError, ReceivingSummaryView] = for {
        gs <- EitherT(getShipment(sid))
        gi <- EitherT(getInventoryItem(ProductRef(gs.productId)))
      } yield mapToReceivingSummaryView(gs, gi)
      result.value.pipeTo(sender())
      kill()
    case RequestShipment(pid, o, c) =>
      val result: EitherT[Future, HttpClientError, ReceivingSummaryView] = for {
        cs <- EitherT(createShipment(pid, o, c))
        gi <- EitherT(getInventoryItem(pid))
      } yield mapToReceivingSummaryView(cs, gi)
      result.value.pipeTo(sender())
      kill()
    case AcknowledgeShipment(iid, sid, ed, c) =>
      val result: EitherT[Future, HttpClientError, ReceivingSummaryView] = for {
        as <- EitherT(acknowledgeShipment(sid, ed))
        ns <- EitherT(notifySupply(iid, sid, ed, c))
      } yield mapToReceivingSummaryView(as, ns)
      result.value.pipeTo(sender())
      kill()
    case AcceptShipment(iid, sid, d, c) =>
      val result: EitherT[Future, HttpClientError, ReceivingSummaryView] = for {
        as <- EitherT(acceptShipment(sid))
        rs <- EitherT(receiveSupply(iid, sid, d, c))
      } yield mapToReceivingSummaryView(as, rs)
      result.value.pipeTo(sender())
      kill()
  }

  def kill() = {
    context.children foreach { context.stop(_) }
    context.stop(self)
  }

}
