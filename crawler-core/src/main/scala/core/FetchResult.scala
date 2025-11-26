package core

case class FetchResult(taskId: String, url: String, success: Boolean, html: Option[String]) extends Result[String, String] {

    override def payload: String = url
    override def data: Option[String] = html

    override def messageType: String = "FetchResult"
    override def toMap: Map[String, Serializable] =
        Map(
            "type"    -> messageType,
            "taskId"  -> taskId,
            "url"     -> url,
            "success" -> success,
            "html"    -> html
        )
}