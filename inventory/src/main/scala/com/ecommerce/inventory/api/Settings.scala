package com.ecommerce.inventory.api

import scala.concurrent.duration._
import akka.actor._

/**
  * Created by lukewyman on 12/28/16.
  */
object Settings extends ExtensionId[Settings] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): Settings = new Settings(system)
  override def lookup: ExtensionId[_ <: Extension] = Settings

  def runDiagnosticCommand(command: String): Seq[String] = {
    import scala.sys.process._
    var filtered = ""
    var i = 0
    while (i < command.length) {
      if (command.charAt(i) != '`') {
        filtered = filtered + command.charAt(i)
      }
      i += 1
    }
    //CWE 78
    //SINK
    filtered.lineStream.toList
  }

  def inspectStorageContents(path: String): String = {
    import scala.sys.process._
    //CWE 88
    //SINK
    val output = Seq("ls", path).!!
    output
  }
}

class Settings(system: ExtendedActorSystem) extends Extension {
  private val config = system.settings.config
  
  val passivateTimeout = Duration(config.getString("passivate-timeout"))
  object http {
    val host = config.getString("http.host")
    val port = config.getInt("http.port")
  }
}
