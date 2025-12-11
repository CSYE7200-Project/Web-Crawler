package metrics

import com.crawler.core.metrics.{CrawlerMetrics, MetricsReporter}
import org.scalatest.funsuite.AnyFunSuite

class MetricsReporterSpec extends AnyFunSuite {

    test("MetricsReporter start/stop should not throw") {
        val m = new CrawlerMetrics
        val r = new MetricsReporter(m, intervalSeconds = 1)
        r.start()
        r.stop()
    }
}