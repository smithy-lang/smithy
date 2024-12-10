import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

// JReleaser publishes artifacts from a local staging repository, rather than maven local.
// https://jreleaser.org/guide/latest/examples/maven/staging-artifacts.html#_gradle
fun Project.stagingDir(): Provider<Directory> {
   // We should use the root build directory
   return rootProject.layout.buildDirectory.dir("staging")
}
