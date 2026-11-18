package io.github.omochice.vvtts

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ExclusiveLazyTest {
  private val waitSeconds = 5L
  private val blockedMillis = 200L

  @Test
  fun `the value is created once and reused by every later use`() {
    val shared = ExclusiveLazy<Any>()
    var created = 0
    val create = { Any().also { created++ } }

    val first = shared.use(create) { it }
    val second = shared.use(create) { it }

    assertEquals(1, created)
    assertSame(first, second)
  }

  @Test
  fun `a failed creation reaches the caller and the next use creates again`() {
    val shared = ExclusiveLazy<String>()

    assertFailsWith<IllegalStateException> { shared.use({ error("no engine") }) { it } }

    assertEquals("engine", shared.use({ "engine" }) { it })
  }

  @Test
  fun `a second thread waits until the first one leaves`() {
    val shared = ExclusiveLazy<String>()
    val firstInside = AtomicBoolean(false)
    val firstEntered = CountDownLatch(1)
    val releaseFirst = CountDownLatch(1)
    val secondSawFirstInside = AtomicBoolean(false)
    val first = thread {
      shared.use({ "engine" }) {
        firstInside.set(true)
        firstEntered.countDown()
        releaseFirst.await(waitSeconds, TimeUnit.SECONDS)
        firstInside.set(false)
      }
    }
    assertTrue(firstEntered.await(waitSeconds, TimeUnit.SECONDS))

    val second = thread { shared.use({ "engine" }) { secondSawFirstInside.set(firstInside.get()) } }
    second.join(blockedMillis)
    val secondWasBlocked = second.isAlive
    releaseFirst.countDown()
    first.join()
    second.join()

    assertTrue(secondWasBlocked)
    assertFalse(secondSawFirstInside.get())
  }
}
