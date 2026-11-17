package io.github.omochice.vvtts.assets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AssetCatalogTest {
  private val catalog = AssetCatalog.load()

  @Test
  fun `the dictionary archive is the pinned OpenJTalk release asset`() {
    assertEquals(
        RemoteFile(
            fileName = "open_jtalk_dic_utf_8-1.11.tar.gz",
            url =
                "https://github.com/r9y9/open_jtalk/releases/download/v1.11.1/open_jtalk_dic_utf_8-1.11.tar.gz",
            sizeBytes = 23646843,
            sha256 = "fe6ba0e43542cef98339abdffd903e062008ea170b04e7e2a35da805902f382a",
        ),
        catalog.dictionaryArchive,
    )
  }

  @Test
  fun `the terms documents are the README and TERMS of the voice model release`() {
    assertEquals(listOf("README.txt", "TERMS.txt"), catalog.terms.map { it.fileName })
    assertEquals(
        "https://github.com/VOICEVOX/voicevox_vvm/releases/download/0.17.1/TERMS.txt",
        catalog.terms[1].url,
    )
    assertEquals(
        "9c0caf98b31ef82e3686e8fca39d53c85965059d2206be3c83efc3751d32c83a",
        catalog.terms[1].sha256,
    )
  }

  @Test
  fun `voice models are the talk VVM files in numeric order`() {
    assertEquals((0..25).map { "$it.vvm" }, catalog.voiceModels.map { it.file.fileName })
    assertEquals(
        RemoteFile(
            fileName = "15.vvm",
            url = "https://github.com/VOICEVOX/voicevox_vvm/releases/download/0.17.1/15.vvm",
            sizeBytes = 67265793,
            sha256 = "c29b0c0f8e332fcedc2a5de4b4426feb75fc9f587c314da99af8dbc964287221",
        ),
        catalog.voiceModels[15].file,
    )
  }

  @Test
  fun `each voice model lists the voices of the README talk table in table order`() {
    assertEquals(listOf(Voice("冥鳴ひまり", "ノーマル", 14)), catalog.voiceModels[1].voices)
    assertEquals(
        listOf(Voice("あんこもん", "ささやき", 117)),
        catalog.voiceModels[23].voices,
    )
    assertEquals(Voice("四国めたん", "ノーマル", 2), catalog.voiceModels[0].voices.first())
    assertTrue(Voice("ずんだもん", "ノーマル", 3) in catalog.voiceModels[0].voices)
    assertTrue(Voice("†聖騎士 紅桜†", "ノーマル", 51) in catalog.voiceModels[12].voices)
  }

  @Test
  fun `one character's styles can be spread over several voice models`() {
    val zundamon =
        catalog.voiceModels
            .filter { model -> model.voices.any { it.character == "ずんだもん" } }
            .map { it.file.fileName }

    assertEquals(listOf("0.vvm", "5.vvm", "15.vvm"), zundamon)
  }

  @Test
  fun `each supported Android ABI has its own ONNX Runtime archive`() {
    val base =
        "https://github.com/VOICEVOX/onnxruntime-builder/releases/download/voicevox_onnxruntime-1.23.2"
    assertEquals(
        mapOf(
            "arm64-v8a" to
                RemoteFile(
                    fileName = "voicevox_onnxruntime-android-arm64-1.23.2.tgz",
                    url = "$base/voicevox_onnxruntime-android-arm64-1.23.2.tgz",
                    sizeBytes = 6459115,
                    sha256 = "43e6fc2a89ea6412d4cc20215042a3b34da22fdf05c1bb76256a686f82378d46",
                ),
            "x86_64" to
                RemoteFile(
                    fileName = "voicevox_onnxruntime-android-x64-1.23.2.tgz",
                    url = "$base/voicevox_onnxruntime-android-x64-1.23.2.tgz",
                    sizeBytes = 7115144,
                    sha256 = "807e2ea50dfb2a309416adb1cee6edc53d3d8eae1d702ad4e6f199a0f91f2d24",
                ),
        ),
        catalog.onnxRuntimeArchives,
    )
  }

  @Test
  fun `asset rows without ONNX Runtime still give every other entry`() {
    val assets = resourceLines("assets.tsv").filterNot { "onnxruntime" in it }

    val withoutOnnxRuntime = parse(assets, resourceLines("voices.tsv"))

    assertEquals(emptyMap(), withoutOnnxRuntime.onnxRuntimeArchives)
    assertEquals(catalog.dictionaryArchive, withoutOnnxRuntime.dictionaryArchive)
    assertEquals(catalog.terms, withoutOnnxRuntime.terms)
    assertEquals(catalog.voiceModels, withoutOnnxRuntime.voiceModels)
  }

  @Test
  fun `an asset row of no known kind is rejected`() {
    val assets =
        resourceLines("assets.tsv") +
            "voicevox_vvm/0.17.1\ts0.vvm\t1\t${"0".repeat(64)}\thttps://github.com/x/s0.vvm"

    assertFailsWith<IllegalArgumentException> { parse(assets, resourceLines("voices.tsv")) }
  }

  @Test
  fun `a voice of a model missing from the asset rows is rejected`() {
    val voices = resourceLines("voices.tsv") + "26.vvm\t誰か\tノーマル\t999"

    assertFailsWith<IllegalArgumentException> { parse(resourceLines("assets.tsv"), voices) }
  }

  @Test
  fun `a table whose columns differ from the expected ones is rejected`() {
    val voices =
        listOf("file_name\tstyle\tcharacter\tstyle_id") + resourceLines("voices.tsv").drop(1)

    assertFailsWith<IllegalArgumentException> { parse(resourceLines("assets.tsv"), voices) }
  }

  private val allFiles =
      listOf(catalog.dictionaryArchive) +
          catalog.terms +
          catalog.voiceModels.map { it.file } +
          catalog.onnxRuntimeArchives.values

  @Test
  fun `every asset row of the source release data is in the catalog`() {
    assertEquals(31, allFiles.size)
  }

  @Test
  fun `every pinned hash is 64 lowercase hex characters`() {
    val bad = allFiles.filterNot { Regex("[0-9a-f]{64}").matches(it.sha256) }

    assertEquals(emptyList(), bad)
  }

  @Test
  fun `every URL is a GitHub https URL ending with the file name`() {
    val bad = allFiles.filterNot {
      it.url.startsWith("https://github.com/") && it.url.endsWith("/${it.fileName}")
    }

    assertEquals(emptyList(), bad)
  }

  @Test
  fun `every pinned size is positive`() {
    assertEquals(emptyList(), allFiles.filterNot { it.sizeBytes > 0 })
  }

  @Test
  fun `file names are unique`() {
    val names = allFiles.map { it.fileName }

    assertEquals(names.distinct(), names)
  }

  @Test
  fun `every voice model has at least one voice`() {
    assertEquals(emptyList(), catalog.voiceModels.filter { it.voices.isEmpty() })
  }

  @Test
  fun `style ids are unique across the catalog`() {
    val ids = catalog.voiceModels.flatMap { model -> model.voices.map { it.styleId } }

    assertEquals(128, ids.size)
    assertEquals(ids.distinct(), ids)
  }

  private fun resourceLines(name: String): List<String> =
      AssetCatalog::class.java.getResourceAsStream(name)!!.reader().readLines()

  private fun parse(assets: List<String>, voices: List<String>): AssetCatalog =
      AssetCatalog.parse(
          assets.joinToString("\n").reader(),
          voices.joinToString("\n").reader(),
      )
}
