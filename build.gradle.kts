import com.github.gradle.node.npm.task.NpmTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.6.10"

    id("org.springframework.boot") version "2.6.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.github.node-gradle.node") version "3.1.1"
    id("org.jmailen.kotlinter") version "3.7.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("kapt") version kotlinVersion
}

version = "2022.0.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

node {
    version.set("16.13.0")
    download.set(true)
}

dependencies {
    val commonmarkVersion = "0.11.0"
    val gmailApiVersion = "1.24.2"
    val googleApiVersion = "v1-rev101-1.24.1"
    val googleOwaspVersion ="20171016.1"
    implementation("org.springframework.boot:spring-boot-starter-webflux") {
        exclude(module = "hibernate-validator")
    }
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("io.projectreactor.addons:reactor-extra")
    implementation("org.springframework:spring-context-indexer")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.samskivert:jmustache")
    implementation("com.atlassian.commonmark:commonmark:$commonmarkVersion")
    implementation("com.atlassian.commonmark:commonmark-ext-autolink:$commonmarkVersion")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("com.google.api-client:google-api-client:$gmailApiVersion")
    implementation("com.google.apis:google-api-services-gmail:$googleApiVersion")
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:$googleOwaspVersion")

    kapt("org.springframework.boot:spring-boot-configuration-processor")

    runtimeOnly("de.flapdoodle.embed:de.flapdoodle.embed.mongo")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.ninja-squad:springmockk:2.0.0")
    testImplementation("io.mockk:mockk:1.9.3")
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

tasks.register<Copy>("copyJsVendors") {
    dependsOn(tasks.npmInstall)
    from("node_modules/bootstrap/dist/js/bootstrap.bundle.js","node_modules/qrcode-svg/dist/qrcode.min.js")
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

kotlinter {
    disabledRules = arrayOf("import-ordering")
}