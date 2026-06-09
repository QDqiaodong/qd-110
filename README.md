# 个人作息与习惯养成助手

> 面向有自律提升需求的用户，用于制定每日作息、培养生活习惯，记录完成情况并生成阶段性统计。纯个人事务管理与数据记录，无打卡变现、社群付费等功能。

---

## 📋 项目特性

### 核心功能
- **习惯清单创建** - 自定义生活、学习、作息类习惯，设置每日执行时间与提醒规则
- **每日完成打卡** - 每日核对各项习惯执行情况，标记完成、未完成状态，留存每日记录
- **作息模板管理** - 选用系统预置模板或自定义作息表，灵活调整每日时间规划
- **阶段性数据统计** - 按周、月统计习惯完成率，以数据形式直观展示个人自律情况

### 技术栈

| 层级 | 技术选型 | 说明 |
|------|----------|------|
| 前端 | Vue 3 + Vite + Pinia + Vant + ECharts | 组件按需引入，本地离线缓存 |
| 后端 | Spring Boot 3.3 + JDK 17 + MyBatis-Plus | RESTful API，Redis 缓存 |
| 数据库 | MySQL 8.0 + Redis 7 | MySQL 持久化存储 + Redis 短期缓存 |
| 部署 | Docker + docker-compose + Nginx | 全链路容器化，分层缓存构建 |

### 端口分配表（固定端口，禁止冲突）

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 | 3010 | Nginx 静态资源 |
| 后端 | 8090 | Spring Boot API |
| MySQL | 3313 | 数据库（避开默认3306） |
| Redis | 6382 | 缓存（避开默认6379） |

---

## 🚀 快速开始

### 方式一：一键启动脚本（推荐）

```bash
# 自动检测端口、构建、启动
./scripts/start.sh
```

### 方式二：Docker Compose 手动启动

```bash
# 1. 确保 .env 文件已配置好
# 2. 构建并启动所有服务
docker compose up -d --build

# 3. 查看服务状态
docker compose ps

# 4. 查看日志
docker compose logs -f
```

### 方式三：本地开发模式

#### 前端开发
```bash
cd frontend
npm install
npm run dev
# 访问: http://127.0.0.1:3010
```

#### 后端开发
```bash
cd backend
mvn spring-boot:run
# API: http://127.0.0.1:8090/api
```

---

## 🌐 访问地址

| 服务 | 地址 |
|------|------|
| 前端首页 | http://localhost:3010 |
| 前端首页（IP | http://127.0.0.1:3010 |
| 后端API | http://127.0.0.1:8090/api |
| MySQL | 127.0.0.1:3313 |
| Redis | 127.0.0.1:6382 |

> ✅ localhost 和 127.0.0.1 访问必须一致，启动后可运行 `./scripts/verify.sh` 验证

---

## 📁 项目结构

```
.
├── frontend/                     # 前端项目 (Vue3 + Vite)
│   ├── src/
│   │   ├── views/            # 页面组件
│   │   │   ├── Checkin.vue    # 今日打卡
│   │   │   ├── Habits.vue   # 习惯清单
│   │   │   ├── Schedule.vue  # 作息模板
│   │   │   └── Stats.vue      # 数据统计
│   │   ├── store/habit.js     # Pinia 状态管理
│   │   ├── router/            # 路由配置
│   │   ├── utils/              # 工具函数
│   │   └── assets/styles/     # 全局样式
│   ├── Dockerfile              # 前端 Docker 构建
│   ├── vite.config.js          # Vite 配置（严格绑定127.0.0.1）
│   └── package.json
│
├── backend/                      # 后端项目 (SpringBoot 3.3)
│   ├── src/main/
│   │   ├── java/com/habit/
│   │   │   ├── controller/     # REST API 控制器
│   │   │   ├── service/        # 业务逻辑 + Redis 缓存
│   │   │   ├── mapper/       # MyBatis-Plus Mapper
│   │   │   ├── entity/         # 数据库实体
│   │   │   ├── dto/            # 数据传输对象
│   │   │   └── config/         # Redis/CORS 等配置
│   │   └── resources/
│   │       ├── schema.sql        # 数据库初始化
│   │       ├── mapper/         # XML 映射文件
│   │       └── application.yml   # 应用配置
│   ├── Dockerfile              # 后端 Docker 构建
│   ├── settings.xml           # Maven 国内镜像配置
│   └── pom.xml
│
├── docker/                      # Docker 配置
│   └── nginx.conf            # Nginx 反向代理
│
├── scripts/                     # 辅助脚本
│   ├── start.sh            # 一键启动脚本
│   └── verify.sh          # 端口自检脚本
│
├── .env                         # 全局环境变量（端口、镜像源）
├── docker-compose.yml           # 容器编排配置
└── README.md
```

