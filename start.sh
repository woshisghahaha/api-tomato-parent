#!/bin/sh

# 启动多个JAR包，每个JAR包后台运行
java -jar api-tomato-backend.jar --spring.profiles.active=prod &
java -jar Tomato-Api-Gateway.jar --server.port=8090 &
java -jar Tomato-Api-Interface.jar --server.port=8123 &

# 保持容器运行
tail -f /dev/null
