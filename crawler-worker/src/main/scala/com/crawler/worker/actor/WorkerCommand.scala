// File: crawler-worker/src/main/scala/com/crawler/worker/actor/WorkerCommand.scala
package com.crawler.worker.actor

import com.crawler.core.protocol.FetchTask
import com.crawler.worker.fetcher.FetchResponse

sealed trait WorkerCommand

case class ProcessTask(task: FetchTask) extends WorkerCommand
case class FetchComplete(task: FetchTask, response: FetchResponse) extends WorkerCommand
case object SendHeartbeat extends WorkerCommand
case object Shutdown extends WorkerCommand