// File: crawler-core/src/main/scala/com/crawler/core/metrics/MetricsReporter.scala
package com.crawler.core.metrics

import org.slf4j.LoggerFactory
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

class MetricsReporter(
                       metrics: CrawlerMetrics,
                       intervalSeconds: Int = 10
                     ) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

  def start(): Unit = {
    scheduler.scheduleAtFixedRate(
      () => reportMetrics(),
      intervalSeconds,
      intervalSeconds,
      TimeUnit.SECONDS
    )
    logger.info(s"Metrics reporter started (interval: ${intervalSeconds}s)")
  }

  private def reportMetrics(): Unit = {
    val snapshot = metrics.getSnapshot
    logger.info(
      f"[METRICS] Crawled: ${snapshot.urlsCrawled}%,d | " +
        f"Success: ${snapshot.successRatePercent}%.1f%% | " +
        f"Rate: ${snapshot.crawlRatePerSecond}%.1f/s | " +
        f"Avg: ${snapshot.avgFetchTimeMs}%.0fms | " +
        f"MB: ${snapshot.bytesDownloaded / 1024.0 / 1024.0}%.1f"
    )
  }

  def stop(): Unit = {
    scheduler.shutdown()
    scheduler.awaitTermination(5, TimeUnit.SECONDS)
  }
}