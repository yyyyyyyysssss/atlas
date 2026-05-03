# Atlas

[![Deployment Status](https://img.shields.io/badge/status-deployed-brightgreen)](https://atlas.ys0921.sbs:4443)

> Atlas 是一个企业级管理平台，集成统一认证、权限控制、实时消息、通知分发、文件管理和前端管理控制台。

## 在线预览

- https://atlas.ys0921.sbs:4443

**默认登录账号**：`admin` / `123456`

## 主要功能亮点

### 认证与授权

- **JWT 认证体系**：使用 `JwtUtils` 生成和校验 JWT access/refresh token，支持签名校验和有效期控制。
- **一次性令牌登录**：支持 `OTT`（One-Time Token）魔法链接登录，增强邮箱登录体验与一次登录流程。
- **记住我登录**：基于 Header 的 RememberMe 服务实现，兼顾安全与用户体验。
- **多种认证方式**：支持用户名/密码、邮箱验证码、刷新令牌、一次性令牌、第三方登录认证。
- **OAuth2 预留能力**：已在 `auth-service` 与 `gateway` 中预留 OAuth2 路径与授权界面，便于后续接入第三方登录与授权。
- **统一权限校验**：后端通过 `SecurityConfig` 组合多种 `AuthenticationProvider`，实现权限边界与资源访问控制。

### 实时消息与通知

- **SSE 实时推送**：`notification-service` 提供 `SseSessionManager` 和前端 `SseManager`，支持持久连接、自动重连与心跳维持。
- **事件驱动**：通过 Spring `ApplicationEventPublisher` 发布连接/断开事件，实现通知链路的解耦与扩展。
- **通知模板与多渠道**：`NotificationRequest` 构建器采用 Builder 模式，支持模板消息、文本消息、卡片消息、文件通知与多渠道配置。
- **多渠道通知支持**：可通过 `email()`、`sms()`、`inbox()` 等方式发起通知，支持系统消息、邮件、短信和站内消息等多种渠道。
- **设计模式驱动**：通知模块使用 Builder、Template、Strategy 和流式消息处理，具备良好扩展性和渠道插拔能力。
- **广播与定向发送**：支持 SSE 广播、用户定向推送、通知模版渲染与事件分类。

### 消息与数据流

- **Redis 流与队列**：公共模块提供 Redisson Stream 生产者/消费者、分布式延迟队列、广播消费抽象，适用于消息通知、分布式任务与事件总线。
- **高并发会话管理**：使用 Redis 作为会话和安全上下文存储，实现网关层 Token 解析与跨服务共享。

### 文件与资源管理

- **文件服务能力**：`file-service` 提供文件上传、下载、访问权限、断点续传和文件预览能力。
- **对象存储适配**：基于 MinIO/对象存储的 `AbstractFileService` 抽象，支持不同存储后端扩展。

### 前端管理控制台

- **React/Vite 单页应用**：`web-ui` 采用 React 19、Vite、Ant Design 和 Tailwind CSS，提供现代化可视化管理界面。
- **实时通知体验**：前端集成 SSE 连接管理器，实现消息通知、状态更新和事件驱动 UI 刷新。
- **多语言 &国际化**：支持多语言配置与用户界面本地化。

### 可选 AI 扩展

- `ai-service` 提供基于 FastAPI 的 AI 服务入口，可作为智能问答、语义辅助或业务助手的扩展组件。

## 模块架构

### 公共基础模块

- `atlas-common/common-core`
- `atlas-common/common-redis`
- `atlas-common/common-mybatis`
- `atlas-common/common-security`

公共模块提供安全、缓存、MyBatis 封装、HTTP 工具、异常与响应标准等基础能力。

### 后端服务模块

- `gateway`：API 网关与统一入口，负责路由转发、鉴权、限流与安全上下文管理。
- `auth-service`：认证与登录服务，承担用户登录、Token 管理、一次性令牌、邮箱验证码与第三方认证。
- `user-service`：用户与组织管理服务，处理用户资料、角色权限、账号信息等。
- `file-service`：文件管理服务，支持文件存储、上传、下载和访问控制。
- `notification-service`：通知与事件服务，负责消息推送、SSE 订阅、通知模板和渠道分发。

### 前端与 AI 模块

- `web-ui`：前端管理控制台，整合仪表盘、列表、表单、通知中心与实时事件。
- `ai-service`：可选 AI 服务，用于智能对话/辅助功能。

## 关键技术栈

- Java 21
- Spring Boot 3.5.13
- Spring Cloud 2025.0.2
- Maven
- React 19
- Vite
- Ant Design
- Tailwind CSS
- Python 3
- FastAPI
- Redis / Redisson
- Docker
- Kubernetes

## 重要说明

- 已部署预览地址：`https://atlas.ys0921.sbs:4443`
- `ai-service` 为可选扩展模块，不属于主 Maven 聚合构建。
- 通知与实时事件模块基于可扩展的模板与渠道设计，适合后续接入更多消息渠道。
- 认证模块已预留 OAuth2 路径与第三方登录能力，适配未来 SSO/OAuth2 扩展。

## 快速定位

- `pom.xml` - 根 Maven 聚合配置
- `deploy-template.yaml` / `deploy-web-template.yaml` - Kubernetes 部署模板
- `auth-service/src/main/java/com/atlas/auth/config/security/SecurityConfig.java` - 认证与授权核心配置
- `atlas-common/common-security/src/main/java/com/atlas/security/utils/JwtUtils.java` - JWT 生成与校验
- `notification-service/src/main/java/com/atlas/notification/sse/SseSessionManager.java` - SSE 实时会话管理
- `atlas-common/common-core/src/main/java/com/atlas/common/core/api/notification/builder/NotificationRequest.java` - 通知构建器与多渠道设计
- `web-ui/src/sse/SseManager.js` - 前端 SSE 管理

## 贡献与合作

欢迎通过 GitHub 提交 issue 或 pull request，分享你的改进建议、问题报告和功能扩展方案。

- 如果你希望增强认证能力，可关注 `auth-service` 的 OAuth2/SAML/SSO 集成路径。
- 如果你希望扩展通知渠道，可在 `notification-service` 中补充更多 `NotificationRequest` 渠道实现。
- 如果你希望增强前端体验，可在 `web-ui` 中添加更多实时通知、仪表盘和业务组件。

感谢你的关注和贡献，让 Atlas 更适合企业级管理场景。
