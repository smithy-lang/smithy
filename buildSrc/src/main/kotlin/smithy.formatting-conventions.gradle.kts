import com.diffplug.spotless.FormatterFunc
import java.io.Serializable
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
        leadingTabsToSpaces()
        endWithNewline()
        eclipse().configFile("${project.rootDir}/config/spotless/formatting.xml")

        // Fixes for some strange formatting applied by eclipse:
        // see: https://github.com/kamkie/demo-spring-jsf/blob/bcacb9dc90273a5f8d2569470c5bf67b171c7d62/build.gradle.kts#L159
        // These have to be implemented with anonymous classes this way instead of lambdas because of:
        // https://github.com/diffplug/spotless/issues/2387
        custom("Lambda fix", object : Serializable, FormatterFunc {
            override fun apply(input: String) : String {
                return input.replace("} )", "})").replace("} ,", "},")
            }
        })
        custom("Long literal fix", object : Serializable, FormatterFunc {
            override fun apply(input: String) : String {
                return Pattern.compile("([0-9_]+) [Ll]").matcher(input).replaceAll("\$1L")
            }
        })

        // Static first, then everything else alphabetically
        removeUnusedImports()
        importOrder("\\#", "")
        // Ignore generated code for formatter check
        targetExclude("*/build/**/*.*")
    }

    // Formatting for build.gradle.kts files
    kotlinGradle {
        ktlint()
        leadingTabsToSpaces()
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
