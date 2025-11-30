package core.protocol

import java.time.Instant
import core.Task


/** Worker Register */
final case class RegisterWorker(id: String, host: String)

/** Master ACK */
case object Ack

/** Worker heartbeat */
final case class Heartbeat(id: String, timestamp: Instant = Instant.now)

/** Master task assignment */
final case class AssignTask(task: Task[_])

/** Client send task to Master */
final case class SubmitTask(task: Task[_])