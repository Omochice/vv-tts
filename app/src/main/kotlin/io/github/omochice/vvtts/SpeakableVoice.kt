package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.AssetLayout
import io.github.omochice.vvtts.assets.Voice
import io.github.omochice.vvtts.assets.VoiceModel

/**
 * A VOICEVOX style the engine offers as a voice.
 *
 * @property name the name client apps store and show, such as `ずんだもん（ノーマル）`.
 * @property voiceModelFileName the voice model that contains [styleId], such as `0.vvm`.
 * @property styleId the style VOICEVOX speaks with.
 */
data class SpeakableVoice(val name: String, val voiceModelFileName: String, val styleId: Int) {
  companion object {
    /** The voice that speaks [voice] from the voice model named [voiceModelFileName]. */
    fun of(voice: Voice, voiceModelFileName: String): SpeakableVoice =
        SpeakableVoice("${voice.character}（${voice.style}）", voiceModelFileName, voice.styleId)
  }
}

/** Returns the voices of the [voiceModels] that [isInstalled] accepts, in catalog order. */
fun speakableVoices(
    voiceModels: List<VoiceModel>,
    isInstalled: (voiceModelFileName: String) -> Boolean,
): List<SpeakableVoice> =
    voiceModels
        .filter { isInstalled(it.file.fileName) }
        .flatMap { model -> model.voices.map { SpeakableVoice.of(it, model.file.fileName) } }

/**
 * Returns the voices the assets in [layout] can speak, in catalog order.
 *
 * The engine can speak at all exactly when this is not empty.
 */
fun installedVoices(voiceModels: List<VoiceModel>, layout: AssetLayout): List<SpeakableVoice> =
    if (layout.hasBaseParts()) speakableVoices(voiceModels, layout::hasVoiceModel) else emptyList()

/**
 * Chooses the voice to speak with among [speakable]: the [requested] one, else the [preferred] one,
 * else the first; null when [speakable] is empty.
 */
fun resolveVoice(
    requested: String?,
    preferred: String?,
    speakable: List<SpeakableVoice>,
): SpeakableVoice? =
    speakable.find { it.name == requested }
        ?: speakable.find { it.name == preferred }
        ?: speakable.firstOrNull()
