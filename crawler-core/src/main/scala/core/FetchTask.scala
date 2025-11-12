package core

case class FetchTask(taskId: String, url: String) extends SerializableMessage {
    override def messageType: String = "FetchTask"
    override def toMap: Map[String, Serializable] = Map("type" -> messageType, "taskId" -> taskId, "url" -> url)
}
