package io.github.omochice.vvtts.assets

import java.io.EOFException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** How an [HttpRangeFetcher.fetch] call ended without failing. */
enum class FetchEnd {
  /** The partial file holds every byte, or the server has no bytes past it. */
  FINISHED,

  /** A stop was requested before the transfer finished. */
  STOPPED,
}

/** Signals a response status that neither carries the file nor says it is already complete. */
class UnexpectedStatusException(val statusCode: Int) : IOException("HTTP status $statusCode")

/**
 * Brings a local partial copy of a remote file up to its full length over HTTP.
 *
 * @param connectTimeoutMillis how long to wait for a connection, in milliseconds.
 * @param readTimeoutMillis how long a single read may wait for data, in milliseconds.
 */
class HttpRangeFetcher(
    private val connectTimeoutMillis: Int = 30_000,
    private val readTimeoutMillis: Int = 30_000,
) {
  /**
   * Appends the bytes of [url] that [partial] does not hold yet, never growing it past
   * [totalBytes].
   *
   * When the server ignores the range and sends the whole file, [partial] is rewritten from its
   * start.
   *
   * The content is not verified here.
   *
   * @param totalBytes the pinned length of the file, in bytes.
   * @param onProgress receives the bytes [partial] holds and [totalBytes] after every write.
   * @param isStopRequested consulted before every read.
   * @throws UnexpectedStatusException if the server answers with neither the file nor 416.
   * @throws IOException if the transfer breaks off before [partial] reaches [totalBytes].
   */
  fun fetch(
      url: String,
      partial: File,
      totalBytes: Long,
      onProgress: (bytesSoFar: Long, totalBytes: Long) -> Unit,
      isStopRequested: () -> Boolean,
  ): FetchEnd {
    val offset = partial.length()
    val connection = open(url, offset)
    try {
      val append =
          when (val status = connection.responseCode) {
            HTTP_RANGE_NOT_SATISFIABLE -> return FetchEnd.FINISHED
            HttpURLConnection.HTTP_PARTIAL if offset > 0 -> true
            HttpURLConnection.HTTP_OK -> false
            else -> throw UnexpectedStatusException(status)
          }
      var written = if (append) offset else 0
      connection.inputStream.use { input ->
        FileOutputStream(partial, append).use { output ->
          val buffer = ByteArray(BUFFER_BYTES)
          while (written < totalBytes) {
            if (isStopRequested()) return FetchEnd.STOPPED
            val read =
                input.read(buffer, 0, minOf(buffer.size.toLong(), totalBytes - written).toInt())
            if (read < 0) break
            output.write(buffer, 0, read)
            written += read
            onProgress(written, totalBytes)
          }
        }
      }
      // Checked by length because a fixed-length body cut short can end like a complete one.
      if (written < totalBytes) {
        throw EOFException("$url ended after $written of $totalBytes bytes")
      }
      return FetchEnd.FINISHED
    } finally {
      connection.disconnect()
    }
  }

  private fun open(url: String, offset: Long): HttpURLConnection =
      (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = connectTimeoutMillis
        readTimeout = readTimeoutMillis
        // Stated rather than left to the HTTP stack, because under a content coding a range counts
        // bytes of the encoded body instead of the pinned file.
        setRequestProperty("Accept-Encoding", "identity")
        if (offset > 0) setRequestProperty("Range", "bytes=$offset-")
      }
}

private const val HTTP_RANGE_NOT_SATISFIABLE = 416
private const val BUFFER_BYTES = 64 * 1024
