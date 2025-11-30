package worker

import core.FetchTask
import core.protocol.JsonProtocol
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.kafka.{ConsumerSettings, Subscriptions}
import org.apache.pekko.kafka.scaladsl.Consumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.apache.pekko.stream.SystemMaterializer

object WorkerKafkaConsumer {

    def run(worker: ActorRef[FetchTask])
           (implicit system: org.apache.pekko.actor.ActorSystem): Unit = {
        println("[WorkerConsumer] Starting consumer...")


        implicit val mat = SystemMaterializer(system).materializer
        val settings = ConsumerSettings(system.toTyped, new ByteArrayDeserializer, new ByteArrayDeserializer)
            .withBootstrapServers("localhost:9092")
            .withGroupId("worker-test")
            .withProperty("auto.offset.reset", "earliest")


        Consumer
            .plainSource(settings, Subscriptions.topics("crawl-tasks"))
            .map { msg =>
                println("[WorkerConsumer] GOT MESSAGE from Kafka")
                println(s"[WorkerConsumer] Raw bytes length = ${msg.value().length}")

                val task = JsonProtocol.decode(msg.value()).asInstanceOf[FetchTask]
                println(s"[WorkerConsumer] Decoded Task = $task")

                worker ! task
            }
            .runWith(Sink.ignore)
    }
}
