plugins {
    `java-platform`
    id("smithy.publishing-conventions")
}

description = "Smithy BOM (Bill of Materials) for dependency version management"

extra["displayName"] = "Smithy :: BOM"

configurePublishing {
    customComponent = components["javaPlatform"]
}

// Auto-discover all published subprojects and add them as BOM constraints.
gradle.projectsEvaluated {
    dependencies {
        constraints {
            rootProject.subprojects
                .filter { it != project && it.plugins.hasPlugin("maven-publish") }
                .sortedBy { it.path }
                .forEach { api(it) }
        }
    }
}
