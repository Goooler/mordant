include("mordant")
include("samples:markdown")
include("samples:progress")
include("samples:table")


@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "mordant-root"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
