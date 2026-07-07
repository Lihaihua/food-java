# 三勾点餐系统-连锁店版

**面向开发、二开友好的开源餐饮连锁系统**

三勾点餐系统基于 Spring Boot + Element Plus + uni-app 打造的面向开发的连锁餐饮小程序商城，方便二次开发或直接使用，可发布到多端，包括微信小程序、微信公众号、QQ小程序、支付宝小程序、字节跳动小程序、百度小程序、Android端、iOS端。

---

## 项目简介

三勾点餐系统是一套完整的连锁餐饮 SAAS 解决方案，支持多租户、多门店管理，提供从商品管理、订单处理、会员营销、门店管理到数据统计的全流程业务能力。系统分为后端服务（jjj_food_chain）、SAAS管理端前端（jjj_food_chain_admin）、店铺端前端（jjj_food_chain_shop）、移动端前端（jjj_food_chain_app）四个部分。

## 项目特色

- **SAAS 支持**：无限多开、实现多租户应用开发，支持连锁门店管理
- **前后分离**：开发更清晰、分工更明确、提升开发效率
- **Element Plus**：基于饿了么团队 UI 库、用户体验超棒
- **Spring Boot**：国内流行的 Java 框架、结构代码清晰
- **极易二开**：代码结构清晰、快速开发应用
- **多平台支持**：微信小程序、H5、微信公众号、支付宝小程序、App 打包，开发不浪费
- **开发规范**：前后端高度一致的权限控制、实现项目规范
- **三端分离**：SAAS管理后台（admin）、店铺管理（shop）、移动端（app）独立开发，共用后端服务
- **模块化设计**：后端采用 Maven 多模块架构，各业务模块职责清晰，易于扩展

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Java 8 + Spring Boot 2.3.12 |
| ORM 框架 | MyBatis-Plus 3.4.1 |
| 权限框架 | Shiro 1.10.0 + JWT 3.10.1 |
| 缓存 | Redis + Spring Cache |
| 构建工具 | Maven 多模块 |
| 数据库 | MySQL 5.7+ |
| SAAS管理端前端 | Vue 3 + Vite + Element Plus + Pinia |
| 店铺端前端 | Vue 3 + Vite + Element Plus + Pinia |
| 移动端前端 | uni-app + Vue 3（支持微信小程序、H5、多端发布） |
| 微信集成 | weixin-java-sdk 4.7.2.B |
| 云存储 | 七牛云 / 阿里云 OSS / 腾讯云 COS |
| 文档工具 | Knife4j 2.0.2（Swagger UI 增强版） |

## 项目源码

| 项目目录 | 说明 | 开发工具 | 核心技术 |
|---------|------|---------|---------|
| database | 数据库 | MySQL 5.7、MySQL 8.0 | - |
| jjj_food_chain | Java 后端 | IntelliJ IDEA | Spring Boot 2.3.12.RELEASE |
| jjj_food_chain_admin | SAAS管理端后台 | HBuilderX、VSCode 等 JS 开发工具 | Vue 3、Element Plus |
| jjj_food_chain_shop | 店铺端管理后台 | HBuilderX、VSCode 等 JS 开发工具 | Vue 3、Element Plus |
| jjj_food_chain_app | 移动端 | HBuilderX | Vue 3、uni-app |

## 后端架构

### 模块划分

后端采用 Maven 多模块架构，各模块职责清晰：

```
jjj_food_chain/
├── bootstrap/              # 启动模块（主入口）
├── config/                 # 配置模块（Shiro、Redis、Swagger 等）
├── jjj-common/             # 公共模块
│   ├── entity/             #   数据库实体（MyBatis-Plus）
│   ├── mapper/             #   Mapper 接口
│   ├── service/            #   Service 接口和实现
│   ├── enums/              #   业务枚举
│   └── util/               #   工具类
├── jjj-admin/              # SAAS管理端 API（平台运营）
│   └── controller/         #   管理端接口（/api/admin/*）
├── jjj-shop/               # 店铺端 API（商家管理）
│   └── controller/         #   店铺端接口（/api/shop/*）
├── jjj-front/              # 移动端 API（用户端）
│   └── controller/         #   移动端接口（/api/front/*）
├── jjj-job/                # 定时任务模块
├── generator/              # 代码生成器
└── boot-admin/             # Spring Boot Admin 监控
```

