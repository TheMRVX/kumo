# KUMO VPN Specific Guidelines & Invariants

## 1. Upstream & Repository Boundaries
- Origin: `TheMRVX/kumo-vpn` (Private repository).
- Upstream: `https://github.com/2dust/v2rayNG.git` (tracked on branch `master`).
- Do not add or merge third-party fork code unless explicitly requested by the user.

## 2. Localization Invariants (Strict EN & FA Only)
- The application officially supports ONLY two languages: English (`en`) and Persian (`fa`).
- Never reintroduce deleted localizations (`values-ar`, `values-bn`, `values-bqi-rIR`, `values-ru`, `values-vi`, `values-zh-rCN`, `values-zh-rTW`).
- `build.gradle.kts` MUST keep `localeFilters` limited strictly to `["en", "fa"]`.
- `Language.kt` and `arrays.xml` must only offer `AUTO`, `ENGLISH`, and `PERSIAN`.
- Strict 1:1 key parity rule: Any new string key added to `values/strings.xml` MUST be immediately added to `values-fa/strings.xml` with proper Persian translation.

## 3. Upstream Synchronization Discipline
- Whenever syncing/merging from `upstream/master`:
  - Consult the conflict resolution guide in `CHANGELOG.md`.
  - Discard upstream reintroductions of deleted locales, `TranslatorsActivity`, `translators.json`, or regional Russia routing (`custom_routing_white_russia`).
  - Maintain `app_name` as `KUMO VPN`.

## 4. UI Shell Modernization Architecture
- Keep native core, HEV tunnel, and VPN background service (`CoreVpnService`) intact and isolated.
- The new UI shell must be implemented as clean, modern Jetpack Compose screens that observe `MainViewModel` states without breaking underlying VPN lifecycle.
