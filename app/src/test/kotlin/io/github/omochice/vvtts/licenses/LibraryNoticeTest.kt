package io.github.omochice.vvtts.licenses

import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import kotlin.test.Test
import kotlin.test.assertEquals

private fun license(
    name: String,
    content: String? = "$name text",
    url: String? = null,
    hash: String = name,
) = License(name = name, url = url, licenseContent = content, hash = hash)

private fun library(name: String, version: String? = "1.0", vararg licenses: License) =
    Library(
        uniqueId = "group:$name",
        artifactVersion = version,
        name = name,
        description = null,
        website = null,
        developers = emptyList(),
        organization = null,
        scm = null,
        licenses = licenses.toSet(),
    )

class LibraryNoticeTest {
  @Test
  fun `a library becomes a row with its name, version and license names`() {
    val notices = libraryNoticesOf(listOf(library("Gson", "2.10.1", license("Apache-2.0"))))

    assertEquals(listOf("Gson"), notices.map { it.name })
    assertEquals(listOf("2.10.1"), notices.map { it.version })
    assertEquals(listOf(listOf("Apache-2.0")), notices.map { it.licenseNames })
  }

  @Test
  fun `rows are sorted by name regardless of case`() {
    val notices =
        libraryNoticesOf(
            listOf(
                library("open_jtalk-rs"),
                library("Gson"),
                library("Open JTalk"),
                library("MeCab"),
            )
        )

    assertEquals(listOf("Gson", "MeCab", "Open JTalk", "open_jtalk-rs"), notices.map { it.name })
  }

  @Test
  fun `the licenses of a library with several are sorted by name`() {
    val notice =
        libraryNoticesOf(
                listOf(
                    library(
                        "Jakarta Annotations API",
                        "2.1.1",
                        license("The GNU General Public License"),
                        license("Eclipse Public License - v 2.0"),
                    )
                )
            )
            .single()

    assertEquals(
        listOf("Eclipse Public License - v 2.0", "The GNU General Public License"),
        notice.licenseNames,
    )
    assertEquals(
        listOf(
            LicenseBody.Text("Eclipse Public License - v 2.0 text"),
            LicenseBody.Text("The GNU General Public License text"),
        ),
        notice.licenses.map { it.body },
    )
  }

  @Test
  fun `a library without licenses has no license names`() {
    val notice = libraryNoticesOf(listOf(library("Unknown"))).single()

    assertEquals(emptyList(), notice.licenseNames)
  }

  @Test
  fun `a blank version is no version`() {
    val notices = libraryNoticesOf(listOf(library("A", null), library("B", " ")))

    assertEquals(listOf(null, null), notices.map { it.version })
  }

  @Test
  fun `a license without text points to its url`() {
    val body =
        libraryNoticesOf(
                listOf(library("A", "1", license("EPL", content = null, url = "https://e.example")))
            )
            .single()
            .licenses
            .single()
            .body

    assertEquals(LicenseBody.Elsewhere("https://e.example"), body)
  }

  @Test
  fun `a blank license text counts as no text`() {
    val body =
        libraryNoticesOf(
                listOf(
                    library("A", "1", license("EPL", content = "  \n", url = "https://e.example"))
                )
            )
            .single()
            .licenses
            .single()
            .body

    assertEquals(LicenseBody.Elsewhere("https://e.example"), body)
  }

  @Test
  fun `a license with neither text nor url is missing`() {
    val body =
        libraryNoticesOf(listOf(library("A", "1", license("EPL", content = null, url = " "))))
            .single()
            .licenses
            .single()
            .body

    assertEquals(LicenseBody.Missing, body)
  }
}
