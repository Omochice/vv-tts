package io.github.omochice.vvtts

/**
 * A value created on first use, kept once created, and used by one thread at a time.
 *
 * A failed creation keeps nothing, so the next use tries again.
 */
class ExclusiveLazy<T : Any> {
  private var value: T? = null

  // One lock covers both creation and use.
  // Kotlin's lazy would serialize creation only, and the values held here are not thread-safe.
  /**
   * Runs [block] with the value, creating it with [create] first if no use has created it yet.
   *
   * A caller waits while another thread is inside [use].
   *
   * @throws Exception whatever [create] or [block] throws.
   */
  @Synchronized
  fun <R> use(create: () -> T, block: (T) -> R): R = block(value ?: create().also { value = it })
}
