package com.crawler.core.protocol

case class FetchResult(
                        taskId: String,
                        workerId: String,
                        url: String,
                        success: Boolean,
                        statusCode: Int,
                        contentLength: Long,
                        fetchTimeMs: Long,
                        htmlContent: Option[String] = None,
                        extractedLinks: List[String] = List.empty,
                        errorMessage: Option[String] = None,
                        timestamp: Long = System.currentTimeMillis()
                      ) extends CrawlerMessage {
  override def messageType: String = "FetchResult"
}