plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.kmp.library) apply false
}

// Every library module publishes with the same POM; the sample does not publish.
subprojects {
    if (!name.startsWith("lsdo")) return@subprojects
    apply(plugin = "maven-publish")
    extensions.configure<PublishingExtension> {
        publications.withType<MavenPublication>().configureEach {
            pom {
                name.set(project.name)
                description.set("LS-DO: let agents and scripts list an app's screens, run their actions and read what is rendered (debug builds).")
                url.set("https://github.com/axellaffite/lsdo")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/license/mit")
                    }
                }
                developers {
                    developer {
                        id.set("axellaffite")
                        name.set("axellaffite")
                    }
                }
                scm {
                    url.set("https://github.com/axellaffite/lsdo")
                    connection.set("scm:git:https://github.com/axellaffite/lsdo.git")
                }
            }
        }
    }
}
