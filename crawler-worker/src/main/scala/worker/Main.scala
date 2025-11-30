package worker

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object Main {

    def main(args: Array[String]): Unit = {

        val root = Behaviors.setup[Nothing] { ctx =>

            val worker = ctx.spawn(WorkerActor(), "worker")

            implicit val classicSystem = ctx.system.classicSystem

            WorkerKafkaConsumer.run(worker)(classicSystem)

            Behaviors.empty
        }
        ActorSystem[Nothing](root, "WorkerSystem")
    }
}