---

## ⚙️ 配置说明

### .env 环境变量

```bash
# Docker 镜像仓库（统一国内镜像源）
DOCKER_REGISTRY=docker.m.daocloud.io/library/

# 端口配置
FRONTEND_PORT=3010
BACKEND_PORT=8090
MYSQL_PORT=3313
REDIS_PORT=6382

# 数据库配置
MYSQL_ROOT_PASSWORD=habit123456
MYSQL_DATABASE=habit_db

# 容器名称
CONTAINER_FRONTEND=habit-assistant-frontend
CONTAINER_BACKEND=habit-assistant-backend
CONTAINER_MYSQL=habit-assistant-mysql
CONTAINER_REDIS=habit-assistant-redis
```

### Docker 构建缓存优化

**前端 Dockerfile 分层策略：
1. 先复制 `package.json` + `package-lock.json`
2. 安装依赖（淘宝 npm 国内镜像）
3. 复制源码
4. 构建项目

> ✅ package.json 不变更时，依赖层缓存复用

**后端 Dockerfile 分层策略：
1. 复制 `settings.xml`（Maven 国内镜像配置
2. 复制 `pom.xml`
3. 下载依赖
4. 复制源码
5. 编译打包
> ✅ pom.xml 不变更时，依赖层缓存复用

### Redis 缓存策略

| 缓存内容 | 过期时间 | 说明 |
|----------|----------|------|
| 习惯列表 | 1 小时 | 高频读多写少 |
| 每日打卡记录 | 24 小时 | 按天过期自动清理 |
| 作息模板 | 2 小时 | 系统预置数据 |
| 统计数据 | 30 分钟 | 自动失效重新计算 |

---

## 📝 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/habits | 获取习惯列表 |
| POST | /api/habits | 创建习惯 |
| PUT | /api/habits/{id} | 更新习惯 |
| DELETE | /api/habits/{id} | 删除习惯 |
| GET | /api/checkins/{date} | 获取某日打卡记录 |
| POST | /api/checkins | 切换打卡状态 |
| GET | /api/schedules/templates | 获取作息模板列表 |
| GET | /api/schedules/current | 获取当前使用的作息模板 |
| PUT | /api/schedules/current | 设置当前作息模板 |
| GET | /api/stats/week | 获取本周统计数据 |
| GET | /api/stats/month | 获取本月统计数据 |

---

## 🔍 自检与验证

### 运行端口自检脚本：

```bash
./scripts/verify.sh
```

脚本会检查：
- ✅ 所有端口是否正常监听
- ✅ 127.0.0.1 和 localhost 访问一致性
- ✅ 页面标题是否一致
- ✅ HTTP 响应是否正常

### 手动验证

```bash
# 检查端口占用
lsof -nP -iTCP:3010 -sTCP:LISTEN

# 验证前端访问
curl -sS http://127.0.0.1:3010 | head -20
curl -sS http://localhost:3010 | head -20
```

---

## 📌 端口约束规则

本项目严格遵守以下端口规则：

1. ✅ 所有端口固定，禁止自动变更
2. ✅ 所有服务绑定 `127.0.0.1，禁止 `0.0.0.0`
3. ✅ Docker 端口映射格式：`127.0.0.1:${PORT}:容器端口`
4. ✅ Vite 配置 `strictPort: true`，端口占用直接报错
5. ✅ 避开默认端口：80、443、8080、3306、6379 等
6. ✅ 启动前自动检测端口冲突

---

## 🛠️ 常用命令

```bash
# 启动服务
docker compose up -d --build

# 停止服务
docker compose down

# 查看日志
docker compose logs -f [服务名]

# 重启某个服务
docker compose restart [服务名]

# 进入容器
docker exec -it habit-assistant-mysql mysql -uroot -phabit123456

# 清理构建缓存（强制重新下载依赖）
docker compose build --no-cache
```

---

## 📄 License

MIT License
