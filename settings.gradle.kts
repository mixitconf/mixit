// Required since Boot 2 and JUnit 5 Gradle plugin are not available from Gradle portal
pluginManagement {
    repositories {
        maven("https://jcenter.bintray.com/")
        maven("https://repo.spring.io/milestone")
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.junit.platform.gradle.plugin") {
                useModule("org.junit.platform:junit-platform-gradle-plugin:${requested.version}")
            }
            if (requested.id.id == "org.springframework.boot") {
                useModule("org.springframework.boot:spring-boot-gradle-plugin:${requested.version}")
            }
        }
    }
}