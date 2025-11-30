package demo

import core.protocol.JsonProtocol
import core.{FetchTask}

object DemoMain {
    def main(args: Array[String]): Unit = {
        val task = FetchTask("1", "https://example.com")
        val bytes = JsonProtocol.encode(task)
        println("Encoded JSON: " + new String(bytes, "UTF-8"))
        val decoded = JsonProtocol.decode(bytes)
        println("Decoded object: " + decoded)
    }
}
