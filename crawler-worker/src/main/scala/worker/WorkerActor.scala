package worker


import core.{FetchTask, FetchResult}
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object WorkerActor {

    def apply(): Behavior[FetchTask] =
        Behaviors.receive { (ctx, task) =>

            ctx.log.info(s"[Worker] Received task: ${task.url}")

            val html = "<html>OK</html>"

            val result = FetchResult(
                taskId = task.taskId,
                url = task.url,
                success = true,
                html = Some(html)
            )

            implicit val classicSystem = ctx.system.classicSystem
            WorkerKafkaProducer.sendResult(result)

            Behaviors.same
        }
}