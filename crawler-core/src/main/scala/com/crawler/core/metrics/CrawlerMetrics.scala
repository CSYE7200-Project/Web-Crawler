package com.crawler.core.metrics

import java.util.concurrent.atomic.{AtomicLong, LongAdder}
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

class CrawlerMetrics {
  // Counters
  private val urlsCrawled = new LongAdder()
  private val urlsSucceeded = new LongAdder()
  private val urlsFailed = new LongAdder()
  private val bytesDownloaded = new LongAdder()
  private val linksExtracted = new LongAdder()

  // Timing
  private val totalFetchTimeMs = new LongAdder()
  private val fetchCount = new LongAdder()

  // Per-domain stats
  private val domainCounts = new ConcurrentHashMap[String, LongAdder]()

  // Status code distribution
  private val statusCodeCounts = new ConcurrentHashMap[Int, LongAdder]()

  // Error tracking
  private val errorCounts = new ConcurrentHashMap[String, LongAdder]()

  // Timestamps
  private val startTime = System.currentTimeMillis()
  private val lastActivityTime = new AtomicLong(System.currentTimeMillis())

  def recordSuccess(url: String, fetchTimeMs: Long, contentLength: Long, linksFound: Int): Unit = {
    urlsCrawled.increment()
    urlsSucceeded.increment()
    bytesDownloaded.add(contentLength)
    linksExtracted.add(linksFound)
    totalFetchTimeMs.add(fetchTimeMs)
    fetchCount.increment()
    lastActivityTime.set(System.currentTimeMillis())

    val domain = extractDomain(url)
    domainCounts.computeIfAbsent(domain, _ => new LongAdder()).increment()
  }

  def recordFailure(url: String, errorType: String, statusCode: Option[Int] = None): Unit = {
    urlsCrawled.increment()
    urlsFailed.increment()
    lastActivityTime.set(System.currentTimeMillis())

    errorCounts.computeIfAbsent(errorType, _ => new LongAdder()).increment()
    statusCode.foreach { code =>
      statusCodeCounts.computeIfAbsent(code, _ => new LongAdder()).increment()
    }

    val domain = extractDomain(url)
    domainCounts.computeIfAbsent(domain, _ => new LongAdder()).increment()
  }

  def recordStatusCode(code: Int): Unit = {
    statusCodeCounts.computeIfAbsent(code, _ => new LongAdder()).increment()
  }

  def getUrlsCrawled: Long = urlsCrawled.sum()
  def getUrlsSucceeded: Long = urlsSucceeded.sum()
  def getUrlsFailed: Long = urlsFailed.sum()
  def getBytesDownloaded: Long = bytesDownloaded.sum()
  def getLinksExtracted: Long = linksExtracted.sum()

  def getSuccessRate: Double = {
    val total = urlsCrawled.sum()
    if (total == 0) 0.0 else urlsSucceeded.sum().toDouble / total * 100
  }

  def getAvgFetchTimeMs: Double = {
    val count = fetchCount.sum()
    if (count == 0) 0.0 else totalFetchTimeMs.sum().toDouble / count
  }

  def getCrawlRate: Double = {
    val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
    if (elapsedSeconds == 0) 0.0 else urlsCrawled.sum() / elapsedSeconds
  }

  def getElapsedTimeSeconds: Long = (System.currentTimeMillis() - startTime) / 1000

  def getDomainStats: Map[String, Long] = {
    domainCounts.asScala.map { case (k, v) => k -> v.sum() }.toMap
  }

  def getStatusCodeStats: Map[Int, Long] = {
    statusCodeCounts.asScala.map { case (k, v) => k -> v.sum() }.toMap
  }

  def getErrorStats: Map[String, Long] = {
    errorCounts.asScala.map { case (k, v) => k -> v.sum() }.toMap
  }

  def getSnapshot: MetricsSnapshot = MetricsSnapshot(
    urlsCrawled = urlsCrawled.sum(),
    urlsSucceeded = urlsSucceeded.sum(),
    urlsFailed = urlsFailed.sum(),
    bytesDownloaded = bytesDownloaded.sum(),
    linksExtracted = linksExtracted.sum(),
    avgFetchTimeMs = getAvgFetchTimeMs,
    crawlRatePerSecond = getCrawlRate,
    successRatePercent = getSuccessRate,
    elapsedTimeSeconds = getElapsedTimeSeconds,
    topDomains = getDomainStats.toSeq.sortBy(-_._2).take(10).toMap,
    statusCodes = getStatusCodeStats,
    errors = getErrorStats
  )

  def printSummary(): Unit = {
    val snapshot = getSnapshot
    println("\n" + "=" * 60)
    println("           CRAWLER METRICS SUMMARY")
    println("=" * 60)
    println(f"URLs Crawled:      ${snapshot.urlsCrawled}%,d")
    println(f"  - Succeeded:     ${snapshot.urlsSucceeded}%,d")
    println(f"  - Failed:        ${snapshot.urlsFailed}%,d")
    println(f"Success Rate:      ${snapshot.successRatePercent}%.1f%%")
    println(f"Bytes Downloaded:  ${snapshot.bytesDownloaded / 1024.0 / 1024.0}%.2f MB")
    println(f"Links Extracted:   ${snapshot.linksExtracted}%,d")
    println(f"Avg Fetch Time:    ${snapshot.avgFetchTimeMs}%.0f ms")
    println(f"Crawl Rate:        ${snapshot.crawlRatePerSecond}%.2f URLs/sec")
    println(f"Elapsed Time:      ${snapshot.elapsedTimeSeconds}%d seconds")

    if (snapshot.topDomains.nonEmpty) {
      println("\nTop Domains:")
      snapshot.topDomains.foreach { case (domain, count) =>
        println(f"  $domain%-30s $count%,d")
      }
    }

    if (snapshot.statusCodes.nonEmpty) {
      println("\nStatus Codes:")
      snapshot.statusCodes.toSeq.sortBy(_._1).foreach { case (code, count) =>
        println(f"  $code%d: $count%,d")
      }
    }

    if (snapshot.errors.nonEmpty) {
      println("\nErrors:")
      snapshot.errors.foreach { case (error, count) =>
        println(f"  $error%-25s $count%,d")
      }
    }
    println("=" * 60 + "\n")
  }

  private def extractDomain(url: String): String = {
    try {
      val uri = new java.net.URI(url)
      Option(uri.getHost).getOrElse("unknown")
    } catch {
      case _: Exception => "invalid"
    }
  }
}