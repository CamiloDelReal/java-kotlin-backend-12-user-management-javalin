import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
    application
}

group = "org.xapps.services"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin:5.6.3")

    implementation("org.slf4j:slf4j-simple:2.0.7")

    implementation("org.jetbrains.exposed:exposed-core:0.44.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.44.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.0")

    implementation("com.mysql:mysql-connector-j:8.2.0")

    implementation("at.favre.lib:bcrypt:0.10.2")
//    implementation("com.auth0:java-jwt:4.4.0")
    implementation("com.github.kmehrunes:javalin-jwt:0.7.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.7.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.12.7.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")


    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}