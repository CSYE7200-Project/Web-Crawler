package fetcher

import com.crawler.worker.fetcher.FetchResponse
import org.scalatest.funsuite.AnyFunSuite


class FetchResponseSpec extends AnyFunSuite {

    test("FetchResponse stores all fields correctly in success case") {

        val resp = FetchResponse(
            url = "https://a.com",
            statusCode = 200,
            contentType = Some("text/html"),
            contentLength = 1024,
            body = Some("<html></html>"),
            fetchTimeMs = 50,
            errorMessage = None
        )

        assert(resp.url == "https://a.com")
        assert(resp.statusCode == 200)
        assert(resp.contentType.contains("text/html"))
        assert(resp.contentLength == 1024)
        assert(resp.body.contains("<html></html>"))
        assert(resp.fetchTimeMs == 50)
        assert(resp.errorMessage.isEmpty)
        assert(resp.isSuccess)  // 200 → true
    }

    test("FetchResponse handles error and missing body/contentType") {

        val resp = FetchResponse(
            url = "https://b.com",
            statusCode = 500,
            contentType = None,
            contentLength = 0,
            body = None,
            fetchTimeMs = 30,
            errorMessage = Some("Internal server error")
        )

        assert(resp.url == "https://b.com")
        assert(resp.statusCode == 500)
        assert(resp.contentType.isEmpty)
        assert(resp.contentLength == 0)
        assert(resp.body.isEmpty)
        assert(resp.fetchTimeMs == 30)
        assert(resp.errorMessage.contains("Internal server error"))
        assert(!resp.isSuccess)  // 500 → false
    }

    test("isSuccess returns true only for status codes 200–299") {
        assert(FetchResponse("u", 199, None, 0, None, 0).isSuccess == false)
        assert(FetchResponse("u", 200, None, 0, None, 0).isSuccess == true)
        assert(FetchResponse("u", 250, None, 0, None, 0).isSuccess == true)
        assert(FetchResponse("u", 299, None, 0, None, 0).isSuccess == true)
        assert(FetchResponse("u", 300, None, 0, None, 0).isSuccess == false)
    }
}
