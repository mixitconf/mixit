[![Travis CI](https://api.travis-ci.org/mixitconf/mixit.svg?branch=master)](https://travis-ci.org/mixitconf/mixit)

# MiXiT website

This project purpose is to power the MiXiT website.

## Software design

This project software design goal is to demonstrate what a functional web application
developed with Spring Framework 5 and Kotlin can look like:
 - Reactive and non-blocking
 - More functional style and less annotation based than typical Spring applications
 - Leverage Kotlin features like [Kotlin extensions](https://kotlinlang.org/docs/reference/extensions.html) and [reified type parameters](https://kotlinlang.org/docs/reference/inline-functions.html#reified-type-parameters) for cleaner code
 - Simple, fast to start, efficient request processing, low memory consumption
 - [Constructor based injection](http://olivergierke.de/2013/11/why-field-injection-is-evil/)
 - Immutable Pojos
 - Cloud Native
 
 You can see a variant of this project software design in [this functional-bean-registration branch](https://github.com/mix-it/mixit/tree/functional-bean-registration):
  - [Functional bean registration](https://github.com/mix-it/mixit/blob/functional-bean-registration/src/main/kotlin/Context.kt)
  - Pure Spring Framework project (without Spring Boot)
  - No CGLIB proxy
  - No kotlin-spring plugin needed

### Technologies used

 - Language: [Kotlin](https://kotlin.link/) 
 - Web framework: [Spring Boot](https://projects.spring.io/spring-boot/) and [Spring Web Reactive Functional](https://spring.io/blog/2016/09/22/new-in-spring-5-functional-web-framework)
 - Engine: [Netty](http://netty.io/) used for client and server
 - Templates: [Mustache](https://github.com/samskivert/jmustache) (will be migrated later to [Kotlin typesafe templates](https://github.com/sdeleuze/kotlin-script-templating))
 - Reactive API: [Reactor](http://projectreactor.io/)
 - Persistence : [Spring Data Reactive MongoDB](https://spring.io/blog/2016/11/28/going-reactive-with-spring-data)
 - Build: [Gradle Script Kotlin](https://github.com/gradle/gradle-script-kotlin)
 - Testing: [Junit](http://junit.org/) 
 
### TODO

 - Use `::findAll` instead of `this@BlogController::findAll` when [KT-15667](https://youtrack.jetbrains.com/issue/KT-15667) will be fixed
 - Use [Kotlin Javascript](https://kotlinlang.org/docs/reference/js-overview.html) when generated JS size will be optimized or [compile to WebAssembly](https://discuss.kotlinlang.org/t/webassembly-support/1722/) if supported
 - Update to [Gradle Kotlin Script 0.9.0](https://github.com/gradle/gradle-script-kotlin/milestone/13) to [avoid freezing IDEA when modifying `build.gradle.kts`](https://github.com/gradle/gradle-script-kotlin/issues/249)  
 
## Developer guide

### Prerequisite
 - Install [Git](https://git-scm.com/)
 - [Fork](https://github.com/mix-it/mixit#fork-destination-box) and clone [the project](https://github.com/mix-it/mixit)
 - [Install Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
 - [Install Gulp](http://gulpjs.com/) for development mode 
 - Depending on your platform, you may have to install libpng (On mac with `brew install libpng` for example)
 
### Run the app in dev mod using command line
 - Run `./gradlew bootRun -Pembed-mongo` in another terminal
 - Run `gulp watch` in another terminal
 - Open `http://localhost:8080/` in your browser
 - If you want to debug the app, add `--debug-jvm` parameter to Gradle command line
 
Sass, TypeScript, `messages*.properties` and templates should be live reloaded.

### Import and run the project in IDEA
 - Make sure you have at least IntelliJ IDEA `2016.3.6` or `2017.1.x` and IDEA Kotlin plugin `1.1.1+` (menu Tools -> Kotlin -> configure Kotlin Plugin Updates -> make sure "Stable" channel is selected -> check for updates now -> restart IDE after the update)
 - Import it in IDEA as a Gradle project **and make sure to uncheck "Create Module for each SourceSet"** to avoid a nasty bug that prevent to run the app in IDEA
 - In IntelliJ IDEA, right click on `Application.kt` then `Run ...` or `Debug ...`
 - Run `gulp watch` in another terminal
 - Open `http://localhost:8080/` in your browser
 
Sass, TypeScript, `messages*.properties` and templates should be live reloaded.
 
### Package and run the application from the executable JAR:
```
./gradlew clean bootJar
java -jar build/libs/mixit-1.0.0-SNAPSHOT.jar
```

### Deploy the app on Cloud Foundry
```
./gradlew clean bootJar
cf push
```

### Copy PROD data to src/main/resources/data
 
```
curl https://mixitconf.org/api/blog | python -m json.tool > blog.json
curl https://mixitconf.org/api/event | python -m json.tool > events.json
curl https://mixitconf.org/api/user | python -m json.tool > users.json
curl https://mixitconf.org/api/2017/talk | python -m json.tool > talks_2017.json
commit -a -m "Update data from PROD"
```