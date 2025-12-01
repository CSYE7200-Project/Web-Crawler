// File: crawler-master/src/main/scala/com/crawler/master/actor/MasterCommand.scala
package com.crawler.master.actor

import com.crawler.core.protocol.{RegisterWorker, WorkerHeartbeat, FetchResult}

sealed trait MasterCommand

case class WorkerRegistration(msg: RegisterWorker) extends MasterCommand
case class HeartbeatReceived(msg: WorkerHeartbeat) extends MasterCommand
case class ResultReceived(msg: FetchResult) extends MasterCommand
case class AddUrls(urls: List[String]) extends MasterCommand
case object DistributeTasks extends MasterCommand
case object CheckWorkerHealth extends MasterCommand
case object PrintStatus extends MasterCommand
case object Shutdown extends MasterCommand