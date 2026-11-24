# LS-DO

A debug-only interface that lets AI agents (and scripts) drive a Kotlin Multiplatform app by
meaning instead of pixels. Every screen on show answers four questions over a loopback HTTP port:

- **ls**: what is this screen's state, and which actions are available right now?
- **do**: run one of those actions, through the same code the UI calls, and answer once the
  screen is idle with every screen's new state.
- **ui**: what is actually rendered (the accessibility tree, as compact text)?
- **catalog**: every screen the app has, shown or not, with its actions and its state's shape.

Screens declare a `@Serializable` state and a sealed `@Serializable` intent type; the JSON schemas
an agent reads are derived from those types, so they cannot drift from the code.

## Why

- **Meaning over pixels.** An agent that taps coordinates guesses what a screen means. With LS-DO
  it reads `{"count": 2}` and calls `add {"n": 1}`, and a refusal comes back as a sentence
  ("Only positive numbers.") instead of a screen that did not change.
- **State-aware.** Each screen lists the actions that make sense now and, under `unavailable`, those
  that exist but do not. A `do` waits for the screen to stop being busy before it answers.
- **The UI keeps the state honest.** Driving by state alone would let a broken screen pass. `ui`
  reads what the platform renders (Compose semantics on Android, UIKit accessibility on iOS), so a
  check can compare the two: the state says three rows, does the screen show three rows?
- **Traces and a verifier.** `LSDO_TRACE=<dir>` records each step (intent, answer, rendered UI,
  screenshot). The [`lsdo-verifier`](agents/lsdo-verifier.md) agent reads a trace and reports, step
  by step, where the UI and the state disagree, and which steps need a look at the screenshot.

It is a testing and development tool: it ships in debug builds only.

## What it looks like

The [sample](sample) is a console JVM program with one counter screen (`./gradlew :sample:run`),
driven with [`scripts/lsdo`](scripts/lsdo):

```console
$ export LSDO_URL=http://127.0.0.1:8765
$ scripts/lsdo ls
{
  "screens": [
    {
      "id": "counter",
      "busy": false,
      "state": {
        "count": 0,
        "saving": false
      },
      "actions": [
        {
          "name": "add",
          "description": "Add n to the count",
          "params": {
            "type": "object",
            "properties": {
              "n": {
                "type": "integer",
                "description": "How much to add; positive"
              }
            }
          }
        },
        {
          "name": "slow_add",
          "description": "Add n after a delay, as a save to a server would; the screen is busy meanwhile. Not while saving",
          "params": {
            "type": "object",
            "properties": {
              "n": {
                "type": "integer"
              }
            }
          }
        }
      ],
      "unavailable": [
        "reset"
      ]
    }
  ]
}
```

```console
$ scripts/lsdo do counter add '{"n":2}'
{
  "ok": true,
  "message": null,
  "screens": [
    {
      "id": "counter",
      "busy": false,
      "state": {
        "count": 2,
        "saving": false
      },
      "actions": [ "add", "reset", "slow_add" with their schemas ]
    }
  ]
}

$ scripts/lsdo do counter add '{"n":-1}'
{
  "ok": false,
  "message": "Only positive numbers.",
  ...

$ scripts/lsdo do counter slow_add '{"n":3}'    # answers after the save, about a second later
{
  "ok": true,
  "message": null,
  "screens": [ { "id": "counter", "busy": false, "state": { "count": 5, "saving": false }, ... } ]
}

$ scripts/lsdo do counter reset; scripts/lsdo do counter reset
...
{
  "ok": false,
  "message": "\"reset\" is not available on \"counter\" now. Available: add, slow_add.",
  ...
```

```console
$ scripts/lsdo ui
window
  heading "Counter"
  text "Count" = "0"
  button "Add" [click]
  button "Reset" {disabled} [click]
```

(A console program renders nothing, so the sample hands the registry a stand-in UI built from its
state. In an app, `lsdo-runtime-compose` reads the real tree.)

```console
$ scripts/lsdo catalog
{
  "screens": [
    {
      "name": "counter",
      "description": "A counter. Always shown.",
      "actions": [ "add", "reset", "slow_add" ],
      "shown": true
    },
    {
      "name": "settings",
      "description": "The app's settings. Never shown in this sample: it is here to show the catalog.",
      "actions": [ "set_dark" ],
      "shown": false
    }
  ]
}
```

