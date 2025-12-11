package fetcher

import com.crawler.worker.fetcher.{FetchResponse, HttpFetcher}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class HttpFetcherSpec extends AnyFunSuite {

    class FakeFetcher extends HttpFetcher {
        var closed = false

        override def fetch(url: String): Future[FetchResponse] =
            Future.successful(
                FetchResponse(url, 200, Some(url), 10, Some("ok"), 5)
            )

        override def close(): Unit = closed = true
    }

    test("FakeFetcher returns FetchResponse") {
        val f = new FakeFetcher
        val resp = Await.result(f.fetch("https://x.com"), 1.second)

        assert(resp.url == "https://x.com")
        assert(resp.statusCode == 200)
    }

    test("FakeFetcher close() should set flag") {
        val f = new FakeFetcher
        f.close()
        assert(f.closed)
    }
}
