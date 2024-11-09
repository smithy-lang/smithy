pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        val smithyGradleVersion : String by settings
        id("software.amazon.smithy.gradle.smithy-jar") version smithyGradleVersion
    }
}

rootProject.name = "smithy"

val subProjects = listOf(
    "smithy-aws-iam-traits",
    "smithy-aws-traits",
    "smithy-aws-apigateway-traits",
    "smithy-aws-apigateway-openapi",
    "smithy-aws-protocol-tests",
    "smithy-cli",
    "smithy-codegen-core",
    "smithy-build",
    "smithy-model",
    "smithy-diff",
    "smithy-linters",
    "smithy-mqtt-traits",
    "smithy-jsonschema",
    "smithy-openapi",
    "smithy-openapi-traits",
    "smithy-utils",
    "smithy-protocol-test-traits",
    "smithy-jmespath",
    "smithy-waiters",
    "smithy-aws-cloudformation-traits",
    "smithy-aws-cloudformation",
    "smithy-validation-model",
    "smithy-rules-engine",
    "smithy-smoke-test-traits",
    "smithy-syntax",
    "smithy-aws-endpoints",
    "smithy-aws-smoke-test-model",
    "smithy-protocol-traits",
    "smithy-protocol-tests",
    "smithy-trait-codegen"
)

subProjects.forEach {
    include(":$it")
}