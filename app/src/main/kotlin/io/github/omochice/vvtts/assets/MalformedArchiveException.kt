package io.github.omochice.vvtts.assets

import java.io.IOException

/** Signals an archive that does not have the exact shape the pinned archives are known to have. */
class MalformedArchiveException(message: String, cause: Throwable? = null) :
    IOException(message, cause)
