package com.crawler.core.protocol

// Base trait for all Kafka messages
trait CrawlerMessage {
  def messageType: String
}