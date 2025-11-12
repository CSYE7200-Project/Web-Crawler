package core

case class FetchResult(taskId: String, url: String, success: Boolean, html: Option[String])
    extends Result[String, String] {
    override def payload = url
    override def data = html
    override def messageType = "FetchResult"
    override def toMap: Map[String, Serializable] =
        Map("taskId" -> taskId, "url" -> url, "success" -> success, "html" -> html.getOrElse(""))
}