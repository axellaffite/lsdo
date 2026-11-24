# HTTP API

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

