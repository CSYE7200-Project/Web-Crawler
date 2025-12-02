//package com.crawler.worker
//
//import org.apache.pekko.actor.typed.ActorSystem
//import com.crawler.core.kafka._
//import com.crawler.core.metrics.{CrawlerMetrics, MetricsReporter}
//import com.crawler.core.protocol._
//import com.crawler.worker.actor._
//import org.slf4j.LoggerFactory
//
//import java.util.UUID
//import java.util.concurrent.CountDownLatch
//import java.util.concurrent.atomic.AtomicBoolean
//import scala.concurrent.ExecutionContext.Implicits.global
//
//object WorkerMain {
//  private val logger = LoggerFactory.getLogger(getClass)
//
//  // Flag to signal shutdown
//  private val running = new AtomicBoolean(true)
//  private val shutdownLatch = new CountDownLatch(1)
//
//  def main(args: Array[String]): Unit = {
//    // Generate unique worker ID
//    val workerId = s"worker-${UUID.randomUUID().toString.take(8)}"
//    logger.info(s"Starting Crawler Worker: $workerId")
//
//    // Configuration
//    val kafkaConfig = KafkaConfig()
//    val maxConcurrency = 10
//
//    // Initialize components
//    val producer = new KafkaProducerWrapper(kafkaConfig)
//    val metrics = new CrawlerMetrics()
//    val metricsReporter = new MetricsReporter(metrics, intervalSeconds = 15)
//
//    // Create actor system
//    val system: ActorSystem[WorkerCommand] = ActorSystem(
//      WorkerActor(workerId, producer, metrics, maxConcurrency),
//      "crawler-worker"
//    )
//
//    // Start metrics reporter
//    metricsReporter.start()
//
//    // Start Kafka consumer for tasks
//    val consumer = new KafkaConsumerWrapper(
//      kafkaConfig,
//      Seq(KafkaTopics.Tasks),
//      groupIdSuffix = s"-$workerId"
//    )
//
//    // Consumer thread
//    val consumerThread = new Thread(() => {
//      logger.info(s"Worker $workerId Kafka consumer started")
//      try {
//        while (running.get()) {
//          try {
//            consumer.poll().foreach {
//              case (key, task: FetchTask) if key == workerId || key == null =>
//                system ! ProcessTask(task)
//              case _ => // Ignore tasks for other workers
//            }
//            Thread.sleep(10)
//          } catch {
//            case _: InterruptedException =>
//              logger.debug("Consumer thread interrupted")
//              running.set(false)
//            case e: Exception =>
//              logger.error(s"Consumer error: ${e.getMessage}")
//          }
//        }
//      } finally {
//        // Close consumer in the same thread that uses it
//        try {
//          consumer.close()
//          logger.info("Kafka consumer closed")
//        } catch {
//          case e: Exception => logger.warn(s"Error closing consumer: ${e.getMessage}")
//        }
//        shutdownLatch.countDown()
//      }
//    }, s"consumer-$workerId")
//    consumerThread.setDaemon(false)
//    consumerThread.start()
//
//    // Shutdown hook
//    Runtime.getRuntime.addShutdownHook(new Thread(() => {
//      logger.info(s"Shutting down worker $workerId...")
//
//      // Signal consumer to stop
//      running.set(false)
//      consumer.stop()
//
//      // Wait for consumer thread to finish (with timeout)
//      try {
//        shutdownLatch.await(5, java.util.concurrent.TimeUnit.SECONDS)
//      } catch {
//        case _: InterruptedException => // Ignore
//      }
//
//      // Stop actor system
//      system ! Shutdown
//
//      // Stop metrics reporter
//      metricsReporter.stop()
//
//      // Print final metrics summary
//      println("\n")
//      metrics.printSummary()
//
//      logger.info(s"Worker $workerId shutdown complete")
//    }, "shutdown-hook"))
//
//    logger.info(s"Worker $workerId is running. Press Ctrl+C to stop.")
//
//    // Keep main thread alive
//    try {
//      while (running.get()) {
//        Thread.sleep(1000)
//      }
//    } catch {
//      case _: InterruptedException => // Normal shutdown
//    }
//  }
//}