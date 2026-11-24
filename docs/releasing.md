# Releasing

Releases go to Maven Central (`io.github.axellaffite.lsdo`) from a tag, through
[.github/workflows/release.yml](../.github/workflows/release.yml).

1. In `CHANGELOG.md`, turn `## X.Y.Z (unreleased)` into `## X.Y.Z (YYYY-MM-DD)` and commit to `main`.
   The workflow refuses a tag without a `## X.Y.Z` entry.
2. Tag and push: `git tag vX.Y.Z && git push origin vX.Y.Z`.
3. The workflow (macOS, for the iOS klibs) runs `./gradlew build`, then
   `publishAndReleaseToMavenCentral` with `VERSION_NAME=X.Y.Z`: it uploads, waits for the Central
   Portal to validate, and releases. Artifacts show up on Maven Central 10 to 30 minutes later.

Repository secrets (Settings, Secrets and variables, Actions):

| Secret | What |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` | Central Portal user token name |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal user token password |
| `SIGNING_KEY` | `gpg --export-secret-keys --armor <key id>` (public key on a keyserver) |
| `SIGNING_KEY_PASSWORD` | The key's passphrase; leave unset if it has none |

Locally, `./gradlew publishToMavenLocal` needs no key; `-PVERSION_NAME=X.Y.Z` sets the version
(default `0.1.0-SNAPSHOT` in `gradle.properties`). Signing turns on when `signingInMemoryKey` is set.
