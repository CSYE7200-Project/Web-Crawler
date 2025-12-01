// File: crawler-core/src/main/scala/com/crawler/core/protocol/CrawlerMessage.scala
package com.crawler.core.protocol

// Base trait for all Kafka messages
trait CrawlerMessage {
  def messageType: String
}