关键设计：
- 三端（admin/shop/front）共用同一套 Entity、Mapper、Service
- 通过不同的 Controller 层实现接口隔离和权限控制
- 公共业务逻辑统一在 jjj-common 模块维护

### 核心能力

| 能力 | 实现 |
|------|------|
| 统一返回格式 | `ApiResult<T>` 封装 code/message/data |
| 认证鉴权 | Shiro + JWT（token 签发/校验/刷新） |
| 参数校验 | Hibernate Validator + 自定义校验器 |
| XSS 防护 | XSS 过滤器 |
| 文件上传 | 工厂模式，支持本地/七牛云/阿里云/腾讯云 |
| 分页查询 | MyBatis-Plus 分页插件 |
| 全局异常 | 统一异常处理 |
| 操作日志 | AOP 切面自动记录 |
| 定时任务 | ShedLock 分布式锁 + Spring @Scheduled |
| 接口文档 | Knife4j 可视化接口文档 |
| 微信支付 | 微信小程序支付、微信公众号支付 |
| 物流查询 | 快递100 物流查询接口 |
| 打印功能 | 小票打印、订单打印 |

### 多端 API 隔离

后端通过 URL 前缀区分不同端的 API：

| 端 | URL 前缀 | 对应模块 | 认证方式 |
|----|---------|---------|---------|
| SAAS管理端 | `/api/admin/` | `jjj-admin` | Header Token |
| 店铺端 | `/api/shop/` | `jjj-shop` | Header Token |
| 移动端 | `/api/front/` | `jjj-front` | app_id + Token |

默认端口：`8891`

## 项目结构

```
根目录
├── jjj_food_chain/                   # 后端 Java 源码
│   ├── bootstrap/                    #   启动模块
│   ├── config/                       #   配置模块
│   ├── jjj-common/                   #   公共模块（Entity/Mapper/Service）
│   ├── jjj-admin/                    #   SAAS管理端 API
│   ├── jjj-shop/                     #   店铺端 API
│   ├── jjj-front/                    #   移动端 API
│   ├── jjj-job/                      #   定时任务
│   ├── generator/                    #   代码生成器
│   └── boot-admin/                   #   Spring Boot Admin 监控
├── jjj_food_chain_admin/             # SAAS管理端前端（Vue 3）
├── jjj_food_chain_shop/              # 店铺端前端（Vue 3）
├── jjj_food_chain_app/               # 移动端前端（uni-app）
├── db/                               # 数据库脚本
```

## 功能模块

### SAAS管理端（jjj_food_chain_admin）

面向平台超级管理员使用，管理整个 SAAS 平台的基础配置和多租户管理。

| 模块 | 功能 |
|------|------|
| 租户管理 | 租户列表、租户审核、租户配置、多租户隔离 |
| 门店管理 | 连锁门店管理、门店审核、门店配置 |
| 权限管理 | 管理员账号、角色权限、菜单管理 |
| 区域管理 | 省市区数据维护 |
| 系统设置 | 系统参数配置、全局设置 |
| 应用管理 | 插件管理 |

### 店铺端（jjj_food_chain_shop）

面向店铺运营人员使用，管理店铺日常运营和点餐业务。

