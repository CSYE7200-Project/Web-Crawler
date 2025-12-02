// File: crawler-master/src/main/scala/com/crawler/master/CrawlerLauncher.scala
package com.crawler.master

import com.crawler.core.metrics.{CrawlerMetrics, MetricsReporter}
import com.crawler.core.frontier.UrlFrontier
import com.crawler.api.PageParser
import com.crawler.worker.fetcher.{SimpleHttpFetcher, FetchResponse}
import org.slf4j.LoggerFactory
import sun.misc.{Signal, SignalHandler}

import java.util.concurrent.{ExecutorService, Executors, TimeUnit}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import scala.io.Source
import scala.util.{Failure, Success, Using}

/**
 * Simplified Distributed Web Crawler
 * - Uses Java HttpClient (no Netty issues)
 * - Direct thread pool for parallelism
 */
object CrawlerLauncher {
  private val logger = LoggerFactory.getLogger(getClass)
  private val running = new AtomicBoolean(true)

  def main(args: Array[String]): Unit = {
    // Parse arguments - with debug output
    val numWorkers = args.headOption.flatMap(s => scala.util.Try(s.toInt).toOption).getOrElse(10)
    val concurrency = args.lift(1).flatMap(s => scala.util.Try(s.toInt).toOption).getOrElse(10)
    val totalParallelism = numWorkers * concurrency

    println("=" * 60)
    println("       DISTRIBUTED WEB CRAWLER")
    println("=" * 60)
    println(s"  Args received:      ${args.mkString(", ")}")
    println(s"  Workers:            $numWorkers")
    println(s"  Concurrency/Worker: $concurrency")
    println(s"  Total Parallelism:  $totalParallelism")
    println("=" * 60)
    println("  Press Ctrl+C to stop and see final metrics")
    println("=" * 60 + "\n")

    // Thread pool for parallel fetching
    val executor: ExecutorService = Executors.newFixedThreadPool(totalParallelism)

    // Components
    val frontier = new UrlFrontier(expectedUrls = 100000, maxDepth = 3)
    val metrics = new CrawlerMetrics()
    val metricsReporter = new MetricsReporter(metrics, intervalSeconds = 5)
    val fetcher = new SimpleHttpFetcher()  // Using simple HTTP client now
    val parser = new PageParser()

    // Track in-flight requests
    val inFlight = new AtomicInteger(0)
    val maxInFlight = totalParallelism

    // Load seed URLs
    loadSeedUrls("seed_urls.txt", frontier)

    // Start metrics reporter
    metricsReporter.start()

    logger.info(s"✅ Crawler started with $totalParallelism parallel fetchers\n")

    // Submit initial batch of tasks
    val submittedCount = new AtomicInteger(0)

    // Main crawl loop
    val crawlThread = new Thread(() => {
      while (running.get()) {
        try {
          // Submit tasks while we have capacity and URLs
          while (running.get() && inFlight.get() < maxInFlight && frontier.size > 0) {
            frontier.getNext match {
              case Some(pUrl) =>
                inFlight.incrementAndGet()
                submittedCount.incrementAndGet()

                executor.submit(new Runnable {
                  override def run(): Unit = {
                    try {
                      val response = fetcher.fetch(pUrl.url)

                      if (response.isSuccess && response.body.isDefined) {
                        val links = parser.extractLinks(response.body.get, pUrl.url)
                        metrics.recordSuccess(pUrl.url, response.fetchTimeMs, response.contentLength, links.size)
                        metrics.recordStatusCode(response.statusCode)

                        // Add new links to frontier (limit depth)
                        if (pUrl.depth < 2) {
                          frontier.addUrls(links.take(50), pUrl.depth + 1, pUrl.priority - 1)
                        }
                      } else {
                        metrics.recordFailure(pUrl.url, response.errorMessage.getOrElse("HTTP " + response.statusCode), Some(response.statusCode))
                        if (response.statusCode > 0) {
                          metrics.recordStatusCode(response.statusCode)
                        }
                      }
                    } catch {
                      case e: Exception =>
                        metrics.recordFailure(pUrl.url, e.getClass.getSimpleName)
                    } finally {
                      inFlight.decrementAndGet()
                    }
                  }
                })

              case None =>
                Thread.sleep(50)
            }
          }

          // Wait a bit before checking again
          Thread.sleep(10)

          // Check if we're done
          if (frontier.size == 0 && inFlight.get() == 0) {
            logger.info("Frontier empty and no in-flight requests. Stopping...")
            running.set(false)
          }
        } catch {
          case _: InterruptedException => running.set(false)
          case e: Exception => logger.error(s"Crawl error: ${e.getMessage}")
        }
      }
    }, "crawl-loop")
    crawlThread.start()

    // Signal handler for Ctrl+C
    val shutdownHandler = new SignalHandler {
      override def handle(sig: Signal): Unit = {
        println("\n\n" + "=" * 60)
        println("  SHUTTING DOWN - PLEASE WAIT FOR FINAL METRICS...")
        println("=" * 60)

        running.set(false)

        // Wait for in-flight to complete (max 5 seconds)
        var waited = 0
        while (inFlight.get() > 0 && waited < 50) {
          Thread.sleep(100)
          waited += 1
        }

        metricsReporter.stop()
        executor.shutdownNow()
        fetcher.close()

        // Print final metrics
        println("\n")
        println("=" * 60)
        println(s"  FINAL RESULTS: $numWorkers WORKERS x $concurrency CONCURRENCY")
        println("=" * 60)
        metrics.printSummary(numWorkers)
        frontier.printStats()

        logger.info("Crawler shutdown complete")
        System.exit(0)
      }
    }

    try {
      Signal.handle(new Signal("INT"), shutdownHandler)
      Signal.handle(new Signal("TERM"), shutdownHandler)
    } catch {
      case e: Exception => logger.warn(s"Could not register signal handler: ${e.getMessage}")
    }

    // Keep main thread alive
    try {
      crawlThread.join()
    } catch {
      case _: InterruptedException =>
    }

    // Final cleanup if we reach here normally
    metricsReporter.stop()
    executor.shutdown()
    executor.awaitTermination(10, TimeUnit.SECONDS)

    println("\n")
    println("=" * 60)
    println(s"  FINAL RESULTS: $numWorkers WORKERS x $concurrency CONCURRENCY")
    println("=" * 60)
    metrics.printSummary(numWorkers)
    frontier.printStats()
  }

