// Configures Jazzer-based fuzz testing in a dedicated "fuzz" source set and task.
//
// Mirrors the approach used in smithy-java: fuzz tests live under src/fuzz/java and are run with the
// `fuzz` task, which enables Jazzer's fuzzing mode. Without JAZZER_FUZZ set, Jazzer @FuzzTest methods
// still run as ordinary JUnit tests over the seed corpus, so they also execute as regression tests.

// Workaround per: https://github.com/gradle/gradle/issues/15383
val Project.libs get() = the<org.gradle.accessors.dm.LibrariesForLibs>()

configure<SourceSetContainer> {
    val main by getting
    val test by getting

    // A separate "fuzz" source set so fuzz harnesses don't affect the main test compile/run. Sources
    // and seed corpora are picked up from src/fuzz/java and src/fuzz/resources by convention.
    create("fuzz") {
        compileClasspath += main.output + configurations["testRuntimeClasspath"] + configurations["testCompileClasspath"]
        runtimeClasspath += output + compileClasspath + test.runtimeClasspath + test.output
    }
}

dependencies {
    "fuzzImplementation"(libs.jazzer.junit)
    // The fuzz source set is run with JUnit Platform, so it needs the engine + launcher at runtime
    // (test's testRuntimeOnly deps don't transitively reach a separate source set).
    "fuzzImplementation"(libs.junit.jupiter.api)
    "fuzzRuntimeOnly"(libs.junit.jupiter.engine)
    "fuzzRuntimeOnly"(libs.junit.platform.launcher)
}

// fuzz harnesses are not production code; don't lint them with spotbugs.
tasks.findByName("spotbugsFuzz")?.apply {
    enabled = false
}

// junit6 / Jazzer require a modern bytecode level for the fuzz sources.
tasks.named<JavaCompile>("compileFuzzJava") {
    options.release.set(17)
}

val fuzz = tasks.register<Test>("fuzz") {
    description = "Run fuzz tests using Jazzer"
    group = "verification"

    // Fuzzing is non-deterministic; never treat the result as up-to-date.
    outputs.upToDateWhen { false }

    val fuzzSourceSet = project.the<SourceSetContainer>()["fuzz"]
    testClassesDirs = fuzzSourceSet.output.classesDirs
    classpath = fuzzSourceSet.runtimeClasspath

    useJUnitPlatform()

    // Isolate each fuzz target in its own JVM.
    setForkEvery(1)

    maxHeapSize = "2048m"

    // Enable Jazzer's actual fuzzing mode (otherwise @FuzzTest only replays the seed corpus).
    environment("JAZZER_FUZZ", "1")

    // Use value-profile feedback to get past magic-number / string comparisons.
    systemProperty("jazzer.valueprofile", "1")

    // Only instrument Smithy classes for coverage feedback.
    systemProperty("jazzer.instrumentation_includes", "software.amazon.smithy.**")

    // Per-target time budget; override with -Pfuzz.maxDuration=10m (default keeps CI runs short).
    val maxDuration = project.findProperty("fuzz.maxDuration") ?: "60s"
    systemProperty("jazzer.max_duration", maxDuration)
}
