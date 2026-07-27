/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import java.util.zip.ZipFile

plugins {
    id("smithy.module-conventions")
}

description = "Packages this module's skill files as a versioned, dependency-consumable artifact. The " +
    "artifact is inert content: reviewed Agent Skill files packaged verbatim, with no code and nothing " +
    "that runs at install or agent runtime."

extra["displayName"] = "Smithy :: Agent Skills"
extra["moduleName"] = "software.amazon.smithy.agentskills"

// This module has no Java source. The reviewed skill files live under src/main/resources/skills/, so the
// standard java resource pipeline packages them into the JAR under a top-level skills/ entry path with no
// custom wiring. The content is consumed by unpacking at build time; nothing discovers, enumerates, or
// loads it at runtime.

val skillsSourceDir = layout.projectDirectory.dir("src/main/resources/skills")

// Fail the build if the JAR's skills/ entries do not byte-for-byte match the source tree. The resource
// pipeline copies verbatim by default, and this check guards against any future change that would filter,
// expand, or otherwise transform the content: a consumer unpacking the JAR must get exactly what a
// consumer cloning the repository at the same commit gets.
val verifySkillsVerbatim by tasks.registering {
    description = "Verifies the packaged skills/ entries are byte-identical to the module's skills/ source."
    group = "verification"
    dependsOn(tasks.jar)

    val sourceDir = skillsSourceDir.asFile
    val jarFile = tasks.jar.flatMap { it.archiveFile }

    inputs.dir(sourceDir)
    inputs.file(jarFile)

    doLast {
        // Map every source file under skills/ to its relative "skills/<path>" entry name and content hash.
        val sourceEntries = sourceDir.walkTopDown()
            .filter { it.isFile }
            .associate { file ->
                "skills/" + sourceDir.toPath().relativize(file.toPath()).toString().replace('\\', '/') to
                    file.readBytes().toList()
            }

        val jarSkillEntries = mutableMapOf<String, List<Byte>>()
        ZipFile(jarFile.get().asFile).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith("skills/") }
                .forEach { entry ->
                    jarSkillEntries[entry.name] = zip.getInputStream(entry).readBytes().toList()
                }
        }

        val missingFromJar = sourceEntries.keys - jarSkillEntries.keys
        val extraInJar = jarSkillEntries.keys - sourceEntries.keys
        val mismatched = sourceEntries.keys.intersect(jarSkillEntries.keys)
            .filter { sourceEntries[it] != jarSkillEntries[it] }

        val problems = buildList {
            if (missingFromJar.isNotEmpty()) add("missing from JAR: ${missingFromJar.sorted()}")
            if (extraInJar.isNotEmpty()) add("unexpected extra entries in JAR: ${extraInJar.sorted()}")
            if (mismatched.isNotEmpty()) add("content differs from source: ${mismatched.sorted()}")
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "smithy-agent-skills JAR is not a verbatim copy of the module's skills/ source:\n" +
                    problems.joinToString("\n") { "  - $it" }
            )
        }

        logger.lifecycle("Verified ${sourceEntries.size} skills/ entries are packaged verbatim.")
    }
}

tasks.check {
    dependsOn(verifySkillsVerbatim)
}
