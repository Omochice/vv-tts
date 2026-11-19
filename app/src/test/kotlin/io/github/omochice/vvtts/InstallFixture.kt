package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.DownloadResult
import io.github.omochice.vvtts.assets.InstallResult
import io.github.omochice.vvtts.assets.RemoteFile
import java.io.IOException

internal fun placeholderFile(fileName: String) =
    RemoteFile(fileName, "https://example.invalid/$fileName", 1, "0".repeat(64))

internal fun transferFailure(file: RemoteFile) =
    InstallResult.DownloadFailed(file, DownloadResult.TransferFailed(IOException()))
