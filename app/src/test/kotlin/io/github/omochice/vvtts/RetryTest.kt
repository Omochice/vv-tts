package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.DownloadResult
import io.github.omochice.vvtts.assets.InstallResult
import io.github.omochice.vvtts.assets.RemoteFile
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RetryTest {
  private val file = RemoteFile("0.vvm", "https://example.invalid/0.vvm", 1, "0".repeat(64))
  private val transferFailed =
      InstallResult.DownloadFailed(file, DownloadResult.TransferFailed(IOException()))
  private val corrupt = InstallResult.DownloadFailed(file, DownloadResult.Corrupt)

  @Test
  fun `a transfer that broke off is retryable`() {
    assertTrue(isRetryable(transferFailed))
  }

  @Test
  fun `a completed install is not retryable`() {
    assertFalse(isRetryable(InstallResult.Installed))
  }

  @Test
  fun `a stopped install is not retryable`() {
    assertFalse(isRetryable(InstallResult.Stopped))
  }

  @Test
  fun `a file not matching its pin is not retryable`() {
    assertFalse(isRetryable(corrupt))
  }

  @Test
  fun `an unexpected HTTP status is not retryable`() {
    assertFalse(
        isRetryable(InstallResult.DownloadFailed(file, DownloadResult.UnexpectedStatus(404)))
    )
  }

  @Test
  fun `a placement failure is not retryable`() {
    assertFalse(isRetryable(InstallResult.PlacementFailed(file, IOException())))
  }

  @Test
  fun `a device without an ONNX Runtime build is not retryable`() {
    assertFalse(isRetryable(InstallResult.NoOnnxRuntimeFor("mips")))
  }

  /** Runs [installWithRetries] over [results] in turn, then installs, and records what it did. */
  private class Recorder(results: List<InstallResult>, private val stopAfterPause: Int? = null) {
    private val remaining = ArrayDeque(results)
    var attempts = 0
    var pauses = 0
    val retries = mutableListOf<Pair<Int, InstallResult.Failure>>()

    fun run(maxAttempts: Int = 5): InstallResult =
        installWithRetries(
            maxAttempts,
            attempt = {
              attempts++
              remaining.removeFirstOrNull() ?: InstallResult.Installed
            },
            pause = { pauses++ },
            isStopRequested = { stopAfterPause != null && pauses >= stopAfterPause },
            onRetry = { failedAttempt, failure -> retries += failedAttempt to failure },
        )
  }

  @Test
  fun `an attempt that installs is not repeated`() {
    val recorder = Recorder(listOf(InstallResult.Installed))

    assertEquals(InstallResult.Installed, recorder.run())
    assertEquals(1, recorder.attempts)
    assertEquals(0, recorder.pauses)
  }

  @Test
  fun `a broken-off transfer is attempted again after a pause`() {
    val recorder = Recorder(listOf(transferFailed, InstallResult.Installed))

    assertEquals(InstallResult.Installed, recorder.run())
    assertEquals(2, recorder.attempts)
    assertEquals(1, recorder.pauses)
    assertEquals(listOf<Pair<Int, InstallResult.Failure>>(1 to transferFailed), recorder.retries)
  }

  @Test
  fun `attempts stop at the maximum and the last failure is returned`() {
    val recorder = Recorder(List(3) { transferFailed })

    assertEquals(transferFailed, recorder.run(maxAttempts = 3))
    assertEquals(3, recorder.attempts)
    assertEquals(2, recorder.pauses)
    assertEquals(listOf(1, 2), recorder.retries.map { it.first })
  }

  @Test
  fun `a failure that is not retryable is returned at once`() {
    val recorder = Recorder(listOf(corrupt, InstallResult.Installed))

    assertEquals(corrupt, recorder.run())
    assertEquals(1, recorder.attempts)
    assertEquals(0, recorder.pauses)
  }

  @Test
  fun `a stop requested during the pause ends without another attempt`() {
    val recorder = Recorder(listOf(transferFailed, InstallResult.Installed), stopAfterPause = 1)

    assertEquals(InstallResult.Stopped, recorder.run())
    assertEquals(1, recorder.attempts)
  }

  @Test
  fun `a stopped attempt is not repeated`() {
    val recorder = Recorder(listOf(InstallResult.Stopped, InstallResult.Installed))

    assertEquals(InstallResult.Stopped, recorder.run())
    assertEquals(1, recorder.attempts)
  }
}
