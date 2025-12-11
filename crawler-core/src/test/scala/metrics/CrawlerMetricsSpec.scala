package metrics

import com.crawler.core.metrics.CrawlerMetrics
import org.scalatest.funsuite.AnyFunSuite

class CrawlerMetricsSpec extends AnyFunSuite {

    test("recordSuccess should increment counters") {
        val m = new CrawlerMetrics
        m.recordSuccess("http://a.com", 100, 5000, 3)

        val snap = m.getSnapshot
        assert(snap.urlsCrawled == 1)
        assert(snap.urlsSucceeded == 1)
        assert(snap.bytesDownloaded == 5000)
        assert(snap.linksExtracted == 3)
    }

    test("recordFailure should increment failure counters") {
        val m = new CrawlerMetrics
        m.recordFailure("http://b.com", "timeout", Some(500))

        val snap = m.getSnapshot
        assert(snap.urlsFailed == 1)
        assert(snap.statusCodes.getOrElse(500, 0L) == 1L)
        assert(snap.errors.getOrElse("timeout", 0L) == 1L)
    }

    test("getAvgFetchTimeMs computation") {
        val m = new CrawlerMetrics
        m.recordSuccess("u", 100, 0, 0)
        m.recordSuccess("u", 300, 0, 0)
        assert(m.getAvgFetchTimeMs == 200.0)
    }
}
