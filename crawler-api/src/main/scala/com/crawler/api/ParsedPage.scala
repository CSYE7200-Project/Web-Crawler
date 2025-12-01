// File: crawler-api/src/main/scala/com/crawler/api/ParsedPage.scala
package com.crawler.api

case class ParsedPage(
                       title: String,
                       description: Option[String],
                       text: String,
                       links: List[String],
                       images: List[String],
                       headings: List[String],
                       metaTags: Map[String, String]
                     )