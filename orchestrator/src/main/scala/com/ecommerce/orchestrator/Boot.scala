package com.ecommerce.orchestrator

import akka.actor.ActorSystem
import com.ecommerce.orchestrator.api.OrchestratorServiceSupport

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by lukewyman on 1/1/17.
  */
object Boot extends App with OrchestratorServiceSupport {
  implicit val system = ActorSystem("orchestrator")

  start
  
  // Keep the application running
  Await.result(system.whenTerminated, Duration.Inf)
}
