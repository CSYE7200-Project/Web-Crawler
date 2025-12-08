package protocol

import com.crawler.core.protocol.RegisterWorker
import org.scalatest.funsuite.AnyFunSuite

class RegisterWorkerSpec extends AnyFunSuite {

    test("RegisterWorker should expose messageType") {
        val r = RegisterWorker("w1", "localhost", 9000, 4)
        assert(r.messageType == "RegisterWorker")
    }

    test("RegisterWorker should store metadata") {
        val r = RegisterWorker("wX", "host", 1234, 99)
        assert(r.workerId == "wX")
        assert(r.host == "host")
        assert(r.port == 1234)
        assert(r.maxConcurrency == 99)
    }
}
