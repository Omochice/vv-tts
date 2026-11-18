package io.github.omochice.vvtts

import android.content.Context
import io.github.omochice.vvtts.assets.AssetLayout
import io.github.omochice.vvtts.dictionary.DictionaryChanges
import io.github.omochice.vvtts.dictionary.UserDictionaryFile
import io.github.omochice.vvtts.voicevox.VoicevoxSpeechEngine

/**
 * The one VOICEVOX engine of the process, created on first use and used by one thread at a time.
 *
 * Every component reaches the engine through here, so it is never created twice.
 */
internal object SharedSpeechEngine {
  // Not per service instance, because the framework recreates the service on every bind and
  // VOICEVOX CORE's Java API has no close, so each connection leaked about 390 MB of native heap.
  // One user at a time, because an old instance can still be synthesizing when a new one starts.
  private val engine = ExclusiveLazy<VoicevoxSpeechEngine>()

  /**
   * Runs [block] with the engine, creating it from the assets of [context] first if needed.
   *
   * Blocks while another thread uses the engine, which a synthesis can do for seconds.
   *
   * @throws Exception whatever creating the engine or [block] throws.
   */
  fun <R> useEngine(context: Context, block: (SpeechEngine) -> R): R =
      engine.use({ create(context.applicationContext) }, block)

  /**
   * Runs [block] with the engine as a [WordPronouncer], as [useEngine] does.
   *
   * @throws Exception whatever creating the engine or [block] throws.
   */
  fun <R> usePronouncer(context: Context, block: (WordPronouncer) -> R): R =
      engine.use({ create(context.applicationContext) }, block)

  private fun create(context: Context) =
      VoicevoxSpeechEngine(
          AssetLayout(context.filesDir),
          context.cacheDir,
          DictionaryChanges(UserDictionaryFile(context.filesDir)),
      )
}
