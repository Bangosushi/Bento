plugins {
    id ("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    java
}

base    { archivesName = "bento-mod" }
version  = "1.0.0"
group    = "com.bento"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
    withSourcesJar()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
}

tasks.processResources {
    inputs.property("version", version)
    filesMatching("fabric.mod.json") { expand("version" to version) }
}

tasks.withType<JavaCompile> { options.release = 25 }
