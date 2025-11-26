package core

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka.ProducerSettings
import org.apache.pekko.kafka.scaladsl.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.pekko.stream.scaladsl.Source

object KafkaProducerUtil {
    implicit val system = ActorSystem("KafkaUtil")

    private val settings =
        ProducerSettings(system, new ByteArraySerializer, new ByteArraySerializer)
            .withBootstrapServers("localhost:9092")

    def send(topic: String, value: Array[Byte]): Unit = {
        Source.single(new ProducerRecord[Array[Byte], Array[Byte]](topic, value))
            .runWith(Producer.plainSink(settings))
    }
}