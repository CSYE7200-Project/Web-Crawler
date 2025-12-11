package frontier

import com.crawler.core.frontier.UrlFrontier
import org.scalatest.funsuite.AnyFunSuite

class UrlFrontierSpec extends AnyFunSuite {

    test("addUrl should add valid URL and dedupe duplicates") {
        val f = new UrlFrontier()
        assert(f.addUrl("http://a.com"))
        assert(!f.addUrl("http://a.com"))  // duplicate
        assert(f.getUrlsDeduped == 1)
    }

    test("maxDepth enforcement") {
        val f = new UrlFrontier(maxDepth = 1)
        assert(f.addUrl("http://a.com", depth = 0))
        assert(!f.addUrl("http://a.com/b", depth = 2))
    }

    test("normalizeUrl should lowercase + remove trailing slash") {
        val f = new UrlFrontier()
        assert(f.addUrl("HTTP://A.COM/"))
        assert(f.wasSeen("http://a.com"))
    }

    test("getNext returns URLs in priority order") {
        val f = new UrlFrontier()

        f.addUrl("http://a.com", priority = 1)
        f.addUrl("http://b.com", priority = 10)

        val next = f.getNext
        assert(next.exists(_.url.contains("b.com")))
    }

    test("getBatch returns up to requested size") {
        val f = new UrlFrontier()
        f.addUrl("http://a.com")
        f.addUrl("http://b.com")

        val b = f.getBatch(1)
        assert(b.size == 1)
    }
}