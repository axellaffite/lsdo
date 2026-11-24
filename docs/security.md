# Security and limits

- **Debug builds only.** Keep the runtime out of release builds with the [recipes](debug-only.md) and their
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

