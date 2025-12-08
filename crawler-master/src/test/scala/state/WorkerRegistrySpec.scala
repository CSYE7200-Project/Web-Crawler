package state

import com.crawler.master.state.WorkerRegistry
import org.scalatest.funsuite.AnyFunSuite


class WorkerRegistrySpec extends AnyFunSuite {

    test("register should create worker entry") {
        val reg = new WorkerRegistry()
        reg.register("w1", "localhost", 9000, 4)

        val alive = reg.getAliveWorkers
        assert(alive.exists(_.workerId == "w1"))
    }

    test("assignTask should add task to assignedTasks") {
        val reg = new WorkerRegistry()
        reg.register("w", "h", 1, 1)

        reg.assignTask("w", "task-1")
        assert(reg.getAliveWorkers.head.assignedTasks.contains("task-1"))
    }

    test("completeTask should remove assigned task") {
        val reg = new WorkerRegistry()
        reg.register("w", "h", 1, 1)
        reg.assignTask("w", "t1")

        reg.completeTask("w", "t1")
        assert(!reg.getAliveWorkers.head.assignedTasks.contains("t1"))
    }

    test("getAvailableWorker should return worker if below concurrency") {
        val reg = new WorkerRegistry()
        reg.register("w", "h", 1, maxConcurrency = 2)
        reg.assignTask("w", "t1")

        val available = reg.getAvailableWorker
        assert(available.isDefined) // still one slot left
    }

    test("getAvailableWorker should return None when busy") {
        val reg = new WorkerRegistry()
        reg.register("w", "h", 1, maxConcurrency = 1)
        reg.assignTask("w", "t1")

        assert(reg.getAvailableWorker.isEmpty)
    }

    test("getDeadWorkerTasks returns tasks assigned to dead workers") {
        val reg = new WorkerRegistry()
        reg.register("w", "h", 1, 1)
        reg.assignTask("w", "t1")

        // Make worker look dead
        Thread.sleep(50)
        reg.checkDeadWorkers()

        assert(reg.getDeadWorkerTasks("w") == Set("t1"))
    }
}
