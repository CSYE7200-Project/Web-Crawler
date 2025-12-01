package com.crawler.worker.actor

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.crawler.core.protocol._
import com.crawler.core.kafka.{KafkaProducerWrapper, KafkaTopics}
import com.crawler.core.metrics.CrawlerMetrics
import com.crawler.worker.fetcher.{HttpFetcher, FetchResponse}
import com.crawler.api.PageParser
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object WorkerActor {
  private val logger = LoggerFactory.getLogger(getClass)

  def apply(
             workerId: String,
             producer: KafkaProducerWrapper,
             metrics: CrawlerMetrics,
             maxConcurrency: Int = 10
           )(implicit ec: ExecutionContext): Behavior[WorkerCommand] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(SendHeartbeat, 5.seconds)

        val fetcher = new HttpFetcher()
        val parser = new PageParser()

        val registration = RegisterWorker(
          workerId = workerId,
          host = java.net.InetAddress.getLocalHost.getHostName,
          port = 0,
          maxConcurrency = maxConcurrency
        )
        producer.send(KafkaTopics.Registration, workerId, registration)
        logger.info(s"Worker $workerId registered with master")

        new WorkerActorImpl(context, timers, workerId, producer, fetcher, parser, metrics, maxConcurrency).running(0)
      }
    }
  }
}

private class WorkerActorImpl(
                               context: ActorContext[WorkerCommand],
                               timers: TimerScheduler[WorkerCommand],
                               workerId: String,
                               producer: KafkaProducerWrapper,
                               fetcher: HttpFetcher,
                               parser: PageParser,
                               metrics: CrawlerMetrics,
                               maxConcurrency: Int
                             )(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(getClass)

  def running(inFlight: Int): Behavior[WorkerCommand] = Behaviors.receiveMessage {
    case ProcessTask(task) if inFlight >= maxConcurrency =>
      logger.warn(s"Worker at max concurrency ($maxConcurrency), dropping task ${task.taskId}")
      Behaviors.same

    case ProcessTask(task) =>
      logger.debug(s"Processing task ${task.taskId}: ${task.url}")

      val self = context.self
      fetcher.fetch(task.url).onComplete {
        case Success(response) => self ! FetchComplete(task, response)
        case Failure(e) =>
          self ! FetchComplete(task, FetchResponse(
            url = task.url,
            statusCode = 0,
            contentType = None,
            contentLength = 0,
            body = None,
            fetchTimeMs = 0,
            errorMessage = Some(e.getMessage)
          ))
      }

      running(inFlight + 1)

    case FetchComplete(task, response) =>
      val extractedLinks = if (response.isSuccess && response.body.isDefined) {
        parser.extractLinks(response.body.get, task.url)
      } else {
        List.empty
      }

      if (response.isSuccess) {
        metrics.recordSuccess(task.url, response.fetchTimeMs, response.contentLength, extractedLinks.size)
      } else {
        metrics.recordFailure(task.url, response.errorMessage.getOrElse("Unknown"))
      }

      val result = FetchResult(
        taskId = task.taskId,
        workerId = workerId,
        url = task.url,
        success = response.isSuccess,
        statusCode = response.statusCode,
        contentLength = response.contentLength,
        fetchTimeMs = response.fetchTimeMs,
        htmlContent = None,
        extractedLinks = extractedLinks.take(100),
        errorMessage = response.errorMessage
      )

      producer.send(KafkaTopics.Results, workerId, result)
      logger.debug(s"Completed task ${task.taskId}: ${response.statusCode} (${response.fetchTimeMs}ms)")

      running(inFlight - 1)

    case SendHeartbeat =>
      val snapshot = metrics.getSnapshot
      val heartbeat = WorkerHeartbeat(
        workerId = workerId,
        urlsCrawled = snapshot.urlsCrawled,
        urlsSucceeded = snapshot.urlsSucceeded,
        urlsFailed = snapshot.urlsFailed,
        avgFetchTimeMs = snapshot.avgFetchTimeMs,
        currentQueueSize = inFlight,
        cpuUsage = 0,
        memoryUsageMb = Runtime.getRuntime.totalMemory() / 1024 / 1024
      )
      producer.send(KafkaTopics.Heartbeats, workerId, heartbeat)
      Behaviors.same

    case Shutdown =>
      logger.info(s"Worker $workerId shutting down...")
      fetcher.close()
      producer.close()
      Behaviors.stopped
  }
}