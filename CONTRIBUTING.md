# Contributing

Thanks for taking the time to contribute to WorldScanner!

## Project layout

- `core/` — library module (NBT, Anvil, model, filter, scan, analysis)
- `cli/` — console application built on top of `core`
- `ui/` — Compose for Desktop GUI built on top of `core`

## Getting started

```bat
gradlew.bat build          # compile + run all tests
gradlew.bat :core:test     # core tests only
gradlew.bat :ui:run        # run the desktop GUI for manual testing
```

Requirements: JDK 21+.

## How to contribute

1. **Open an issue** describing the bug or feature before starting large changes.
2. **Fork** the repository and create a branch for your work.
3. Follow the existing code style (see `.editorconfig`, Kotlin official style).
4. Add or update **tests** for your change — all logic lives in `core/src/test`.
5. Run `gradlew build` and make sure everything passes.
6. Open a pull request with a clear description.

## Compatibility notes

- The scanner must stay **version-agnostic**: parse NBT structurally, never gate
  behaviour on a hard-coded `DataVersion`.
- When adding container paths, update `ItemStackExtractor` (and its tests) — it
  is the single place that knows where Minecraft stores inventories.
- When changing the filter grammar, update `ItemFilterParser`, `ComponentItemMatcher`
  and their tests, and keep the GUI's live validation and the CLI `--filter` in sync.
- Keep the CLI output readable both with and without ANSI colors.
- GUI behaviour (state, validation, scan lifecycle) lives in `ScanViewModel`
  (`ui/src/main/kotlin/org/worldscanner/ui/ScanViewModel.kt`); `App.kt` is pure
  layout. Keep business logic out of composables.

## Releasing

1. Bump `version` in `build.gradle.kts` and `Main.kt`'s fallback constant.
2. Update `CHANGELOG.md`.
3. Tag the release and push.
