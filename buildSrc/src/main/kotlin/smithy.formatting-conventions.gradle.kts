plugins {
    checkstyle
    id("com.diffplug.spotless")
}

// TODO: Add spotless java configuration
/*
 * Formatting
 * ==================
 * see: https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md
 */
spotless {
    // Formatting for build.gradle.kts files
    kotlinGradle {
        ktlint()
        indentWithSpaces()
        trimTrailingWhitespace()
        endWithNewline()
        licenseHeaderFile(
            "${project.rootDir}/config/spotless/license-header.txt",
            "import|tasks|apply|plugins|rootProject"
        )
    }
}
