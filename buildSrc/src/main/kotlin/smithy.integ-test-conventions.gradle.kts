import org.gradle.api.tasks.testing.logging.TestExceptionFormat

configure<SourceSetContainer> {
    val main by getting
    val test by getting
    create("it") {
        compileClasspath += main.output + configurations["testRuntimeClasspath"] + configurations["testCompileClasspath"]
        runtimeClasspath += output + compileClasspath + test.runtimeClasspath + test.output
    }
}

// Add the integ test task
tasks {
    register<Test>("integ") {
        useJUnitPlatform()
        testClassesDirs = project.the<SourceSetContainer>()["it"].output.classesDirs
        classpath = project.the<SourceSetContainer>()["it"].runtimeClasspath

        // only log verbosely if the property is set like we do for unit-tests
        if (project.hasProperty("log-tests")) {
            testLogging {
                events("passed", "skipped", "failed")
                exceptionFormat = TestExceptionFormat.FULL
            }
        }
    }
}