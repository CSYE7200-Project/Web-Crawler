package core

import org.apache.pekko.actor.typed.{Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.kafka.ProducerSettings
import org.apache.pekko.kafka.scaladsl.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.SystemMaterializer

sealed trait KafkaProduceCommand
case class Produce(topic: String, bytes: Array[Byte]) extends KafkaProduceCommand

object KafkaProducerActor {

    def apply(settings: ProducerSettings[Array[Byte], Array[Byte]]): Behavior[KafkaProduceCommand] =
        Behaviors.setup { ctx =>
            implicit val mat = SystemMaterializer(ctx.system).materializer
            Behaviors.receiveMessage {
                case Produce(topic, bytes) =>
                    Source
                        .single(new ProducerRecord[Array[Byte], Array[Byte]](topic, bytes))
                        .runWith(Producer.plainSink(settings))

                    Behaviors.same
            }
        }
}