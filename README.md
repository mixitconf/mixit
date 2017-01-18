[![Travis CI](https://api.travis-ci.org/mix-it/mixit.svg?branch=master)](https://travis-ci.org/mix-it/mixit)

# MiXiT 2017 website

This project purpose is to power the MiXiT 2017 website.

# Software design

This project software design goal is to demonstrate what a functional web application
developed with Spring Framework 5 and Kotlin can look like:
 - Reactive
 - Simple, fast to start, efficient request processing, low memory consumption
 - Cloud Native
 - [Constructor based injection](http://olivergierke.de/2013/11/why-field-injection-is-evil/)
 - More functional style and less annotation based than typical Spring applications
 - Immutable Pojos
 - Take advantage of [Kotlin extensions](https://kotlinlang.org/docs/reference/extensions.html) and [reified type parameters](https://kotlinlang.org/docs/reference/inline-functions.html#reified-type-parameters)

The technologies used are:
 - Language: [Kotlin](https://kotlin.link/) 
 - Web framework: [Spring Boot](https://projects.spring.io/spring-boot/) and [Spring Web Reactive Functional](https://spring.io/blog/2016/09/22/new-in-spring-5-functional-web-framework)
 - Engine: [Netty](http://netty.io/) used for client and server
 - Templates: [Mustache](https://github.com/samskivert/jmustache) (will be migrated later to [Kotlin typesafe templates](https://github.com/sdeleuze/kotlin-script-templating))
 - Reactive API: [Reactor](http://projectreactor.io/)
 - Persistence : [Spring Data Reactive MongoDB](https://spring.io/blog/2016/11/28/going-reactive-with-spring-data)
 - Build: [Gradle Script Kotlin](https://github.com/gradle/gradle-script-kotlin)
 - Testing: [Spek](https://jetbrains.github.io/spek/)
 
# Getting started

Prerequisite:
 - [Install MongoDB](https://www.mongodb.com/download-center) and run `mongod`
 - [Install Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

To import the project in IDEA:
 - Clone the project `git clone https://github.com/mix-it/mixit.git`
 - Make sure you have at least IntelliJ IDEA 2016.3 and IDEA Kotlin plugin 1.0.6+ (we are waiting IDEA Kotlin plugin 1.1-M05 to have Gradle Kotlin autocomplete)
 - Install Spek IDEA plugin
 - Import it in IDEA as a Gradle project
 - Right click on `Application.kt` -> Run mixit.ApplicationKt

To run the application:
```
./gradlew bootRun
```

To package and run the application from the executable JAR:
```
./gradlew build
java -jar build/libs/mixit-all.jar
```

To deploy the app on CF:
```
./gradlew clean build shadowJar
cf push
```

To test the application from a browser, go to `http://localhost:8080/`.
Technical demo is available at `http://localhost:8080/sample` 

