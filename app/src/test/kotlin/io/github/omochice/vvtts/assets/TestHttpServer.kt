package io.github.omochice.vvtts.assets

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.Collections

/**
 * A real HTTP server on a loopback ephemeral port whose single route `/asset` answers with
 * [handler], recording the `Range` header of every request it receives.
 */
internal class TestHttpServer(var handler: (HttpExchange) -> Unit) : AutoCloseable {
  private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

  /** The `Range` header of each request in arrival order, null where none was sent. */
  val ranges: MutableList<String?> = Collections.synchronizedList(mutableListOf())

  val url: String
    get() = "http://127.0.0.1:${server.address.port}/asset"

  init {
    server.createContext("/asset") { exchange ->
      ranges += exchange.requestHeaders.getFirst("Range")
      exchange.use { handler(it) }
    }
    server.start()
  }

  override fun close() = server.stop(0)

  companion object {
    fun full(body: ByteArray): (HttpExchange) -> Unit = { exchange ->
      exchange.sendResponseHeaders(200, body.size.toLong())
      exchange.responseBody.write(body)
    }

    fun redirect(location: String): (HttpExchange) -> Unit = { exchange ->
      exchange.responseHeaders.add("Location", location)
      exchange.sendResponseHeaders(302, -1)
    }

    /** Sends the first [sentBytes] of [body], then stalls for [stallMillis] before finishing. */
    fun stalling(body: ByteArray, sentBytes: Int, stallMillis: Long): (HttpExchange) -> Unit =
        { exchange ->
          exchange.sendResponseHeaders(200, body.size.toLong())
          exchange.responseBody.write(body, 0, sentBytes)
          exchange.responseBody.flush()
          Thread.sleep(stallMillis)
          exchange.responseBody.write(body, sentBytes, body.size - sentBytes)
        }

    /** Announces the whole [body] but closes the connection after its first [sentBytes]. */
    fun aborted(body: ByteArray, sentBytes: Int): (HttpExchange) -> Unit = { exchange ->
      exchange.sendResponseHeaders(200, body.size.toLong())
      exchange.responseBody.write(body, 0, sentBytes)
      exchange.responseBody.flush()
    }

    /** Honours a `Range: bytes=N-` request with 206, and answers 416 when N is past the end. */
    fun ranged(body: ByteArray): (HttpExchange) -> Unit = { exchange ->
      val start =
          exchange.requestHeaders.getFirst("Range")?.removePrefix("bytes=")?.removeSuffix("-")
      when {
        start == null -> full(body)(exchange)
        start.toInt() >= body.size -> {
          exchange.responseHeaders.add("Content-Range", "bytes */${body.size}")
          exchange.sendResponseHeaders(416, -1)
        }
        else -> {
          val from = start.toInt()
          exchange.responseHeaders.add(
              "Content-Range",
              "bytes $from-${body.size - 1}/${body.size}",
          )
          exchange.sendResponseHeaders(206, (body.size - from).toLong())
          exchange.responseBody.write(body, from, body.size - from)
        }
      }
    }
  }
}
