package actor

import com.crawler.core.protocol.{FetchResult, RegisterWorker, WorkerHeartbeat}
import com.crawler.master.actor.{HeartbeatReceived, ResultReceived, WorkerRegistration}
import org.scalatest.funsuite.AnyFunSuite

class MasterCommandSpec extends AnyFunSuite {

    test("WorkerRegistration wraps RegisterWorker") {
        val c = WorkerRegistration(RegisterWorker("w", "h", 1, 1))
        assert(c.msg.workerId == "w")
    }

    test("HeartbeatReceived wraps WorkerHeartbeat") {
        val c = HeartbeatReceived(WorkerHeartbeat("w",0,0,0,0,0,0.0,0))
        assert(c.msg.workerId == "w")
    }

    test("ResultReceived wraps FetchResult") {
        val c = ResultReceived(FetchResult("t","w","u",true,200,10,10))
        assert(c.msg.workerId == "w")
    }
}