package HelloActor

import org.apache.pekko.actor.typed.{ActorSystem, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors


object HelloActor {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
        context.log.info(s"Received message: $message")
        Behaviors.same
    }
}

object Main {
    def main(args: Array[String]): Unit = {
        val system = ActorSystem(HelloActor(), "CrawlerSystem")
        system ! "Hello Pekko!"
        Thread.sleep(1000)
        system.terminate()
    }
}