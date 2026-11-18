package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.AssetCatalog
import io.github.omochice.vvtts.assets.Voice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharacterStylesTest {
  @Test
  fun `the styles of one character are gathered in catalog order`() {
    assertEquals(
        listOf(
            CharacterStyles("四国めたん", listOf("ノーマル", "あまあま")),
            CharacterStyles("ずんだもん", listOf("ノーマル")),
        ),
        characterStylesOf(
            listOf(
                Voice("四国めたん", "ノーマル", 2),
                Voice("四国めたん", "あまあま", 0),
                Voice("ずんだもん", "ノーマル", 3),
            )
        ),
    )
  }

  @Test
  fun `characters keep the order of their first voice even when their voices are apart`() {
    assertEquals(
        listOf(
            CharacterStyles("ずんだもん", listOf("ささやき", "ヒソヒソ")),
            CharacterStyles("九州そら", listOf("ささやき")),
        ),
        characterStylesOf(
            listOf(
                Voice("ずんだもん", "ささやき", 22),
                Voice("九州そら", "ささやき", 19),
                Voice("ずんだもん", "ヒソヒソ", 38),
            )
        ),
    )
  }

  @Test
  fun `no voices make no characters`() {
    assertTrue(characterStylesOf(emptyList()).isEmpty())
  }

  @Test
  fun `a character is labelled with its styles in full-width parentheses`() {
    assertEquals(
        "ずんだもん（ノーマル、あまあま、ツンツン、セクシー）",
        CharacterStyles("ずんだもん", listOf("ノーマル", "あまあま", "ツンツン", "セクシー")).label,
    )
  }

  @Test
  fun `the first bundled voice model holds four characters`() {
    val first = AssetCatalog.load().voiceModels.first()

    assertEquals(
        listOf(
            "四国めたん（ノーマル、あまあま、ツンツン、セクシー）",
            "ずんだもん（ノーマル、あまあま、ツンツン、セクシー）",
            "春日部つむぎ（ノーマル）",
            "雨晴はう（ノーマル）",
        ),
        characterStylesOf(first.voices).map { it.label },
    )
  }
}
