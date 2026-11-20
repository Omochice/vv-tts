package io.github.omochice.vvtts.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import java.io.File
import kotlin.test.Test

private const val APP = "io.github.omochice.vvtts"
private const val ASSETS = "$APP.assets"
private const val VOICEVOX_CORE = "jp.hiroshiba.voicevoxcore"

class ImportBoundaryTest {
  // Not scopeFromProduction, which finds the project root by a Gradle wrapper or a .git directory
  // and so, in a git worktree of this project without a wrapper, checks the enclosing checkout.
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
