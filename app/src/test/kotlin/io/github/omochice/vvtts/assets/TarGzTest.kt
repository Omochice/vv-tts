package io.github.omochice.vvtts.assets

import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.FileAlreadyExistsException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class TarGzTest {
  @get:Rule val tmp = TemporaryFolder()

  private fun extract(archive: ByteArray): File {
    val destination = tmp.newFolder()
    extractTarGz(archive.inputStream(), destination)
    return destination
  }

  @Test
  fun `a file under the top-level directory lands directly under the destination`() {
    val archive = TarFixture().directory("top/").file("top/VERSION_NUMBER", "1.23.2\n").gzip()

    val destination = extract(archive)

    assertEquals("1.23.2\n", File(destination, "VERSION_NUMBER").readText())
  }

  @Test
  fun `a file in a nested directory lands in the same nested directory under the destination`() {
    val archive =
        TarFixture()
            .directory("top/")
            .directory("top/lib/")
            .file("top/lib/libvoicevox_onnxruntime.so", "elf")
            .gzip()

    val destination = extract(archive)

    assertEquals("elf", File(destination, "lib/libvoicevox_onnxruntime.so").readText())
  }

  @Test
  fun `files whose sizes are not multiples of the block size keep their exact contents`() {
    val large = ByteArray(70_000) { (it * 7).toByte() }
    val archive =
        TarFixture()
            .directory("top/")
            .file("top/sys.dic", large)
            .file("top/COPYING", "x")
            .file("top/empty", ByteArray(0))
            .file("top/block", ByteArray(512) { 1 })
            .gzip()

    val destination = extract(archive)

    assertContentEquals(large, File(destination, "sys.dic").readBytes())
    assertEquals("x", File(destination, "COPYING").readText())
    assertEquals(0, File(destination, "empty").length())
    assertContentEquals(ByteArray(512) { 1 }, File(destination, "block").readBytes())
  }

  @Test
  fun `a symbolic link entry is rejected`() {
    val archive = TarFixture().directory("top/").entry("top/link", '2', ByteArray(0)).gzip()

    assertFailsWith<MalformedArchiveException> { extract(archive) }
  }

  @Test
  fun `an entry climbing out of the destination is rejected and nothing is written outside`() {
    val archive = TarFixture().directory("top/").file("top/../escaped", "x").gzip()
    val destination = tmp.newFolder("nested", "destination")

    assertFailsWith<MalformedArchiveException> {
      extractTarGz(archive.inputStream(), destination)
    }
    assertFalse(File(destination.parentFile, "escaped").exists())
  }

  @Test
  fun `an entry under a second top-level directory is rejected`() {
    val archive = TarFixture().directory("top/").file("top/a", "a").file("other/b", "b").gzip()

    assertFailsWith<MalformedArchiveException> { extract(archive) }
  }

  @Test
  fun `a file at the archive root is rejected`() {
    val archive = TarFixture().file("README", "r").gzip()

    assertFailsWith<MalformedArchiveException> { extract(archive) }
  }

  @Test
  fun `an absolute entry path is rejected`() {
    val archive = TarFixture().directory("/top/").file("/top/a", "a").gzip()

    assertFailsWith<MalformedArchiveException> { extract(archive) }
  }

  @Test
  fun `a top-level directory named dot is rejected`() {
    val archive = TarFixture().directory("./").file("./a", "a").gzip()

    assertFailsWith<MalformedArchiveException> { extract(archive) }
  }

  @Test
  fun `an archive cut inside an entry body is rejected`() {
    val tar = TarFixture().directory("top/").file("top/a", ByteArray(2000)).tarBytes()

    assertFailsWith<MalformedArchiveException> {
      extract(TarFixture.gzip(tar.copyOf(512 + 512 + 1000)))
    }
  }

  @Test
  fun `an empty stream is rejected`() {
    assertFailsWith<MalformedArchiveException> { extract(ByteArray(0)) }
  }

  @Test
  fun `a stream that is not gzip is rejected`() {
    val plainTar = TarFixture().directory("top/").file("top/a", "a").tarBytes()

    assertFailsWith<MalformedArchiveException> { extract(plainTar) }
  }

  @Test
  fun `an archive without its end-of-archive block is rejected`() {
    val archive = TarFixture().directory("top/").file("top/a", "a").gzip(endMarker = false)

    assertFailsWith<MalformedArchiveException> { extract(archive) }
  }

  @Test
  fun `a gzip stream cut short is rejected`() {
    val noise = ByteArray(20_000) { (it * 7919 % 251).toByte() }
    val archive = TarFixture().directory("top/").file("top/a", noise).gzip()

    assertFailsWith<MalformedArchiveException> { extract(archive.copyOf(archive.size / 2)) }
  }

  @Test
  fun `a gzip stream missing its trailing checksum is rejected`() {
    val archive = TarFixture().directory("top/").file("top/a", "a").gzip()

    assertFailsWith<MalformedArchiveException> { extract(archive.copyOf(archive.size - 8)) }
  }

  @Test
  fun `data after the end-of-archive marker is rejected`() {
    val tar = TarFixture().directory("top/").file("top/a", "a").tarBytes() + "junk".toByteArray()

    assertFailsWith<MalformedArchiveException> { extract(TarFixture.gzip(tar)) }
  }

  @Test
  fun `zero padding after the end-of-archive marker is accepted`() {
    val tar = TarFixture().directory("top/").file("top/a", "a").tarBytes() + ByteArray(8192)

    assertEquals("a", File(extract(TarFixture.gzip(tar)), "a").readText())
  }

  @Test
  fun `a header whose checksum does not match is rejected`() {
    val archive =
        TarFixture()
            .directory("top/")
            .entry("top/a", '0', "a".toByteArray(), corruptChecksum = true)
            .gzip()

    assertFailsWith<MalformedArchiveException> { extract(archive) }
  }

  @Test
  fun `a size field that is not octal is rejected`() {
    val archive =
        TarFixture()
            .directory("top/")
            .entry("top/a", '0', "a".toByteArray(), sizeField = "00000000009\u0000")
            .gzip()

    assertFailsWith<MalformedArchiveException> { extract(archive) }
  }

  @Test
  fun `a header in a format other than GNU tar is rejected`() {
    val archive =
        TarFixture()
            .directory("top/")
            .entry("top/a", '0', "a".toByteArray(), magic = "ustar\u000000")
            .gzip()

    assertFailsWith<MalformedArchiveException> { extract(archive) }
  }

  @Test
  fun `a file entry is not written over a file that already exists`() {
    val archive =
        TarFixture().directory("top/").file("top/a", "first").file("top/a", "second").gzip()
    val destination = tmp.newFolder()

    assertFailsWith<FileAlreadyExistsException> {
      extractTarGz(archive.inputStream(), destination)
    }
    assertEquals("first", File(destination, "a").readText())
  }

  private class CloseRecordingStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
    var isClosed = false

    override fun close() {
      isClosed = true
    }
  }

  @Test
  fun `the input is closed once the archive is extracted`() {
    val input = CloseRecordingStream(TarFixture().directory("top/").file("top/a", "a").gzip())

    extractTarGz(input, tmp.newFolder())

    assertTrue(input.isClosed)
  }

  @Test
  fun `the input is closed when it is not gzip`() {
    val input = CloseRecordingStream(TarFixture().directory("top/").file("top/a", "a").tarBytes())

    assertFailsWith<MalformedArchiveException> { extractTarGz(input, tmp.newFolder()) }

    assertTrue(input.isClosed)
  }

  @Test
  fun `the input is closed when an entry is rejected`() {
    val input =
        CloseRecordingStream(
            TarFixture().directory("top/").entry("top/link", '2', ByteArray(0)).gzip()
        )

    assertFailsWith<MalformedArchiveException> { extractTarGz(input, tmp.newFolder()) }

    assertTrue(input.isClosed)
  }
}
