pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
  repositories {
    google()
    mavenCentral()
    maven {
      name = "voicevoxCore"
      url =
          uri(
              providers.environmentVariable("VOICEVOX_CORE_MAVEN_REPO").orNull
                  ?: error(
                      "VOICEVOX_CORE_MAVEN_REPO is not set; enter the dev shell with `nix develop`"
                  )
          )
      content { includeGroup("jp.hiroshiba.voicevoxcore") }
    }
  }
}

rootProject.name = "vv-tts"

include(":app", ":text")
