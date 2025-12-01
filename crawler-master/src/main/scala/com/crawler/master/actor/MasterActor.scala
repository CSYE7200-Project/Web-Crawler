// File: crawler-master/src/main/scala/com/crawler/master/actor/MasterActor.scala
package com.crawler.master.actor

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.crawler.core.protocol._
import com.crawler.core.kafka.{KafkaProducerWrapper, KafkaTopics}
import com.crawler.core.metrics.CrawlerMetrics
import com.crawler.core.frontier.UrlFrontier
import com.crawler.master.state.WorkerRegistry
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.concurrent.duration._

object MasterActor {
  private val logger = LoggerFactory.getLogger(getClass)

  def apply(
             producer: KafkaProducerWrapper,
             frontier: UrlFrontier,
             metrics: CrawlerMetrics,
             maxDepth: Int = 2
           ): Behavior[MasterCommand] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(DistributeTasks, 500.millis)
        timers.startTimerAtFixedRate(CheckWorkerHealth, 10.seconds)
        timers.startTimerAtFixedRate(PrintStatus, 30.seconds)

        new MasterActorImpl(context, timers, producer, frontier, metrics, maxDepth).running()
      }
    }
  }
}

private class MasterActorImpl(
                               context: ActorContext[MasterCommand],
                               timers: TimerScheduler[MasterCommand],
                               producer: KafkaProducerWrapper,
                               frontier: UrlFrontier,
                               metrics: CrawlerMetrics,
                               maxDepth: Int
                             ) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val registry = new WorkerRegistry()
  private var totalTasksCreated = 0L
  private var totalResultsReceived = 0L

  def running(): Behavior[MasterCommand] = Behaviors.receiveMessage {
    case WorkerRegistration(msg) =>
      handleRegistration(msg)
      Behaviors.same

    case HeartbeatReceived(msg) =>
      registry.updateHeartbeat(msg)
      Behaviors.same

    case ResultReceived(result) =>
      handleResult(result)
      Behaviors.same

    case AddUrls(urls) =>
      val added = frontier.addUrls(urls, depth = 0, priority = 10)
      logger.info(s"Added $added new URLs to frontier (${urls.size} submitted)")
      Behaviors.same

    case DistributeTasks =>
      distributeTasks()
      Behaviors.same

    case CheckWorkerHealth =>
      checkWorkerHealth()
      Behaviors.same

    case PrintStatus =>
      printStatus()
      Behaviors.same

    case Shutdown =>
      logger.info("Master shutting down...")
      producer.close()
      Behaviors.stopped
  }

  private def handleRegistration(msg: RegisterWorker): Unit = {
    registry.register(msg.workerId, msg.host, msg.port, msg.maxConcurrency)
    logger.info(s"Worker registered: ${msg.workerId} at ${msg.host}:${msg.port}")

    val ack = WorkerRegistered(msg.workerId, acknowledged = true)
    producer.send(KafkaTopics.Control, msg.workerId, ack)
  }

  private def handleResult(result: FetchResult): Unit = {
    totalResultsReceived += 1
    registry.completeTask(result.workerId, result.taskId)

    if (result.success) {
      metrics.recordSuccess(
        result.url,
        result.fetchTimeMs,
        result.contentLength,
        result.extractedLinks.size
      )
      metrics.recordStatusCode(result.statusCode)

      if (result.extractedLinks.nonEmpty) {
        val currentDepth = 1
        if (currentDepth < maxDepth) {
          val added = frontier.addUrls(result.extractedLinks, currentDepth + 1, priority = 5)
          if (added > 0) {
            logger.debug(s"Added $added new links from ${result.url}")
          }
        }
      }
    } else {
      metrics.recordFailure(result.url, result.errorMessage.getOrElse("Unknown"), Some(result.statusCode))
    }
  }

  private def distributeTasks(): Unit = {
    var distributed = 0

    while (frontier.size > 0 && registry.getAvailableWorker.isDefined) {
      registry.getAvailableWorker.foreach { worker =>
        frontier.getNext.foreach { pUrl =>
          val taskId = s"task-${UUID.randomUUID().toString.take(8)}"
          val task = FetchTask(
            taskId = taskId,
            url = pUrl.url,
            depth = pUrl.depth,
            maxDepth = maxDepth,
            priority = pUrl.priority
          )

          producer.send(KafkaTopics.Tasks, worker.workerId, task)
          registry.assignTask(worker.workerId, taskId)
          totalTasksCreated += 1
          distributed += 1
        }
      }

      if (distributed >= 100) {
        return
      }
    }

    if (distributed > 0) {
      logger.debug(s"Distributed $distributed tasks")
    }
  }

  private def checkWorkerHealth(): Unit = {
    val deadWorkers = registry.checkDeadWorkers()
    deadWorkers.foreach { workerId =>
      logger.warn(s"Worker $workerId appears dead, reassigning tasks")
      val tasks = registry.getDeadWorkerTasks(workerId)
      tasks.foreach { taskId =>
        logger.warn(s"Task $taskId needs reassignment")
      }
    }
  }

  private def printStatus(): Unit = {
    val snapshot = metrics.getSnapshot
    logger.info(
      f"[STATUS] Workers: ${registry.aliveWorkerCount}/${registry.workerCount} | " +
        f"Frontier: ${frontier.size} | " +
        f"Tasks: $totalTasksCreated created, $totalResultsReceived completed | " +
        f"Crawled: ${snapshot.urlsCrawled} (${snapshot.successRatePercent}%.1f%% success)"
    )
  }
}