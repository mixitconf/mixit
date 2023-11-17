[![Travis CI](https://api.travis-ci.org/mixitconf/mixit.svg?branch=master)](https://travis-ci.org/mixitconf/mixit)

# MiXiT website

This project purpose is to power the MiXiT website.

## Software design

This project software design goal is to demonstrate what a functional web application
developed with Spring Boot 2, Spring WebFlux and Kotlin can look like:
 - Reactive and non-blocking
 - More functional style and less annotation based than typical Spring applications
 - Leverage Kotlin features like [Kotlin extensions](https://kotlinlang.org/docs/reference/extensions.html) and [reified type parameters](https://kotlinlang.org/docs/reference/inline-functions.html#reified-type-parameters) for cleaner code
 - Simple, fast to start, efficient request processing, low memory consumption
 - [Constructor based injection](http://olivergierke.de/2013/11/why-field-injection-is-evil/)
 - Immutable Pojos
 - Cloud Native

### Technologies used

 - Language: [Kotlin](https://kotlin.link/) 
 - Framework: [Spring Boot 3.x](https://projects.spring.io/spring-boot/) with [Spring 5 Kotlin support](https://docs.spring.io/spring-framework/docs/5.0.x/spring-framework-reference/kotlin.html) and [Spring WebFlux functional](https://docs.spring.io/spring-framework/docs/5.0.x/spring-framework-reference/reactive-web.html)
 - Engine: [Netty](http://netty.io/) used for client and server
 - Templates: [Mustache](https://github.com/samskivert/jmustache) (will be migrated later to [Kotlin typesafe templates](https://github.com/sdeleuze/kotlin-script-templating))
 - Reactive API: [Reactor](http://projectreactor.io/)
 - Persistence : [Spring Data Reactive MongoDB](https://spring.io/blog/2016/11/28/going-reactive-with-spring-data)
 - Build: [Gradle Script Kotlin](https://github.com/gradle/gradle-script-kotlin)
 - Testing: [Junit 5](http://junit.org/) 
 
### TODO

 - Use [Kotlin Javascript](https://kotlinlang.org/docs/reference/js-overview.html) with [dead code elimination](https://kotlinlang.org/docs/reference/javascript-dce.html) tool  
 
## Developer guide

### Prerequisite
 - Install [Git](https://git-scm.com/)
 - Install [Docker](https://www.docker.com/#)
 - [Fork](https://github.com/mix-it/mixit#fork-destination-box) and clone [the project](https://github.com/mix-it/mixit)
 - [Install Java 11](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

### Start the database
We use a mongodb instance and this database is managed with a docker file
 - Run `docker compose up` to start the database and the console
 - Open `http://0.0.0.0:8081` to open the mongodb console
 - At any moment you can stop and remove and restart the container if you need to reinit the database 

### Run the app in dev mod using command line
 - Run `./gradlew bootRun` in another terminal
 - Run `./gradlew watch` in another terminal
 - Open `http://localhost:8080/` in your browser
 - If you want to debug the app, add `--debug-jvm` parameter to Gradle command line
 
Sass, TypeScript, `messages*.properties` and templates should be live reloaded.

### Import and run the project in IDEA
 - Make sure you have at least IntelliJ IDEA `2017.2.x` and IDEA Kotlin plugin `1.1.4+` (menu Tools -> Kotlin -> configure Kotlin Plugin Updates -> make sure "Stable" channel is selected -> check for updates now -> restart IDE after the update)
 - Import it in IDEA as a Gradle project
 - In IntelliJ IDEA, right click on `Application.kt` then `Run ...` or `Debug ...`
 - Run `./gradlew watch` in another terminal
 - Open `http://localhost:8080/` in your browser
 
Sass, TypeScript, `messages*.properties` and templates should be live reloaded.
 
### Package and run the application from the executable JAR:
```
./gradlew clean build
java -jar build/libs/mixit-1.0.0-SNAPSHOT.jar
```

### Deploy the app on  Clever Cloud
> When you merge a PR into `prod` branch (usually from `master` branch) it will trigger a deployment on Clever Cloud.

### Copy PROD data to src/main/resources/data
 
```
curl https://mixitconf.org/api/blog | python3 -m json.tool > blog.json
curl https://mixitconf.org/api/event | python3 -m json.tool > events.json
curl https://mixitconf.org/api/event/images | python3 -m json.tool > events_image.json
curl https://mixitconf.org/api/user | python3 -m json.tool > users.json
curl https://mixitconf.org/api/2024/talk | python3 -m json.tool > talks_2024.json
git commit -a -m "Update data from PROD"
```
