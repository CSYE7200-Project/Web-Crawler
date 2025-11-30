package master

import core.FetchTask
import core.protocol.JsonProtocol
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka.ProducerSettings
import org.apache.pekko.kafka.scaladsl.Producer
import org.apache.pekko.stream.SystemMaterializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.pekko.actor.typed.scaladsl.adapter._

object MasterKafkaProducer {

    def sendTask(task: FetchTask)(implicit system: ActorSystem): Unit = {
        println(s"[MasterProducer] Sending task to Kafka: ${task.url}")

        implicit val mat = SystemMaterializer(system).materializer

        val settings =
            ProducerSettings(system.toTyped, new ByteArraySerializer, new ByteArraySerializer)
                .withBootstrapServers("localhost:9092")

        val bytes = JsonProtocol.encode(task)

        Source
            .single(new ProducerRecord[Array[Byte], Array[Byte]]("crawl-tasks", bytes))
            .runWith(Producer.plainSink(settings))
    }
}