package com.ecommerce.inventory

import akka.actor.ActorSystem
import com.ecommerce.inventory.api.InventoryServiceSupport
import com.ecommerce.inventory.backend.InventoryItems

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Boot extends App with InventoryServiceSupport {
  implicit val system = ActorSystem("inventory")

  val inventoryItems = system.actorOf(InventoryItems.props)

  start(inventoryItems)

  Await.result(system.whenTerminated, Duration.Inf)
}
