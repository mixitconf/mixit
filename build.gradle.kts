import com.moowork.gradle.gulp.GulpTask
import com.moowork.gradle.node.yarn.YarnInstallTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.3.11"
    val nodePluginVersion = "1.2.0"
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.kapt") version kotlinVersion
    id("com.moowork.node") version nodePluginVersion
    id("com.moowork.gulp") version nodePluginVersion
    id("org.springframework.boot") version "2.1.2.RELEASE"
    id("io.spring.dependency-management") version "1.0.6.RELEASE"
}

version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

node {
    version = "9.2.0"
    yarnVersion = "1.6.0"
    download = true
}

dependencies {
    val commonmarkVersion = "0.11.0"

    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile("org.jetbrains.kotlin:kotlin-reflect")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin")

    compile("org.springframework.boot:spring-boot-starter-webflux") {
        exclude(module = "hibernate-validator")
    }
    compileOnly("org.springframework:spring-context-indexer")
    compile("org.springframework.boot:spring-boot-starter-mail")
    compile("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    compile("org.springframework.boot:spring-boot-devtools")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    
    runtime("de.flapdoodle.embed:de.flapdoodle.embed.mongo")
    compile("com.samskivert:jmustache")
    compile("com.atlassian.commonmark:commonmark:$commonmarkVersion")
    compile("com.atlassian.commonmark:commonmark-ext-autolink:$commonmarkVersion")
    compile("com.google.api-client:google-api-client:1.23.0")
    compile("com.google.apis:google-api-services-gmail:v1-rev81-1.23.0")
    compile("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20171016.1")

    testCompile("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testCompile("io.projectreactor:reactor-test")
}

task<GulpTask>("gulpBuild") {
    dependsOn(YarnInstallTask.NAME)
    inputs.dir("src/main/sass")
    inputs.dir("src/main/ts")
    inputs.dir("src/main/images")
    outputs.dir("build/resources/main/static")
}

task<GulpTask>("gulpClean") {
    dependsOn(YarnInstallTask.NAME)
    inputs.dir("build/.tmp")
    outputs.dir("build/resources/main/static")
}

tasks.getByName("processResources").dependsOn("gulpBuild")
