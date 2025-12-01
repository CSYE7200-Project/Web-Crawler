package com.crawler.master.state

import com.crawler.core.protocol.WorkerHeartbeat
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

class WorkerRegistry(heartbeatTimeoutMs: Long = 30000) {
  private val workers = new ConcurrentHashMap[String, WorkerInfo]()

  def register(workerId: String, host: String, port: Int, maxConcurrency: Int): Unit = {
    val now = System.currentTimeMillis()
    workers.put(workerId, WorkerInfo(
      workerId = workerId,
      host = host,
      port = port,
      maxConcurrency = maxConcurrency,
      registeredAt = now,
      lastHeartbeat = now
    ))
  }

  def updateHeartbeat(heartbeat: WorkerHeartbeat): Unit = {
    workers.computeIfPresent(heartbeat.workerId, (_, info) =>
      info.copy(
        lastHeartbeat = heartbeat.timestamp,
        urlsCrawled = heartbeat.urlsCrawled,
        avgFetchTimeMs = heartbeat.avgFetchTimeMs,
        currentQueueSize = heartbeat.currentQueueSize,
        isAlive = true
      )
    )
  }

  def assignTask(workerId: String, taskId: String): Unit = {
    workers.computeIfPresent(workerId, (_, info) =>
      info.copy(assignedTasks = info.assignedTasks + taskId)
    )
  }

  def completeTask(workerId: String, taskId: String): Unit = {
    workers.computeIfPresent(workerId, (_, info) =>
      info.copy(assignedTasks = info.assignedTasks - taskId)
    )
  }

  def getWorker(workerId: String): Option[WorkerInfo] = Option(workers.get(workerId))

  def getAllWorkers: Seq[WorkerInfo] = workers.values().asScala.toSeq

  def getAliveWorkers: Seq[WorkerInfo] = {
    val now = System.currentTimeMillis()
    getAllWorkers.filter(w => w.isAlive && (now - w.lastHeartbeat) < heartbeatTimeoutMs)
  }

  def getAvailableWorker: Option[WorkerInfo] = {
    getAliveWorkers
      .filter(w => w.assignedTasks.size < w.maxConcurrency)
      .sortBy(w => w.assignedTasks.size)
      .headOption
  }

  def checkDeadWorkers(): Seq[String] = {
    val now = System.currentTimeMillis()
    val deadWorkerIds = scala.collection.mutable.ArrayBuffer[String]()

    workers.forEach { (id, info) =>
      if (info.isAlive && (now - info.lastHeartbeat) > heartbeatTimeoutMs) {
        workers.put(id, info.copy(isAlive = false))
        deadWorkerIds += id
      }
    }

    deadWorkerIds.toSeq
  }

  def getDeadWorkerTasks(workerId: String): Set[String] = {
    getWorker(workerId).map(_.assignedTasks).getOrElse(Set.empty)
  }

  def workerCount: Int = workers.size()
  def aliveWorkerCount: Int = getAliveWorkers.size
}