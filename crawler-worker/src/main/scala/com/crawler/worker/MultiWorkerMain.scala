// File: crawler-worker/src/main/scala/com/crawler/worker/MultiWorkerMain.scala
package com.crawler.worker

import org.apache.pekko.actor.typed.ActorSystem
import com.crawler.core.kafka._
import com.crawler.core.metrics.{CrawlerMetrics, MetricsReporter}
import com.crawler.core.protocol._
import com.crawler.worker.actor._
import org.slf4j.LoggerFactory

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.ExecutionContext.Implicits.global

object MultiWorkerMain {
  private val logger = LoggerFactory.getLogger(getClass)
  private val running = new AtomicBoolean(true)

  // ========== CONFIGURATION ==========
  val DEFAULT_NUM_WORKERS = 10
  val DEFAULT_CONCURRENCY = 10
  // ====================================

  def main(args: Array[String]): Unit = {
    val numWorkers = args.headOption.flatMap(_.toIntOption).getOrElse(DEFAULT_NUM_WORKERS)
    val concurrency = args.lift(1).flatMap(_.toIntOption).getOrElse(DEFAULT_CONCURRENCY)

    logger.info("=" * 60)
    logger.info(s"  STARTING $numWorkers WORKERS")
    logger.info(s"  Concurrency per worker: $concurrency")
    logger.info(s"  Total parallel fetches: ${numWorkers * concurrency}")
    logger.info("=" * 60)

    val kafkaConfig = KafkaConfig()

    // Combined metrics for all workers
    val combinedMetrics = new CrawlerMetrics()
    val metricsReporter = new MetricsReporter(combinedMetrics, intervalSeconds = 10)
    metricsReporter.start()

    // Store all workers for shutdown
    case class WorkerInstance(
                               workerId: String,
                               system: ActorSystem[WorkerCommand],
                               producer: KafkaProducerWrapper,
                               consumer: KafkaConsumerWrapper,
                               thread: Thread
                             )

    // Start multiple workers
    val workers = (1 to numWorkers).map { i =>
      val workerId = s"worker-${UUID.randomUUID().toString.take(8)}"
      val producer = new KafkaProducerWrapper(kafkaConfig)

      // Each worker gets its own actor system
      val system = ActorSystem(
        WorkerActor(workerId, producer, combinedMetrics, concurrency),
        s"worker-system-$i"
      )

      // Shared consumer group = Kafka load balances tasks across workers
      val consumer = new KafkaConsumerWrapper(
        kafkaConfig,
        Seq(KafkaTopics.Tasks),
        groupIdSuffix = "-multi-workers"  // SAME group = load balanced!
      )

      // Consumer thread for this worker
      val consumerThread = new Thread(() => {
        logger.debug(s"Worker $workerId consumer started")
        try {
          while (running.get()) {
            try {
              consumer.poll().foreach {
                case (_, task: FetchTask) =>
                  system ! ProcessTask(task)
                case _ =>
              }
              Thread.sleep(5)
            } catch {
              case _: InterruptedException =>
                running.set(false)
              case e: Exception =>
                if (running.get()) logger.error(s"Consumer error: ${e.getMessage}")
            }
          }
        } finally {
          try { consumer.close() } catch { case _: Exception => }
        }
      }, s"consumer-$workerId")
      consumerThread.setDaemon(true)
      consumerThread.start()

      logger.info(s"✓ Worker $i started: $workerId")

      WorkerInstance(workerId, system, producer, consumer, consumerThread)
    }.toList

    logger.info(s"\n✅ All $numWorkers workers running!\n")

    // Shutdown hook
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      logger.info(s"\nShutting down $numWorkers workers...")
      running.set(false)

      // Stop all consumers first
      workers.foreach { w =>
        w.consumer.stop()
      }

      Thread.sleep(1000)

      // Stop all actor systems
      workers.foreach { w =>
        w.system ! Shutdown
      }

      Thread.sleep(1000)

      // Stop metrics
      metricsReporter.stop()

      // Print combined metrics
      println("\n")
      println("=" * 60)
      println(s"  COMBINED METRICS FOR $numWorkers WORKERS")
      println("=" * 60)
      combinedMetrics.printSummary(numWorkers)

      logger.info("All workers shutdown complete")
    }, "shutdown-hook"))

    // Keep main thread alive
    try {
      while (running.get()) {
        Thread.sleep(1000)
      }
    } catch {
      case _: InterruptedException =>
    }
  }
}