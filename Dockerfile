
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B clean package -DskipTests

FROM tomcat:11.0-jdk21-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /app/target/SmartTodoList.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["sh", "-c", "sed -i 's/port=\"8080\"/port=\"${PORT:-8080}\"/' /usr/local/tomcat/conf/server.xml && sed -i 's/<Server port=\"8005\" shutdown=\"SHUTDOWN\">/<Server port=\"-1\" shutdown=\"SHUTDOWN\">/' /usr/local/tomcat/conf/server.xml && catalina.sh run"]
