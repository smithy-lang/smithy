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
