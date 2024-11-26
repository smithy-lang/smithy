//apply(plugin = "com.github.johnrengelman.shadow")
//
//// This is a little hacky, but currently needed to build a shadowed CLI JAR and smithy-syntax JAR with the same
//// customizations as other JARs.
//if (project.name != "smithy-cli" && project.name != "smithy-syntax") {
//    tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
//        isEnabled = false
//    }
//}
