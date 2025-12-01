package com.crawler.core.protocol

case class FetchTask(
                      taskId: String,
                      url: String,
                      depth: Int = 0,
                      maxDepth: Int = 2,
                      priority: Int = 0,
                      retryCount: Int = 0,
                      parentUrl: Option[String] = None,
                      timestamp: Long = System.currentTimeMillis()
                    ) extends CrawlerMessage {
  override def messageType: String = "FetchTask"
}