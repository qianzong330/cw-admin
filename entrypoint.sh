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
    # 先检查是否有预编译的 jar，没有则打包
    if [ -f "target/accounting-0.0.1-SNAPSHOT.jar" ]; then
        echo "Using existing jar..."
    else
        echo "Building jar..."
        mvn clean package -DskipTests
    fi
    java -jar target/accounting-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
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
