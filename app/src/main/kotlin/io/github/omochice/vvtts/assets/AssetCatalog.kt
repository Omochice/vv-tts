package io.github.omochice.vvtts.assets

import java.io.Reader

/** One speaking style of one character, as VOICEVOX addresses it by [styleId]. */
data class Voice(val character: String, val style: String, val styleId: Int)

/** A VVM voice model file and the voices it can synthesize. */
data class VoiceModel(val file: RemoteFile, val voices: List<Voice>)

/**
 * The pinned set of remote files the app downloads after the user accepts their terms.
 *
 * @property dictionaryArchive the gzipped tar of the OpenJTalk dictionary.
 * @property terms the documents whose terms the user accepts before voice models are downloaded.
 * @property voiceModels the talk voice models, ordered by the number in their file names.
 * @property onnxRuntimeArchives the gzipped tar of the ONNX Runtime library for each Android ABI
 *   name, such as `arm64-v8a`; empty when the asset table lists none.
 */
class AssetCatalog(
    val dictionaryArchive: RemoteFile,
    val terms: List<RemoteFile>,
    val voiceModels: List<VoiceModel>,
    val onnxRuntimeArchives: Map<String, RemoteFile>,
) {
  companion object {
    /**
     * Reads the catalog bundled with the app.
     *
     * @throws IllegalArgumentException if the bundled tables are inconsistent, as [parse] does.
     */
    fun load(): AssetCatalog =
        resource("assets.tsv").use { assets ->
          resource("voices.tsv").use { voices -> parse(assets, voices) }
        }

    /**
     * Builds a catalog from the tab-separated asset table and voice table, each with a header line.
     *
     * @throws IllegalArgumentException if a header or row is malformed, an asset row is of no known
     *   kind, there is not exactly one dictionary, or a voice names a model absent from [assets].
     */
    fun parse(assets: Reader, voices: Reader): AssetCatalog {
      val voicesByModel =
          readTable(voices, VOICE_COLUMNS)
              .groupBy(
                  { it[0] },
                  { Voice(character = it[1], style = it[2], styleId = it[3].toInt()) },
              )
      val entries =
          readTable(assets, ASSET_COLUMNS).map { row ->
            classify(release = row[0], RemoteFile(row[1], row[4], row[2].toLong(), row[3]))
          }
      val dictionaries = entries.filterIsInstance<Entry.Dictionary>()
      require(dictionaries.size == 1) { "expected one dictionary, found ${dictionaries.size}" }
      val models = entries.filterIsInstance<Entry.Model>().map { it.file }
      val orphans = voicesByModel.keys - models.map { it.fileName }.toSet()
      require(orphans.isEmpty()) { "voices name models missing from the asset rows: $orphans" }
      return AssetCatalog(
          dictionaryArchive = dictionaries.single().file,
          terms = entries.filterIsInstance<Entry.Terms>().map { it.file },
          voiceModels =
              models
                  .sortedBy { it.fileName.removeSuffix(".vvm").toInt() }
                  .map { VoiceModel(it, voicesByModel[it.fileName].orEmpty()) },
          onnxRuntimeArchives =
              entries.filterIsInstance<Entry.OnnxRuntime>().associate { it.abi to it.file },
      )
    }

    private fun readTable(text: Reader, columns: List<String>): List<List<String>> {
      val lines = text.readLines()
      require(lines.firstOrNull()?.split('\t') == columns) {
        "expected columns $columns, found ${lines.firstOrNull()}"
      }
      return lines.drop(1).map { line ->
        line.split('\t').also { require(it.size == columns.size) { "malformed row: $line" } }
      }
    }

    private sealed interface Entry {
      data class Dictionary(val file: RemoteFile) : Entry

      data class Terms(val file: RemoteFile) : Entry

      data class Model(val file: RemoteFile) : Entry

      data class OnnxRuntime(val abi: String, val file: RemoteFile) : Entry
    }

    /**
     * Classifies by release and file name rather than by a kind column in the table, so that the
     * table stays a plain copy of the release listings and a version bump only regenerates it.
     */
    private fun classify(release: String, file: RemoteFile): Entry {
      val name = file.fileName
      return when {
        release.startsWith("open_jtalk/") -> Entry.Dictionary(file)
        release.startsWith("voicevox_vvm/") && name in TERMS_FILE_NAMES -> Entry.Terms(file)
        release.startsWith("voicevox_vvm/") && TALK_MODEL.matches(name) -> Entry.Model(file)
        release.startsWith("onnxruntime-builder/") -> {
          val abi = ONNX_RUNTIME_ABIS.entries.find { (arch, _) -> "-android-$arch-" in name }
          requireNotNull(abi) { "no Android ABI for $name" }
          Entry.OnnxRuntime(abi.value, file)
        }
        else -> throw IllegalArgumentException("$name of $release is of no known kind")
      }
    }

    private fun resource(name: String): Reader =
        checkNotNull(AssetCatalog::class.java.getResourceAsStream(name)) { "missing $name" }
            .reader(Charsets.UTF_8)

    private val ASSET_COLUMNS = listOf("release", "file_name", "size_bytes", "sha256", "url")
    private val VOICE_COLUMNS = listOf("file_name", "character", "style", "style_id")
    private val TERMS_FILE_NAMES = setOf("README.txt", "TERMS.txt")
    private val TALK_MODEL = Regex("""\d+\.vvm""")

    /** Architecture names in the ONNX Runtime archive names, mapped to Android ABI names. */
    private val ONNX_RUNTIME_ABIS = mapOf("arm64" to "arm64-v8a", "x64" to "x86_64")
  }
}
