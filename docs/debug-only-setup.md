# Debug-only setup per platform

The contract (`lsdo-core`, `lsdo-compose`) is a normal dependency. The runtime is not: it carries a
web server and must never reach a release build. Each platform has its own way to keep it out.

**Android**: `debugImplementation` plus two source-set twins.

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("io.github.axellaffite.lsdo:lsdo-compose:0.1.0-SNAPSHOT")
    debugImplementation("io.github.axellaffite.lsdo:lsdo-runtime-compose:0.1.0-SNAPSHOT")
}
```

```kotlin
// src/debug/kotlin/.../DevTools.kt
internal object DevTools {
    fun start() { LsDoDevTools.start(catalog = AppLsDoScreens) }
}

// src/release/kotlin/.../DevTools.kt
internal object DevTools {
    fun start() = Unit
}

// src/main/kotlin/.../App.kt
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DevTools.start() // before any Compose view exists, or ui misses the roots made earlier
    }
}
```

`LsDoDevTools.start` must run in `Application.onCreate`: the Compose capture hooks into
`ViewRootForTest.onViewCreatedCallback`, which only sees views created after it. Then
`scripts/lsdo` forwards the port with adb (`ANDROID_SERIAL` picks the device).

A check that fails the build if the runtime reaches the release graph anyway:

```kotlin
// app/build.gradle.kts
val checkReleaseHasNoLsDo by tasks.registering {
    group = "verification"
    description = "Fails if the LS-DO runtime is in the release runtime dependency graph."
    val release = configurations.named("releaseRuntimeClasspath").flatMap { it.incoming.resolutionResult.rootComponent }
    doLast {
        val seen = mutableSetOf<org.gradle.api.artifacts.result.ResolvedComponentResult>()
        fun walk(component: org.gradle.api.artifacts.result.ResolvedComponentResult) {
            if (!seen.add(component)) return
            component.dependencies
                .filterIsInstance<org.gradle.api.artifacts.result.ResolvedDependencyResult>()
                .forEach { walk(it.selected) }
        }
        walk(release.get())
        val leaked = seen.map { it.id.displayName }.filter { "lsdo-runtime" in it }
        check(leaked.isEmpty()) { "LS-DO runtime in the release build: $leaked" }
    }
}
tasks.named("check") { dependsOn(checkReleaseHasNoLsDo) }
tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach { dependsOn(checkReleaseHasNoLsDo) }
```

**iOS**: Kotlin/Native has no debug source sets, so the shared module picks one of two source
directories when Gradle configures, from Xcode's `CONFIGURATION` (set by the "embed and sign"
build phase):

```kotlin
// shared/build.gradle.kts (the module that builds the iOS framework)
val xcodeConfiguration = providers.environmentVariable("CONFIGURATION").orNull
val lsdoEnabled = providers.gradleProperty("lsdo.enabled").map(String::toBoolean)
    .getOrElse(xcodeConfiguration == "Debug")
check(!(lsdoEnabled && xcodeConfiguration == "Release")) { "LS-DO cannot be built into a Release app." }

kotlin {
    sourceSets {
        iosMain {
            kotlin.srcDir(if (lsdoEnabled) "src/iosLsDo/kotlin" else "src/iosNoLsDo/kotlin")
            if (lsdoEnabled) dependencies { implementation("io.github.axellaffite.lsdo:lsdo-runtime-compose:0.1.0-SNAPSHOT") }
        }
    }
}
```

```kotlin
// src/iosLsDo/kotlin/.../LsDoStart.kt
internal fun startLsDo() { LsDoDevTools.start(catalog = AppLsDoScreens) }

// src/iosNoLsDo/kotlin/.../LsDoStart.kt
internal fun startLsDo() = Unit
```

Call `startLsDo()` from your iOS entry point (before the first `ComposeUIViewController`). And a
check on the linked binary, for anything that slipped through:

```kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink>()
    .matching { it.name.startsWith("linkRelease") }
    .configureEach {
        val binary = outputFile
        doLast {
            val file = binary.get().let { if (it.isDirectory) it.resolve(it.nameWithoutExtension) else it }
            val leaked = "io.github.axellaffite.lsdo.runtime" in String(file.readBytes(), Charsets.ISO_8859_1)
            check(!leaked) { "LS-DO runtime linked into the release binary ${file.name}." }
        }
    }
```

The simulator shares the Mac's loopback: `LSDO_URL=http://127.0.0.1:8765 scripts/lsdo ls`. A real
iPhone needs the port forwarded over USB first (`iproxy 8765 8765`, from libimobiledevice).

**JVM / desktop**: no capture is built in; use `lsdo-runtime` directly, as the sample does:

```kotlin
val registry = LsDoRegistry(catalog = AppLsDoScreens)   // capture = { ... } to serve /ui
LsDo.install(registry)
startLsDoServer(registry)                               // 127.0.0.1:8765
```

(`LsDoRegistry(context = ...)` sets where actions run; `LsDoDevTools` uses the main thread.)

A future Gradle plugin could wire the release-leak checks for you; for now they are recipes.

