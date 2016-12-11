# Mix-IT 2017 website

This project purpose is to power the Mix-IT 2017 website.

# Software design

This project software design is an opinionated view of what could be the next generation Java Web applications:
 - Simple
 - Reactive
 - Fast to start, efficient request processing, no classpath scanning
 - Low memory consumption
 - Cloud Native

The technologies used are:
 - Language: [Kotlin](https://kotlin.link/) 
 - Web framework: [Spring Web Reactive Functional](https://spring.io/blog/2016/09/22/new-in-spring-5-functional-web-framework)
 - Templates: [Handlebars](https://github.com/jknack/handlebars.java)
 - Reactive API: [Reactor](http://projectreactor.io/)
 - Persistence : [Requery](https://github.com/requery/requery) + [H2](http://www.h2database.com/)
 - Build: [Gradle Script Kotlin](https://github.com/gradle/gradle-script-kotlin)
 - Testing: [JUnit 5](http://junit.org/junit5/)
 
# Work in progress

 - Dependency injection will be updated with the Spring 5 functional bean API when available
 - Requery Reactor integration will be contributed and used

# Getting started

To import the project in IDEA:
 - Clone the project `git clone https://github.com/mix-it/mixit.git`
 - Build it to generate required classes via the annotation pre-processor: `/.gradlew build`
 - Make sure you have at least IntelliJ IDEA 2016.3
 - Update IDEA Kotlin plugin to 1.1: Tools -> Kotlin -> Configure Kotlin Plugin Updates -> Early Access Preview 1.1
 - Import it in IDEA as a Gradle project

To run the application:
```
./gradlew run
```

To package and run the application from the executable JAR:
```
./gradlew shadowJar
java -jar build/libs/mixit-1.0.0-SNAPSHOT-all.jar
```

To enable live reload of static resources:
```
./gradlew processResources -t
```

To test the application from a browser, go to `http://localhost:8080/`.


 