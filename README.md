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

### Prerequisite
 - Install [Git](https://git-scm.com/)
 - [Fork](https://github.com/mix-it/mixit#fork-destination-box) and clone [the project](https://github.com/mix-it/mixit)
 - [Install MongoDB](https://www.mongodb.com/download-center)
 - [Install Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
 - [Install Gulp](http://gulpjs.com/) for development mode 
 - Depending on your platform, you may have to install libpng (On mac with `brew install libpng` for example)
 
### Run the app in dev mod using command line
 - Run `mongod`
 - Run `./gradlew bootRun` in another terminal
 - Run `gulp watch` in another terminal
 - Open `http://localhost:8080/` in your browser
 - If you just want to restart the app: `./gradlew bootRun -x gulpBuild`
 - If you want to debug the app, add `--debug-jvm` parameter to Gradle command line
 
Sass, TypeScript, `messages*.properties` and templates should be live reloaded.

### Import and run the project in IDEA
 - Make sure you have at least IntelliJ IDEA `2016.3` and IDEA Kotlin plugin `1.1.0` (menu Tools -> Kotlin -> configure Kotlin Plugin Updates -> make sure "Stable" channel is selected -> check for updates now -> restart IDE after the update)
 - Import it in IDEA as a Gradle project **and make sure to uncheck "Create Module for each SourceSet"** to avoid a nasty bug that prevent to run the app in IDEA
 - Run `mongod` in command line
 - In IntelliJ IDEA, right click on `Application.kt` then `Run ...` or `Debug ...`
 - Run `gulp watch` in another terminal
 - Open `http://localhost:8080/` in your browser
 
Sass, TypeScript, `messages*.properties` and templates should be live reloaded.
 
### Package and run the application from the executable JAR:
```
./gradlew clean build
java -jar build/libs/mixit-1.0.0-SNAPSHOT.jar
```

###  Deploy the app on Cloud Foundry
```
./gradlew clean build
cf push
```
