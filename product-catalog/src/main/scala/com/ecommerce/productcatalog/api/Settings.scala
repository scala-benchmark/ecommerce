package com.ecommerce.productcatalog.api

import scala.concurrent.duration._
import akka.actor._

object Settings extends ExtensionId[Settings] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): Settings = new Settings(system)
  override def lookup: ExtensionId[_ <: Extension] = Settings
}

class Settings(system: ExtendedActorSystem) extends Extension {
  private val config = system.settings.config

  object http {
    val host = config.getString("http.host")
    val port = config.getInt("http.port")
  }
}
