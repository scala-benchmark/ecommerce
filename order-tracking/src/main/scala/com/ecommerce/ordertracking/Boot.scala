package com.ecommerce.ordertracking

import akka.actor.ActorSystem
import com.ecommerce.ordertracking.api.OrdersServiceSupport
import com.ecommerce.ordertracking.backend.Orders

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Boot extends App with OrdersServiceSupport {
  implicit val system = ActorSystem("orders")

  val orders = system.actorOf(Orders.props, Orders.name)

  start(orders)

  Await.result(system.whenTerminated, Duration.Inf)
}
