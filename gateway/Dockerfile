FROM crpi-ka9mjef5kpn3p7h6.cn-shanghai.personal.cr.aliyuncs.com/ys0921/openjdk:21-jdk-alpine

# 镜像元数据标签
LABEL maintainer="Yan Shuai" \
      project="atlas" \
      module="gateway"

# 设置时区
RUN apk add --no-cache tzdata && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo "Asia/Shanghai" > /etc/timezone

# 创建非 root 用户以增强生产安全性
RUN addgroup -S app && adduser -S app -G app
USER app:app

# 工作目录
WORKDIR /app

COPY target/*.jar app.jar

# 设置 JVM 运行参数
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]