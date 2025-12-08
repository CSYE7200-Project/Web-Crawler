package protocol

import com.crawler.core.protocol.WorkerHeartbeat
import org.scalatest.funsuite.AnyFunSuite

class WorkerHeartbeatSpec extends AnyFunSuite {

    test("WorkerHeartbeat should expose messageType") {
        val h = WorkerHeartbeat("w1", 10, 9, 1, 50, 0, 10.0, 128)
        assert(h.messageType == "WorkerHeartbeat")
    }

    test("WorkerHeartbeat should store data correctly") {
        val h = WorkerHeartbeat("w9", 1,2,3,4,5,6.0,7)
        assert(h.cpuUsage == 6.0)
        assert(h.memoryUsageMb == 7)
    }
}
