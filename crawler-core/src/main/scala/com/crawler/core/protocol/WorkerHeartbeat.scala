package com.crawler.core.protocol

case class WorkerHeartbeat(
                            workerId: String,
                            urlsCrawled: Long,
                            urlsSucceeded: Long,
                            urlsFailed: Long,
                            avgFetchTimeMs: Double,
                            currentQueueSize: Int,
                            cpuUsage: Double,
                            memoryUsageMb: Long,
                            timestamp: Long = System.currentTimeMillis()
                          ) extends CrawlerMessage {
  override def messageType: String = "WorkerHeartbeat"
}