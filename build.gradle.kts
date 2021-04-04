import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val githubToken: String by project

plugins {
    kotlin("jvm") version "1.4.32"
    id("com.expediagroup.graphql") version "4.0.0-alpha.17"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    application
}

group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.32")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.2")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.2")
    implementation("ch.qos.logback:logback-classic:1.2.1")
    implementation("com.expediagroup:graphql-kotlin-ktor-client:4+") {
        exclude("com.expediagroup", "graphql-kotlin-client-serialization")
    }
    implementation("com.expediagroup", "graphql-kotlin-client-jackson", "4+")
    implementation("io.ktor:ktor-client-apache:1.5.2")
    implementation("io.ktor:ktor-client-logging:1.5.2")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
    mainClassName = "MainKt"
}

val graphqlIntrospectSchema by tasks.getting(GraphQLIntrospectSchemaTask::class) {
    endpoint.set("https://api.github.com/graphql")
    headers.set(
        mapOf(
            "Authorization" to "bearer $githubToken"
        )
    )
}

val graphqlGenerateClient by tasks.getting(com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask::class) {
    packageName.set("generated")
    schemaFile.set(graphqlIntrospectSchema.outputFile)
    dependsOn("graphqlIntrospectSchema")
}
project.setProperty("mainClassName", "MainKt")
tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("shadow")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "MainKt"))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}
