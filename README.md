# Collect - 地图要素采集系统

基于天地图的地理要素采集与管理系统，支持点/线/面要素的交互绘制、属性录入、图层管理与空间查询。

> **声明：本项目全部代码由 AI 生成，未经过人工编写或修改。**

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 + Vite | 3.5 / 8.0 |
| UI 组件库 | Element Plus | 2.9 |
| 地图引擎 | OpenLayers | 10.9 |
| 状态管理 | Pinia | 2.3 |
| 路由 | Vue Router | 4.5 |
| HTTP 客户端 | Axios | 1.7 |
| 后端框架 | Spring Boot | 2.7.18 |
| 安全框架 | Spring Security + JWT | - |
| ORM | MyBatis-Plus | 3.5.3 |
| 数据库 | PostgreSQL | - |
| 构建工具 | Maven | 3.9 |
| Java | JDK 1.8 | 1.8.0_181 |

## 功能特性

### 用户认证
- 用户注册与登录（JWT 鉴权）
- 密码前端 SHA-256 哈希 + 后端 BCrypt 加密双重保护
- Spring Security 统一认证与授权

### 地图底图
- 集成天地图 CGCS2000（EPSG:4490）坐标系
- 电子地图 / 影像地图一键切换（单按钮切换模式）

### 要素绘制
- 点 / 线 / 面 三种要素类型交互绘制
- 绘制完成自动弹出属性填写表单
- 属性包含：名称、行政区划（级联选择）、区划代码、地址、备注
- 线/面绘制右键菜单：确定完成 / 取消重绘
- 右键点位不参与图形，点位不足时提示继续绘制
- 绘制时禁用双击缩放，防止误触

### 图层管理
- 按要素类型（点/线/面）分图层显示
- 图层勾选控制显隐
- 新增要素后自动刷新图层

### 空间查询
- 点击查询模式（开关控制）
- 点击要素弹出详情面板
- 面板跟随地图移动（地理坐标锚定）

### 地图控件
- 放大 / 缩小按钮
- 行政区划级联选择与地图定位

## 项目结构

```
collect/
├── backend/                          # 后端 Spring Boot 工程
│   ├── src/main/java/com/example/auth/
│   │   ├── config/                   # 安全配置、JWT 过滤器
│   │   ├── controller/               # REST 控制器
│   │   │   ├── AuthController        # 认证接口（注册/登录）
│   │   │   ├── MapFeatureController  # 地图要素 CRUD
│   │   │   └── XzqhController       # 行政区划查询
│   │   ├── entity/                   # 实体类
│   │   ├── exception/                # 全局异常处理 & 统一响应
│   │   ├── mapper/                   # MyBatis-Plus Mapper
│   │   ├── service/                  # 业务逻辑层
│   │   └── util/                     # JWT 工具类
│   └── src/main/resources/
│       ├── application.yml           # 应用配置
│       └── schema.sql                # 数据库建表脚本
│
├── frontend/                         # 前端 Vue3 工程
│   ├── src/
│   │   ├── api/                      # 接口封装（auth / feature / xzqh）
│   │   ├── assets/                   # 静态资源（底图图标 SVG）
│   │   ├── components/               # 组件
│   │   │   ├── DrawToolbar.vue       # 绘制工具栏（点/线/面 + 右键菜单）
│   │   │   ├── MapControls.vue       # 地图控件（缩放/查询/图层）
│   │   │   ├── FeatureFormDialog.vue # 要素属性填写弹窗
│   │   │   └── XzqhSelector.vue      # 行政区划级联选择
│   │   ├── router/                   # 路由配置
│   │   ├── stores/                   # Pinia 状态管理
│   │   ├── utils/                    # 工具函数
│   │   │   ├── request.js            # Axios 封装（JWT 拦截器）
│   │   │   ├── tdtLayers.js          # 天地图图层工厂
│   │   │   └── crypto.js             # SHA-256 哈希
│   │   └── views/                    # 页面
│   │       ├── Login.vue             # 登录
│   │       ├── Register.vue          # 注册
│   │       ├── Home.vue              # 首页
│   │       └── MapView.vue           # 地图主页面
│   └── vite.config.js                # Vite 配置（含代理）
│
└── rebuild_map_feature.sql           # 地图要素表结构升级脚本
```

## 快速开始

### 环境要求

- Java 1.8+
- Node.js 16+
- PostgreSQL 12+
- Maven 3.6+

### 1. 数据库准备

创建 PostgreSQL 数据库，项目启动时会通过 `schema.sql` 自动建表：

```sql
-- 数据库需已存在，schema.sql 使用 IF NOT EXISTS 保证幂等
-- 默认连接: localhost:5432/postgres
```

如需修改数据库连接信息，编辑 `backend/src/main/resources/application.yml`。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端启动在 `http://localhost:8090`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动在 `http://localhost:5173`，API 请求通过 Vite 代理转发到后端。

### 4. 天地图 Token

项目使用的天地图 Token 配置在 `frontend/src/utils/tdtLayers.js` 中。如需更换为自己的 Token，修改该文件中的 `TDT_TOKEN` 常量即可。

## 核心接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |
| GET | /api/features/all | 获取全部地图要素 |
| POST | /api/features | 新增地图要素 |
| GET | /api/xzqh/provinces | 获取省级区划 |
| GET | /api/xzqh/cities?provinceCode=xx | 获取市级区划 |
| GET | /api/xzqh/districts?cityCode=xx | 获取区县级区划 |

## 数据库表结构

### sys_user - 系统用户表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| username | VARCHAR(50) | 用户名（唯一） |
| password | VARCHAR(200) | 密码（BCrypt 加密） |
| nickname | VARCHAR(50) | 昵称 |
| status | SMALLINT | 状态：1-正常，0-禁用 |

### map_feature - 地图要素表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| type | SMALLINT | 要素类型：1=点 2=线 3=面 |
| geometry | TEXT | WKT 格式几何数据 |
| name | VARCHAR(100) | 名称 |
| xzqhname | VARCHAR(100) | 行政区划名称 |
| code | VARCHAR(20) | 行政区划代码 |
| address | VARCHAR(200) | 地址 |
| remark | VARCHAR(500) | 备注 |
| created_by | VARCHAR(50) | 创建人 |

## AI 生成声明

本项目从需求分析、架构设计到全部前后端代码编写，均由 AI 完成，涵盖了：

- 数据库设计与建表脚本
- Spring Boot 后端全部业务逻辑（认证、CRUD、异常处理）
- Vue 3 前端全部页面与组件
- OpenLayers 地图集成与交互（绘制、查询、图层管理）
- 天地图 EPSG:4490 坐标系适配
- JWT 认证与安全配置

## License

MIT