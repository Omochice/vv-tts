package io.github.omochice.vvtts.assets

import java.io.File

/**
 * Where the installed speech assets live under [root].
 *
 * @property onnxRuntimeLibrary the ONNX Runtime shared library VOICEVOX CORE loads by path.
 * @property dictionaryDir the directory holding the OpenJTalk dictionary files directly.
 */
class AssetLayout(root: File) {
  val onnxRuntimeLibrary = File(root, "ort/lib/libvoicevox_onnxruntime.so")
  val dictionaryDir = File(root, "dict")
  private val voiceModelDir = File(root, "vvm")

  /** Returns where the voice model named [fileName], such as `0.vvm`, is installed. */
  fun voiceModel(fileName: String): File = File(voiceModelDir, fileName)

  /**
   * Tells whether everything needed to speak with the voice model [voiceModelFileName] is present.
   */
  fun isInstalled(voiceModelFileName: String): Boolean =
      onnxRuntimeLibrary.isFile &&
          !dictionaryDir.list().isNullOrEmpty() &&
          voiceModel(voiceModelFileName).isFile
}
