package worker

import core.{Heartbeat, Result, Task}

trait Worker[P, D, T <: Task[P], R <: Result[P, D]] {
    def id: String
    def execute(task: T): R
    def heartbeat(): Heartbeat
}