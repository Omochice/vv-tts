package io.github.omochice.vvtts.assets

import java.io.File
import java.io.IOException

/** What became of one [AssetInstaller.install] call. */
sealed interface InstallResult {
  /** Everything needed to speak with the requested voice model is present. */
  data object Installed : InstallResult

  /** A stop was requested; downloads received so far are kept for the next call to resume. */
  data object Stopped : InstallResult

  /** The call gave up; parts installed before the failure stay installed. */
  sealed interface Failure : InstallResult

  /** The catalog has no ONNX Runtime build for the Android ABI named [abi]. */
  data class NoOnnxRuntimeFor(val abi: String) : Failure

  /**
   * Downloading [file] ended with [result], which is never [DownloadResult.Installed] or
   * [DownloadResult.Stopped].
   */
  data class DownloadFailed(val file: RemoteFile, val result: DownloadResult) : Failure

  /** [file] was downloaded but could not be verified, unpacked or moved into place. */
  data class PlacementFailed(val file: RemoteFile, val cause: IOException) : Failure
}

/**
 * Brings voice models to a speakable state by installing whatever [layout] lacks from [catalog].
 *
 * @param abi the Android ABI name, such as `arm64-v8a`, whose ONNX Runtime build is installed.
 */
class AssetInstaller(
    private val catalog: AssetCatalog,
    private val layout: AssetLayout,
    private val abi: String,
    private val downloader: AssetDownloader = AssetDownloader(),
) {
  /**
   * Installs the ONNX Runtime library, the dictionary and the voice model named
   * [voiceModelFileName], skipping each one [layout] already has.
   *
   * Calls run one at a time across the whole process, so that two calls never append to the same
   * partial download.
   *
   * @param onProgress receives the bytes this call has downloaded so far, which never decrease, and
   *   the total it has to download, both in bytes.
   * @param isStopRequested consulted before every network read.
   * @throws IllegalArgumentException if [catalog] has no voice model named [voiceModelFileName].
   */
  fun install(
      voiceModelFileName: String,
      onProgress: (doneBytes: Long, totalBytes: Long) -> Unit,
      isStopRequested: () -> Boolean,
  ): InstallResult {
    val voiceModel = voiceModelNamed(voiceModelFileName)
    synchronized(installLock) {
      if (!layout.hasOnnxRuntime() && abi !in catalog.onnxRuntimeArchives) {
        return InstallResult.NoOnnxRuntimeFor(abi)
      }
      val parts = missingParts(voiceModel)
      val progress = Progress(parts.downloadBytes(), onProgress)
      for (part in parts) {
        installPart(part, progress, isStopRequested)?.let {
          return it
        }
      }
      return InstallResult.Installed
    }
  }

  /**
   * Returns how many bytes an [install] of the voice model named [voiceModelFileName] would
   * download now, which is also the total it reports progress against.
   *
   * @throws IllegalArgumentException if [catalog] has no voice model named [voiceModelFileName].
   */
  fun bytesToDownload(voiceModelFileName: String): Long =
      missingParts(voiceModelNamed(voiceModelFileName)).downloadBytes()

  private fun voiceModelNamed(fileName: String): VoiceModel {
    val voiceModel = catalog.voiceModels.find { it.file.fileName == fileName }
    requireNotNull(voiceModel) { "the catalog has no voice model named $fileName" }
    return voiceModel
  }

  /** Lists what [layout] lacks, leaving out an ONNX Runtime the catalog has no build of. */
  private fun missingParts(voiceModel: VoiceModel): List<Part> = buildList {
    if (!layout.hasOnnxRuntime()) {
      catalog.onnxRuntimeArchives[abi]?.let { add(archivePart(it, layout.onnxRuntimeDir)) }
    }
    if (!layout.hasDictionary()) {
      add(archivePart(catalog.dictionaryArchive, layout.dictionaryDir))
    }
    val fileName = voiceModel.file.fileName
    if (!layout.hasVoiceModel(fileName)) {
      add(Part(voiceModel.file, layout.voiceModel(fileName), extractTo = null))
    }
  }

  private fun List<Part>.downloadBytes(): Long =
      filterNot { it.download.exists() }.sumOf { it.remote.sizeBytes }

  private fun archivePart(archive: RemoteFile, extractTo: File) =
      Part(archive, File(layout.downloadsDir, archive.fileName), extractTo)

  /** Returns null once [part] is in place, or the result that ends the call. */
  private fun installPart(
      part: Part,
      progress: Progress,
      isStopRequested: () -> Boolean,
  ): InstallResult? {
    val needsDownload = !part.download.exists()
    val downloaded =
        try {
          part.download.parentFile?.mkdirs()
          downloader.download(
              part.remote,
              part.download,
              { bytesSoFar, _ -> progress.reportCurrentFile(bytesSoFar) },
              isStopRequested,
          )
        } catch (e: IOException) {
          return InstallResult.PlacementFailed(part.remote, e)
        }
    when (downloaded) {
      DownloadResult.Installed -> Unit
      DownloadResult.Stopped -> return InstallResult.Stopped
      else -> return InstallResult.DownloadFailed(part.remote, downloaded)
    }
    if (needsDownload) progress.completeFile(part.remote.sizeBytes)
    val extractTo = part.extractTo ?: return null
    try {
      installExtracted(part.download, extractTo)
    } catch (e: MalformedArchiveException) {
      // Discarded rather than kept for the next call, because a verified archive that does not
      // extract would fail the same way every time.
      part.download.delete()
      return InstallResult.PlacementFailed(part.remote, e)
    } catch (e: IOException) {
      return InstallResult.PlacementFailed(part.remote, e)
    }
    // A failed delete is ignored, because the part is already in place and the leftover archive
    // only takes space.
    part.download.delete()
    return null
  }

  /**
   * One missing part: [remote] downloaded to [download], then extracted to [extractTo] unless that
   * is null.
   */
  private class Part(val remote: RemoteFile, val download: File, val extractTo: File?)

  private class Progress(
      private val totalBytes: Long,
      private val onProgress: (doneBytes: Long, totalBytes: Long) -> Unit,
  ) {
    private var completedBytes = 0L

    fun reportCurrentFile(bytesSoFar: Long) = onProgress(completedBytes + bytesSoFar, totalBytes)

    fun completeFile(sizeBytes: Long) {
      completedBytes += sizeBytes
      onProgress(completedBytes, totalBytes)
    }
  }
}

private val installLock = Any()
