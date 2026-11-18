plugins {
  alias(libs.plugins.android.application)
}

android {
  namespace = "io.github.omochice.vvtts"
  compileSdk = 37

  defaultConfig {
    applicationId = "io.github.omochice.vvtts"
    minSdk = 26
    targetSdk = 37
    versionCode = 1
    versionName = "0.1.0"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  implementation(project(":text"))
  implementation(libs.voicevox.core)

  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test.junit)
}
