package com.crawler.api

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.util.Try

class ContentExtractor {

  def extract(html: String): Try[ExtractedContent] = {
    Try {
      val doc = Jsoup.parse(html)

      doc.select("script, style, nav, header, footer, aside, .sidebar, .comments, .advertisement").remove()

      val title = doc.title()
      val mainContent = extractMainContent(doc)
      val wordCount = mainContent.split("\\s+").length

      ExtractedContent(
        title = title,
        mainContent = mainContent,
        wordCount = wordCount,
        language = extractLanguage(doc),
        author = extractAuthor(doc),
        publishDate = extractPublishDate(doc)
      )
    }
  }

  private def extractMainContent(doc: Document): String = {
    val mainSelectors = Seq(
      "article", "main", ".content", ".post-content",
      ".article-body", ".entry-content", "#content"
    )

    mainSelectors
      .map(sel => doc.select(sel).text())
      .find(_.length > 100)
      .getOrElse(doc.body().text())
  }

  private def extractLanguage(doc: Document): Option[String] = {
    Option(doc.select("html[lang]").attr("lang")).filter(_.nonEmpty)
  }

  private def extractAuthor(doc: Document): Option[String] = {
    Option(doc.select("meta[name=author]").attr("content")).filter(_.nonEmpty)
      .orElse(Option(doc.select(".author, .byline").first()).map(_.text()))
  }

  private def extractPublishDate(doc: Document): Option[String] = {
    Option(doc.select("meta[property=article:published_time]").attr("content")).filter(_.nonEmpty)
      .orElse(Option(doc.select("time[datetime]").attr("datetime")).filter(_.nonEmpty))
  }
}