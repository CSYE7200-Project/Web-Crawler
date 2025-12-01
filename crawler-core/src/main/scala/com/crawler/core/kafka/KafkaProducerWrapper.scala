// File: crawler-core/src/main/scala/com/crawler/core/kafka/KafkaProducerWrapper.scala
package com.crawler.core.kafka

import org.apache.kafka.clients.producer._
import org.apache.kafka.common.serialization.StringSerializer
import io.circe.syntax._
import com.crawler.core.protocol._
import com.crawler.core.protocol.MessageCodecs._
import org.slf4j.LoggerFactory

import java.util.Properties
import scala.concurrent.{Future, Promise}
import scala.util.Try

class KafkaProducerWrapper(config: KafkaConfig) {
  private val logger = LoggerFactory.getLogger(getClass)

  private val props = new Properties()
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
  props.put(ProducerConfig.ACKS_CONFIG, "1")
  props.put(ProducerConfig.RETRIES_CONFIG, "3")
  props.put(ProducerConfig.LINGER_MS_CONFIG, "5")
  props.put(ProducerConfig.BATCH_SIZE_CONFIG, "16384")

  private val producer = new KafkaProducer[String, String](props)

  def send(topic: String, key: String, message: CrawlerMessage): Future[RecordMetadata] = {
    val promise = Promise[RecordMetadata]()
    val json = message.asJson.noSpaces
    val record = new ProducerRecord[String, String](topic, key, json)

    producer.send(record, new Callback {
      override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
        if (exception != null) {
          logger.error(s"Failed to send message to $topic: ${exception.getMessage}")
          promise.failure(exception)
        } else {
          logger.debug(s"Sent message to $topic partition ${metadata.partition()} offset ${metadata.offset()}")
          promise.success(metadata)
        }
      }
    })

    promise.future
  }

  def sendSync(topic: String, key: String, message: CrawlerMessage): Try[RecordMetadata] = {
    Try {
      val json = message.asJson.noSpaces
      val record = new ProducerRecord[String, String](topic, key, json)
      producer.send(record).get()
    }
  }

  def close(): Unit = {
    producer.flush()
    producer.close()
  }
}