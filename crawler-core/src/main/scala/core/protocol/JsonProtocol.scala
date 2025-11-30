package core.protocol

import core.{FetchResult, FetchTask, SerializableMessage}
import org.json4s.{DefaultFormats, Extraction}
import org.json4s.native.JsonMethods.{compact, parse, render}

object JsonProtocol extends Protocol[SerializableMessage] {
    implicit val formats: DefaultFormats.type = DefaultFormats

    override def encode(msg: SerializableMessage): Array[Byte] = {
        val json = Extraction.decompose(msg.toMap)
        compact(render(json)).getBytes("UTF-8")
    }

    override def decode(bytes: Array[Byte]): SerializableMessage = {
        val str = new String(bytes, "UTF-8")
        val json = parse(str)
        val objType = (json \ "type").extract[String]
        objType match {
            case "FetchTask" => json.extract[FetchTask]
            case "FetchResult" => json.extract[FetchResult]
            case _ => GenericEntity(json.extract[Map[String, Serializable]])
        }
    }
}

case class GenericEntity(fields: Map[String, Serializable]) extends SerializableMessage {
    override def messageType: String = fields.getOrElse("type", "Unknown").toString
    override def toMap: Map[String, Serializable] = fields
}