plugins {
    kotlin("jvm")
    application
}

description = "WorldScanner command-line interface for scanning Minecraft worlds from the console."

dependencies {
    implementation(project(":core"))
}

application {
    applicationName = "worldscanner"
    mainClass.set("org.worldscanner.cli.MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.worldscanner.cli.MainKt"
        attributes["Implementation-Title"] = "WorldScanner CLI"
        attributes["Implementation-Version"] = project.version
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
