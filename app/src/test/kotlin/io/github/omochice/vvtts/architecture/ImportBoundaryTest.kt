package io.github.omochice.vvtts.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import java.io.File
import kotlin.test.Test

private const val APP = "io.github.omochice.vvtts"
private const val ASSETS = "$APP.assets"
private const val DICTIONARY = "$APP.dictionary"
private const val LICENSES = "$APP.licenses"
private const val VOICEVOX = "$APP.voicevox"
private const val VOICEVOX_CORE = "jp.hiroshiba.voicevoxcore"
private const val ABOUT_LIBRARIES = "com.mikepenz.aboutlibraries"

private fun String?.isWithin(packageName: String): Boolean =
    this == packageName || this?.startsWith("$packageName.") == true

class ImportBoundaryTest {
  // Not scopeFromProduction, which finds the project root by a Gradle wrapper or a .git directory
  // and so, in a git worktree of this project without a wrapper, checks the enclosing checkout.
  private val production = Konsist.scopeFromExternalDirectory(File("src/main").absolutePath)

  @Test
  fun `assets imports neither Android nor the rest of the app`() {
    production.files
        .filter { it.packagee?.name.isWithin(ASSETS) }
        .assertFalse(strict = true) { file ->
          file.hasImport { import ->
            import.name.startsWith("android.") ||
                (import.name.startsWith("$APP.") && !import.name.startsWith("$ASSETS."))
          }
        }
  }

  @Test
  fun `dictionary imports neither Android nor VOICEVOX CORE`() {
    production.files
        .filter { it.packagee?.name.isWithin(DICTIONARY) }
        .assertFalse(strict = true) { file ->
          file.hasImport { import ->
            import.name.startsWith("android.") || import.name.startsWith("$VOICEVOX_CORE.")
          }
        }
  }

  @Test
  fun `only the voicevox package imports VOICEVOX CORE`() {
    production.files
        .filterNot { it.packagee?.name.isWithin(VOICEVOX) }
        .assertFalse(strict = true) { file ->
          file.hasImport { it.name.startsWith("$VOICEVOX_CORE.") }
        }
  }

  @Test
  fun `only the licenses package imports AboutLibraries`() {
    production.files
        .filterNot { it.packagee?.name.isWithin(LICENSES) }
        .assertFalse(strict = true) { file ->
          file.hasImport { it.name.startsWith("$ABOUT_LIBRARIES.") }
        }
  }
}
