import org.jetbrains.kotlin.noarg.gradle.NoArgExtension
import com.moowork.gradle.gulp.GulpTask
import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.yarn.YarnInstallTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    val kotlinVersion = "1.1.0-beta-17"
    val springBootVersion = "2.0.0.BUILD-SNAPSHOT"
    extra["kotlinVersion"] = kotlinVersion
    extra["springBootVersion"] = springBootVersion

    repositories {
        mavenCentral()
        maven { setUrl("https://plugins.gradle.org/m2/") }
        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap-1.1") }
        maven { setUrl("https://repo.spring.io/snapshot") }
        maven { setUrl("https://repo.spring.io/milestone") }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
        classpath("org.jetbrains.kotlin:kotlin-noarg:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-allopen:$kotlinVersion")
        classpath("com.moowork.gradle:gradle-node-plugin:1.1.1")
    }
}

apply {
    plugin("idea")
    plugin("kotlin")
    plugin("kotlin-noarg")
    plugin("kotlin-spring")
    plugin("com.moowork.node")
    plugin("com.moowork.gulp")
    plugin("org.springframework.boot")
}

version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { setUrl("https://dl.bintray.com/jetbrains/spek") }
    maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap-1.1") }
    maven { setUrl("https://repo.spring.io/milestone") }
    maven { setUrl("https://repo.spring.io/snapshot") }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

configure<NodeExtension> {
    version = "6.9.4"
    download = true
}

configure<NoArgExtension> {
    annotation("org.springframework.data.mongodb.core.mapping.Document")
}

val kotlinVersion = extra["kotlinVersion"] as String
val springVersion = "5.0.0.BUILD-SNAPSHOT"
val springBootVersion = extra["springBootVersion"] as String
val springDataVersion = "2.0.0.BUILD-SNAPSHOT"
val jacksonVersion = "2.8.5"
val reactorVersion = "3.0.4.RELEASE"

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    compile("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    compile("org.springframework.boot.experimental:spring-boot-starter-web-reactive:0.1.0.BUILD-SNAPSHOT") {
        exclude(module= "spring-boot-starter-tomcat")
        exclude(module= "hibernate-validator")
    }
    testCompile("org.springframework.boot:spring-boot-starter-test")

    compile("com.samskivert:jmustache:1.13")
    compile("com.atlassian.commonmark:commonmark:0.8.0")

    compile("io.projectreactor:reactor-core:$reactorVersion")
    compile("io.projectreactor.ipc:reactor-netty:0.6.0.RELEASE")
    testCompile("io.projectreactor.addons:reactor-test:$reactorVersion")

    compile("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    compile("commons-logging:commons-logging:1.2")
    compile("org.slf4j:slf4j-api:1.7.21")
    compile("ch.qos.logback:logback-classic:1.1.7")

    compile("org.springframework.data:spring-data-mongodb:$springDataVersion")
    compile("org.springframework.data:spring-data-commons:$springDataVersion")
    compile("org.mongodb:mongodb-driver-reactivestreams:1.2.0")
}

task<GulpTask>("gulpBuild") {
    dependsOn(YarnInstallTask.NAME)
    inputs.dir("src/main/sass")
    inputs.dir("src/main/ts")
    inputs.dir("src/main/iamges")
    inputs.dir("build/.tmp")
    outputs.dir("build/resources/static")
    args = listOf("default")
}

tasks.getByName("processResources").dependsOn("gulpBuild")