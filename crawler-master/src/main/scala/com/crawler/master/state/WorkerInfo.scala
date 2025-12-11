package com.crawler.master.state

case class WorkerInfo(
                       workerId: String,
                       host: String,
                       port: Int,
                       maxConcurrency: Int,
                       registeredAt: Long,
                       lastHeartbeat: Long,
                       urlsCrawled: Long = 0,
                       avgFetchTimeMs: Double = 0,
                       currentQueueSize: Int = 0,
                       isAlive: Boolean = true,
                       assignedTasks: Set[String] = Set.empty
                     )