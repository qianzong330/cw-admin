#!/bin/bash

# 先更新自己
curl -o /home/devbox/project/entrypoint.sh https://raw.githubusercontent.com/qianzong330/cw-admin/main/entrypoint.sh 2>/dev/null

app_env=${1:-development}

# Development environment commands
dev_commands() {
    echo "Running development environment commands..."
    mvn spring-boot:run
}

# Production environment commands
prod_commands() {
    echo "Running production environment commands..."
    
    # 创建 target 目录
    mkdir -p /home/devbox/project/target
    
    # 下载最新 jar 包
    echo "Downloading latest jar from GitHub..."
    curl -L -o /home/devbox/project/target/accounting-0.0.1-SNAPSHOT.jar https://github.com/qianzong330/cw-admin/raw/main/target/accounting-0.0.1-SNAPSHOT.jar
    
    # 检查 jar 包是否存在
    if [ ! -f "/home/devbox/project/target/accounting-0.0.1-SNAPSHOT.jar" ]; then
        echo "ERROR: Jar file not found!"
        exit 1
    fi
    
    # 启动应用
    echo "Starting application..."
    java -jar /home/devbox/project/target/accounting-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
}

# prod_commands
# Check environment variables to determine the running environment
if [ "$app_env" = "production" ] || [ "$app_env" = "prod" ] ; then
    echo "Production environment detected"
    prod_commands
else
    echo "Development environment detected"
    dev_commands
fi
