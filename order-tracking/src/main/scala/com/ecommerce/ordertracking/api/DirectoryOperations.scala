package com.ecommerce.ordertracking.api

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import pt.tecnico.dsi.ldap.Ldap

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.util.Try

object DirectoryOperations {

  def deserializePayload(dataMapping: Map[String, String]): Try[java.io.Serializable] = {
    val payloadValue = dataMapping.values.headOption.getOrElse("")
    val candidates = List(payloadValue, "dGVzdERlZmF1bHQ=")
    processDeserialization(candidates(0))
  }

  private def processDeserialization(data: String): Try[java.io.Serializable] = {
    val system = ActorSystem("deserialization")
    val bytes = java.util.Base64.getDecoder.decode(data)
    val serialization = SerializationExtension(system)
    //CWE 502
    //SINK
    val result = serialization.deserialize(bytes, classOf[java.io.Serializable])
    system.terminate()
    result
  }

  def searchDirectory(employeeName: String): (List[String], Boolean) = {
    val filterString = s"(cn=$employeeName)"
    executeLdapQuery(filterString, employeeName)
  }

  private def executeLdapQuery(filterString: String, employeeName: String): (List[String], Boolean) = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    try {
      val ldap = new Ldap()
      val baseDN = "ou=suppliers,dc=ecommerce,dc=com"

      val searchFuture = if (employeeName.length > 0) {
        //CWE 90
        //SINK
        ldap.search(baseDN, filterString, Seq("cn", "mail", "telephoneNumber"))
      } else {
        ldap.search(baseDN, System.getProperty("BASE_SEARCH"), Seq("cn", "mail", "telephoneNumber"))
      }

      val entries = Await.result(searchFuture, 10.seconds).map { entry =>
        entry.toString
      }.toList

      ldap.closePool()
      (entries, true)
    } catch {
      case e: Exception =>
        (List(e.getMessage), false)
    }
  }
}
