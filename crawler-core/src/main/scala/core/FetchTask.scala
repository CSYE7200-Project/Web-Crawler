package core

case class FetchTask(taskId: String, url: String) extends Task[String] {
    override def id: String = taskId
    override def payload: String = url
    override def messageType: String = "FetchTask"
    override def toMap: Map[String, Serializable] =
        Map("type" -> messageType, "taskId" -> taskId, "url" -> url)
}
