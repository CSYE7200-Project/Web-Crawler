// File: crawler-core/src/main/scala/com/crawler/core/kafka/KafkaConsumerWrapper.scala
package com.crawler.core.kafka

import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.serialization.StringDeserializer
import com.crawler.core.protocol._
import com.crawler.core.protocol.MessageCodecs._
import org.slf4j.LoggerFactory

import java.time.Duration
import java.util.Properties
import scala.jdk.CollectionConverters._

class KafkaConsumerWrapper(
                            config: KafkaConfig,
                            topics: Seq[String],
                            groupIdSuffix: String = ""
                          ) {
  private val logger = LoggerFactory.getLogger(getClass)

  private val props = new Properties()
  props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
  props.put(ConsumerConfig.GROUP_ID_CONFIG, config.groupId + groupIdSuffix)
  props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
  props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
  props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.autoOffsetReset)
  props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, config.enableAutoCommit.toString)
  props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, config.maxPollRecords.toString)
  props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, config.sessionTimeoutMs.toString)
  props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, config.heartbeatIntervalMs.toString)

  private val consumer = new KafkaConsumer[String, String](props)
  consumer.subscribe(topics.asJava)

  @volatile private var running = true

  def poll(timeout: Duration = Duration.ofMillis(100)): Seq[(String, CrawlerMessage)] = {
    if (!running) return Seq.empty

    val records = consumer.poll(timeout)
    records.asScala.toSeq.flatMap { record =>
      decodeMessage(record.value()) match {
        case Right(msg) => Some((record.key(), msg))
        case Left(err) =>
          logger.warn(s"Failed to decode message: ${err.getMessage}")
          None
      }
    }
  }

  def pollRaw(timeout: Duration = Duration.ofMillis(100)): Seq[ConsumerRecord[String, String]] = {
    if (!running) return Seq.empty
    consumer.poll(timeout).asScala.toSeq
  }

  def commitSync(): Unit = consumer.commitSync()

  def stop(): Unit = {
    running = false
  }

  def close(): Unit = {
    running = false
    consumer.close()
  }
}