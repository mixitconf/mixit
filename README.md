# Mix-IT 2017 website

This project purpose is to power the Mix-IT 2017 website.

# Software design

This project software design is an opinionated view of what could be the next generation Java Web applications:
 - Reactive
 - Simple, fast to start, efficient request processing, no classpath scanning, low memory consumption
 - Cloud Native
 - [Constructor based injection](http://olivergierke.de/2013/11/why-field-injection-is-evil/)
 - More functional style and less annotation based than typical Spring applications

The technologies used are:
 - Language: [Kotlin](https://kotlin.link/) 
 - Web framework: [Spring Web Reactive Functional](https://spring.io/blog/2016/09/22/new-in-spring-5-functional-web-framework)
 - Engine: [Netty](http://netty.io/) used for client and server
 - Templates: [Handlebars](https://github.com/jknack/handlebars.java)
 - Reactive API: [Reactor](http://projectreactor.io/)
 - Persistence : [Spring Data Reactive MongoDB](https://spring.io/blog/2016/11/28/going-reactive-with-spring-data)
 - Build: [Gradle Script Kotlin](https://github.com/gradle/gradle-script-kotlin)
 - Testing: [JUnit 5](http://junit.org/junit5/)
 
# TODO

 - Switch from Tomcat to Reactor Netty
 - Dependency injection will be updated with the [Spring 5 functional bean API when available](https://jira.spring.io/browse/SPR-14832)

# Getting started

To import the project in IDEA:
 - Clone the project `git clone https://github.com/mix-it/mixit.git`
 - Make sure you have at least IntelliJ IDEA 2016.3
 - Update IDEA Kotlin plugin to 1.1: Tools -> Kotlin -> Configure Kotlin Plugin Updates -> Early Access Preview 1.1
 - Import it in IDEA as a Gradle project
 - Right click on `Main.kt` -> Run mixit.MainKt

To run the application:
```
./gradlew run
```

To package and run the application from the executable JAR:
```
./gradlew shadowJar
java -jar build/libs/mixit-all.jar
```

To test the application from a browser, go to `http://localhost:8080/`.

# FAQ

How can I register a new `Foo` singleton bean ?

Add `beanFactory.register(Foo::class)` line in `Application#appContext()`.

How can I register a new Handlebars helper ?

Add a `HelperSource` class in `HandlebarsHelpers` and register the bean in `Application#appContext()` like it has been done for `IfEqHelperSource`.
