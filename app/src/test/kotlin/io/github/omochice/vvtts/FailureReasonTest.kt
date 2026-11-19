package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.DownloadResult
import io.github.omochice.vvtts.assets.InstallResult
import io.github.omochice.vvtts.assets.RemoteFile
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class FailureReasonTest {
  private val file = RemoteFile("0.vvm", "https://example.invalid/0.vvm", 1, "0".repeat(64))

  private fun downloadFailed(result: DownloadResult.Failure) =
      InstallResult.DownloadFailed(file, result)

  @Test
  fun `a transfer that broke off is a transfer failure`() {
    assertEquals(
        FailureReason.TRANSFER,
        failureReasonOf(downloadFailed(DownloadResult.TransferFailed(IOException()))),
    )
  }

  @Test
  fun `an unexpected HTTP status is a server failure`() {
    assertEquals(
        FailureReason.SERVER,
        failureReasonOf(downloadFailed(DownloadResult.UnexpectedStatus(503))),
    )
  }

  @Test
  fun `a file not matching its pin is corrupt`() {
    assertEquals(FailureReason.CORRUPT, failureReasonOf(downloadFailed(DownloadResult.Corrupt)))
  }

  @Test
  fun `a placement failure is a storage failure`() {
    assertEquals(
        FailureReason.STORAGE,
        failureReasonOf(InstallResult.PlacementFailed(file, IOException())),
    )
  }

  @Test
  fun `a missing ONNX Runtime build means the device is unsupported`() {
    assertEquals(
        FailureReason.UNSUPPORTED_DEVICE,
        failureReasonOf(InstallResult.NoOnnxRuntimeFor("mips")),
    )
  }
}
