package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.AssetCatalog
import io.github.omochice.vvtts.assets.AssetDownloader
import io.github.omochice.vvtts.assets.AssetLayout
import io.github.omochice.vvtts.assets.DownloadResult
import io.github.omochice.vvtts.assets.onnxRuntimeTermsIn
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONException
import org.json.JSONObject

/** One terms document, shown verbatim under [title]. */
data class TermsDocument(val title: String, val text: String)

/**
 * The terms the user agrees to before the download.
 *
 * @property voiceModelTerms the documents published with the voice models.
 * @property onnxRuntimeTerms the terms published with the ONNX Runtime release.
 * @property fingerprint the [consentFingerprint] that agreeing to these terms records.
 */
class Terms(
    val voiceModelTerms: List<TermsDocument>,
    val onnxRuntimeTerms: String,
    val fingerprint: String,
)

/** Fetches the terms of the assets in [catalog], keeping the voice model ones under [layout]. */
class TermsLoader(
    private val catalog: AssetCatalog,
    private val layout: AssetLayout,
    private val downloader: AssetDownloader = AssetDownloader(),
) {
  /**
   * Fetches every terms document, reusing voice model documents fetched before.
   *
   * @throws IOException if any document cannot be fetched, or the release notes carry no terms.
   */
  fun load(): Terms {
    val releaseApiUrl =
        catalog.onnxRuntimeReleaseApiUrl ?: throw IOException("the catalog names no ONNX Runtime")
    layout.termsDir.mkdirs()
    val voiceModelTerms =
        catalog.terms.map { remote ->
          val target = File(layout.termsDir, remote.fileName)
          val result = downloader.download(remote, target, isStopRequested = { false })
          if (result != DownloadResult.Installed) throw IOException("${remote.fileName}: $result")
          TermsDocument(remote.fileName, target.readText(Charsets.UTF_8))
        }
    val notes = releaseNotes(releaseApiUrl)
    val onnxRuntimeTerms =
        onnxRuntimeTermsIn(notes) ?: throw IOException("no terms in the notes of $releaseApiUrl")
    return Terms(
        voiceModelTerms,
        onnxRuntimeTerms,
        consentFingerprint(catalog.terms, releaseApiUrl),
    )
  }

  private fun releaseNotes(releaseApiUrl: String): String {
    val connection =
        (URL(releaseApiUrl).openConnection() as HttpURLConnection).apply {
          connectTimeout = TIMEOUT_MILLIS
          readTimeout = TIMEOUT_MILLIS
          setRequestProperty("Accept", "application/vnd.github+json")
          // GitHub rejects requests without one; a fixed product name carries nothing about the
          // user.
          setRequestProperty("User-Agent", "vv-tts")
        }
    try {
      val status = connection.responseCode
      if (status != HttpURLConnection.HTTP_OK) throw IOException("$releaseApiUrl: HTTP $status")
      val body = connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
      return try {
        JSONObject(body).getString("body")
      } catch (e: JSONException) {
        throw IOException("$releaseApiUrl: no release notes", e)
      }
    } finally {
      connection.disconnect()
    }
  }

  private companion object {
    const val TIMEOUT_MILLIS = 30_000
  }
}
