#!/bin/bash

# 配置 Git
git config --global --add safe.directory /home/devbox/project 2>/dev/null

# 更新代码
cd /home/devbox/project
git pull origin main

# 打包
mvn clean package -DskipTests

# 启动
java -jar target/accounting-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
