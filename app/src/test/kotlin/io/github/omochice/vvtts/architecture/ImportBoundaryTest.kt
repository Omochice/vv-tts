package io.github.omochice.vvtts.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import java.io.File
import kotlin.test.Test

private const val APP = "io.github.omochice.vvtts"
private const val ASSETS = "$APP.assets"
private const val VOICEVOX_CORE = "jp.hiroshiba.voicevoxcore"

class ImportBoundaryTest {
  // Resolved from the module directory, where Gradle runs unit tests, rather than through
  // scopeFromProduction.
  // Konsist finds the project root by looking for a Gradle wrapper or a .git directory, and this
  // project has no wrapper, so inside a git worktree it would resolve to the enclosing checkout and
  // check that checkout's sources instead of these.
  private val production = Konsist.scopeFromExternalDirectory(File("src/main").absolutePath)

  @Test
  fun `assets imports neither Android nor the rest of the app`() {
    production.files
        .filter {
          it.packagee?.name.let { name -> name == ASSETS || name?.startsWith("$ASSETS.") == true }
        }
        .assertFalse(strict = true) { file ->
          file.hasImport { import ->
            import.name.startsWith("android.") ||
                (import.name.startsWith("$APP.") && !import.name.startsWith("$ASSETS."))
          }
        }
  }

  @Test
  fun `only the speech engine imports VOICEVOX CORE`() {
    production.files
        .filter { it.name != "VoicevoxSpeechEngine" }
        .assertFalse(strict = true) { file ->
          file.hasImport { it.name.startsWith("$VOICEVOX_CORE.") }
        }
  }
}
