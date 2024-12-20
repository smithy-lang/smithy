plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Plugins used by buildSrc scripts
    implementation(libs.spotbugs)
    implementation(libs.spotless)
    implementation(libs.jmh)
    // https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
