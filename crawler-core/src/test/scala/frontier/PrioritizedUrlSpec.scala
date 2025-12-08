package frontier

import com.crawler.core.frontier.PrioritizedUrl
import org.scalatest.funsuite.AnyFunSuite

class PrioritizedUrlSpec extends AnyFunSuite {

    test("Higher priority comes first") {
        val a = PrioritizedUrl("a", 0, priority = 10, timestamp = 1)
        val b = PrioritizedUrl("b", 0, priority = 5, timestamp = 1)
        assert(a.compareTo(b) < 0)
    }

    test("Tie priority → earlier timestamp first") {
        val a = PrioritizedUrl("a", 0, 10, timestamp = 1)
        val b = PrioritizedUrl("b", 0, 10, timestamp = 5)
        assert(a.compareTo(b) < 0)
    }
}
