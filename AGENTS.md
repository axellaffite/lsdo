# AGENTS.md: adding LS-DO to a screen

For coding agents working on an app that uses LS-DO. To drive an app instead, read
[skills/lsdo/SKILL.md](skills/lsdo/SKILL.md).

## Add a screen

1. **State**: a `@Serializable` data class holding what the user sees and what decides which
   actions apply. Put `@Doc` on fields whose meaning is not obvious. Never put secrets in it
   (tokens, passwords, keys): drop them or replace them (`hasPassword: Boolean`, `"••••"`).
2. **Intent**: a sealed `@Serializable` interface, one subclass per action.
   - `@SerialName("snake_case")` is the action's name. Without it the name is the class's full name.
   - `@Doc` says what it does **and when it applies** ("One folder up. Not at the top").
   - Parameters are constructor properties; give defaults where the UI has them; `@Doc` them if unclear.
3. **Screen**: implement `LsDoScreen<State, Intent>`, usually on the ViewModel.
   - `override val lsdo = lsdoSpec<State, Intent>()` and `override val state: StateFlow<State>`.
   - When `state` is a serializable view of the UI state, derive it with `mapState`/`combineState`,
     never `stateIn(scope, SharingStarted.Eagerly)`: that keeps a coroutine alive on the scope's dispatcher.
   - `perform(intent)` calls **the same functions the UI calls**. Never write a second code path.
   - Return `null` when done, or a sentence saying why it was refused and what to do instead.
     Throw only for real errors (they come back as `"<action>" failed: ...`).
   - `availableIntents(state)`: the action names that make sense now. Leave the default (null)
     only if every action always applies.
   - `isBusy(state)`: true while loading or saving. A `do` waits for it before checking availability
     and before answering. If work runs in the background without a busy flag, add one.
   - Keep the mapping in its own file (`XxxLsDo.kt`: intent type, `performIntent`, `intentsFor`)
     when the ViewModel is large.
4. **Register**: `RegisterLsDo("name", screen)` in the screen's composable (`lsdo-compose`).
   Without Compose: `val remove = LsDo.register("name", screen)`, then `remove()` when it leaves.
5. **Catalog**: add `LsDoEntry("name", "<what it is>. Reached by: <route>.", lsdoSpec<State, Intent>())`
   to the app's catalog list. If the app commits `lsdo-catalog.json`, regenerate it (see
   [docs/catalog.md](docs/catalog.md)).
6. **Test** through `LsDoRegistry` (`lsdo-runtime` as a test dependency), not by calling `perform`:

   ```kotlin
   val registry = LsDoRegistry()
   registry.register("counter", screen)
   val outcome = registry.perform("counter", LsDoJson.parseToJsonElement("""{"type":"add","n":2}"""))
   assertTrue(outcome.ok, outcome.message)
   ```

   Cover: each action's effect, each refusal message, availability in each state, busy waiting.

## Rules

- `lsdo-runtime` and `lsdo-runtime-compose` are debug-only. Never add them to a release
  configuration or to `src/main`/`iosMain` directly; follow [docs/debug-only-setup.md](docs/debug-only-setup.md).
- Every state field and action an agent needs must be in the types: the schemas come from them.
- Renaming a `@SerialName` breaks scripts and traces: treat names as API.

## Verify on a device

1. Install the debug build and open the screen.
2. `scripts/lsdo ls <name>`: the state is right, the actions listed match the state, `unavailable`
   holds the rest.
3. `scripts/lsdo do <name> <action> '<json>'` for each action: `ok`, the new state, the refusals.
4. `scripts/lsdo ui`: every state fact a user sees has a line; no control for an unavailable action.
5. For a scenario: `LSDO_TRACE=<dir> scripts/lsdo do ...` per step, then give `<dir>` to the
   [lsdo-verifier](agents/lsdo-verifier.md) agent and fix its mismatches.
