import com.moowork.gradle.gulp.GulpTask
import com.moowork.gradle.node.yarn.YarnInstallTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
        maven { setUrl("https://repo.spring.io/milestone") }
        maven { setUrl("https://repo.spring.io/snapshot") }
    }

    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:2.0.0.BUILD-SNAPSHOT")
    }
}

plugins {
    val kotlinVersion = "1.1.4-2"
    val nodePluginVersion = "1.1.1"
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("com.moowork.node") version nodePluginVersion
    id("com.moowork.gulp") version nodePluginVersion
    id("io.spring.dependency-management") version "1.0.3.RELEASE"
}

apply {
    plugin("org.springframework.boot")
}

version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { setUrl("https://repo.spring.io/milestone") }
    maven { setUrl("https://repo.spring.io/snapshot") }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
		freeCompilerArgs = listOf("-Xjsr305-annotations=enable")
    }
}

node {
    version = "6.9.4"
    download = true
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jre8")
    compile("org.jetbrains.kotlin:kotlin-reflect")

    compile("org.springframework.boot:spring-boot-starter-webflux") {
        exclude(module = "hibernate-validator")
    }
    compileOnly("org.springframework:spring-context-indexer")
    compile("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    testCompile("org.springframework.boot:spring-boot-starter-test")
    runtime("de.flapdoodle.embed:de.flapdoodle.embed.mongo")
    compile("com.samskivert:jmustache:1.13")
    compile("com.atlassian.commonmark:commonmark:0.9.0")
    compile("com.atlassian.commonmark:commonmark-ext-autolink:0.9.0")

	compile("com.google.code.findbugs:jsr305:3.0.2") // Needed for now, could be removed when KT-19419 will be fixed
    
    testCompile("io.projectreactor:reactor-test")

    compile("com.fasterxml.jackson.module:jackson-module-kotlin")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}

task<GulpTask>("gulpBuild") {
    dependsOn(YarnInstallTask.NAME)
    inputs.dir("src/main/sass")
    inputs.dir("src/main/ts")
    inputs.dir("src/main/images")
    outputs.dir("build/resources/main/static")
    args = listOf("build")
}

task<GulpTask>("gulpClean") {
    dependsOn(YarnInstallTask.NAME)
    inputs.dir("build/.tmp")
    outputs.dir("build/resources/main/static")
    args = listOf("clean")
}

tasks.getByName("processResources").dependsOn("gulpBuild")
tasks.getByName("clean").dependsOn("gulpClean")