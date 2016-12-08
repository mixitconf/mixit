# Mix-IT 2017 website

To run the application:
```
./gradlew bootRun
```

To package and run the application from the executable JAR:
```
./gradlew build
cd build/libs/
java -jar mixit-1.0.0-SNAPSHOT.jar
```

To enable live reload of static resources:
```
./gradlew processResources -t
```

To test the application from a browser:
```
http://localhost:8080/index.html
http://localhost:8080/user/1
```