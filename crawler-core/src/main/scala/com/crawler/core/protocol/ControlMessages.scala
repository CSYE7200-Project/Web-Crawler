// File: crawler-core/src/main/scala/com/crawler/core/protocol/ControlMessages.scala
package com.crawler.core.protocol

case class ShutdownWorker(
                           workerId: String,
                           graceful: Boolean = true
                         ) extends CrawlerMessage {
  override def messageType: String = "ShutdownWorker"
}

case class PauseWorker(workerId: String) extends CrawlerMessage {
  override def messageType: String = "PauseWorker"
}

case class ResumeWorker(workerId: String) extends CrawlerMessage {
  override def messageType: String = "ResumeWorker"
}

case class UrlBatch(
                     batchId: String,
                     urls: List[String],
                     depth: Int,
                     maxDepth: Int
                   ) extends CrawlerMessage {
  override def messageType: String = "UrlBatch"
}