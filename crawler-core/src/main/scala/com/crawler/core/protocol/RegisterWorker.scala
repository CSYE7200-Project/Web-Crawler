// File: crawler-core/src/main/scala/com/crawler/core/protocol/RegisterWorker.scala
package com.crawler.core.protocol

case class RegisterWorker(
                           workerId: String,
                           host: String,
                           port: Int,
                           maxConcurrency: Int,
                           timestamp: Long = System.currentTimeMillis()
                         ) extends CrawlerMessage {
  override def messageType: String = "RegisterWorker"
}