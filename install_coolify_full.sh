#!/bin/bash

# ============================================
# Coolify 一键安装脚本 (CentOS 7.9)
# ============================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的信息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# ============================================
# 1. Root 用户检查
# ============================================
if [ "$EUID" -ne 0 ]; then
    print_error "请使用 root 用户运行此脚本！"
    echo "使用方法: sudo bash install_coolify_full.sh"
    exit 1
fi

print_success "Root 用户检查通过"

# ============================================
# 2. 系统更新和基础工具安装
# ============================================
print_info "正在更新系统并安装基础工具..."
yum update -y

# 安装必要的工具
yum install -y curl git wget vim firewalld yum-utils device-mapper-persistent-data lvm2

print_success "系统更新和基础工具安装完成"

# ============================================
# 3. Docker 自动化安装
# ============================================
print_info "开始安装 Docker..."

# 卸载旧版本 Docker（如果有）
print_info "卸载旧版本 Docker..."
yum remove -y docker docker-client docker-client-latest docker-common docker-latest docker-latest-logrotate docker-logrotate docker-engine

# 使用阿里云 Docker 源
print_info "添加阿里云 Docker 源..."
yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo

# 安装 Docker CE 和 Docker Compose 插件
print_info "安装 Docker CE 和 Docker Compose..."
yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 启动 Docker 并设置开机自启
print_info "启动 Docker 服务..."
systemctl start docker
systemctl enable docker

# 验证 Docker 安装
if docker --version &> /dev/null; then
    print_success "Docker 安装成功: $(docker --version)"
else
    print_error "Docker 安装失败"
    exit 1
fi

# ============================================
# 4. 配置国内 Docker 镜像加速
# ============================================
print_info "配置国内 Docker 镜像加速..."

# 创建 Docker 配置目录
mkdir -p /etc/docker

# 写入镜像加速配置
cat > /etc/docker/daemon.json << 'EOF'
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com",
    "https://docker.m.daocloud.io",
    "https://dockerhub.azk8s.cn",
    "https://docker.mirrors.sjtug.sjtu.edu.cn"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
EOF

print_success "Docker 镜像加速配置完成"

# 重启 Docker 使配置生效
print_info "重启 Docker 服务..."
systemctl restart docker

# 验证镜像加速是否生效
print_info "验证 Docker 镜像加速配置..."
docker info | grep -A 10 "Registry Mirrors"

print_success "Docker 镜像加速配置生效"

# ============================================
# 5. 防火墙配置
# ============================================
print_info "配置防火墙..."

# 检查 firewalld 状态，如果未启动则启动
if ! systemctl is-active --quiet firewalld; then
    print_info "启动 firewalld 服务..."
    systemctl start firewalld
fi

# 设置开机自启
systemctl enable firewalld

# 开放所需端口
print_info "开放端口: 22(SSH), 80(HTTP), 443(HTTPS), 3000(Coolify)..."
firewall-cmd --permanent --add-port=22/tcp
firewall-cmd --permanent --add-port=80/tcp
firewall-cmd --permanent --add-port=443/tcp
firewall-cmd --permanent --add-port=3000/tcp

# 重载防火墙规则
firewall-cmd --reload

print_success "防火墙配置完成"
print_info "已开放端口:"
firewall-cmd --list-ports

# ============================================
# 6. 执行 Coolify 安装
# ============================================
print_info "开始安装 Coolify..."

# 定义安装脚本 URL
COOLIFY_INSTALL_URL="https://cdn.coollabs.io/coolify/install.sh"
COOLIFY_INSTALL_URL_MIRROR="https://ghproxy.com/https://raw.githubusercontent.com/coollabsio/coolify/main/scripts/install.sh"

# 尝试使用官方脚本安装
print_info "尝试从官方源安装 Coolify..."
if curl -fsSL "$COOLIFY_INSTALL_URL" -o /tmp/coolify_install.sh; then
    print_success "官方安装脚本下载成功"
    bash /tmp/coolify_install.sh
else
    print_warning "官方源下载失败，尝试使用镜像源..."
    if curl -fsSL "$COOLIFY_INSTALL_URL_MIRROR" -o /tmp/coolify_install.sh; then
        print_success "镜像源安装脚本下载成功"
        bash /tmp/coolify_install.sh
    else
        print_error "Coolify 安装脚本下载失败，请检查网络连接"
        exit 1
    fi
fi

# 检查 Coolify 是否安装成功
if [ -d "/data/coolify" ]; then
    print_success "Coolify 安装成功"
else
    print_error "Coolify 安装可能失败，未找到安装目录 /data/coolify"
    exit 1
fi

# ============================================
# 7. 结果反馈
# ============================================
print_info "获取服务器公网 IP..."

# 尝试多种方式获取公网 IP
PUBLIC_IP=$(curl -s https://api.ipify.org 2>/dev/null || \
            curl -s https://ifconfig.me 2>/dev/null || \
            curl -s https://icanhazip.com 2>/dev/null || \
            echo "无法获取")

echo ""
echo "=========================================="
echo -e "${GREEN}Coolify 安装完成！${NC}"
echo "=========================================="
echo ""
echo -e "🌐 访问地址: ${GREEN}http://$PUBLIC_IP:3000${NC}"
echo ""
echo "📋 初始信息查看方式:"
echo "   1. 初始密码查看: cat /data/coolify/coolify/.env | grep ROOT_PASSWORD"
echo "   2. 或者查看邮件通知（如果配置了 SMTP）"
echo ""
echo "🔧 常用命令:"
echo "   - 查看 Coolify 状态: docker ps | grep coolify"
echo "   - 重启 Coolify: cd /data/coolify && docker compose restart"
echo "   - 查看日志: cd /data/coolify && docker compose logs -f"
echo ""
echo "⚠️  安全提示:"
echo "   - 首次登录后请立即修改默认密码"
echo "   - 建议配置 HTTPS 和域名访问"
echo "   - 定期备份 /data/coolify 目录"
echo ""
echo "=========================================="

# 清理临时文件
rm -f /tmp/coolify_install.sh

print_success "脚本执行完成！"
