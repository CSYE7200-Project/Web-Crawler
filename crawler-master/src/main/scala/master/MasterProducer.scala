package master

import core._
import core.protocol.JsonProtocol
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka.ProducerSettings
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.kafka.scaladsl.Producer

object MasterProducer  {
    def main(args: Array[String]): Unit = {
        implicit val system = ActorSystem("MasterSystem")

        val producerSettings =
            ProducerSettings(system, new ByteArraySerializer, new ByteArraySerializer)
                .withBootstrapServers("localhost:9092")

        val task = FetchTask("task-001", "https://example.com")
        val bytes = JsonProtocol.encode(task)

        Source.single(new ProducerRecord[Array[Byte], Array[Byte]]("crawl-tasks", bytes))
            .runWith(Producer.plainSink(producerSettings))

        println(s"[Master] publish task：${task.taskId} -> ${task.url}")
    }
}