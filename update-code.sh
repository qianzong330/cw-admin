#!/bin/bash
# 强制拉取最新代码脚本
# 版本：v1.1 - 添加Git提交信息验证

echo "=== 开始更新代码 ==="

# 进入项目目录
cd /opt/cw-admin

# 备份当前代码（可选）
echo "备份当前代码..."
cp -r /opt/cw-admin /opt/cw-admin-backup-$(date +%Y%m%d%H%M%S)

# 下载最新代码
echo "下载最新代码..."
cd /opt
rm -f cw-admin.zip
wget https://github.com/qianzong330/cw-admin/archive/refs/heads/main.zip -O cw-admin.zip

# 解压
echo "解压代码..."
rm -rf cw-admin-main
unzip -q cw-admin.zip

# 替换旧代码
echo "替换旧代码..."
rm -rf cw-admin
mv cw-admin-main cw-admin

# 验证
echo "=== 验证代码版本 ==="
cd /opt/cw-admin

# 显示最后一次提交信息
echo "最后一次提交："
git log --oneline -1 2>/dev/null || echo "  通过ZIP下载，无Git提交记录"

# 显示OSS配置验证
echo ""
echo "OSS配置验证："
cat src/main/resources/application-prod.properties | grep -A2 "阿里云 OSS"

echo ""
echo "=== 代码更新完成 ==="
echo "开始编译..."
echo ""

# 编译
cd /opt/cw-admin
mvn clean package -DskipTests

# 复制jar包
cp target/accounting-0.0.1-SNAPSHOT.jar /opt/accounting/

echo ""
echo "=== 编译完成 ==="
echo "请去1Panel手动重启 accounting-app 容器"
