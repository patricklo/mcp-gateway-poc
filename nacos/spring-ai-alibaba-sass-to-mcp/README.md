# Spring AI Alibaba SASS to MCP 项目
https://juejin.cn/post/7612624423944175643
## 项目简介

本项目是一个基于 Spring Boot 4.0.0 和 Spring Alibaba AI 的示例项目，实现了 SASS 到 MCP 的服务调用架构。项目包含三个模块：

1. **sass-to-mcp-sass**：订单服务模块，提供订单详情、订单列表和订单商品列表接口
2. **sass-to-mcp-mcp**：MCP服务端模块，服务注册到 Nacos，调用 SASS 服务的 API 接口
3. **sass-to-mcp-agent**：AI代理模块，实现 MCP 客户端，通过 WebSocket 与用户交互

## 技术栈

- Spring Boot 4.0.0
- Spring Cloud Alibaba
- Spring AI
- WebSocket
- Nacos 服务发现
- WebClient
- SLF4J 日志
- OkHttp 测试

## 项目结构

```
spring-ai-alibaba-sass-to-mcp/
├── sass-to-mcp-sass/         # 订单服务模块
├── sass-to-mcp-mcp/          # MCP服务端模块
├── sass-to-mcp-agent/         # AI代理模块
└── README.md                  # 项目说明
```

## 模块说明

### 1. sass-to-mcp-sass（订单服务）

- 端口：8081
- 接口：
  - GET /api/order/detail - 订单详情
  - GET /api/order/list - 订单列表
  - GET /api/order/items - 订单商品列表

### 2. sass-to-mcp-mcp（MCP服务端）

- 端口：8082
- 服务注册：Nacos
- 接口：
  - GET /api/mcp/order/detail - 订单详情
  - GET /api/mcp/order/list - 订单列表
  - GET /api/mcp/order/items - 订单商品列表

### 3. sass-to-mcp-agent（AI代理）

- 端口：8083
- WebSocket：/ws/chat
- 聊天界面：http://localhost:8083/chat.html

## 快速开始

### 1. 启动 Nacos 服务

确保本地 Nacos 服务已启动，默认地址：http://localhost:8848

### 2. 编译项目

```bash
mvn clean package -DskipTests
```

### 3. 启动服务

按以下顺序启动服务：

1. **启动订单服务**
   ```bash
   cd sass-to-mcp-sass
   mvn spring-boot:run
   ```

2. **启动 MCP 服务**
   ```bash
   cd sass-to-mcp-mcp
   mvn spring-boot:run
   ```

3. **启动 AI 代理服务**
   ```bash
   cd sass-to-mcp-agent
   mvn spring-boot:run
   ```

### 4. 访问测试

- 订单服务接口：http://localhost:8081/api/order/list
- MCP服务接口：http://localhost:8082/api/mcp/order/list
- AI聊天界面：http://localhost:8083/chat.html

## 聊天界面使用

在聊天界面中，您可以输入以下指令：

1. **订单详情 [订单号]** - 查询指定订单的详情
2. **订单列表** - 查询订单列表
3. **订单商品 [订单号]** - 查询指定订单的商品列表

## 日志配置

- 日志文件路径：d:/log/spring-ai-alibaba-sass-to-mcp/
- 日志级别：info
- 包含 traceId 打印

## 注意事项

- JDK 版本要求：JDK 25
- 编码：UTF-8
- 配置文件：YAML 格式
- 日志：XML 配置文件
