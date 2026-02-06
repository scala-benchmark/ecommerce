package com.ecommerce.payment

import akka.actor.ActorSystem
import com.ecommerce.payment.api.PaymentServiceSupport

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by lukewyman on 2/3/17.
  */
object Boot extends App with PaymentServiceSupport {
  implicit val system = ActorSystem("payment")

  start
  
  // Keep the application running
  Await.result(system.whenTerminated, Duration.Inf)
}
