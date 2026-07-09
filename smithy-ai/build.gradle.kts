/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import java.nio.file.Files

plugins {
    id("smithy.module-conventions")
}

description = "Bundles Smithy AI agent skills plus an API to discover them."

extra["displayName"] = "Smithy :: AI"
extra["moduleName"] = "software.amazon.smithy.ai"

dependencies {
    // IoUtils for reading bundled resources. api because AiSkill surfaces reads to consumers.
    api(project(":smithy-utils"))
}

// The browsable content roots. These live at the module root (not under src/main/resources) so the
// tree is publishable to GitHub as-is and mirrors github.com/aws/agent-toolkit-for-aws. The build
// stages them onto the classpath under META-INF/smithy-ai/ (see stageAiContent) and emits an index
// beside them (see generateAiIndex), because a JAR cannot enumerate a directory at runtime -- the
// same reason ModelDiscovery reads META-INF/smithy/manifest rather than listing a resource dir.
val contentRoots = mapOf("skills" to layout.projectDirectory.dir("skills"))

val stagedContentDir = layout.buildDirectory.dir("generated-resources/ai-content")

val stageAiContent by tasks.registering(Sync::class) {
    into(stagedContentDir.map { it.dir("META-INF/smithy-ai") })
    for ((name, dir) in contentRoots) {
        from(dir) { into(name) }
    }
}

// Emit one index file per content root listing every file leaf (path relative to the root). The
// first path segment is the skill name; the remaining segments are the per-item file list an
// installer needs to copy the whole tree. Sorted for build determinism.
val generateAiIndex by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated-resources/ai-index/META-INF/smithy-ai")
    // Track only the roots that actually exist as inputs; a missing root simply produces an empty
    // index, but the index file still ships.
    contentRoots.values
            .filter { Files.isDirectory(it.asFile.toPath()) }
            .forEach { inputs.dir(it) }
    outputs.dir(outDir)
    doLast {
        for ((name, dir) in contentRoots) {
            val root = dir.asFile.toPath()
            val lines = if (Files.isDirectory(root)) {
                Files.walk(root).use { stream ->
                    stream.filter { Files.isRegularFile(it) }
                            .map { root.relativize(it).toString().replace('\\', '/') }
                            .sorted()
                            .toList()
                }
            } else {
                emptyList()
            }
            val index = outDir.get().file("$name.index").asFile
            index.parentFile.mkdirs()
            index.writeText(lines.joinToString("\n", postfix = if (lines.isEmpty()) "" else "\n"))
        }
    }
}

sourceSets {
    main {
        resources {
            // Map the generated dirs onto the resource path. `builtBy` attaches the producing task
            // to each dir so every consumer (processResources, sourcesJar, ...) infers the task
            // dependency -- Gradle 9 fails the build on an undeclared implicit dependency otherwise.
            srcDir(files(stagedContentDir).builtBy(stageAiContent))
            srcDir(files(layout.buildDirectory.dir("generated-resources/ai-index")).builtBy(generateAiIndex))
        }
    }
}
