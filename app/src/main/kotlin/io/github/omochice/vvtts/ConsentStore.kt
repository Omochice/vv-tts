package io.github.omochice.vvtts

import android.content.Context

/** Remembers which terms the user agreed to, by their [consentFingerprint]. */
internal class ConsentStore(context: Context) {
  private val preferences = context.getSharedPreferences("consent", Context.MODE_PRIVATE)

  /** Tells whether the user agreed to the terms identified by [fingerprint]. */
  fun isGiven(fingerprint: String): Boolean =
      preferences.getString(KEY_FINGERPRINT, null) == fingerprint

  /** Records that the user agreed to the terms identified by [fingerprint]. */
  fun record(fingerprint: String) {
    preferences.edit().putString(KEY_FINGERPRINT, fingerprint).apply()
  }

  private companion object {
    const val KEY_FINGERPRINT = "fingerprint"
  }
}
