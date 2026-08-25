// Maven Central is reached through Google's official GCS mirror rather than repo1.maven.org:
// systemd-resolved returns SERVFAIL on repo1's Cloudflare CNAME on the build machine, and Gradle
// aborts the whole resolution when one repository fails at the network level, so listing
// mavenCentral() as a fallback would break the build rather than back it up. Same content, same
// host family as dl.google.com. Rationale and the DNS fix are in docs/STACK.md.

pluginManagement {
    repositories {
        google()
        maven(url = "https://maven-central.storage-download.googleapis.com/maven2")
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        maven(url = "https://maven-central.storage-download.googleapis.com/maven2")
    }
}

rootProject.name = "JobCalender"
include(":app")
