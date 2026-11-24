# Changelog

## 0.1.0 (unreleased)

First release, extracted from the app it was built in.

- `lsdo-core`: the contract (`LsDoScreen`, `LsDoSpec`/`lsdoSpec`, `@Doc`, `LsDo`, `LsDoEntry`).
- `lsdo-compose`: `RegisterLsDo(name, screen)`.
- `lsdo-runtime`: the registry (idle waiting, availability), JSON schemas from serial descriptors,
  the loopback HTTP server (`/ls`, `/do`, `/ui`, `/catalog`, `X-LsDo` header), `UiNode`, the catalog.
- `lsdo-runtime-compose`: Android Compose semantics and iOS UIKit accessibility capture,
  `LsDoDevTools.start`.
- An action on a busy screen now waits (up to the timeout) for it to settle before its availability
  is judged, so a first action on a still-loading screen is not refused.
- `scripts/lsdo` (ls, do, ui, catalog, traces with screenshots) and the `lsdo-verifier` agent.
