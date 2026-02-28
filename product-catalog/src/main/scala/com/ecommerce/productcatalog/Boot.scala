package com.ecommerce.productcatalog

import akka.actor.ActorSystem
import com.ecommerce.productcatalog.api.ProductServiceSupport

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Boot extends App with ProductServiceSupport {
  implicit val system = ActorSystem("productcatalog")

  start()

  Await.result(system.whenTerminated, Duration.Inf)
}
