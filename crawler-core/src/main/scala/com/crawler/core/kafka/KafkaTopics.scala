// File: crawler-core/src/main/scala/com/crawler/core/kafka/KafkaTopics.scala
package com.crawler.core.kafka

object KafkaTopics {
  val Registration = "crawler-registration"
  val Tasks = "crawler-tasks"
  val Results = "crawler-results"
  val Heartbeats = "crawler-heartbeats"
  val Control = "crawler-control"
  val UrlFrontier = "crawler-url-frontier"

  val all: Seq[String] = Seq(Registration, Tasks, Results, Heartbeats, Control, UrlFrontier)
}