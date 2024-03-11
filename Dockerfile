FROM eclipse-temurin:17-jre-alpine
WORKDIR /
ADD ./target/SessionService.jar SessionService.jar
ENTRYPOINT ["java", "-Dprocess.name=SessionService", "-jar", "SessionService.jar"]
