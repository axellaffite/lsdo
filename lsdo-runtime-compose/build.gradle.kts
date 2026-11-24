plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.maven.publish)
}

kotlin {
    explicitApi()
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())

    android {
        namespace = "io.github.axellaffite.lsdo.runtime.compose"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":lsdo-runtime"))
        }
        androidMain.dependencies {
            // Only the semantics tree is read: no @Composable here, so no Compose compiler plugin.
            implementation(libs.compose.ui)
            implementation(libs.coroutines.android)
        }
    }
}
