package master

import core._
import core.protocol.JsonProtocol
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka.scaladsl.Consumer
import org.apache.pekko.kafka.{ConsumerSettings, Subscriptions}
import org.apache.kafka.common.serialization.ByteArrayDeserializer

object MasterConsumer {
    def main(args: Array[String]): Unit = {
        implicit val system = ActorSystem("MasterConsumerSystem")

        val consumerSettings =
            ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
                .withBootstrapServers("localhost:9092")
                .withGroupId("master-group")

        Consumer
            .plainSource(consumerSettings, Subscriptions.topics("crawl-results"))
            .map { msg =>
                val res = JsonProtocol.decode(msg.value()).asInstanceOf[FetchResult]
                println(s"[Master] received result: ${res.taskId}, status=${res.success}")
            }
            .runForeach(_ => ())
    }
}