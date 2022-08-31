FROM amazoncorretto:8
WORKDIR /
ADD ./target/SessionService.jar SessionService.jar
ENTRYPOINT ["java", "-Dprocess.name=SessionService", "-jar", "SessionService.jar"]
