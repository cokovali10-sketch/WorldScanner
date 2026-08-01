# WorldScanner

**WorldScanner** scans Minecraft: Java Edition world saves directly from Anvil
region files (`.mca`) to find items, containers and entity inventories. It uses
**real NBT parsing** — no brittle byte-string searching — so results are
accurate and reliable across game versions.

The project is a clean Kotlin rewrite of the original Java tool, split into two
modules:

| Module  | Purpose                                                     |
| ------- | ----------------------------------------------------------- |
| `core`  | Library: NBT parser, Anvil region reader, analysis, search  |
| `cli`   | Console interface for running everything from the terminal  |

## Features

- **True NBT parsing** (`TAG_Compound`, `TAG_List`, ...) instead of raw byte matching
- Scans **block entities** (chests, barrels, furnaces, hoppers, spawners, ...) and **entities** (dropped items, armor stands, minecarts, ...)
- **Recursive search inside nested containers** — e.g. a shulker box inside a chest
- Supports the **modern item format 1.20.5+** (`components`, `container.Items`) and the **legacy format** (`tag`, `BlockEntityTag`)
- Multi-threaded region scanning with shared positional reads and thread-local decompression buffers
- Decompression of **gzip**, **zlib**, **LZ4** and raw chunks
- `stats` command: item / block-entity / entity frequency, plus **world version detection** from chunk `DataVersion`
- Filters by dimension and region, result limits, `--threads` parallelism
- Progress bar and **ANSI colors** in the terminal
- JSON and CSV export
- `--version` prints the tool version

## Requirements

- **JDK 21+**
- Minecraft: Java Edition 1.18+ (older formats are read too)

## Quick start (Windows)

```bat
worldscanner.bat help
worldscanner.bat info  C:/path/to/world
worldscanner.bat stats C:/path/to/world
worldscanner.bat find  C:/path/to/world --item diamond --summary
worldscanner.bat find  C:/path/to/world --items=shulker_box,bundle --limit=50
worldscanner.bat find  C:/path/to/world --item netherite_ingot --json=out.json --csv=out.csv
```

On macOS / Linux the same commands work via `./worldscanner`.

> On the **first run** the launcher builds a standalone distribution automatically,
> so subsequent runs are fast and do not need Gradle. `run.bat` is an alias for
> `worldscanner.bat` for backward compatibility.

## Command reference

```
worldscanner info  <world-path>               Inspect a world
worldscanner stats <world-path>               Item / block-entity / entity statistics
worldscanner find  <world-path> --item <id>   Search for items
worldscanner --version                        Print the tool version
worldscanner help                             Show usage
```

### `find` options

| Option               | Description                                        |
| -------------------- | -------------------------------------------------- |
| `--item <id>`        | Item to find, repeatable (e.g. `--item diamond`)   |
| `--items=a,b,c`      | Comma-separated list of items                      |
| `--dimension=<dim>`  | Limit to `overworld`, `nether` or `end`            |
| `--region=<rx,rz>`   | Limit to a single region file                      |
| `--limit=<N>`        | Stop after N results                               |
| `--threads=<N>`      | Worker threads (default: CPU count)                |
| `--json=<file>`      | Export results to JSON                             |
| `--csv=<file>`       | Export results to CSV                              |
| `--summary`          | Compact output instead of a full table             |
| `--color`/`--no-color` | Force ANSI colors on/off                         |

## Minecraft version compatibility

WorldScanner is **not tied to a specific game version**: it parses NBT
structurally and never checks `DataVersion` to decide what to read. Supported:

- **Anvil region files** (`.mca`) — unchanged since 1.2
- **Modern chunk layout 1.18+** — `sections[].block_entities`, root `entities`
- **Item components 1.20.5+** — `minecraft:container`, `minecraft:bundle_contents`, `minecraft:block_entity_data` (this continues into current 26.x versions unchanged)
- **Legacy format (pre-1.20.5)** — `Level.TileEntities`, `tag.BlockEntityTag`

Run `worldscanner stats <world>` to see the world's chunk `DataVersion` range and
confirm compatibility before a full scan.

## Building from source

```bat
gradlew.bat build          # compile + run all tests
gradlew.bat :core:test     # run only the core tests
```

### Standalone distribution (no Gradle needed at runtime)

```bat
gradlew.bat :cli:installDist
cli\build\install\worldscanner\bin\worldscanner.bat find C:/path/to/world --item diamond
```

## Architecture

```
core/src/main/kotlin/org/worldscanner/core/
├── nbt/              NbtType, NbtTag (sealed), NbtReader, NbtWriter
├── anvil/            RegionFile (positional reads), ChunkCompression, RegionDiscovery
├── model/            ItemStack, BlockPos, SearchResult, ItemSource, DimensionType
├── scan/             ScanQuery, ChunkScanner, ChunkStructure, ItemStackExtractor,
│                     WorldScanner (visitor + progress), WorldAnalysis (ChunkAnalyzer)
└── SearchEngine.kt   facade API: find / analyze / describe

cli/src/main/kotlin/org/worldscanner/cli/
├── Main.kt           entry point, command routing
├── CliArgs.kt        argument parsing
├── Ansi.kt           ANSI colors, ProgressBar.kt
└── ReportRenderer.kt, JsonExporter.kt, CsvExporter.kt
```

Data flow: `RegionFile` → chunk decompression → `NbtReader` → `ChunkScanner` →
recursive container walk → `SearchResult`.

## CI

The GitHub Actions workflow in `.github/workflows/ci.yml` builds the project and
runs all tests on JDK 21 for every push and pull request.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](LICENSE)