| 模块 | 功能 |
|------|------|
| 首页看板 | 数据概览、待处理事项、快捷入口 |
| 商品管理 | 商品列表、商品分类、商品规格、商品评价 |
| 订单管理 | 订单列表、订单详情、订单发货、退款/售后处理 |
| 点餐管理 | 堂食点餐、外卖订单、桌台管理、叫号管理 |
| 会员管理 | 会员列表、会员等级、会员标签、余额明细 |
| 营销中心 | 优惠券管理、文章管理、文章分类、专题管理、推荐位、收藏管理 |
| 门店管理 | 门店列表、门店店员、门店订单（自提/核销）、桌台管理 |
| 页面装修 | 首页装修、分类页装修、个人中心装修、底部导航、主题风格 |
| 数据统计 | 销售统计、用户统计、门店统计 |
| 应用设置 | 小程序配置、微信配置 |
| 系统设置 | 店铺信息、交易设置、配送方式、快递公司、退货地址、上传设置、打印机、打印模板、短信设置、客服设置 |
| 权限管理 | 店铺账号、角色权限、登录日志、操作日志 |
| 文件管理 | 文件库、文件分组 |

### 移动端（jjj_food_chain_app）

面向终端用户使用（微信小程序/H5），支持点餐、外卖、自提等多种场景。

| 模块 | 功能 |
|------|------|
| 首页 | 轮播图、商品推荐、分类导航、营销活动入口 |
| 点餐功能 | 扫码点餐、堂食点餐、外卖下单、自提下单 |
| 商品浏览 | 商品列表、商品详情、商品搜索、商品分类 |
| 购物车 | 加入购物车、购物车管理、批量结算 |
| 订单流程 | 确认订单、收银台支付、订单列表、订单详情、物流跟踪、订单评价 |
| 售后服务 | 申请退款、退款详情 |
| 会员中心 | 个人信息、收货地址管理、我的优惠券、我的收藏、我的钱包、积分明细 |
| 营销活动 | 优惠券领取 |
| 内容浏览 | 文章列表、文章详情 |
| 门店服务 | 门店列表、门店详情、门店订单（自提）、桌台选择 |
| 自定义页面 | 支持 DIY 页面装修 |
| 微信功能 | 微信登录、微信支付、手机号绑定 |

## 数据库

`db/` 目录包含数据库初始化和迁移脚本。

## 环境要求

| 环境类型 | 开发工具 | 版本要求 |
|---------|---------|---------|
| Java 后端 | IntelliJ IDEA | JDK 1.8、Maven 3.6+ |
| 后端 Vue 管理页面 | HBuilderX、VSCode 等 JS 开发工具 | Node 16+（推荐16） |
| 前端页面 | HBuilderX | uni-app |
| 数据库 | MySQL | 5.7+ / 8.0+ |
| 缓存 | Redis | 3.0+ |

:warning: **注意**：Node.js 16 版本以下存在兼容性问题，请勿使用

## 快速开始

### 1. 数据库安装

```bash
# MySQL 5.7 或 8.0 新建数据库 jjjfood
CREATE DATABASE jjjfood DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

# 导入脚本 db/init.sql
```

### 2. Redis 安装

```bash
# 安装 Redis，设置密码
```

### 3. 后端启动

```bash
# 用 IDEA 打开 jjj_food_chain 目录，配置 Maven 镜像，编译成功
# 修改 jjj_food_chain/config/src/main/resources/config/application-dev.yml 里面的数据库配置和 Redis 配置
# 启动 SpringBootJjjApplication

# 后端默认端口：8891
# 接口文档地址：http://localhost:8891/doc.html（支持 Swagger/Knife4j）
```

### 4. 店铺端页面启动

```bash
# cmd 进入 jjj_food_chain_shop 目录
cd jjj_food_chain_shop
npm install

# 修改 jjj_food_chain_shop/.env.development 里面的后端地址，默认是 http://127.0.0.1:8891
npm run dev

# 启动成功后进入后台，默认密码 admin/123456
```

### 5. SAAS管理端页面启动

```bash
# cmd 进入 jjj_food_chain_admin 目录
cd jjj_food_chain_admin
npm install

# 修改 .env.development 里面的后端地址，默认是 http://127.0.0.1:8891
npm run dev
```

### 6. uni-app 移动端启动

