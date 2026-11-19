package io.github.omochice.vvtts.licenses

import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License

/**
 * A library the APK carries, as the licenses screen lists it.
 *
 * @property name the name the library is published under.
 * @property version null when the build recorded none.
 * @property licenses sorted by name; empty when the build knows no license for the library.
 */
data class LibraryNotice(
    val name: String,
    val version: String?,
    val licenses: List<LicenseNotice>,
) {
  /** The names of [licenses], in their order. */
  val licenseNames: List<String>
    get() = licenses.map { it.name }
}

/**
 * A license of a [LibraryNotice].
 *
 * @property name the name the license is published under.
 * @property body what the detail view shows for the license.
 */
data class LicenseNotice(val name: String, val body: LicenseBody)

/** What the detail view of a license shows, depending on how much of it the build carries. */
sealed interface LicenseBody {
  /**
   * The build carries the license.
   *
   * @property text the full text of the license.
   */
  data class Text(val text: String) : LicenseBody

  /**
   * The build carries no text, but knows where the license is published.
   *
   * @property url where the text of the license is published.
   */
  data class Elsewhere(val url: String) : LicenseBody

  /** The build carries neither the text nor where to find it. */
  data object Missing : LicenseBody
}

/**
 * Reads the library definitions the AboutLibraries plugin generated into [json] as notices sorted
 * by name.
 *
 * An unreadable [json] gives no notices, because the parser of aboutlibraries-core logs the error
 * and returns nothing rather than throwing.
 */
fun libraryNoticesOf(json: String): List<LibraryNotice> =
    libraryNoticesOf(Libs.Builder().withJson(json).build().libraries)

/** Turns [libraries] into notices sorted by name, ignoring case. */
fun libraryNoticesOf(libraries: Iterable<Library>): List<LibraryNotice> =
    libraries
        .map { library ->
          LibraryNotice(
              name = library.name,
              version = library.artifactVersion?.takeIf { it.isNotBlank() },
              licenses = library.licenses.map(::noticeOf).sortedBy { it.name },
          )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

private fun noticeOf(license: License): LicenseNotice {
  val text = license.licenseContent?.takeIf { it.isNotBlank() }
  val url = license.url?.takeIf { it.isNotBlank() }
  val body =
      when {
        text != null -> LicenseBody.Text(text)
        url != null -> LicenseBody.Elsewhere(url)
        else -> LicenseBody.Missing
      }
  return LicenseNotice(license.name, body)
}
