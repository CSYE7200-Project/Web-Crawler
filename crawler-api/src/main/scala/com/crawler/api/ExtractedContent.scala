package com.crawler.api

case class ExtractedContent(
                             title: String,
                             mainContent: String,
                             wordCount: Int,
                             language: Option[String],
                             author: Option[String],
                             publishDate: Option[String]
                           )