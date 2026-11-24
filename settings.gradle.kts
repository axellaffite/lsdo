rootProject.name = "lsdo"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":lsdo-core", ":lsdo-compose", ":lsdo-runtime", ":lsdo-runtime-compose", ":sample")
