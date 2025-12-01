package com.crawler.core.metrics

case class MetricsSnapshot(
                            urlsCrawled: Long,
                            urlsSucceeded: Long,
                            urlsFailed: Long,
                            bytesDownloaded: Long,
                            linksExtracted: Long,
                            avgFetchTimeMs: Double,
                            crawlRatePerSecond: Double,
                            successRatePercent: Double,
                            elapsedTimeSeconds: Long,
                            topDomains: Map[String, Long],
                            statusCodes: Map[Int, Long],
                            errors: Map[String, Long]
                          )