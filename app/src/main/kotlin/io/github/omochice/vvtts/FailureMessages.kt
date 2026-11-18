package io.github.omochice.vvtts

/**
 * Returns the string resource that tells the user what failed, with an action where one is not
 * obvious.
 */
internal fun FailureReason.messageRes(): Int =
    when (this) {
      FailureReason.TRANSFER -> R.string.failure_transfer
      FailureReason.SERVER -> R.string.failure_server
      FailureReason.CORRUPT -> R.string.failure_corrupt
      FailureReason.STORAGE -> R.string.failure_storage
      FailureReason.UNSUPPORTED_DEVICE -> R.string.failure_unsupported_device
    }
