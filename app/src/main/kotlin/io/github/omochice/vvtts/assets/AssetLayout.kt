package io.github.omochice.vvtts.assets

import java.io.File

/**
 * Where the installed speech assets live under [root].
 *
 * @property onnxRuntimeDir the directory the ONNX Runtime archive is extracted into.
 * @property onnxRuntimeLibrary the ONNX Runtime shared library VOICEVOX CORE loads by path.
 * @property dictionaryDir the directory holding the OpenJTalk dictionary files directly.
 * @property downloadsDir the directory holding archives while they are downloaded and unpacked.
 * @property termsDir the directory holding the downloaded terms documents.
 */
class AssetLayout(root: File) {
  val onnxRuntimeDir = File(root, "ort")
  val onnxRuntimeLibrary = File(onnxRuntimeDir, "lib/libvoicevox_onnxruntime.so")
  val downloadsDir = File(root, "downloads")
  val termsDir = File(root, "terms")
  val dictionaryDir = File(root, "dict")
  private val voiceModelDir = File(root, "vvm")

  /** Returns where the voice model named [fileName], such as `0.vvm`, is installed. */
  fun voiceModel(fileName: String): File = File(voiceModelDir, fileName)

  /** Tells whether the ONNX Runtime library is present. */
  fun hasOnnxRuntime(): Boolean = onnxRuntimeLibrary.isFile

  /** Tells whether the dictionary directory holds any file. */
  fun hasDictionary(): Boolean = !dictionaryDir.list().isNullOrEmpty()

  /** Tells whether the voice model named [fileName] is present. */
  fun hasVoiceModel(fileName: String): Boolean = voiceModel(fileName).isFile

  /**
   * Tells whether everything needed to speak with the voice model [voiceModelFileName] is present.
   */
  fun isInstalled(voiceModelFileName: String): Boolean =
      hasOnnxRuntime() && hasDictionary() && hasVoiceModel(voiceModelFileName)
}
