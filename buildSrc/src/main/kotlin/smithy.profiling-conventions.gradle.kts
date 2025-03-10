plugins {
    id("me.champeau.jmh")
}

jmh {
    timeUnit = "us"
}

tasks {
    processJmhResources {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}

// We don't need to lint benchmarks.
tasks.findByName("spotbugsJmh")?.apply {
    enabled = false
}
