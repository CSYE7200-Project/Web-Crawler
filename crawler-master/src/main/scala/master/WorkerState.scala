package master

import java.time.Instant

case class WorkerState(id: String, host: String, lastHeartbeat: Instant)
