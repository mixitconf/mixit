import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import java.util.concurrent.TimeUnit

buildscript {
	val kotlinVersion = "1.0.5-2"
	extra["kotlinVersion"] = kotlinVersion

	repositories {
		mavenCentral()
		jcenter()
	}

	dependencies {
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
		classpath("com.github.jengelman.gradle.plugins:shadow:1.2.4")
		classpath("org.junit.platform:junit-platform-gradle-plugin:1.0.0-M3")
	}
}

apply {
	plugin("kotlin")
	plugin("application")
	plugin("org.junit.platform.gradle.plugin")
	plugin("com.github.johnrengelman.shadow")
}

version = "1.0.0-SNAPSHOT"

repositories {
	mavenCentral()
	jcenter()
	maven { setUrl("https://repo.spring.io/snapshot") }
}

configure<JavaPluginConvention> {
	setSourceCompatibility(1.8)
	setTargetCompatibility(1.8)
	sourceSets.getByName("main").java.srcDirs("$buildDir/generated/source/kapt/main")
}

configure<KaptExtension> {
	generateStubs = true
}

configure<ApplicationPluginConvention> {
	mainClassName = "mixit.MainKt"
}

configurations.all {
	it.resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

val kotlinVersion = extra["kotlinVersion"] as String
val springVersion = "5.0.0.BUILD-SNAPSHOT"
val jacksonVersion = "2.8.4"
val reactorVersion = "3.0.3.RELEASE"
val tomcatVersion = "8.5.8"
val requeryVersion = "1.0.2"
val junitJupiterVersion = "5.0.0-M3"

dependencies {
	compile("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
	compile("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

	compile("org.springframework:spring-web-reactive:$springVersion")
	// TODO Remove the spring-context-support dependency when https://jira.spring.io/browse/SPR-14908 will be fixed
	compile("org.springframework:spring-context-support:$springVersion")

	compile("io.projectreactor:reactor-core:$reactorVersion")
	testCompile("io.projectreactor.addons:reactor-test:$reactorVersion")
	testCompile("io.projectreactor.ipc:reactor-netty:0.6.0.BUILD-SNAPSHOT")

	compile("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
	compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

	compile("org.apache.tomcat:tomcat-util:$tomcatVersion")
	compile("org.apache.tomcat.embed:tomcat-embed-core:$tomcatVersion")

	compile("commons-logging:commons-logging:1.2")
	compile("org.slf4j:slf4j-api:1.7.21")
	compile("ch.qos.logback:logback-classic:1.1.7")

	compile("io.requery:requery:$requeryVersion")
	compile("io.requery:requery-kotlin:$requeryVersion")
	kapt("io.requery:requery-processor:$requeryVersion")
	compile("com.h2database:h2:1.4.191")

	testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
	testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}


