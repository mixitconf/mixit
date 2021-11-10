import com.github.gradle.node.npm.task.NpmTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.5.31"
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.kapt") version kotlinVersion
    id("com.github.node-gradle.node") version "3.1.1"
    id("org.springframework.boot") version "2.2.2.RELEASE"
    id("io.spring.dependency-management") version "1.0.8.RELEASE"
}

version = "2022.0.0-SNAPHOT"

repositories {
    mavenCentral()
}

node {
    version.set("16.13.0")
    download.set(true)
}

dependencies {
    val commonmarkVersion = "0.11.0"

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.boot:spring-boot-starter-webflux") {
        exclude(module = "hibernate-validator")
    }
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.springframework:spring-context-indexer")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework.boot:spring-boot-devtools")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    runtimeOnly("de.flapdoodle.embed:de.flapdoodle.embed.mongo")
    implementation("com.samskivert:jmustache")
    implementation("com.atlassian.commonmark:commonmark:$commonmarkVersion")
    implementation("com.atlassian.commonmark:commonmark-ext-autolink:$commonmarkVersion")
    implementation("com.google.api-client:google-api-client:1.23.0")
    implementation("com.google.apis:google-api-services-gmail:v1-rev81-1.23.0")
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20171016.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("com.ninja-squad:springmockk:2.0.0")
    testImplementation("io.mockk:mockk:1.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("io.projectreactor:reactor-test")

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

/**
 * All images are copied in jar resources. Image optimization was made before with a Gulp plugin. Now we use a bot
 * on Github
 */
tasks.register<Copy>("copyImages") {
    from("src/main/images")
    include("**/*.svg", "**/*.png", "**/*.jpg")
    into(layout.buildDirectory.dir("resources/main/static/images"))
}

/**
 * Sass file are compiled by node libs, and autoprefixed, and minified
 */
tasks.register<NpmTask>("compileSass") {
    dependsOn(tasks.npmInstall)
    npmCommand.set(listOf("run", "scss"))
    args.set(listOf("--", "--out-dir", "${buildDir}/npm-output"))
    inputs.dir("src/main/sass")
    outputs.dir("${buildDir}/resources/main/static/css")
}

/**
 * For the moment website use CSS foundation framework based on Jquery. We need to copy vendor libs in the ressources
 * directory
 */
tasks.register<Copy>("copyJsVendors") {
    from("node_modules/foundation-sites/dist/js/foundation.js", "node_modules/jquery/dist/jquery.js")
    into(layout.buildDirectory.dir("resources/main/static/js"))
}

/**
 * Javascript files are genrated from Typescript
 */
tasks.register<NpmTask>("compileTypescript") {
    dependsOn(tasks.npmInstall)
    npmCommand.set(listOf("run", "typescript"))
    args.set(listOf("--", "--out-dir", "${buildDir}/npm-output"))
    inputs.dir("src/main/ts")
    outputs.dir("${buildDir}/resources/main/static/js")
}

/**
 * In dev mode tou can launch this task to
 * > automatically update css on sass file update,
 * > automatically update template  on template change,
 * > automatically update messages on i18n property update,
 */
tasks.register<NpmTask>("watch") {
    dependsOn(tasks.npmInstall)
    npmCommand.set(listOf("run", "watch"))
    args.set(listOf("--", "--out-dir", "${buildDir}/npm-output"))
}

tasks.getByName("processResources").dependsOn(
    "copyImages",
    "copyJsVendors",
    "compileSass",
    "compileTypescript"
)
