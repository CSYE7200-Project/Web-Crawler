package master

import core.{FetchTask}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

object MasterActor {

    def apply(worker: ActorRef[FetchTask]): Behavior[MasterCommand] =
        Behaviors.receive { (ctx, msg) =>
            msg match {

                case AssignTask(task) =>
                    ctx.log.info(s"[Master] Sending task to Kafka → ${task.url}")
                    implicit val classic = ctx.system.classicSystem
                    MasterKafkaProducer.sendTask(task)

                case WrappedResult(res) =>
                    ctx.log.info(s"[Master] Received result: ${res.taskId} → success=${res.success}")
            }

            Behaviors.same
        }
}