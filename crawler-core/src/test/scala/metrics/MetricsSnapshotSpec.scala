package metrics

import com.crawler.core.metrics.MetricsSnapshot
import org.scalatest.funsuite.AnyFunSuite

class MetricsSnapshotSpec extends AnyFunSuite {

    test("MetricsSnapshot stores values correctly") {
        val s = MetricsSnapshot(1,2,3,4,5,6,7,8,9, Map("a"->1), Map(200->1), Map("err"->2))
        assert(s.urlsCrawled == 1)
        assert(s.statusCodes(200) == 1)
    }
}