  private def loadSeedUrls(filename: String, frontier: UrlFrontier): Unit = {
    Using(Source.fromFile(filename)) { source =>
      val urls = source.getLines()
        .map(_.trim)
        .filter(line => line.nonEmpty && !line.startsWith("#"))
        .toList
      val added = frontier.addUrls(urls, depth = 0, priority = 10)
      logger.info(s"Loaded $added seed URLs from $filename")
    } match {
      case Success(_) =>
      case Failure(e) =>
        logger.warn(s"Could not load seed URLs: ${e.getMessage}")
        val defaultUrls = List(
          "https://en.wikipedia.org/wiki/Web_crawler",
          "https://en.wikipedia.org/wiki/Scala_(programming_language)",
          "https://en.wikipedia.org/wiki/Apache_Kafka",
          "https://en.wikipedia.org/wiki/Distributed_computing",
          "https://en.wikipedia.org/wiki/Machine_learning",
          "https://news.ycombinator.com",
          "https://github.com/apache/kafka",
          "https://github.com/scala/scala",
          "https://www.reddit.com/r/programming",
          "https://www.reddit.com/r/scala"
        )
        frontier.addUrls(defaultUrls, depth = 0, priority = 10)
        logger.info(s"Using ${defaultUrls.size} default seed URLs")
    }
  }
}