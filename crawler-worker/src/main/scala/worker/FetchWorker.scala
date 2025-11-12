package worker

import core.{FetchResult, FetchTask, Heartbeat}

class FetchWorker(val id: String) extends Worker[String, String, FetchTask, FetchResult] {
    override def execute(task: FetchTask): FetchResult =
        FetchResult(task.id, task.url, success = true, html = Some("<html></html>"))

    override def heartbeat(): Heartbeat = Heartbeat(id)
}