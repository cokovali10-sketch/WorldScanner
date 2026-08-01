# Changelog

All notable changes to this project are documented in this file.

## [Unreleased]

### Added
- Item **component filter engine** in `core` (`org.worldscanner.core.filter`):
  - SNBT/component parser (`ItemFilterParser`) for `/give`-style filters such as
    `diamond_sword[enchantments={mending:1,sharpness:5},damage=150]`
  - `ItemMatcher` interface and `ComponentItemMatcher` implementation matching
    both the modern `components` branch (1.20.5+) and the legacy `tag` branch
  - Supported components: `enchantments` (minimum level), `damage`, `custom_name`,
    `custom_data` (partial/nested match) and arbitrary raw components
- CLI `--filter` / `-f` flag on the `find` command
- Result table now includes a `chunk` coordinate column (region + chunk + X/Y/Z)
- **Desktop GUI** module (`ui`): Compose for Desktop app with MVVM
  (`ScanViewModel` + `StateFlow<UiState>`), folder picker, live SNBT filter
  validation, asynchronous scanning with per-chunk progress and a results table
  with a copy-coordinates action
- `gui.bat` launcher (and `run.bat gui` alias) for starting the GUI on Windows

## [2.0.0] - 2026-08-01

### Added
- Complete rewrite in Kotlin (Gradle multi-module: `core` + `cli`)
- True binary NBT parser (`NbtReader` / `NbtWriter`) instead of byte-string searching
- Deep parsing of block entities and entities with recursive search in nested containers (shulker boxes, bundles)
- Support for the modern item format 1.20.5+ and the legacy format
- Multi-threaded region scanning with shared positional reads and progress callbacks
- Decompression of gzip, zlib, LZ4 and raw chunks
- `stats` command: item / block-entity / entity frequency and world version detection via chunk `DataVersion`
- `SearchEngine` facade API (`find` / `analyze` / `describe`)
- Filters by dimension and region, result limits, `--threads` parallelism
- ANSI colors and a progress bar in the CLI (`--color` / `--no-color`), `--version` flag
- Standalone distribution via `:cli:installDist`, one-command launchers (`worldscanner.bat` / `./worldscanner`)
- GitHub Actions CI (`.github/workflows/ci.yml`)
- JSON and CSV export
- JUnit tests: NBT round-trip, nested-container recursion, end-to-end `.mca` scanning, world analysis
- MIT license

### Removed
- Legacy Java implementation with `containsKeyword` searching in raw chunk bytes
