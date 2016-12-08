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
}

val kotlinVersion = extra["kotlinVersion"] as String

dependencies {
	compile("org.jetbrains.kotlin:kotlin-stdlib:${kotlinVersion}")
}


