plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.detekt)
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

  lint {
    warningsAsErrors = true
    abortOnError = true
    // Until the app has its own icon, a placeholder would only ship as if it were the real one.
    // Remove this line together with adding the icon.
    disable += "MissingApplicationIcon"
  }
}

dependencies {
  implementation(project(":text"))
  implementation(libs.voicevox.core)

  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test.junit)
}

detekt {
  buildUponDefaultConfig = true
  config.setFrom(rootProject.file("config/detekt/detekt.yaml"))
}
