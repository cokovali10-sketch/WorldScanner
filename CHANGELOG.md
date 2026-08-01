# Changelog

All notable changes to this project are documented in this file.

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
