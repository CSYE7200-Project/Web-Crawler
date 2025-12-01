// File: crawler-core/src/main/scala/com/crawler/core/kafka/KafkaConfig.scala
package com.crawler.core.kafka

case class KafkaConfig(
                        bootstrapServers: String = "localhost:9092",
                        groupId: String = "crawler-group",
                        autoOffsetReset: String = "earliest",
                        enableAutoCommit: Boolean = true,
                        maxPollRecords: Int = 100,
                        sessionTimeoutMs: Int = 30000,
                        heartbeatIntervalMs: Int = 10000
                      )