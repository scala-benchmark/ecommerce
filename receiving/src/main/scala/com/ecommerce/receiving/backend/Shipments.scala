package com.ecommerce.receiving.backend

import akka.actor.{Props, Actor}

/**
  * Created by lukewyman on 2/5/17.
  */
object Shipments {
  val props = Props(new Shipments)

  val name = "shipments"
}

class Shipments extends Actor {

  def receive: Receive = {
    case msg => 
      // Placeholder implementation for shipments actor
      sender() ! akka.actor.Status.Failure(new UnsupportedOperationException("Shipments not implemented"))
  }
}
