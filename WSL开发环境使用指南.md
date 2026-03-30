# WSL开发环境使用指南

## 环境概览

| 组件 | 版本 | 说明 |
|------|------|------|
| WSL | 2 | Windows Subsystem for Linux |
| Ubuntu | 22.04 LTS | 长期支持版本 |
| 用户名 | nova | 默认用户 |
| 密码 | nova | sudo密码 |

---

## 已安装的开发环境

### 1. Java开发环境

**已安装版本：**
- OpenJDK 8
- OpenJDK 17 (LTS)
- OpenJDK 21 (LTS) - 当前默认

**切换Java版本：**
```bash
# 查看所有已安装的Java版本
sudo update-alternatives --config java

# 查看当前Java版本
java -version

# 设置JAVA_HOME环境变量（添加到 ~/.bashrc）
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

### 2. Maven构建工具

**版本：** Apache Maven 3.6.3

**镜像配置：** 已配置阿里云镜像 (~/.m2/settings.xml)

**常用命令：**
```bash
# 创建新项目
mvn archetype:generate -DgroupId=com.example -DartifactId=my-app -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false

# 编译项目
mvn compile

# 打包项目
mvn package

# 清理并打包
mvn clean package
```

### 3. Node.js环境

**已安装版本：**
- Node.js 18
- Node.js 20
- Node.js 22 (当前默认: v22.22.1)

**版本管理器：** n

**切换Node.js版本：**
```bash
# 查看已安装版本
n ls

# 切换到指定版本
sudo n 18
sudo n 20
sudo n 22

# 安装新版本
sudo n 24
```

**npm镜像：** 已配置淘宝镜像

### 4. Python环境

**版本：** Python 3.10.12

**pip镜像：** 已配置阿里云镜像 (~/.pip/pip.conf)

**常用命令：**
```bash
# 安装包
pip3 install package_name

# 创建虚拟环境
python3 -m venv myenv

# 激活虚拟环境
source myenv/bin/activate

# 退出虚拟环境
deactivate
```

### 5. Docker容器服务

**版本：** Docker 28.2.2

**镜像加速：** 已配置6个国内镜像源
- docker.1ms.run
- docker.xuanyuan.me
- docker.m.daocloud.io
- dockerhub.icu
- docker.udayun.com
- docker.awsl9527.cn

**启动Docker服务：**
```bash
sudo service docker start
```

**已创建的容器：**

| 容器名 | 镜像 | 端口 | 状态 |
|--------|------|------|------|
| mysql | mysql:8.0 | 3306 | ✅ 运行中 |
| redis | redis:7 | 6379 | ✅ 运行中 |

**容器管理命令：**
```bash
# 查看容器状态
docker ps -a

# 启动容器
docker start mysql redis

# 停止容器
docker stop mysql redis

# 进入MySQL容器
docker exec -it mysql bash

# 进入Redis容器
docker exec -it redis bash
```

**常用Docker命令：**
```bash
# 查看运行中的容器
docker ps

# 查看所有容器
docker ps -a

# 启动容器
docker start mysql redis

# 停止容器
docker stop mysql redis

# 进入容器
docker exec -it mysql bash

# 查看容器日志
docker logs mysql
```

---

## 镜像源配置汇总

| 工具 | 镜像源 | 配置文件 |
|------|--------|----------|
| apt | 阿里云 | /etc/apt/sources.list |
| pip | 阿里云 | ~/.pip/pip.conf |
| npm | 淘宝 | ~/.npmrc |
| Maven | 阿里云 | ~/.m2/settings.xml |
| Docker | 国内镜像 | /etc/docker/daemon.json |

---

## 端口映射说明

### 已配置的端口转发

| 服务 | WSL端口 | Windows访问地址 | 状态 |
|------|---------|-----------------|------|
| MySQL | 3306 | localhost:3306 | ✅ 已配置 |
| Redis | 6379 | localhost:6379 | ✅ 已配置 |
| Web应用 | 8080 | http://localhost:8080 | 按需配置 |

### 端口转发配置方法

由于WSL 2每次重启后IP地址可能变化，需要配置端口转发。已创建自动脚本：

**脚本位置：** `C:\Users\Assassin\Documents\WSL-PortForward.ps1`

**手动执行端口转发（管理员PowerShell）：**
```powershell
# 获取WSL IP地址
$wslIp = (wsl hostname -I).Trim().Split()[0]

# 配置MySQL端口转发
netsh interface portproxy delete v4tov4 listenport=3306 listenaddress=127.0.0.1 2>$null
netsh interface portproxy add v4tov4 listenport=3306 listenaddress=127.0.0.1 connectport=3306 connectaddress=$wslIp

# 配置Redis端口转发
netsh interface portproxy delete v4tov4 listenport=6379 listenaddress=127.0.0.1 2>$null
netsh interface portproxy add v4tov4 listenport=6379 listenaddress=127.0.0.1 connectport=6379 connectaddress=$wslIp

# 验证配置
netsh interface portproxy show all
```

**开机自动执行（已创建计划任务）：**
- 任务名称：WSL-PortForward
- 触发器：用户登录时
- 执行权限：最高权限

### 添加新端口转发

如需添加其他端口（如8080），在管理员PowerShell中执行：
```powershell
$wslIp = (wsl hostname -I).Trim().Split()[0]
netsh interface portproxy add v4tov4 listenport=8080 listenaddress=127.0.0.1 connectport=8080 connectaddress=$wslIp
```

---

## 常用操作

### 启动WSL
```powershell
# 在Windows PowerShell中执行
wsl -d Ubuntu-22.04
```

### 关闭WSL
```powershell
wsl --shutdown
```

### 文件访问
```bash
# 从WSL访问Windows文件
cd /mnt/c/Users/YourUsername

# 从Windows访问WSL文件
# 在文件资源管理器地址栏输入：
\\wsl$\Ubuntu-22.04\home\nova
```

### 启动Docker服务
```bash
# 每次进入WSL后需要手动启动Docker
sudo service docker start
```

---

## 快速启动脚本

创建启动脚本 `~/start-services.sh`：
```bash
#!/bin/bash
echo "启动Docker服务..."
sudo service docker start
sleep 3
echo "启动MySQL和Redis容器..."
docker start mysql redis 2>/dev/null || echo "容器未创建，请先创建容器"
echo "服务启动完成！"
```

使用方法：
```bash
chmod +x ~/start-services.sh
~/start-services.sh
```

---

## 故障排除

### 1. Docker无法启动
```bash
# 检查Docker服务状态
sudo service docker status

# 查看日志
sudo journalctl -xeu docker.service
```

### 2. 网络问题
```bash
# 重启WSL网络
# 在Windows PowerShell中执行：
wsl --shutdown
# 然后重新打开WSL
```

### 3. 权限问题
```bash
# 将用户添加到docker组（已配置）
sudo usermod -aG docker $USER
# 需要重新登录生效
```

---

## 数据库连接信息

### MySQL
- 主机: localhost
- 端口: 3306
- 用户名: root
- 密码: root123
- 默认数据库: devdb

### Redis
- 主机: localhost
- 端口: 6379
- 密码: 无

---

*文档生成时间: 2026-03-21*
