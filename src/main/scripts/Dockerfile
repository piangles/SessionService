FROM java:8
WORKDIR /
ADD SessionService.jar SessionService.jar
ENTRYPOINT ["java", "-Dprocess.name=SessionService", "-jar", "SessionService.jar"]
