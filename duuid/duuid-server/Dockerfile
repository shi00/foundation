# 第一阶段使用 layertools 的 extract 命令将应用程序拆分为多个层  本次构建标记为builder
FROM bellsoft/liberica-openjdk-alpine:17.0.3.1-2 as builder

WORKDIR application
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract && rm app.jar

FROM bellsoft/liberica-openjdk-alpine:17.0.3.1-2

LABEL maintainer="louis2sin@gmail.com"
LABEL jre="17"
LABEL service-name="duuid-server"

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && mkdir -p /opt/application/logs
ENV JAVA_OPTS="-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -Dreactor.netty.http.server.accessLogEnabled=true -XX:MinRAMPercentage=50.0 -XX:MaxRAMPercentage=80.0 -XX:+UseZGC -XX:+DisableExplicitGC -XX:ReservedCodeCacheSize=64m -XX:InitialCodeCacheSize=64m -XX:ConcGCThreads=2 -XX:ParallelGCThreads=6 -XX:ZCollectionInterval=120 -XX:ZAllocationSpikeTolerance=5 -XX:ZUncommitDelay=600 -XX:+UnlockDiagnosticVMOptions -XX:-ZProactive -Xlog:safepoint,classhisto*=trace,age*,gc*=info:file=/opt/application/logs/gc-%t.log:time,tid,tags:filecount=5,filesize=50m"

WORKDIR application

COPY --from=builder /application/dependencies/ ./
COPY --from=builder /application/snapshot-dependencies/ ./
COPY --from=builder /application/spring-boot-loader/ ./
COPY --from=builder /application/application/ ./

EXPOSE 9001
EXPOSE 27891

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]