# WorldScanner

**WorldScanner** scans Minecraft: Java Edition world saves directly from Anvil
region files (`.mca`) to find items, containers and entity inventories. It uses
**real NBT parsing** — no brittle byte-string searching — so results are
accurate and reliable across game versions.

The project is a clean Kotlin rewrite of the original Java tool, split into three
modules:

| Module  | Purpose                                                     |
| ------- | ----------------------------------------------------------- |
| `core`  | Library: NBT parser, Anvil region reader, analysis, search  |
| `cli`   | Console interface for running everything from the terminal  |
| `ui`    | Compose for Desktop GUI (MVVM): folder picker, SNBT filter, live results table |

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
- **Desktop GUI** (`ui` module): browse for a world folder, type an SNBT filter
  with live validation, scan with a progress bar and copy coordinates from the
  results table

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

| Option                 | Description                                        |
| ---------------------- | -------------------------------------------------- |
| `--item <id>`          | Item to find, repeatable (e.g. `--item diamond`)   |
| `--items=a,b,c`        | Comma-separated list of items                      |
| `--filter <expr>`      | `/give`-style component filter (alias `-f`)        |
| `--dimension=<dim>`    | Limit to `overworld`, `nether` or `end`            |
| `--region=<rx,rz>`     | Limit to a single region file                      |
| `--limit=<N>`          | Stop after N results                               |
| `--threads=<N>`        | Worker threads (default: CPU count)                |
| `--json=<file>`        | Export results to JSON                             |
| `--csv=<file>`         | Export results to CSV                              |
| `--summary`            | Compact output instead of a full table             |
| `--color`/`--no-color` | Force ANSI colors on/off                           |

### `--filter` / item component matching

`--filter` accepts Minecraft 1.20.5+ **item component** syntax (same style as the
`/give` command) to search by more than just the item id. The filter is parsed
with a full SNBT-style component parser and matches both the modern `components`
branch and the legacy pre-1.20.5 `tag` branch, so it works across versions:

```text
diamond_sword[enchantments={mending:1,sharpness:5},damage=150]
netherite_sword[custom_name="King's Blade"]
diamond[custom_data={owner:"koca"}]
[enchantments={silk_touch:1}]          # any item with Silk Touch
```

Supported components: `enchantments` (minimum level per enchantment), `damage`
(exact durability damage), `custom_name` (exact display name) and `custom_data`
(partial match, nested compounds recurse). Any other component key falls back to
a structural match. Combine it with `--item`, `--dimension`, `--region` or
`--limit` as usual. Results always include world coordinates, the region and
chunk, plus the holder type (block entity or entity).

```text
worldscanner find C:/worlds/survival --filter "diamond_sword[enchantments={mending:1}]"
worldscanner find C:/worlds/survival -f "netherite_sword[damage=50,custom_data={owner:\"koca\"}]"
```

> On Windows, quote embedded quotes so the shell preserves them, e.g.
> `-f "netherite_sword[custom_data={owner:\"koca\"}]"`, or write the filter to a
> file and pass it via `--filter=<value>` from a script.

## Minecraft version compatibility

WorldScanner is **not tied to a specific game version**: it parses NBT
structurally and never checks `DataVersion` to decide what to read. Supported:

- **Anvil region files** (`.mca`) — unchanged since 1.2
- **Modern chunk layout 1.18+** — `sections[].block_entities`, root `entities`
- **Item components 1.20.5+** — `minecraft:container`, `minecraft:bundle_contents`, `minecraft:block_entity_data` (this continues into current 26.x versions unchanged)
- **Legacy format (pre-1.20.5)** — `Level.TileEntities`, `tag.BlockEntityTag`

Run `worldscanner stats <world>` to see the world's chunk `DataVersion` range and
confirm compatibility before a full scan.

## Desktop GUI

The `ui` module provides a Compose for Desktop application on top of the same
`core` engine. It runs scans asynchronously (progress shown per chunk), validates
the SNBT filter as you type, and renders results in a table with dimension,
region, coordinates, chunk, container and item columns. Coordinates can be copied
straight to the clipboard.

```bat
gradlew.bat :ui:run                              # run the GUI
gradlew.bat :ui:createDistributable              # build an installer / app bundle
```

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

ui/src/main/kotlin/org/worldscanner/ui/
├── Main.kt           Compose application entry point
├── App.kt            screen layout: path picker, filter, controls, results table
├── ScanViewModel.kt  MVVM view-model (StateFlow<UiState>), async scans on Dispatchers.IO
└── theme/Theme.kt    dark Minecraft-inspired Material 3 theme
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
