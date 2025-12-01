// File: crawler-core/src/main/scala/com/crawler/core/protocol/MessageCodecs.scala
package com.crawler.core.protocol

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

object MessageCodecs {
  // Encoders
  implicit val registerWorkerEncoder: Encoder[RegisterWorker] = deriveEncoder
  implicit val registerWorkerDecoder: Decoder[RegisterWorker] = deriveDecoder

  implicit val workerRegisteredEncoder: Encoder[WorkerRegistered] = deriveEncoder
  implicit val workerRegisteredDecoder: Decoder[WorkerRegistered] = deriveDecoder

  implicit val fetchTaskEncoder: Encoder[FetchTask] = deriveEncoder
  implicit val fetchTaskDecoder: Decoder[FetchTask] = deriveDecoder

  implicit val fetchResultEncoder: Encoder[FetchResult] = deriveEncoder
  implicit val fetchResultDecoder: Decoder[FetchResult] = deriveDecoder

  implicit val workerHeartbeatEncoder: Encoder[WorkerHeartbeat] = deriveEncoder
  implicit val workerHeartbeatDecoder: Decoder[WorkerHeartbeat] = deriveDecoder

  implicit val shutdownWorkerEncoder: Encoder[ShutdownWorker] = deriveEncoder
  implicit val shutdownWorkerDecoder: Decoder[ShutdownWorker] = deriveDecoder

  implicit val pauseWorkerEncoder: Encoder[PauseWorker] = Encoder.instance { m =>
    Json.obj("workerId" -> Json.fromString(m.workerId))
  }
  implicit val pauseWorkerDecoder: Decoder[PauseWorker] = Decoder.instance { c =>
    c.get[String]("workerId").map(PauseWorker)
  }

  implicit val resumeWorkerEncoder: Encoder[ResumeWorker] = Encoder.instance { m =>
    Json.obj("workerId" -> Json.fromString(m.workerId))
  }
  implicit val resumeWorkerDecoder: Decoder[ResumeWorker] = Decoder.instance { c =>
    c.get[String]("workerId").map(ResumeWorker)
  }

  implicit val urlBatchEncoder: Encoder[UrlBatch] = deriveEncoder
  implicit val urlBatchDecoder: Decoder[UrlBatch] = deriveDecoder

  // Generic message encoder
  implicit val crawlerMessageEncoder: Encoder[CrawlerMessage] = Encoder.instance {
    case m: RegisterWorker => m.asJson.deepMerge(Json.obj("_type" -> Json.fromString("RegisterWorker")))
    case m: WorkerRegistered => m.asJson.deepMerge(Json.obj("_type" -> Json.fromString("WorkerRegistered")))
    case m: FetchTask => m.asJson.deepMerge(Json.obj("_type" -> Json.fromString("FetchTask")))
    case m: FetchResult => m.asJson.deepMerge(Json.obj("_type" -> Json.fromString("FetchResult")))
    case m: WorkerHeartbeat => m.asJson.deepMerge(Json.obj("_type" -> Json.fromString("WorkerHeartbeat")))
    case m: ShutdownWorker => m.asJson.deepMerge(Json.obj("_type" -> Json.fromString("ShutdownWorker")))
    case m: PauseWorker => m.asJson.deepMerge(Json.obj("_type" -> Json.fromString("PauseWorker")))
    case m: ResumeWorker => m.asJson.deepMerge(Json.obj("_type" -> Json.fromString("ResumeWorker")))
    case m: UrlBatch => m.asJson.deepMerge(Json.obj("_type" -> Json.fromString("UrlBatch")))
  }

  // Generic message decoder
  def decodeMessage(json: String): Either[Error, CrawlerMessage] = {
    parser.parse(json).flatMap { j =>
      j.hcursor.get[String]("_type").flatMap {
        case "RegisterWorker" => j.as[RegisterWorker]
        case "WorkerRegistered" => j.as[WorkerRegistered]
        case "FetchTask" => j.as[FetchTask]
        case "FetchResult" => j.as[FetchResult]
        case "WorkerHeartbeat" => j.as[WorkerHeartbeat]
        case "ShutdownWorker" => j.as[ShutdownWorker]
        case "PauseWorker" => j.as[PauseWorker]
        case "ResumeWorker" => j.as[ResumeWorker]
        case "UrlBatch" => j.as[UrlBatch]
        case other => Left(DecodingFailure(s"Unknown message type: $other", Nil))
      }
    }
  }
}