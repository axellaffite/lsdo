import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.maven.publish) apply false
}

// GROUP and VERSION_NAME come from gradle.properties; the release workflow passes -PVERSION_NAME=<tag>.
allprojects {
    group = providers.gradleProperty("GROUP").get()
    version = providers.gradleProperty("VERSION_NAME").get()
}

// Every library module applies com.vanniktech.maven.publish and gets the same Central setup and POM.
// The sample does not publish.
subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral(automaticRelease = true)
            signAllPublications()
            pom {
                name.set(project.name)
                description.set("LS-DO: let agents and scripts list an app's screens, run their actions and read what is rendered (debug builds).")
                inceptionYear.set("2026")
                url.set("https://github.com/axellaffite/lsdo")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/license/mit")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("axellaffite")
                        name.set("axellaffite")
                        url.set("https://github.com/axellaffite")
                    }
                }
                scm {
                    url.set("https://github.com/axellaffite/lsdo")
                    connection.set("scm:git:https://github.com/axellaffite/lsdo.git")
                    developerConnection.set("scm:git:ssh://git@github.com/axellaffite/lsdo.git")
                }
            }
        }
        // signAllPublications() requires a signature for every non-SNAPSHOT version. Only require one when a key
        // is given or a Maven Central task is requested, so publishToMavenLocal and build work without a key.
        val hasSigningKey = providers.gradleProperty("signingInMemoryKey").isPresent
        val publishesToCentral = gradle.startParameter.taskNames.any { "MavenCentral" in it }
        extensions.configure<SigningExtension> { isRequired = hasSigningKey || publishesToCentral }
    }
}
