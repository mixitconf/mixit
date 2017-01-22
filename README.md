[![Travis CI](https://api.travis-ci.org/mix-it/mixit.svg?branch=master)](https://travis-ci.org/mix-it/mixit)

# MiXiT 2017 website

This project purpose is to power the MiXiT 2017 website.

## Software design

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
 - Testing: [Junit](http://junit.org/)
 
## Getting started

Prerequisite:
 - [Install MongoDB](https://www.mongodb.com/download-center) and run `mongod`
 - [Install Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
 - Depending on your platform, you may have to install libpng (On mac with `brew install libpng` for example)

To import the project in IDEA:
 - Clone the project `git clone https://github.com/mix-it/mixit.git`
 - Make sure you have at least IntelliJ IDEA 2016.3 and IDEA Kotlin plugin 1.1.0-beta-17+
 - Import it in IDEA as a Gradle project
 - Right click on the project -> Open module settings -> Modules -> mixit -> Kotlin and make sure Kotlin 1.1 language level and that Java 1.8 bytecode options are selected
 - Right click on `Application.kt` -> Run mixit.ApplicationKt

To run the application:
```
./gradlew bootRun
```

To package and run the application from the executable JAR:
```
./gradlew build
java -jar build/libs/mixit-1.0.0-SNAPSHOT.jar
```

To deploy the app on CF:
```
./gradlew clean build
cf push
```

To test the application from a browser, go to `http://localhost:8080/`.
