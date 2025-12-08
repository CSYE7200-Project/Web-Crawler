import com.crawler.api.PageParser
import org.scalatest.funsuite.AnyFunSuite

class PageParserSpec extends AnyFunSuite {

    private val parser = new PageParser()

    private val html =
        """
          |<html>
          |<head>
          |  <title>Test Page</title>
          |  <meta name="description" content="This is a test page.">
          |  <meta property="og:title" content="OpenGraph Title">
          |</head>
          |<body>
          |  <h1>Main Heading</h1>
          |  <h2>Sub Heading</h2>
          |  <p>This is sample text.</p>
          |
          |  <a href="https://example.com/page1">Link1</a>
          |  <a href="/relative/page2">Link2</a>
          |  <a href="javascript:void(0)">Invalid</a>
          |  <a href="https://example.com/file.pdf">PDF</a>
          |
          |  <img src="/img/logo.png">
          |  <img src="https://example.com/picture.jpg">
          |
          |  <script>var x = 1;</script>
          |  <style>.x{}</style>
          |  <noscript>no script text</noscript>
          |</body>
          |</html>
          |""".stripMargin

    private val baseUrl = "https://example.com"

    // ----------------------------------------------------------------------
    test("parse() should extract title, description, text, links, images, headings, metaTags") {
        val result = parser.parse(html, baseUrl)
        assert(result.isSuccess)

        val page = result.get

        assert(page.title == "Test Page")
        assert(page.description.contains("This is a test page."))
        assert(page.headings.contains("Main Heading"))
        assert(page.headings.contains("Sub Heading"))
        assert(page.images.nonEmpty)
        assert(page.links.nonEmpty)
        assert(page.metaTags("og:title") == "OpenGraph Title")
    }

    // ----------------------------------------------------------------------
    test("extractLinks should resolve absolute + relative links, remove invalid, skip extensions") {
        val links = parser.extractLinks(html, baseUrl)

        assert(links.contains("https://example.com/page1"))
        assert(links.contains("https://example.com/relative/page2"))

        // Ensure skipExtensions filtering works (.pdf should be removed)
        assert(!links.exists(_.endsWith(".pdf")))

        // Ensure invalid schemes removed
        assert(!links.exists(_.startsWith("javascript:")))
    }

    // ----------------------------------------------------------------------
    test("extractImages should extract absolute URLs") {
        val doc = org.jsoup.Jsoup.parse(html, baseUrl)
        val images = parser.extractImages(doc, baseUrl)

        assert(images.contains("https://example.com/img/logo.png"))
        assert(images.contains("https://example.com/picture.jpg"))
    }

    // ----------------------------------------------------------------------
    test("extractHeadings should collect h1, h2, h3 text only") {
        val doc = org.jsoup.Jsoup.parse(html, baseUrl)
        val headings = parser.extractHeadings(doc)

        assert(headings == List("Main Heading", "Sub Heading"))
    }

    // ----------------------------------------------------------------------
    test("extractText should remove script/style/noscript content") {
        val doc = org.jsoup.Jsoup.parse(html, baseUrl)
        val text = parser.extractText(doc)

        assert(!text.contains("var x ="))
        assert(!text.contains("no script"))
        assert(!text.contains(".x"))
        assert(text.contains("This is sample text."))
    }

    // ----------------------------------------------------------------------
    test("getMetaDescription should return description content when available") {
        val doc = org.jsoup.Jsoup.parse(html, baseUrl)
        val desc = parser.getMetaDescription(doc)

        assert(desc.contains("This is a test page."))
    }

    // ----------------------------------------------------------------------
    test("extractMetaTags should extract both name and property attributes") {
        val doc = org.jsoup.Jsoup.parse(html, baseUrl)
        val meta = parser.extractMetaTags(doc)

        assert(meta("description") == "This is a test page.")
        assert(meta("og:title") == "OpenGraph Title")
    }

    // ----------------------------------------------------------------------
    test("extractLinks should filter invalid protocols but may include baseUrl via Jsoup behavior") {
        val customHtml =
            """
              |<a href="mailto:test@example.com">Email</a>
              |<a href="#section">Anchor</a>
              |<a href="javascript:void(0)">JS</a>
              |""".stripMargin

        val links = parser.extractLinks(customHtml, baseUrl)

        // Jsoup may generate the base URL if href cannot be resolved
        // So we verify that the *invalid* types are truly removed
        assert(!links.exists(_.startsWith("mailto:")))
        assert(!links.exists(_.startsWith("javascript:")))
        assert(!links.exists(_.startsWith("#")))
    }

    // ----------------------------------------------------------------------
    test("normalizeUrl should lowercase scheme/host but preserve original path case") {
        val input =
            """
              |<a href="HTTPS://Example.Com/ABC///">Link</a>
              |""".stripMargin

        val links = parser.extractLinks(input, baseUrl)

        // normalizeUrl DOES NOT lowercase path → expect ABC, not abc
        assert(links.contains("https://example.com/ABC"))
    }
}