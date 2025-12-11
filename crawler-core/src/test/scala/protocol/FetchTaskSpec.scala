package protocol

import com.crawler.core.protocol.FetchTask
import org.scalatest.funsuite.AnyFunSuite

class FetchTaskSpec extends AnyFunSuite {

    test("FetchTask should expose correct messageType") {
        val t = FetchTask("t1", "http://a.com")
        assert(t.messageType == "FetchTask")
    }

    test("FetchTask should maintain all given fields") {
        val t = FetchTask("id123", "http://abc.com", depth = 1, maxDepth = 5, priority = 10, retryCount = 2, parentUrl = Some("p"))
        assert(t.taskId == "id123")
        assert(t.url == "http://abc.com")
        assert(t.depth == 1)
        assert(t.maxDepth == 5)
        assert(t.priority == 10)
        assert(t.retryCount == 2)
        assert(t.parentUrl.contains("p"))
    }
}
