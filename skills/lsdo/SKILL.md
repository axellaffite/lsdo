---
name: lsdo
description: Drive a running app through LS-DO (scripts/lsdo: catalog, ls, do, ui) to explore it, reproduce a bug, run a scenario or check a change on a device, simulator or desktop. Use when an app exposes LS-DO in its debug build, instead of tapping coordinates or reading screenshots.
---

# Driving an app with LS-DO

LS-DO lets you read each screen's state and run its actions by name. Use `scripts/lsdo` (or the
HTTP API: every request needs the `X-LsDo` header). Address: `LSDO_URL=http://127.0.0.1:8765` for an
iOS simulator or desktop app; unset, it forwards the port over adb to the Android device.

## Loop

1. **Plan with the catalog.** `scripts/lsdo catalog` lists every screen (shown or not), what it is,
   how to reach it, its action names. Run `catalog <screen>` only for a screen you will use: it
   gives each action's meaning and parameters, and the state's shape. Offline, read the app's
   `lsdo-catalog.json` if it has one.
2. **See where you are.** `scripts/lsdo ls` lists the screens on show: `state`, `busy`, `actions`
   available now (with JSON schemas), `unavailable` (exist, but not in this state). `ls <id>` for one.
3. **Act.** `scripts/lsdo do <id> <action> '<json params>'`. The answer comes once the screen is
   idle and holds every screen's new state: read it instead of running `ls` again.
4. **Check what is rendered.** `scripts/lsdo ui` prints the accessibility tree, one line per
   element: `role "text" = "value" {flags} [actions]`. Compare it with the state. `ui bounds` adds
   positions, `ui json` the tree. `LSDO_UI=1` makes every `do` return the UI too.
5. **Screenshot only for what the tree cannot show**: colors, images, truncation, overlap, layout,
   animation.

## Reading answers

- `ok: false` with a sentence: the screen refused (bad parameter, wrong state). The sentence says
  why; fix the call, do not retry it unchanged.
- `"<action>" is not available on "<id>" now. Available: ...`: the state does not allow it. Change
  the state first (see each action's `@Doc` for when it applies).
- `Not an action of "<id>"`: wrong name or missing/mistyped parameter. Check the schema.
- `No screen "<id>"`: not on show. Navigate there (catalog: "Reached by").
- `"<action>" failed: ...`: the app threw. That is a bug to report.
- `ok: true` with `Still busy after ...`: the work outlived the timeout; `ls` again later.
- A second screen of the same name has id `<name>-2`.

## Problems

- A request hangs or times out (`LSDO_TIMEOUT`, default 30 s): the app is likely in the background
  (iOS suspends it) or not running. Bring it to the foreground or relaunch it.
- Connection refused: not a debug build, the app is not started, or the port is not forwarded
  (`adb devices`; for a real iPhone, `iproxy 8765 8765`).
- `403`: the `X-LsDo` header is missing (only when calling the API directly).
- `ui` is `404`: this build has no UI capture; rely on state and screenshots.

## Traces

For a scenario someone must check: `export LSDO_TRACE=<new empty dir>` and run each step with
`scripts/lsdo do`. Each step saves `step-N.json` (intent, answer, UI after) and `step-N.png`
(`LSDO_SCREENSHOT=none` when no device can take one). Then hand the directory to the
`lsdo-verifier` agent with what the scenario should show.
