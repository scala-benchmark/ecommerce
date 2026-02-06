package com.ecommerce.inventory.backend

import akka.serialization.Serializer
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Encoder, Decoder}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.ecommerce.inventory.backend.InventoryItemManager.Event

// Custom ZonedDateTime codecs for circe (replaces io.circe.java8.time)
object ZonedDateTimeCodecs {
  implicit val encodeZonedDateTime: Encoder[ZonedDateTime] = Encoder.encodeString.contramap[ZonedDateTime](_.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
  implicit val decodeZonedDateTime: Decoder[ZonedDateTime] = Decoder.decodeString.map(ZonedDateTime.parse(_, DateTimeFormatter.ISO_ZONED_DATE_TIME))
}

/**
  * Created by lukewyman on 1/5/17.
  */
class ItemEventSerializer extends Serializer {
  import ZonedDateTimeCodecs._
  
  def identifier: Int = 1212121

  def includeManifest: Boolean = false

  def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = decode[Event](new String(bytes))

  def toBinary(obj: AnyRef): Array[Byte] = obj match {
    case event: Event => obj.asInstanceOf[Event].asJson.noSpaces.getBytes
    case other => throw new Exception(s"Cannot serialize ${other} of type ${other.getClass}")
  }
}
