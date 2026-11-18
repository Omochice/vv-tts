package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.DownloadResult
import io.github.omochice.vvtts.assets.InstallResult
import io.github.omochice.vvtts.assets.RemoteFile
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsStatusTest {
  private val file = RemoteFile("5.vvm", "https://example.invalid/5.vvm", 1, "0".repeat(64))
  private val transferFailed =
      InstallResult.DownloadFailed(file, DownloadResult.TransferFailed(IOException()))

  private fun finished(result: InstallResult) = InstallState.Finished("5.vvm", result)

  @Test
  fun `nothing installed and nothing running is not installed`() {
    assertEquals(
        SettingsStatus.NotInstalled,
        settingsStatusOf(InstallState.Idle, pendingVoiceModel = null, isInstalled = false),
    )
  }

  @Test
  fun `assets in place with nothing running are installed`() {
    assertEquals(
        SettingsStatus.Installed,
        settingsStatusOf(InstallState.Idle, pendingVoiceModel = null, isInstalled = true),
    )
  }

  @Test
  fun `a scheduled job that has not started is pending for the voice model it names`() {
    assertEquals(
        SettingsStatus.Pending("5.vvm"),
        settingsStatusOf(InstallState.Idle, pendingVoiceModel = "5.vvm", isInstalled = false),
    )
  }

  @Test
  fun `a failed run that the system will retry is pending`() {
    assertEquals(
        SettingsStatus.Pending("5.vvm"),
        settingsStatusOf(finished(transferFailed), pendingVoiceModel = "5.vvm", false),
    )
  }

  @Test
  fun `a run that gave up retrying a broken-off transfer is failed`() {
    assertEquals(
        SettingsStatus.Failed(FailureReason.TRANSFER),
        settingsStatusOf(finished(transferFailed), pendingVoiceModel = null, false),
    )
  }

  @Test
  fun `a run waiting to retry is retrying, whatever the system lists`() {
    assertEquals(
        SettingsStatus.Retrying("5.vvm", 2, 5),
        settingsStatusOf(InstallState.Retrying("5.vvm", 2, 5), "5.vvm", isInstalled = false),
    )
  }

  @Test
  fun `a run without a known total is preparing`() {
    assertEquals(
        SettingsStatus.Preparing("5.vvm"),
        settingsStatusOf(InstallState.Running("5.vvm", 0, 0), "5.vvm", isInstalled = false),
    )
  }

  @Test
  fun `a run with a known total is downloading`() {
    assertEquals(
        SettingsStatus.Downloading("5.vvm", 10, 100),
        settingsStatusOf(InstallState.Running("5.vvm", 10, 100), "5.vvm", isInstalled = false),
    )
  }

  @Test
  fun `a run names its own voice model rather than the one the system lists`() {
    assertEquals(
        SettingsStatus.Downloading("5.vvm", 10, 100),
        settingsStatusOf(InstallState.Running("5.vvm", 10, 100), "0.vvm", isInstalled = false),
    )
  }

  @Test
  fun `a failed run that is not retried is failed for its reason`() {
    assertEquals(
        SettingsStatus.Failed(FailureReason.CORRUPT),
        settingsStatusOf(
            finished(InstallResult.DownloadFailed(file, DownloadResult.Corrupt)),
            pendingVoiceModel = null,
            isInstalled = false,
        ),
    )
  }

  @Test
  fun `a stopped run that the system will not resume is stopped`() {
    assertEquals(
        SettingsStatus.Stopped,
        settingsStatusOf(finished(InstallResult.Stopped), null, isInstalled = false),
    )
  }

  @Test
  fun `a failed download shows although another voice model is installed`() {
    assertEquals(
        SettingsStatus.Failed(FailureReason.UNSUPPORTED_DEVICE),
        settingsStatusOf(
            finished(InstallResult.NoOnnxRuntimeFor("mips")),
            pendingVoiceModel = null,
            isInstalled = true,
        ),
    )
  }

  @Test
  fun `a stopped download shows although another voice model is installed`() {
    assertEquals(
        SettingsStatus.Stopped,
        settingsStatusOf(finished(InstallResult.Stopped), null, isInstalled = true),
    )
  }

  @Test
  fun `a completed run is installed`() {
    assertEquals(
        SettingsStatus.Installed,
        settingsStatusOf(finished(InstallResult.Installed), null, isInstalled = true),
    )
  }

  @Test
  fun `a completed run whose assets are gone is not installed`() {
    assertEquals(
        SettingsStatus.NotInstalled,
        settingsStatusOf(finished(InstallResult.Installed), null, isInstalled = false),
    )
  }

  @Test
  fun `a download may start only when none is scheduled or running`() {
    assertTrue(SettingsStatus.NotInstalled.allowsDownload)
    assertTrue(SettingsStatus.Installed.allowsDownload)
    assertTrue(SettingsStatus.Stopped.allowsDownload)
    assertTrue(SettingsStatus.Failed(FailureReason.SERVER).allowsDownload)
    assertFalse(SettingsStatus.Pending("5.vvm").allowsDownload)
    assertFalse(SettingsStatus.Preparing("5.vvm").allowsDownload)
    assertFalse(SettingsStatus.Downloading("5.vvm", 1, 2).allowsDownload)
    assertFalse(SettingsStatus.Retrying("5.vvm", 2, 5).allowsDownload)
  }
}
