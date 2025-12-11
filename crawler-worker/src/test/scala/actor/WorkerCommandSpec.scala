package actor

import com.crawler.core.protocol.FetchTask
import com.crawler.worker.actor.{FetchComplete, ProcessTask, SendHeartbeat}
import com.crawler.worker.fetcher.FetchResponse
import org.scalatest.funsuite.AnyFunSuite

class WorkerCommandSpec extends AnyFunSuite {

    test("ProcessTask stores FetchTask") {
        val task = FetchTask("id","https://a.com",0)
        val cmd = ProcessTask(task)
        assert(cmd.task == task)
    }

    test("FetchComplete stores task and response") {
        val t = FetchTask("id","https://b.com",0)
        val r = FetchResponse("https://b.com",200,None,10,Some("ok"),5)
        val cmd = FetchComplete(t,r)

        assert(cmd.task == t)
        assert(cmd.response == r)
    }

    test("SendHeartbeat is a singleton") {
        assert(SendHeartbeat eq SendHeartbeat)
    }
}
