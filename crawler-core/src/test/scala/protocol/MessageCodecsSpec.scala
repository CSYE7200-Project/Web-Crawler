package protocol

import com.crawler.core.protocol._
import com.crawler.core.protocol.MessageCodecs._
import io.circe.syntax._
import org.scalatest.funsuite.AnyFunSuite

class MessageCodecsSpec extends AnyFunSuite {

    test("Round-trip encode/decode FetchTask") {
        val msg: CrawlerMessage = FetchTask("t1", "http://x.com", depth = 2)
        val json = (msg: CrawlerMessage).asJson.noSpaces

        val decoded = MessageCodecs.decodeMessage(json)
        assert(decoded.exists(_.isInstanceOf[FetchTask]))
    }

    test("Round-trip encode/decode FetchResult") {
        val msg: CrawlerMessage =
            FetchResult("id", "w", "u", success = true, 200, 10, 10, None, List("a"), None)

        val json = (msg: CrawlerMessage).asJson.noSpaces

        val decoded = MessageCodecs.decodeMessage(json)
        assert(decoded.exists(_.isInstanceOf[FetchResult]))
    }

    test("PauseWorker custom encoder/decoder") {
        val msg: CrawlerMessage = PauseWorker("w1")
        val json = (msg: CrawlerMessage).asJson.noSpaces

        val decoded = MessageCodecs.decodeMessage(json)
        assert(decoded.exists(_.isInstanceOf[PauseWorker]))
    }

    test("Unknown _type should return error") {
        val badJson =
            """
        {"_type": "DoesNotExist", "field":"x"}
      """
        val decoded = MessageCodecs.decodeMessage(badJson)
        assert(decoded.isLeft)
    }
}