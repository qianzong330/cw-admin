#!/bin/bash

# ============================================
# Coolify 手动安装脚本 (CentOS 7.9)
# 绕过 ghcr.io 镜像拉取问题
# ============================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 创建目录
mkdir -p /data/coolify
cd /data/coolify

print_info "创建 docker-compose.yml..."

cat > docker-compose.yml << 'EOF'
version: '3.8'
services:
  coolify:
    image: ghcr.io/coollabsio/coolify:4.0.0-beta.468
    container_name: coolify
    restart: unless-stopped
    ports:
      - "3000:3000"
    volumes:
      - /data/coolify:/app/data
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      - APP_URL=http://localhost:3000
      - DB_CONNECTION=pgsql
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_DATABASE=coolify
      - DB_USERNAME=coolify
      - DB_PASSWORD=coolify
    depends_on:
      - postgres
      - redis

  postgres:
    image: postgres:15-alpine
    container_name: coolify-postgres
    restart: unless-stopped
    volumes:
      - /data/coolify/postgres:/var/lib/postgresql/data
    environment:
      - POSTGRES_USER=coolify
      - POSTGRES_PASSWORD=coolify
      - POSTGRES_DB=coolify
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    container_name: coolify-redis
    restart: unless-stopped
    volumes:
      - /data/coolify/redis:/data
    ports:
      - "6379:6379"

  coolify-helper:
    image: ghcr.io/coollabsio/coolify-helper:1.0.12
    container_name: coolify-helper
    restart: unless-stopped
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    command: ["tail", "-f", "/dev/null"]

  coolify-realtime:
    image: ghcr.io/coollabsio/coolify-realtime:1.0.11
    container_name: coolify-realtime
    restart: unless-stopped
    environment:
      - PORT=6001
    ports:
      - "6001:6001"
EOF

print_info "创建环境配置文件..."

cat > .env << 'EOF'
APP_NAME=Coolify
APP_ENV=production
APP_KEY=base64:your-random-key-here
APP_DEBUG=false
APP_URL=http://localhost:3000

DB_CONNECTION=pgsql
DB_HOST=postgres
DB_PORT=5432
DB_DATABASE=coolify
DB_USERNAME=coolify
DB_PASSWORD=coolify

REDIS_HOST=redis
REDIS_PORT=6379

PUSHER_APP_ID=
PUSHER_APP_KEY=
PUSHER_APP_SECRET=
PUSHER_HOST=
PUSHER_PORT=443
PUSHER_SCHEME=https
EOF

print_info "启动 Coolify 服务..."
docker compose up -d

print_info "等待服务启动..."
sleep 10

# 检查容器状态
if docker ps | grep -q coolify; then
    print_success "Coolify 启动成功！"
    echo ""
    echo "=========================================="
    echo -e "${GREEN}Coolify 安装完成！${NC}"
    echo "=========================================="
    echo ""
    echo "🌐 访问地址: http://$(curl -s https://api.ipify.org 2>/dev/null || echo 'your-server-ip'):3000"
    echo ""
    echo "⚠️  注意:"
    echo "   - 首次访问需要初始化设置"
    echo "   - 请配置正确的数据库连接"
    echo ""
    echo "🔧 常用命令:"
    echo "   - 查看日志: docker compose logs -f"
    echo "   - 重启服务: docker compose restart"
    echo "   - 停止服务: docker compose down"
    echo "=========================================="
else
    print_error "Coolify 启动失败，请检查日志:"
    docker compose logs
fi
