package com.ecommerce.orchestrator.api

import scala.concurrent.duration._
import akka.actor._
import com.typesafe.config.Config

/**
  * Created by lukewyman on 2/1/17.
  */

object Settings extends ExtensionId[Settings] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): Settings = new Settings(system)
  override def lookup: ExtensionId[_ <: Extension] = Settings
}

class Settings(system: ExtendedActorSystem) extends Extension {
  private val config = system.settings.config
  
  val passivateTimeout = Duration(config.getString("passivate-timeout"))
  object http {
    val host = config.getString("http.host")
    val port = config.getInt("http.port")
  }
}