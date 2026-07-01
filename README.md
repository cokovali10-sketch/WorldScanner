# WorldScanner

WorldScanner is a Java utility for scanning Minecraft Java Edition worlds directly from region files (.mca). It is designed for server admins, map analysis, and quick exploration of world data without external dependencies for the core flow.

## Features

- Scan world folders and discover region files
- Search blocks, items and entities
- Filter by dimension, region, coordinates and radius
- Limit results and print compact summaries
- Export results to JSON, CSV and TXT
- Run in CLI, interactive, menu, or GUI-style launcher modes
- Support multi-target searches in a single run

## Quick start on Windows

Run the launcher:

```bat
run.bat
```

Or run directly:

```bat
gradlew.bat run --args="--interactive"
```

## Example commands

```bat
gradlew.bat run --args="C:/path/to/world scan --summary"
gradlew.bat run --args="C:/path/to/world find block minecraft:diamond_ore --limit=10 --summary"
gradlew.bat run --args="C:/path/to/world find multi block;item;entity minecraft:diamond_ore,minecraft:diamond;minecraft:zombie --summary"
gradlew.bat run --args="C:/path/to/world export json C:/tmp/results.json --summary"
```

## Project status

The project is currently a practical prototype with a solid CLI and search foundation, suitable for further expansion and GitHub publishing.
