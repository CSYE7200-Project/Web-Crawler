package worker

import core._
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka.scaladsl.Consumer
import org.apache.pekko.kafka.{ConsumerSettings, Subscriptions}
import org.apache.kafka.common.serialization.ByteArrayDeserializer

object WorkerConsumer {
    def main(args: Array[String]): Unit = {
        implicit val system = ActorSystem("WorkerSystem")

        val consumerSettings =
            ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
                .withBootstrapServers("localhost:9092")
                .withGroupId("worker-group")

        Consumer
            .plainSource(consumerSettings, Subscriptions.topics("crawl-tasks"))
            .map { msg =>
                val task = JsonProtocol.decode(msg.value()).asInstanceOf[FetchTask]
                println(s"[Worker] received task: ${task.url}")

                val result = FetchResult(task.taskId, task.url, true, Some("<html>ok</html>"))
                KafkaProducerUtil.send("crawl-results", JsonProtocol.encode(result))
            }
            .runForeach(_ => ())
    }
}