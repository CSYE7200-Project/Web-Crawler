package com.crawler.worker.fetcher

case class FetchResponse(
                          url: String,
                          statusCode: Int,
                          contentType: Option[String],
                          contentLength: Long,
                          body: Option[String],
                          fetchTimeMs: Long,
                          errorMessage: Option[String] = None
                        ) {
  def isSuccess: Boolean = statusCode >= 200 && statusCode < 300
}