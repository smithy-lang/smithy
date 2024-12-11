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
    // https://github.com/diffplug/spotless/issues/1819
    implementation(
        when {
            JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_11) -> libs.spotless.jdk11
            else -> libs.spotless.jdk8
        }
    )
    implementation(libs.jmh)
    // https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
