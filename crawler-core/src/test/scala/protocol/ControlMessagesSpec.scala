package protocol

import com.crawler.core.protocol.{PauseWorker, ResumeWorker, ShutdownWorker, UrlBatch}
import org.scalatest.funsuite.AnyFunSuite

class ControlMessagesSpec extends AnyFunSuite {

    test("ShutdownWorker messageType") {
        assert(ShutdownWorker("w", true).messageType == "ShutdownWorker")
    }

    test("PauseWorker messageType") {
        assert(PauseWorker("w").messageType == "PauseWorker")
    }

    test("ResumeWorker messageType") {
        assert(ResumeWorker("w").messageType == "ResumeWorker")
    }

    test("UrlBatch messageType") {
        assert(UrlBatch("id", List("a","b"), 1, 3).messageType == "UrlBatch")
    }
}