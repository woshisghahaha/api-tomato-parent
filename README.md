# API 开放平台项目

> 作者：张宇恒


## 项目简介

一个提供 API 接口供开发者调用的平台。

管理员可以接入并发布接口，统计分析各接口调用情况；用户可以注册登录并开通接口调用权限，然后可以浏览接口及在线调试，还能使用客户端 SDK 轻松在代码中调用接口。

主页（浏览接口）：


使用自己开发的客户端 SDK，一行代码调用接口（未完成）

### 前端

- React 18
- Ant Design Pro 5.x 脚手架
- Ant Design & Procomponents 组件库
- Umi 4 前端框架
- OpenAPI 前端代码生成

### 后端

- Java Spring Boot
- MySQL 数据库
- MyBatis-Plus 及 MyBatis X 自动生成
- API 签名认证（Http 调用）
- Spring Boot Starter（SDK 开发）
- Dubbo 分布式（RPC、Nacos）
- Swagger + Knife4j 接口文档生成
- Spring Cloud Gateway 微服务网关
- Hutool、Apache Common Utils、Gson 等工具库


# 安装流程
前端项目yarn/cnpm编译安装包
npm install --save echarts-for-react 安装图表
npm install --save echarts
安装nacos环境
使用虚拟机打开redis服务器用于缓存


# 项目环境
nodejs 16.14
jdk 1.8
npm 8.3
mysql 8.0.21

