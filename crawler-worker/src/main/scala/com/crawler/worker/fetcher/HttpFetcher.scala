package com.crawler.worker.fetcher

import sttp.client3._
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class HttpFetcher(
                   connectTimeoutMs: Int = 10000,
                   readTimeoutMs: Int = 30000,
                   maxContentLength: Long = 5 * 1024 * 1024,
                   userAgent: String = "DistributedCrawler/1.0 (Educational Project)"
                 )(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(getClass)
  private val backend = AsyncHttpClientFutureBackend()

  def fetch(url: String): Future[FetchResponse] = {
    val startTime = System.currentTimeMillis()

    val request = basicRequest
      .get(uri"$url")
      .header("User-Agent", userAgent)
      .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      .header("Accept-Language", "en-US,en;q=0.5")
      .readTimeout(readTimeoutMs.millis)
      .followRedirects(true)
      .maxRedirects(5)
      .response(asString)

    request.send(backend).map { response =>
      val fetchTime = System.currentTimeMillis() - startTime
      val contentLength = response.body.fold(_ => 0L, _.length.toLong)

      if (contentLength > maxContentLength) {
        FetchResponse(
          url = url,
          statusCode = response.code.code,
          contentType = response.header("Content-Type"),
          contentLength = contentLength,
          body = None,
          fetchTimeMs = fetchTime,
          errorMessage = Some(s"Content too large: $contentLength bytes")
        )
      } else {
        FetchResponse(
          url = url,
          statusCode = response.code.code,
          contentType = response.header("Content-Type"),
          contentLength = contentLength,
          body = response.body.toOption,
          fetchTimeMs = fetchTime
        )
      }
    }.recover { case e: Exception =>
      val fetchTime = System.currentTimeMillis() - startTime
      logger.warn(s"Fetch failed for $url: ${e.getMessage}")
      FetchResponse(
        url = url,
        statusCode = 0,
        contentType = None,
        contentLength = 0,
        body = None,
        fetchTimeMs = fetchTime,
        errorMessage = Some(e.getMessage)
      )
    }
  }

  def close(): Unit = {
    backend.close()
  }
}