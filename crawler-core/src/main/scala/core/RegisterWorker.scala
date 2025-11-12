package core

case class RegisterWorker(id: String, host: String) extends SerializableMessage {
    override def messageType = "RegisterWorker"
    override def toMap: Map[String, Serializable] = Map("id" -> id, "host" -> host)
}