```bash
# 用 HBuilderX 打开 jjj_food_chain_app 目录
# 修改 manifest.json 文件，可视化修改 Web 配置 -> 路由模式为 hash
# 修改 env/development.js 里面的域名，默认是 http://127.0.0.1:8891
# 点击菜单 运行 -> 运行到浏览器 -> Chrome

# 或编译到微信小程序
# 点击菜单 运行 -> 运行到小程序模拟器 -> 微信开发者工具
```

## 部署说明

### 生产环境打包

```bash
# 后端打包
cd jjj_food_chain
mvn clean package -Pprod

# 店铺端打包
cd jjj_food_chain_shop
npm run build

# SAAS管理端打包
cd jjj_food_chain_admin
npm run build

# 移动端打包
# 使用 HBuilderX 打开 jjj_food_chain_app
# 点击菜单 发行 -> 小程序-微信（仅适用于uni-app）
# 或 发行 -> H5
```

### 部署建议

- **后端**：使用 Nginx 反向代理 + Systemd/Supervisor 守护进程
- **前端**：静态文件部署到 Nginx，配置 gzip 压缩
- **数据库**：定期备份，开启慢查询日志
- **Redis**：配置持久化，设置合理的过期策略
- **文件存储**：生产环境建议使用云存储（七牛云/阿里云/腾讯云）

## 技术框架

![技术框架图](https://www.jjjshop.net/gitee/food-java/jiagou.png)

## 系统功能

![系统功能图](https://www.jjjshop.net/gitee/food-java/gongneng.png)

## 项目截图

### 后台管理

| ![后台截图1](https://www.jjjshop.net/gitee/food-java/kjava01.jpg) | ![后台截图2](https://www.jjjshop.net/gitee/food-java/kjava02.jpg) |
|---|---|
| ![后台截图3](https://www.jjjshop.net/gitee/food-java/kjava03.jpg) | ![后台截图4](https://www.jjjshop.net/gitee/food-java/kjava04.jpg) |

### 移动端截图

| ![移动端1](https://www.jjjshop.net/gitee/food-java/kp01.jpg) | ![移动端2](https://www.jjjshop.net/gitee/food-java/kp02.jpg) | ![移动端3](https://www.jjjshop.net/gitee/food-java/kp03.jpg) |
|---|---|---|
| ![移动端4](https://www.jjjshop.net/gitee/food-java/kp04.jpg) | ![移动端5](https://www.jjjshop.net/gitee/food-java/kp05.jpg) | ![移动端6](https://www.jjjshop.net/gitee/food-java/kp06.jpg) |

## 开发指南

| 名称 | 地址 |
|------|------|
| 官方文档 | https://doc.jjjshop.net/ChainJava |
| 视频教程 | https://doc.jjjshop.net/ChainJava?category_id=10039&document_id=1295 |
| 本地安装 | https://doc.jjjshop.net/ChainJava?category_id=10039&document_id=1104 |
| 线上部署 | https://doc.jjjshop.net/ChainJava?category_id=10039&document_id=1118 |
| 二开说明 | https://doc.jjjshop.net/ChainJava?category_id=10039&document_id=1121 |
| 功能说明 | https://doc.jjjshop.net/ChainJava?category_id=10039&document_id=1125 |
| 常见问题 | https://doc.jjjshop.net/ChainJava?category_id=10039&document_id=1130 |
| 接口文档 | 支持 Swagger/Knife4j |

## 特别感谢

- Gitee 官方
- Element Plus: [https://element-plus.gitee.io/zh-CN/](https://element-plus.gitee.io/zh-CN/)
- Vue: [https://cn.vuejs.org/](https://cn.vuejs.org/)
- uni-app: [https://uniapp.dcloud.io/](https://uniapp.dcloud.io/)

## 技术支持

- **官网地址**：[https://www.jjjshop.net](https://www.jjjshop.net)
- **交流QQ群**：173576291
- **问题反馈**：提交 Issue


如果这个项目对你有帮助，欢迎 Star ⭐ 支持！
