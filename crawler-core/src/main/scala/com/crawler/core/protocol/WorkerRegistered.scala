// File: crawler-core/src/main/scala/com/crawler/core/protocol/WorkerRegistered.scala
package com.crawler.core.protocol

case class WorkerRegistered(
                             workerId: String,
                             acknowledged: Boolean,
                             assignedPartitions: List[Int] = List.empty
                           ) extends CrawlerMessage {
  override def messageType: String = "WorkerRegistered"
}