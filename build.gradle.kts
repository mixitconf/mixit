import org.jetbrains.kotlin.noarg.gradle.NoArgExtension
import com.moowork.gradle.gulp.GulpTask
import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.yarn.YarnInstallTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.1.0-rc-91"
    val springBootVersion = "2.0.0.BUILD-SNAPSHOT"

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

val kotlinVersion: String by extra
val springDataVersion = "2.0.0.BUILD-SNAPSHOT"

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlinVersion")
    compile("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    compile("org.springframework.boot.experimental:spring-boot-starter-web-reactive:0.1.0.BUILD-SNAPSHOT") {
        exclude(module = "spring-boot-starter-tomcat")
        exclude(module = "hibernate-validator")
    }
    compile("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    compile("org.springframework.boot:spring-boot-devtools")
    testCompile("org.springframework.boot:spring-boot-starter-test")

    compile("com.samskivert:jmustache:1.13")
    compile("com.atlassian.commonmark:commonmark:0.8.0")
    compile("com.atlassian.commonmark:commonmark-ext-autolink:0.8.0")

    compile("io.projectreactor:reactor-core:3.0.5.RELEASE")
    compile("io.projectreactor.ipc:reactor-ipc:0.6.1.RELEASE")
    compile("io.projectreactor.ipc:reactor-netty:0.6.1.RELEASE")
    compile("io.projectreactor:reactor-kotlin:1.0.0.BUILD-SNAPSHOT")
    testCompile("io.projectreactor.addons:reactor-test:3.0.5.RELEASE")

    compile("com.fasterxml.jackson.module:jackson-module-kotlin")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}

task<GulpTask>("gulpBuild") {
    dependsOn(YarnInstallTask.NAME)
    inputs.dir("src/main/sass")
    inputs.dir("src/main/ts")
    inputs.dir("src/main/images")
    inputs.dir("build/.tmp")
    outputs.dir("build/resources/main/static")
    args = listOf("default")
}

tasks.getByName("processResources").dependsOn("gulpBuild")
