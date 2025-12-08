package state

import com.crawler.master.state.WorkerInfo
import org.scalatest.funsuite.AnyFunSuite

class WorkerInfoSpec extends AnyFunSuite {

    test("WorkerInfo should store fields") {
        val w = WorkerInfo("id", "host", 1, 4, 10L, 20L)
        assert(w.workerId == "id")
        assert(w.host == "host")
        assert(w.maxConcurrency == 4)
        assert(w.registeredAt == 10L)
    }
}
