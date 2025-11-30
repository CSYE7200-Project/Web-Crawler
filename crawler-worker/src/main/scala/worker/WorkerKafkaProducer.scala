package worker

import core.FetchResult
import core.protocol.JsonProtocol
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.pekko.kafka.ProducerSettings
import org.apache.pekko.kafka.scaladsl.Producer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.SystemMaterializer
import org.apache.kafka.clients.producer.ProducerRecord

object WorkerKafkaProducer {

    def sendResult(result: FetchResult)(implicit system: ActorSystem): Unit = {
        println(s"[WorkerProducer] Sending result: ${result.taskId}")

        implicit val mat = SystemMaterializer(system).materializer

        val settings =
            ProducerSettings(system.toTyped, new ByteArraySerializer, new ByteArraySerializer)
                .withBootstrapServers("localhost:9092")

        val bytes = JsonProtocol.encode(result)

        Source
            .single(new ProducerRecord[Array[Byte], Array[Byte]]("crawl-results", bytes))
            .runWith(Producer.plainSink(settings))
    }
}