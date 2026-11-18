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
  private val file = RemoteFile("0.vvm", "https://example.invalid/0.vvm", 1, "0".repeat(64))
  private val transferFailed =
      InstallResult.DownloadFailed(file, DownloadResult.TransferFailed(IOException()))

  @Test
  fun `nothing installed and nothing running is not installed`() {
    assertEquals(
        SettingsStatus.NotInstalled,
        settingsStatusOf(InstallState.Idle, isJobPending = false, isInstalled = false),
    )
  }

  @Test
  fun `assets in place with nothing running are installed`() {
    assertEquals(
        SettingsStatus.Installed,
        settingsStatusOf(InstallState.Idle, isJobPending = false, isInstalled = true),
    )
  }

  @Test
  fun `a scheduled job that has not started is pending`() {
    assertEquals(
        SettingsStatus.Pending,
        settingsStatusOf(InstallState.Idle, isJobPending = true, isInstalled = false),
    )
  }

  @Test
  fun `a failed run that the system will retry is pending`() {
    assertEquals(
        SettingsStatus.Pending,
        settingsStatusOf(InstallState.Finished(transferFailed), isJobPending = true, false),
    )
  }

  @Test
  fun `a run that gave up retrying a broken-off transfer is failed`() {
    assertEquals(
        SettingsStatus.Failed(FailureReason.TRANSFER),
        settingsStatusOf(InstallState.Finished(transferFailed), isJobPending = false, false),
    )
  }

  @Test
  fun `a run waiting to retry is retrying, whatever the system lists`() {
    assertEquals(
        SettingsStatus.Retrying(2, 5),
        settingsStatusOf(InstallState.Retrying(2, 5), isJobPending = true, isInstalled = false),
    )
  }

  @Test
  fun `a run without a known total is preparing`() {
    assertEquals(
        SettingsStatus.Preparing,
        settingsStatusOf(InstallState.Running(0, 0), isJobPending = true, isInstalled = false),
    )
  }

  @Test
  fun `a run with a known total is downloading`() {
    assertEquals(
        SettingsStatus.Downloading(10, 100),
        settingsStatusOf(InstallState.Running(10, 100), isJobPending = true, isInstalled = false),
    )
  }

  @Test
  fun `a failed run that is not retried is failed for its reason`() {
    assertEquals(
        SettingsStatus.Failed(FailureReason.CORRUPT),
        settingsStatusOf(
            InstallState.Finished(InstallResult.DownloadFailed(file, DownloadResult.Corrupt)),
            isJobPending = false,
            isInstalled = false,
        ),
    )
  }

  @Test
  fun `a stopped run that the system will not resume is stopped`() {
    assertEquals(
        SettingsStatus.Stopped,
        settingsStatusOf(InstallState.Finished(InstallResult.Stopped), false, isInstalled = false),
    )
  }

  @Test
  fun `assets in place outweigh an earlier failure`() {
    assertEquals(
        SettingsStatus.Installed,
        settingsStatusOf(
            InstallState.Finished(InstallResult.NoOnnxRuntimeFor("mips")),
            isJobPending = false,
            isInstalled = true,
        ),
    )
  }

  @Test
  fun `a completed run whose assets are gone is not installed`() {
    assertEquals(
        SettingsStatus.NotInstalled,
        settingsStatusOf(InstallState.Finished(InstallResult.Installed), false, false),
    )
  }

  @Test
  fun `a download may start only when nothing is installed, scheduled or running`() {
    assertTrue(SettingsStatus.NotInstalled.allowsDownload)
    assertTrue(SettingsStatus.Stopped.allowsDownload)
    assertTrue(SettingsStatus.Failed(FailureReason.SERVER).allowsDownload)
    assertFalse(SettingsStatus.Installed.allowsDownload)
    assertFalse(SettingsStatus.Pending.allowsDownload)
    assertFalse(SettingsStatus.Preparing.allowsDownload)
    assertFalse(SettingsStatus.Downloading(1, 2).allowsDownload)
    assertFalse(SettingsStatus.Retrying(2, 5).allowsDownload)
  }
}