`scripts/lsdo catalog settings` gives one screen in full: each action's description and parameter
schema, and the schema of its state.

## Modules

| Artifact | Targets | Ships in | What |
|---|---|---|---|
| `lsdo` | jvm, android, iosArm64, iosSimulatorArm64 | every build | The contract: `LsDoScreen`, `LsDoSpec`/`lsdoSpec`, `@Doc`, `LsDo`, `LsDoEntry`. No server, tiny. |
| `lsdo-compose` | jvm, android, ios | every build | `RegisterLsDo(name, screen)`: a screen is drivable while its composable is shown. A no-op when no runtime is installed. |
| `lsdo-runtime` | jvm, android, ios | debug only | The registry (idle waiting, availability), JSON schemas from serial descriptors, the Ktor CIO server on 127.0.0.1, `UiNode`, the catalog. |
| `lsdo-runtime-compose` | android, ios | debug only | UI capture (Android Compose semantics, iOS UIKit accessibility) and `LsDoDevTools.start(catalog, port)`. |

Group `io.github.axellaffite.lsdo`, version `0.1.0-SNAPSHOT` (not on Maven Central yet:
`./gradlew publishToMavenLocal` and add `mavenLocal()`).

## Quick start

### 1. Make a screen drivable

The state is `@Serializable`; the intent is a sealed `@Serializable` type whose subclasses are the
actions (`@SerialName` is the action's name, `@Doc` its description, shown to the agent). The
screen, usually its ViewModel, implements `LsDoScreen` by calling its own functions, the ones the
UI calls:

```kotlin
@Serializable
data class CounterState(
    @Doc("The current count") val count: Int = 0,
    @Doc("True while a slow_add is saving") val saving: Boolean = false,
)

@Serializable
sealed interface CounterIntent {
    @Serializable @SerialName("add") @Doc("Add n to the count")
    data class Add(@Doc("How much to add; positive") val n: Int = 1) : CounterIntent

    @Serializable @SerialName("reset") @Doc("Back to zero. Not at zero")
    data object Reset : CounterIntent
}

class CounterViewModel : ViewModel(), LsDoScreen<CounterState, CounterIntent> {
    override val lsdo = lsdoSpec<CounterState, CounterIntent>()
    private val _state = MutableStateFlow(CounterState())
    override val state: StateFlow<CounterState> = _state.asStateFlow()

    fun add(n: Int) { _state.update { it.copy(count = it.count + n) } }
    fun reset() { _state.update { it.copy(count = 0) } }

    // null: done; a string: why it was refused, in words the agent can act on.
    override suspend fun perform(intent: CounterIntent): String? = when (intent) {
        is CounterIntent.Add -> if (intent.n <= 0) "Only positive numbers." else null.also { add(intent.n) }
        CounterIntent.Reset -> null.also { reset() }
    }

    // Optional: the actions that make sense now (null, the default, means all).
    override fun availableIntents(state: CounterState) =
        if (state.count == 0) setOf("add") else setOf("add", "reset")

    // Optional: a do answers once this is false (loading, saving).
    override fun isBusy(state: CounterState) = state.saving
}
```

A larger real-world adapter keeps the mapping in its own file: the intent type, a
`performIntent` that maps each action to the ViewModel's existing calls, and an `intentsFor(state)`
for availability; the ViewModel's `LsDoScreen` members just delegate to them. A screen whose state
lives in Compose (a tab bar's selected tab) can be a small class that mirrors it into a
`StateFlow` and calls back into the composable to change it.

### 2. Register it while it is shown

```kotlin
@Composable
fun CounterScreen(vm: CounterViewModel = viewModel { CounterViewModel() }) {
    RegisterLsDo("counter", vm)
    // ... the UI
}
```

Two screens of the same name get ids `counter` and `counter-2`. Without Compose, call
`LsDo.register(name, screen)` and the returned function when the screen leaves.

### 3. List every screen in the catalog

```kotlin
val AppLsDoScreens: List<LsDoEntry> = listOf(
    LsDoEntry("counter", "A counter. Reached by: the Counter tab.", lsdoSpec<CounterState, CounterIntent>()),
    // A screen added to LS-DO is added here, with how to reach it.
)
```

### 4. Start the runtime, in debug builds only

The contract (`lsdo`, `lsdo-compose`) is a normal dependency. The runtime is not: it carries a
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

### 5. Keep a catalog file in the repository (optional)

A committed `lsdo-catalog.json` lets an agent plan a route through the app without a device. A JVM
test writes or compares it, so it cannot go stale (the sample does exactly this):

```kotlin
// src/jvmTest/kotlin/.../LsDoCatalogFileTest.kt
class LsDoCatalogFileTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun theCommittedCatalogMatchesTheCode() {
        val file = File(requireNotNull(System.getProperty("lsdo.catalog")))
        val generated = json.encodeToString(JsonElement.serializer(), catalogFull(AppLsDoScreens)) + "\n"
        if (System.getProperty("lsdo.catalog.write") == "true") return file.writeText(generated)
        val committed = if (file.exists()) file.readText() else ""
        assertEquals(generated, committed, "lsdo-catalog.json is stale: run ./gradlew lsdoCatalog")
    }
}
```

```kotlin
// build.gradle.kts of that module (lsdo-runtime as a jvmTest dependency)
val lsdoCatalogFile = rootProject.file("lsdo-catalog.json")
val lsdoCatalog = tasks.register<Test>("lsdoCatalog") {
    val jvmTest = tasks.named<Test>("jvmTest").get()
    testClassesDirs = jvmTest.testClassesDirs
    classpath = jvmTest.classpath
    filter { includeTestsMatching("*LsDoCatalogFileTest") }
    systemProperty("lsdo.catalog", lsdoCatalogFile.absolutePath)
    systemProperty("lsdo.catalog.write", "true")
    outputs.file(lsdoCatalogFile)
    outputs.upToDateWhen { false }
}
tasks.named<Test>("jvmTest") {
    systemProperty("lsdo.catalog", lsdoCatalogFile.absolutePath)
    inputs.files(lsdoCatalogFile).withPropertyName("lsdoCatalog")
    mustRunAfter(lsdoCatalog)
}
```

## The CLI

[`scripts/lsdo`](scripts/lsdo) needs bash, curl and python3 (jq is used when present).

```
scripts/lsdo ls [screen]                  every screen on show, or one
scripts/lsdo do <screen> <action> [json]  run an action: scripts/lsdo do counter add '{"n":2}'
scripts/lsdo ui [bounds|json]             the rendered UI as compact text (bounds: positions; json: the tree)
scripts/lsdo catalog [screen]             every screen of the app, shown or not
```

| Variable | Meaning |
|---|---|
| `LSDO_URL` | The app's address (simulator, desktop). Unset: `adb forward` to an Android device. |
| `LSDO_PORT` | The port adb forwards (default 8765). |
| `LSDO_TRACE=<dir>` | Each `do` also saves `step-N.json` (intent, answer, UI after) and `step-N.png`. |
| `LSDO_SCREENSHOT` | `android`, `ios` or `none`: how traces take screenshots (default: adb, else the booted simulator). |
| `LSDO_TIMEOUT` | Seconds before a request gives up (default 30): a backgrounded iOS app never answers. |
| `LSDO_UI=1` | Every `do` also returns the UI after it. |

`ui` lines read `role "text" = "value" {flags} [actions]`, one element per line, indented by
nesting. It shows no colors, images, truncation or overlap: those need a screenshot.

To check a scenario: run it with `LSDO_TRACE=/tmp/run1`, then give that directory to the
[`lsdo-verifier`](agents/lsdo-verifier.md) agent (copy it into your project's `.claude/agents/`).

## HTTP API

Every request needs an `X-LsDo` header (any value); without it the answer is `403`. Answers are JSON
unless noted. The server listens on `127.0.0.1:8765` by default.

| Request | Answer |
|---|---|
| `GET /ls` | `{"screens": [screen...]}`, each screen: `id`, `busy`, `state`, `actions` (`name`, `description`, `params` as a JSON schema), `unavailable` (names). |
| `GET /ls/{id}` | One screen; `404` if no screen has that id. |
| `POST /do/{id}` | Body: the intent, `{"type": "<action>", ...params}`. Answer: `{"ok", "message", "screens"}`, plus `"ui"` (text) with `?ui=1`. `400` if the body is not JSON, `404` if the screen is unknown, `200` otherwise, also for refusals (`ok: false`). |
| `GET /ui` | Plain text, one line per element; `?bounds=1` adds `@x,y WxH`; `?format=json` gives the `UiNode` tree. `404` when the build has no capture. |
| `GET /catalog` | `{"screens": [{name, description, actions, shown}]}`. |
| `GET /catalog/{name}` | One catalog screen in full: actions with descriptions and parameters, and `state` (the state's schema). |

How a `do` runs:

1. The body is decoded into the screen's intent type; a decoding error is answered as
   `Not an action of "<id>": ...`.
2. If the screen is busy, it waits (up to the timeout, 10 s by default) for it to settle, then
   checks the action is available; if not, `"<action>" is not available on "<id>" now. Available: ...`.
3. `perform` runs in the registry's context (the main thread with `LsDoDevTools`). A returned string
   is a refusal (`ok: false`); an exception is answered as `"<action>" failed: <message>`.
4. It waits 300 ms for work to start, then until the screen is not busy (up to the timeout; past
   it, `message` says "Still busy...") and answers with every screen.

## Security

- **Debug builds only.** Keep the runtime out of release builds with the recipes above and their
  checks. The contract modules contain no server.
- **Loopback only.** The server binds `127.0.0.1`: other machines on the network cannot reach it.
  A device is reached through `adb forward` or the simulator's shared loopback.
- **The `X-LsDo` header.** A web page in a browser on the same device could send simple requests to
  loopback; a custom header forces a CORS preflight the server never approves, so it cannot.
- **Any local process can still drive the app**, as with any debug port. Do not run debug builds
  with real accounts on shared devices.
- **Mask secrets in state.** The state is served as is. Keep tokens and passwords out of it, or
  replace them (`"password": "••••"`, `"hasPassword": true`). Password fields are masked in `ui`.

## Limits

- One process, one server: `LsDoDevTools.start` is meant to be called once.
- `ui` sees what the accessibility tree exposes. Custom drawing without semantics is invisible to
  it, and iOS bounds are in points.
- Schemas cover the JSON Schema subset agents need (objects, arrays, maps, enums, nullables, sealed
  types as `oneOf`, recursion as `$ref` by serial name); contextual and open polymorphic types come
  out as `{}`.
- Waiting for idle relies on `isBusy`: a screen that does work without saying so may answer early.
- No Android or iOS sample app yet; the capture code comes from an app where it runs on devices,
  but it is only compiled in this repository's CI.

## Related projects

- **[mcp_flutter / mcp_toolkit](https://github.com/Arenukvern/mcp_flutter)**: an MCP server and
  in-app toolkit for Flutter debug apps (screenshots, widget tree, errors, app-registered tools).
  Flutter only, and its tools are hand-written functions, not screens with typed state and actions.
- **WebMCP / MCP-B**: web pages expose tools to agents in the browser. Web only; tools are per page
  and nothing compares them with what is rendered.
- **CopilotKit**: gives an in-app AI copilot access to app state and actions (React). A product
  feature for end users rather than a debug and testing interface.
- **[Chrome DevTools for agents, third-party tools](https://developer.chrome.com/docs/devtools/agents/use-cases/third-party-tools)**:
  a page registers tools that expose its state to an agent driving Chrome. The same idea for the web,
  without per-screen availability or a catalog.
- **Callstack agent-device**: lets agents drive iOS and Android devices from outside, through
  accessibility snapshots and taps. Outside-in; LS-DO adds the app's own meaning and pairs well with it.
- **Playwright MCP / agent-browser**: browser automation for agents through accessibility snapshots.
  Web only, outside-in.
- **Compose Hot Reload MCP**: an experimental MCP server in Compose Hot Reload that lets coding agents
  reload a running Compose desktop app, take screenshots and inspect its semantic tree. About editing
  code live, not driving screens by their actions.
- **cmp-bridge**: connects a running Compose Multiplatform app to an agent.

What LS-DO combines: actions and state declared by the app itself, a snapshot of what is really
rendered next to them, schemas derived from the Kotlin types, availability and busy-waiting per
screen, and a catalog of the screens not on show, aimed at testing and verifying apps.

## Building

```
./gradlew build                     # JDK 21 toolchain, Android SDK (local.properties sdk.dir or ANDROID_HOME)
./gradlew :lsdo-runtime:iosSimulatorArm64Test   # macOS
./gradlew :sample:run               # then LSDO_URL=http://127.0.0.1:8765 scripts/lsdo ls
./gradlew publishToMavenLocal
```

## License

[MIT](LICENSE)
