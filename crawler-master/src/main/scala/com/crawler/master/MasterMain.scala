package com.crawler.master

import org.apache.pekko.actor.typed.ActorSystem
import com.crawler.core.kafka._
import com.crawler.core.metrics.{CrawlerMetrics, MetricsReporter}
import com.crawler.core.frontier.UrlFrontier
import com.crawler.core.protocol._
import com.crawler.master.actor._
import org.slf4j.LoggerFactory

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import scala.io.Source
import scala.util.{Failure, Success, Using}

object MasterMain {
  private val logger = LoggerFactory.getLogger(getClass)

  // Flag to signal shutdown
  private val running = new AtomicBoolean(true)
  private val shutdownLatch = new CountDownLatch(1)

  def main(args: Array[String]): Unit = {
    logger.info("Starting Crawler Master...")

    // Configuration
    val kafkaConfig = KafkaConfig()
    val maxDepth = 2

    // Initialize components
    val producer = new KafkaProducerWrapper(kafkaConfig)
    val frontier = new UrlFrontier(expectedUrls = 100000, maxDepth = maxDepth)
    val metrics = new CrawlerMetrics()
    val metricsReporter = new MetricsReporter(metrics, intervalSeconds = 15)

    // Create actor system
    val system: ActorSystem[MasterCommand] = ActorSystem(
      MasterActor(producer, frontier, metrics, maxDepth),
      "crawler-master"
    )

    // Start metrics reporter
    metricsReporter.start()

    // Start Kafka consumer in separate thread
    val consumer = new KafkaConsumerWrapper(
      kafkaConfig,
      Seq(KafkaTopics.Registration, KafkaTopics.Results, KafkaTopics.Heartbeats),
      groupIdSuffix = "-master"
    )

    val consumerThread = new Thread(() => {
      logger.info("Master Kafka consumer started")
      try {
        while (running.get()) {
          try {
            consumer.poll().foreach {
              case (_, msg: RegisterWorker) => system ! WorkerRegistration(msg)
              case (_, msg: WorkerHeartbeat) => system ! HeartbeatReceived(msg)
              case (_, msg: FetchResult) => system ! ResultReceived(msg)
              case _ => // Ignore other messages
            }
            Thread.sleep(50)
          } catch {
            case _: InterruptedException =>
              logger.debug("Consumer thread interrupted")
              running.set(false)
            case e: Exception =>
              logger.error(s"Consumer error: ${e.getMessage}")
          }
        }
      } finally {
        // Close consumer in the same thread
        try {
          consumer.close()
          logger.info("Kafka consumer closed")
        } catch {
          case e: Exception => logger.warn(s"Error closing consumer: ${e.getMessage}")
        }
        shutdownLatch.countDown()
      }
    }, "master-consumer")
    consumerThread.setDaemon(false)
    consumerThread.start()

    // Load seed URLs
    loadSeedUrls("seed_urls.txt", system)

    // Shutdown hook
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      logger.info("Shutting down master...")

      // Signal consumer to stop
      running.set(false)
      consumer.stop()

      // Wait for consumer thread to finish
      try {
        shutdownLatch.await(5, java.util.concurrent.TimeUnit.SECONDS)
      } catch {
        case _: InterruptedException => // Ignore
      }

      // Stop actor system
      system ! Shutdown

      // Stop metrics reporter
      metricsReporter.stop()

      // Print final metrics summary
      println("\n")
      metrics.printSummary()

      // Print frontier stats
      frontier.printStats()

      logger.info("Master shutdown complete")
    }, "shutdown-hook"))

    logger.info("Master is running. Press Ctrl+C to stop.")

    // Keep main thread alive
    try {
      while (running.get()) {
        Thread.sleep(1000)
      }
    } catch {
      case _: InterruptedException => // Normal shutdown
    }
  }

  private def loadSeedUrls(filename: String, system: ActorSystem[MasterCommand]): Unit = {
    Using(Source.fromFile(filename)) { source =>
      val urls = source.getLines()
        .map(_.trim)
        .filter(line => line.nonEmpty && !line.startsWith("#"))
        .toList
      system ! AddUrls(urls)
      logger.info(s"Loaded ${urls.size} seed URLs from $filename")
    } match {
      case Success(_) => // OK
      case Failure(e) =>
        logger.warn(s"Could not load seed URLs from $filename: ${e.getMessage}")
        val defaultUrls = List(
          "https://en.wikipedia.org/wiki/Web_crawler",
          "https://news.ycombinator.com",
          "https://www.reddit.com/r/programming"
        )
        system ! AddUrls(defaultUrls)
        logger.info(s"Using ${defaultUrls.size} default seed URLs")
    }
  }
}