#!/bin/bash

app_env=${1:-development}

# Development environment commands
dev_commands() {
    echo "Running development environment commands..."
    mvn spring-boot:run
}

# Production environment commands
prod_commands() {
    echo "Running production environment commands..."
    
    # 下载最新 jar 包
    echo "Downloading latest jar from GitHub..."
    curl -L -o /home/devbox/project/target/accounting-0.0.1-SNAPSHOT.jar https://github.com/qianzong330/cw-admin/raw/main/target/accounting-0.0.1-SNAPSHOT.jar
    
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
