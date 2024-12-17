import java.util.regex.Pattern

plugins {
    id("com.diffplug.spotless")
}

/*
 * Formatting
 * ==================
 * see: https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md
 */
spotless {
    java {
        // Enforce a common license header on all files
        licenseHeaderFile("${project.rootDir}/config/spotless/license-header.txt")
            .onlyIfContentMatches("^((?!SKIPLICENSECHECK)[\\s\\S])*\$")
        indentWithSpaces()
        endWithNewline()
        eclipse().configFile("${project.rootDir}/config/spotless/formatting.xml")
        // Fixes for some strange formatting applied by eclipse:
        // see: https://github.com/kamkie/demo-spring-jsf/blob/bcacb9dc90273a5f8d2569470c5bf67b171c7d62/build.gradle.kts#L159
        custom("Lambda fix") { it.replace("} )", "})").replace("} ,", "},") }
        custom("Long literal fix") { Pattern.compile("([0-9_]+) [Ll]").matcher(it).replaceAll("\$1L") }
        // Static first, then everything else alphabetically
        removeUnusedImports()
        importOrder("\\#", "")
        // Ignore generated code for formatter check
        targetExclude("*/build/**/*.*")
    }

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
    tasks {
        // If the property "noFormat" is set, don't auto-format source file (like in CI)
        if(!project.hasProperty("noFormat")) {
            build {
                dependsOn(spotlessApply)
            }
        }
    }
}
