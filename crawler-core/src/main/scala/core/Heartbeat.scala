package core

import java.time.Instant

case class Heartbeat(id: String, timestamp: Instant = Instant.now) extends SerializableMessage {
    override def messageType = "Heartbeat"
    override def toMap: Map[String, Serializable] = Map("id" -> id, "timestamp" -> timestamp.toString)
}