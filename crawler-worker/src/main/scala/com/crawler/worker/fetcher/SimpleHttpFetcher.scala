// File: crawler-worker/src/main/scala/com/crawler/worker/fetcher/SimpleHttpFetcher.scala
package com.crawler.worker.fetcher

import org.slf4j.LoggerFactory

import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration
import scala.jdk.OptionConverters._
import scala.util.{Failure, Success, Try}

/**
 * Simple HTTP fetcher using Java 11+ HttpClient
 * No Netty, no async complications
 */
class SimpleHttpFetcher(
                         connectTimeoutMs: Int = 10000,
                         readTimeoutMs: Int = 30000,
                         maxContentLength: Long = 5 * 1024 * 1024,
                         userAgent: String = "DistributedCrawler/1.0 (Educational Project)"
                       ) {
  private val logger = LoggerFactory.getLogger(getClass)

  private val client = HttpClient.newBuilder()
    .connectTimeout(Duration.ofMillis(connectTimeoutMs))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()

  def fetch(url: String): FetchResponse = {
    val startTime = System.currentTimeMillis()

    Try {
      val request = HttpRequest.newBuilder()
        .uri(java.net.URI.create(url))
        .timeout(Duration.ofMillis(readTimeoutMs))
        .header("User-Agent", userAgent)
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .header("Accept-Language", "en-US,en;q=0.5")
        .GET()
        .build()

      val response = client.send(request, HttpResponse.BodyHandlers.ofString())
      val fetchTime = System.currentTimeMillis() - startTime
      val body = response.body()
      val contentLength = body.length.toLong
      val contentType = response.headers().firstValue("Content-Type").toScala

      if (contentLength > maxContentLength) {
        FetchResponse(
          url = url,
          statusCode = response.statusCode(),
          contentType = contentType,
          contentLength = contentLength,
          body = None,
          fetchTimeMs = fetchTime,
          errorMessage = Some(s"Content too large: $contentLength bytes")
        )
      } else {
        FetchResponse(
          url = url,
          statusCode = response.statusCode(),
          contentType = contentType,
          contentLength = contentLength,
          body = Some(body),
          fetchTimeMs = fetchTime
        )
      }
    } match {
      case Success(response) => response
      case Failure(e) =>
        val fetchTime = System.currentTimeMillis() - startTime
        logger.debug(s"Fetch failed for $url: ${e.getMessage}")
        FetchResponse(
          url = url,
          statusCode = 0,
          contentType = None,
          contentLength = 0,
          body = None,
          fetchTimeMs = fetchTime,
          errorMessage = Some(e.getClass.getSimpleName + ": " + e.getMessage)
        )
    }
  }

  def close(): Unit = {
    // Java HttpClient doesn't need explicit close
  }
}