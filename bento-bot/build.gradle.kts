plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dm)
    java
}

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }

configurations.compileOnly { extendsFrom(configurations.annotationProcessor.get()) }

dependencies {
    implementation(libs.spring.web)
    implementation(libs.spring.jpa)
    implementation(libs.spring.validation)
    // Exclude opus — we don't need voice
    implementation(libs.jda) { exclude(group = "club.minnced", module = "opus-java") }
    implementation(libs.flyway.core)
    implementation(libs.flyway.pg)
    runtimeOnly(libs.postgresql)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(libs.spring.test)
}

tasks.withType<Test> { useJUnitPlatform() }
