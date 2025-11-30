package master

import core.{FetchTask}
import org.apache.pekko.actor.typed.{ActorSystem, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object Main {

    def main(args: Array[String]): Unit = {

        val root: Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>

            val master = ctx.spawn(MasterActor(null), "master")

            implicit val classic = ctx.system.classicSystem

            MasterKafkaConsumer.run(master)

            master ! AssignTask(FetchTask("task-567", "https://example567.com"))

            Behaviors.empty
        }

        ActorSystem[Nothing](root, "MasterSystem")
    }
}