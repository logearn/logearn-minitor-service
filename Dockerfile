# 该镜像需要依赖的基础镜像
FROM fabric8/java-alpine-openjdk11-jre
# 作者
MAINTAINER xiaolingyong

# 调整时区
RUN rm -f /etc/localtime \
&& ln -sv /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
&& echo "Asia/Shanghai" > /etc/timezone

# 将当前目录下的jar包复制到docker容器的/目录下
COPY deployments/agents/* /agents/
COPY src/main/resources/kafka_conf /app
COPY target/*.jar /app/app.jar
# 指定docker容器启动时运行jar包
EXPOSE 9111
ENTRYPOINT ["nohup","java", "-javaagent:/agents/newrelic.jar", "-jar", "/app/app.jar", "&"]
#"-XX:+PrintGCDetails",