# 第一阶段使用 layertools 的 extract 命令将应用程序拆分为多个层  本次构建标记为builder
FROM openjdk:11.0.13-jre-slim as builder

WORKDIR application
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract && rm app.jar

FROM openjdk:11.0.13-jre-slim

LABEL maintainer="louis2sin@gmail.com"
LABEL jre="11.0.13"
LABEL service-name="duuid-server"

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENV JAVA_OPTS="-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -Dreactor.netty.http.server.accessLogEnabled=true -Xms256m -Xmx256m"

WORKDIR application

COPY --from=builder /application/dependencies/ ./
COPY --from=builder /application/snapshot-dependencies/ ./
COPY --from=builder /application/spring-boot-loader/ ./
COPY --from=builder /application/application/ ./

EXPOSE 9001

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]