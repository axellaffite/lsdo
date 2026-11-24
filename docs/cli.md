# The CLI

[`scripts/lsdo`](../scripts/lsdo) needs bash, curl and python3 (jq is used when present).

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
[`lsdo-verifier`](../agents/lsdo-verifier.md) agent (copy it into your project's `.claude/agents/`).

