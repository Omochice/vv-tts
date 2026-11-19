plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.aboutlibraries.android)
  alias(libs.plugins.detekt)
  alias(libs.plugins.kover)
}

android {
  namespace = "io.github.omochice.vvtts"
  compileSdk = 37

  defaultConfig {
    applicationId = "io.github.omochice.vvtts"
    minSdk = 34
    targetSdk = 37
    versionCode = 1
    versionName = "0.1.0"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  sourceSets {
    getByName("main") {
      resources.directories.add(
          providers.environmentVariable("VV_TTS_TERMS_RESOURCES").orNull
              ?: error("VV_TTS_TERMS_RESOURCES is not set; enter the dev shell with `nix develop`")
      )
    }
  }

  lint {
    warningsAsErrors = true
    abortOnError = true
    // Until the app has its own icon, a placeholder would only ship as if it were the real one.
    disable += "MissingApplicationIcon"
    // Otherwise a new release upstream fails the build of a change unrelated to it.
    disable += listOf("AndroidGradlePluginVersion", "GradleDependency")
  }
}

dependencies {
  implementation(project(":text"))
  implementation(libs.voicevox.core)

  testImplementation(libs.junit)
  testImplementation(libs.konsist)
  testImplementation(libs.kotlin.test.junit)

  kover(project(":text"))
}

// Kover merges a dependency's variant only when its name and origin match.
// :text is a JVM module and cannot offer the Android-origin debug variant.
// So both modules define this custom variant instead.
kover {
  currentProject { createVariant("merged") { add("debug") } }
  reports { variant("merged") { xml { onCheck = true } } }
}

detekt {
  buildUponDefaultConfig = true
  config.setFrom(rootProject.file("config/detekt/detekt.yaml"))
}

aboutLibraries {
  // Fetching licenses online would make the generated list depend on the network and on a GitHub
  // token.
  // The license texts the POMs leave out come from config/licenses instead.
  offlineMode = true
  collect {
    configPath = file("../config")
    // A BOM puts no code into the APK, so listing it would show a license the app does not carry.
    includePlatform = false
  }
}
