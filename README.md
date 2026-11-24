# LS-DO

`for screen in $(ls); do …; done`

LS-DO is that loop for apps: list each screen's state and actions, then do one.

LS-DO is a debug-only interface that lets AI agents and scripts drive a Kotlin Multiplatform app by
meaning instead of pixels. Each screen on show lists its serialized state and the actions available
now (`ls`), runs one through the same code the UI calls (`do`), and shows what is actually rendered
(`ui`), all over a loopback HTTP port. The app also publishes a catalog of every screen, and the JSON
schemas agents read are derived from your Kotlin types.

## Quick start

**1. Make a screen drivable** (usually its ViewModel; `lsdo` and `lsdo-compose` are normal dependencies):

```kotlin
@Serializable
data class CounterState(@Doc("The current count") val count: Int = 0)

@Serializable
sealed interface CounterIntent {
    @Serializable @SerialName("add") @Doc("Add n to the count")
    data class Add(val n: Int = 1) : CounterIntent
}

class CounterViewModel : ViewModel(), LsDoScreen<CounterState, CounterIntent> {
    override val lsdo = lsdoSpec<CounterState, CounterIntent>()
    private val _state = MutableStateFlow(CounterState())
    override val state: StateFlow<CounterState> = _state.asStateFlow()

    fun add(n: Int) = _state.update { it.copy(count = it.count + n) }

    // null: done; a string: why it was refused.
    override suspend fun perform(intent: CounterIntent): String? = when (intent) {
        is CounterIntent.Add -> if (intent.n <= 0) "Only positive numbers." else null.also { add(intent.n) }
    }
}

@Composable
fun CounterScreen(vm: CounterViewModel) {
    RegisterLsDo("counter", vm)
    // ... the UI, calling vm.add
}
```

**2. Start it in debug builds only** (`lsdo-runtime-compose` as `debugImplementation`, called from
`src/debug`, in `Application.onCreate` on Android; see [debug-only setup](docs/debug-only-setup.md)):

```kotlin
LsDoDevTools.start(catalog = listOf(LsDoEntry("counter", "A counter. Reached by: the Counter tab.", lsdoSpec<CounterState, CounterIntent>())))
```

**3. Drive it** with [`scripts/lsdo`](scripts/lsdo) (adb forwarding by default; `LSDO_URL` for a simulator or desktop):

```console
$ scripts/lsdo ls                              # state + available actions with JSON schemas
$ scripts/lsdo do counter add '{"n":2}'        # answers once the screen is idle, with the new state
$ scripts/lsdo ui                              # what is rendered
window
  text "Count" = "2"
  button "Add" [click]
```

Try it without a device: `./gradlew :sample:run`, then `LSDO_URL=http://127.0.0.1:8765 scripts/lsdo ls`.

## Modules

`io.github.axellaffite.lsdo:<artifact>:0.1.0-SNAPSHOT` (`./gradlew publishToMavenLocal` for now).

- `lsdo`: the contract. Every build.
- `lsdo-compose`: `RegisterLsDo`. Every build.
- `lsdo-runtime`: registry, schemas, server, catalog. Debug only.
- `lsdo-runtime-compose`: Android and iOS UI capture, `LsDoDevTools.start`. Debug only.

## More

- [AGENTS.md](AGENTS.md): adding LS-DO to a screen, for coding agents.
- [skills/lsdo](skills/lsdo/SKILL.md): driving an app with LS-DO, for agents (Claude Code skill).
- [Debug-only setup per platform](docs/debug-only-setup.md), with release-leak checks.
- [CLI](docs/cli.md), [HTTP API](docs/http-api.md), [catalog file](docs/catalog.md).
- [Security and limits](docs/security.md), [related projects](docs/related-projects.md).
- [Trace verifier agent](agents/lsdo-verifier.md).

[MIT](LICENSE)
