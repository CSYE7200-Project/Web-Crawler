package master

import core.FetchResult
import core.protocol.JsonProtocol
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.stream.SystemMaterializer
import org.apache.pekko.kafka.{ConsumerSettings, Subscriptions}
import org.apache.pekko.kafka.scaladsl.Consumer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.adapter._

object MasterKafkaConsumer {

    def run(master: ActorRef[MasterCommand])
           (implicit system: ActorSystem): Unit = {
        println("[MasterConsumer] Starting consumer...")

        implicit val mat = SystemMaterializer(system).materializer

        val settings =
            ConsumerSettings(system.toTyped, new ByteArrayDeserializer, new ByteArrayDeserializer)
                .withBootstrapServers("localhost:9092")
                .withGroupId("master-results")
                .withProperty("auto.offset.reset", "earliest")

        Consumer
            .plainSource(settings, Subscriptions.topics("crawl-results"))
            .map { msg =>
                println("[MasterConsumer] GOT RESULT from Kafka")

                val result = JsonProtocol.decode(msg.value()).asInstanceOf[FetchResult]
                master ! WrappedResult(result)

            }
            .runWith(Sink.ignore)
    }
}