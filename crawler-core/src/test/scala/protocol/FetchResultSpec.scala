package protocol

import com.crawler.core.protocol.FetchResult
import org.scalatest.funsuite.AnyFunSuite

class FetchResultSpec extends AnyFunSuite {

    test("FetchResult should expose messageType") {
        val r = FetchResult("id", "w1", "http://a.com", success = true, 200, 100, 50)
        assert(r.messageType == "FetchResult")
    }

    test("FetchResult should store fields correctly") {
        val r = FetchResult("t1", "worker1", "url", success = false, 500, 0, 100, None, List("a"), Some("err"))
        assert(!r.success)
        assert(r.statusCode == 500)
        assert(r.extractedLinks == List("a"))
        assert(r.errorMessage.contains("err"))
    }
}