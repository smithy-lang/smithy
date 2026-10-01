plugins {
    id("me.champeau.jmh")
}

jmh {
    timeUnit = "us"
    if (project.hasProperty("jmh.includes")) {
        includes.set(listOf(project.property("jmh.includes") as String))
    }
    // Optional command-line overrides for run duration (keep annotation defaults
    // otherwise). Lets a bounded run be requested without editing benchmarks.
    if (project.hasProperty("jmh.fork")) {
        fork.set((project.property("jmh.fork") as String).toInt())
    }
    if (project.hasProperty("jmh.warmupIterations")) {
        warmupIterations.set((project.property("jmh.warmupIterations") as String).toInt())
    }
    if (project.hasProperty("jmh.iterations")) {
        iterations.set((project.property("jmh.iterations") as String).toInt())
    }
    if (project.hasProperty("jmh.profilers")) {
        // Split the profiler LIST on '|' so profiler OPTIONS (which use ';') survive.
        profilers.set((project.property("jmh.profilers") as String).split("|"))
    }
    // Forward the SMF fixture export directory into the forked JMH JVM so
    // benchmarks can write .smf/.json fixtures for out-of-process readers.
    if (project.hasProperty("smf.exportDir")) {
        jvmArgsAppend.add("-Dsmf.exportDir=" + project.property("smf.exportDir"))
    }
}

tasks {
    processJmhResources {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
    // Fix implicit dependency issue with Gradle 9+
    named("jmhRunBytecodeGenerator") {
        dependsOn("jar")
    }
}

// We don't need to lint benchmarks.
tasks.findByName("spotbugsJmh")?.apply {
    enabled = false
}
