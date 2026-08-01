package org.worldscanner.cli

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Parsed command-line arguments.
 */
data class CliArgs(
    val command: Command,
    val worldPath: Path,
    val itemTargets: List<String>,
    /** Raw `--filter` / `-f` value, e.g. `diamond_sword[enchantments={mending:1}]`. */
    val filter: String?,
    val dimension: String?,
    val regionX: Int?,
    val regionZ: Int?,
    val limit: Int?,
    val jsonPath: Path?,
    val csvPath: Path?,
    val summary: Boolean,
    val threads: Int?,
    val color: Boolean?,
    val progress: Boolean?,
) {
    enum class Command { INFO, FIND, STATS }
}

object CliArgsParser {

    fun parse(args: Array<String>): CliArgs? {
        if (args.isEmpty()) return null

        val commandName = args[0].lowercase()
        val command = when (commandName) {
            "info" -> CliArgs.Command.INFO
            "find" -> CliArgs.Command.FIND
            "stats" -> CliArgs.Command.STATS
            "help", "--help", "-h" -> {
                printUsage()
                return null
            }
            else -> return null
        }

        if (args.size < 2) return null
        val worldPath = Paths.get(args[1]).toAbsolutePath()

        val itemTargets = ArrayList<String>()
        var filter: String? = null
        var dimension: String? = null
        var regionX: Int? = null
        var regionZ: Int? = null
        var limit: Int? = null
        var jsonPath: Path? = null
        var csvPath: Path? = null
        var summary = false
        var threads: Int? = null
        var color: Boolean? = null
        var progress: Boolean? = null

        var i = 2
        while (i < args.size) {
            val arg = args[i]
            when {
                arg == "--summary" || arg == "--summary=true" -> summary = true

                arg.startsWith("--item=") -> itemTargets += normalizeItem(arg.removePrefix("--item="))
                arg == "--item" -> {
                    if (i + 1 < args.size) {
                        i += 1
                        itemTargets += normalizeItem(args[i])
                    }
                }
                arg.startsWith("--items=") -> itemTargets += normalizeItems(arg.removePrefix("--items="))

                arg.startsWith("--filter=") -> filter = arg.removePrefix("--filter=")
                arg == "--filter" || arg == "-f" -> {
                    if (i + 1 < args.size) {
                        i += 1
                        filter = args[i]
                    }
                }
                arg.startsWith("-f=") -> filter = arg.removePrefix("-f=")

                arg.startsWith("--dimension=") -> dimension = arg.removePrefix("--dimension=").lowercase()

                arg.startsWith("--region=") -> {
                    val coords = arg.removePrefix("--region=").split(",")
                    if (coords.size == 2) {
                        regionX = coords[0].trim().toIntOrNull()
                        regionZ = coords[1].trim().toIntOrNull()
                    }
                }

                arg.startsWith("--limit=") -> limit = arg.removePrefix("--limit=").toIntOrNull()

                arg.startsWith("--threads=") -> threads = arg.removePrefix("--threads=").toIntOrNull()

                arg == "--color" -> color = true
                arg == "--no-color" -> color = false

                arg == "--progress" -> progress = true
                arg == "--no-progress" -> progress = false

                arg.startsWith("--json=") -> jsonPath = Paths.get(arg.removePrefix("--json="))
                arg == "--json" -> {
                    if (i + 1 < args.size) {
                        i += 1
                        jsonPath = Paths.get(args[i])
                    }
                }

                arg.startsWith("--csv=") -> csvPath = Paths.get(arg.removePrefix("--csv="))
                arg == "--csv" -> {
                    if (i + 1 < args.size) {
                        i += 1
                        csvPath = Paths.get(args[i])
                    }
                }
            }
            i += 1
        }

        if (command == CliArgs.Command.FIND && itemTargets.isEmpty() && filter == null) return null
        if (jsonPath != null && csvPath != null && jsonPath == csvPath) return null

        return CliArgs(
            command = command,
            worldPath = worldPath,
            itemTargets = itemTargets.distinct(),
            filter = filter,
            dimension = dimension,
            regionX = regionX,
            regionZ = regionZ,
            limit = limit,
            jsonPath = jsonPath,
            csvPath = csvPath,
            summary = summary,
            threads = threads,
            color = color,
            progress = progress,
        )
    }

    private fun normalizeItem(raw: String): String {
        val value = raw.trim()
        return if (value.startsWith("minecraft:")) value else "minecraft:$value"
    }

    private fun normalizeItems(raw: String): List<String> =
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { normalizeItem(it) }

    fun printUsage() {
        System.out.println(
            """
            WorldScanner 3.0 - Minecraft Anvil world item scanner

            Usage:
              worldscanner info  <world-path>            Inspect a world
              worldscanner stats <world-path>            Item / block-entity / entity statistics
              worldscanner find  <world-path> --item <id> [options]
                                                         Search for items
              worldscanner --version                     Print the tool version

            Options:
              --item <id>          Item to find (repeatable), e.g. --item diamond
              --items=a,b,c        Comma-separated list of items to find
              --filter <expr>      /give-style component filter, e.g.
                                   "diamond_sword[enchantments={mending:1,sharpness:5},damage=150]"
                                   (short form: -f)
              --dimension=<dim>    Limit to overworld | nether | end
              --region=<rx,rz>     Limit to a single region file
              --limit=<N>          Stop after N results
              --threads=<N>        Worker threads (default: CPU count)
              --json=<file>        Write results to a JSON file
              --csv=<file>         Write results to a CSV file
              --summary            Print a compact summary instead of all rows
              --color / --no-color Force ANSI colors on/off
              --progress / --no-progress
                                   Toggle the progress bar (auto: only on a terminal)

            Examples:
              worldscanner find C:/worlds/survival --item diamond --summary
              worldscanner find C:/worlds/survival --items=shulker_box,bundle --limit=50
              worldscanner find C:/worlds/survival --filter "diamond_sword[enchantments={mending:1}]"
              worldscanner find C:/worlds/survival -f "netherite_sword[damage=50,custom_data={owner:\"koca\"}]"
              worldscanner find C:/worlds/survival --item netherite_sword --json=out.json
              worldscanner stats C:/worlds/survival --dimension=nether
            """.trimIndent(),
        )
    }
}
