package master

import core.{Heartbeat, Result, Task}

trait Master[P, D, T <: Task[P], R <: Result[P, D]] {
    def assignTask(workerId: String): Option[T]
    def collectResult(result: R): Unit
    def receiveHeartbeat(heartbeat: Heartbeat): Unit
}