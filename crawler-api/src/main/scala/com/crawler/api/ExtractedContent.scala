// File: crawler-api/src/main/scala/com/crawler/api/ExtractedContent.scala
package com.crawler.api

case class ExtractedContent(
                             title: String,
                             mainContent: String,
                             wordCount: Int,
                             language: Option[String],
                             author: Option[String],
                             publishDate: Option[String]
                           )