# 基础镜像：使用轻量 JDK17 运行环境（和你项目JDK一致）
FROM openjdk:17-jre-slim

# 设置容器工作目录
WORKDIR /app

# 复制项目打包后的 jar 到容器内，统一命名 app.jar
COPY target/*.jar app.jar

# 设置时区 亚洲/上海
ENV TZ=Asia/Shanghai

# 默认激活生产环境（可外部覆盖）
ENV SPRING_PROFILES_ACTIVE=prod

# 暴露项目端口 8080
EXPOSE 8080

# 容器启动命令
ENTRYPOINT ["java","-jar","app.jar"]