import java.util.concurrent.TimeUnit

buildscript {
	val kotlinVersion = "1.0.5-2"
	extra["kotlinVersion"] = kotlinVersion

	repositories {
		mavenCentral()
	}

	dependencies {
		classpath("org.springframework.boot:spring-boot-gradle-plugin:1.4.2.RELEASE")
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
	}
}

apply {
	plugin("kotlin")
	plugin("org.springframework.boot")
}

version = "1.0.0-SNAPSHOT"


repositories {
	mavenCentral()
	maven { setUrl("https://repo.spring.io/snapshot") }
}

configure<JavaPluginConvention> {
	setSourceCompatibility(1.8)
	setTargetCompatibility(1.8)
}

configurations.all {
	it.resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

val kotlinVersion = extra["kotlinVersion"] as String
val springVersion = "5.0.0.BUILD-SNAPSHOT"
val jacksonVersion = "2.8.4"
val reactorVersion = "3.0.3.RELEASE"

dependencies {
	compile("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

	compile("org.springframework:spring-aop:$springVersion")
	compile("org.springframework:spring-beans:$springVersion")
	compile("org.springframework:spring-context:$springVersion")
	compile("org.springframework:spring-core:$springVersion")
	compile("org.springframework:spring-expression:$springVersion")
	compile("org.springframework:spring-web:$springVersion")
	compile("org.springframework:spring-web-reactive:$springVersion")

	compile("io.projectreactor:reactor-core:$reactorVersion")

	compile("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
	compile("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
	compile("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
	compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

	compile("io.projectreactor.ipc:reactor-netty:0.6.0.BUILD-SNAPSHOT")

	compile("commons-logging:commons-logging:1.2")
	compile("org.slf4j:slf4j-api:1.7.21")
	compile("ch.qos.logback:logback-classic:1.1.7")

}


