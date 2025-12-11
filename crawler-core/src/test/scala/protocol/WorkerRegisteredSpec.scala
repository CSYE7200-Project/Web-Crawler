package protocol

import com.crawler.core.protocol.WorkerRegistered
import org.scalatest.funsuite.AnyFunSuite

class WorkerRegisteredSpec extends AnyFunSuite {

    test("WorkerRegistered should expose messageType") {
        assert(WorkerRegistered("w1", true).messageType == "WorkerRegistered")
    }

    test("WorkerRegistered should store fields") {
        val w = WorkerRegistered("W", acknowledged = false, List(1,2))
        assert(!w.acknowledged)
        assert(w.assignedPartitions == List(1,2))
    }
}
