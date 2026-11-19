import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.detekt)
  alias(libs.plugins.kover)
}

// The target is set directly instead of through jvmToolchain.
// A toolchain makes Gradle look for a separate JDK 17, while the dev shell provides a single JDK.
java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_17
  }
}

dependencies {
  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnitPlatform()
}

// Named after the app's custom variant, which merges this one into its report.
kover { currentProject { createVariant("merged") { add("jvm") } } }

detekt {
  buildUponDefaultConfig = true
  config.setFrom(rootProject.file("config/detekt/detekt.yaml"))
}
