# Docker 镜像构建
FROM maven:3.8.2-jdk-8 as builder

# Copy local code to the container image.
WORKDIR /app
COPY ./api-tomato-backend/target/api-tomato-backend.jar api-tomato-backend.jar
COPY ./api-tomato-gateway/target/Tomato-Api-Gateway.jar Tomato-Api-Gateway.jar
COPY ./api-tomato-interface/target/Tomato-Api-Interface.jar Tomato-Api-Interface.jar

# 定义容器启动时执行的命令
COPY start.sh start.sh

# 赋予启动脚本可执行权限
RUN chmod +x start.sh

# 定义容器启动时执行的命令
CMD ["./start.sh"]
