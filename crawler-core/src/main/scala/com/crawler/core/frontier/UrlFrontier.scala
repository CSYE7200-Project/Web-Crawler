package com.crawler.core.frontier

import com.google.common.hash.{BloomFilter, Funnels}
import org.slf4j.LoggerFactory

import java.nio.charset.StandardCharsets
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue, PriorityBlockingQueue}
import java.util.concurrent.atomic.AtomicLong
import scala.jdk.CollectionConverters._

class UrlFrontier(
                   expectedUrls: Int = 100000,
                   falsePositiveRate: Double = 0.01,
                   maxDepth: Int = 3
                 ) {
  private val logger = LoggerFactory.getLogger(getClass)

  // Bloom filter for URL deduplication
  private val seenUrls: BloomFilter[CharSequence] = BloomFilter.create(
    Funnels.stringFunnel(StandardCharsets.UTF_8),
    expectedUrls,
    falsePositiveRate
  )

  // Priority queue for URL ordering
  private val urlQueue = new PriorityBlockingQueue[PrioritizedUrl]()

  // Per-domain queues for politeness
  private val domainQueues = new ConcurrentHashMap[String, ConcurrentLinkedQueue[PrioritizedUrl]]()

  // Domain last access time for rate limiting
  private val domainLastAccess = new ConcurrentHashMap[String, Long]()

  // Politeness delay per domain (milliseconds)
  private val defaultPolitenessDelayMs = 1000L
  private val domainDelays = new ConcurrentHashMap[String, Long]()

  // Stats
  private val urlsAdded = new AtomicLong(0)
  private val urlsDeduped = new AtomicLong(0)

  def addUrl(url: String, depth: Int = 0, priority: Int = 0): Boolean = {
    if (depth > maxDepth) {
      return false
    }

    val normalizedUrl = normalizeUrl(url)
    if (normalizedUrl.isEmpty) {
      return false
    }

    val normalized = normalizedUrl.get

    // Check bloom filter
    if (seenUrls.mightContain(normalized)) {
      urlsDeduped.incrementAndGet()
      return false
    }

    // Add to bloom filter
    seenUrls.put(normalized)
    urlsAdded.incrementAndGet()

    // Add to priority queue
    val pUrl = PrioritizedUrl(normalized, depth, priority)
    urlQueue.offer(pUrl)

    // Also add to domain-specific queue
    val domain = extractDomain(normalized)
    domainQueues.computeIfAbsent(domain, _ => new ConcurrentLinkedQueue[PrioritizedUrl]()).offer(pUrl)

    true
  }

  def addUrls(urls: Seq[String], depth: Int = 0, priority: Int = 0): Int = {
    urls.count(url => addUrl(url, depth, priority))
  }

  def getNext: Option[PrioritizedUrl] = {
    Option(urlQueue.poll())
  }

  def getNextForDomain(domain: String): Option[PrioritizedUrl] = {
    val queue = domainQueues.get(domain)
    if (queue == null) return None

    val lastAccess = domainLastAccess.getOrDefault(domain, 0L)
    val delay = domainDelays.getOrDefault(domain, defaultPolitenessDelayMs)
    val now = System.currentTimeMillis()

    if (now - lastAccess < delay) {
      None
    } else {
      Option(queue.poll()).map { pUrl =>
        domainLastAccess.put(domain, now)
        pUrl
      }
    }
  }

  def getBatch(size: Int): Seq[PrioritizedUrl] = {
    val batch = scala.collection.mutable.ArrayBuffer[PrioritizedUrl]()
    var i = 0
    while (i < size) {
      val url = urlQueue.poll()
      if (url == null) {
        return batch.toSeq
      }
      batch += url
      i += 1
    }
    batch.toSeq
  }

  def wasSeen(url: String): Boolean = {
    normalizeUrl(url).exists(seenUrls.mightContain)
  }

  def setDomainDelay(domain: String, delayMs: Long): Unit = {
    domainDelays.put(domain, delayMs)
  }

  def size: Int = urlQueue.size()
  def getUrlsAdded: Long = urlsAdded.get()
  def getUrlsDeduped: Long = urlsDeduped.get()
  def getDomainCount: Int = domainQueues.size()

  private def normalizeUrl(url: String): Option[String] = {
    try {
      val trimmed = url.trim.toLowerCase
      if (trimmed.isEmpty || !trimmed.startsWith("http")) {
        return None
      }

      val uri = new java.net.URI(trimmed)
      val normalized = new java.net.URI(
        uri.getScheme,
        uri.getAuthority,
        uri.getPath.replaceAll("/+$", ""),
        uri.getQuery,
        null
      ).toString

      Some(normalized)
    } catch {
      case _: Exception => None
    }
  }

  private def extractDomain(url: String): String = {
    try {
      new java.net.URI(url).getHost
    } catch {
      case _: Exception => "unknown"
    }
  }

  def printStats(): Unit = {
    logger.info(
      s"URL Frontier: ${size} pending, ${getUrlsAdded} added, " +
        s"${getUrlsDeduped} deduped, ${getDomainCount} domains"
    )
  }
}