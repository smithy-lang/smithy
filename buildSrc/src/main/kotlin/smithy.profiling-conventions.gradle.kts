plugins {
    id("me.champeau.jmh")
}

jmh {
    timeUnit = "us"
    if (project.hasProperty("jmh.includes")) {
        includes.set(listOf(project.property("jmh.includes") as String))
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
