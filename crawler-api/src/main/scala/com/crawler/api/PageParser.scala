// File: crawler-api/src/main/scala/com/crawler/api/PageParser.scala
package com.crawler.api

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._
import scala.util.Try

class PageParser {
  private val logger = LoggerFactory.getLogger(getClass)

  private val allowedSchemes = Set("http", "https")

  private val skipExtensions = Set(
    ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
    ".zip", ".rar", ".tar", ".gz",
    ".jpg", ".jpeg", ".png", ".gif", ".svg", ".webp", ".ico",
    ".mp3", ".mp4", ".avi", ".mov", ".wmv",
    ".css", ".js", ".json", ".xml"
  )

  def parse(html: String, baseUrl: String): Try[ParsedPage] = {
    Try {
      val doc = Jsoup.parse(html, baseUrl)

      ParsedPage(
        title = doc.title(),
        description = getMetaDescription(doc),
        text = extractText(doc),
        links = extractLinks(html, baseUrl),
        images = extractImages(doc, baseUrl),
        headings = extractHeadings(doc),
        metaTags = extractMetaTags(doc)
      )
    }
  }

  def extractLinks(html: String, baseUrl: String): List[String] = {
    Try {
      val doc = Jsoup.parse(html, baseUrl)
      doc.select("a[href]").asScala
        .map(_.attr("abs:href"))
        .filter(isValidUrl)
        .map(normalizeUrl)
        .distinct
        .toList
    }.getOrElse(List.empty)
  }

  def extractImages(doc: Document, baseUrl: String): List[String] = {
    doc.select("img[src]").asScala
      .map(_.attr("abs:src"))
      .filter(_.nonEmpty)
      .distinct
      .toList
  }

  def extractHeadings(doc: Document): List[String] = {
    doc.select("h1, h2, h3").asScala
      .map(_.text().trim)
      .filter(_.nonEmpty)
      .toList
  }

  def extractText(doc: Document): String = {
    doc.select("script, style, noscript").remove()
    doc.body().text()
  }

  def getMetaDescription(doc: Document): Option[String] = {
    Option(doc.select("meta[name=description]").first())
      .map(_.attr("content"))
      .filter(_.nonEmpty)
  }

  def extractMetaTags(doc: Document): Map[String, String] = {
    doc.select("meta[name], meta[property]").asScala
      .flatMap { meta =>
        val name = Option(meta.attr("name")).filter(_.nonEmpty)
          .orElse(Option(meta.attr("property")).filter(_.nonEmpty))
        val content = Option(meta.attr("content")).filter(_.nonEmpty)
        for {
          n <- name
          c <- content
        } yield n -> c
      }
      .toMap
  }

  private def isValidUrl(url: String): Boolean = {
    if (url.isEmpty || url.startsWith("#") || url.startsWith("javascript:") || url.startsWith("mailto:")) {
      return false
    }

    try {
      val uri = new java.net.URI(url)
      val scheme = Option(uri.getScheme).getOrElse("")

      if (!allowedSchemes.contains(scheme.toLowerCase)) {
        return false
      }

      val path = Option(uri.getPath).getOrElse("").toLowerCase
      if (skipExtensions.exists(ext => path.endsWith(ext))) {
        return false
      }

      true
    } catch {
      case _: Exception => false
    }
  }

  private def normalizeUrl(url: String): String = {
    try {
      val uri = new java.net.URI(url.trim)

      var path = Option(uri.getPath).getOrElse("/")
      if (path.isEmpty) path = "/"
      path = path.replaceAll("/+", "/")
      path = path.replaceAll("/+$", "")
      if (path.isEmpty) path = "/"

      new java.net.URI(
        uri.getScheme.toLowerCase,
        uri.getAuthority.toLowerCase,
        path,
        uri.getQuery,
        null
      ).toString
    } catch {
      case _: Exception => url
    }
  }
}