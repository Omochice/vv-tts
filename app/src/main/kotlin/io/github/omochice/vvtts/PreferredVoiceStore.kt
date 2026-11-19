package io.github.omochice.vvtts

import android.content.Context

/** Remembers the name of the voice the user chose as the default. */
internal class PreferredVoiceStore(context: Context) {
  private val preferences = context.getSharedPreferences("voice", Context.MODE_PRIVATE)

  /** Returns the [SpeakableVoice.name] last stored, or null when none was. */
  fun get(): String? = preferences.getString(KEY_NAME, null)

  /** Returns the voice among [voices] that speaks when a client asks for none. */
  fun defaultAmong(voices: List<SpeakableVoice>): SpeakableVoice? =
      resolveVoice(null, get(), voices)

  /** Stores [name], a [SpeakableVoice.name], as the default voice. */
  fun set(name: String) {
    preferences.edit().putString(KEY_NAME, name).apply()
  }

  private companion object {
    const val KEY_NAME = "name"
  }
}
