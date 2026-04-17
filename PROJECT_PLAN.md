# Seichou-Logos (成長の軌跡 / 成长之理) 项目开发计划书

## 1. 项目概述 (Project Overview)
「Seichou-Logos」是一款 **以 AI 量化为核心的个人成长养成系统**。
项目定位于“人生养成游戏”，通过 AI 对用户行为的深度价值提取，实现多维度的量化进步。它与时间记录软件互补，专注于记录事件的“质”而非仅仅是“量”。

**核心思想**：
1. **AI 量化为核**：AI 负责将碎片化的生活记录转化为结构化的成长数据。
2. **互补性设计**：不追求精确的时间统计，而是追求对行为意义的深度解读。
3. **极度宽容**：对负面事件做最宽容的理解，保护用户的心理能量。

## 2. 当前开发进度 (Current Status) - 截至 2026-04-17

### 2.1 前端架构 (Frontend Architecture) - 核心功能已闭环
- [x] **技术栈**: Vue 3 + Vite + TypeScript + Tailwind CSS + Pinia + Vue Router。
- [x] **登录鉴权 (Auth & Route Guards)**: 实现了统一精美的 Auth 面板，具备前端路由强制保护与重定向逻辑，已完整集成 Mock Token 并可在本地平滑替换。
- [x] **Store 重构**: `authStore`, `boardStore`, `logStore`, `preferenceStore` 已全部对接后端接口。
- [x] **UI 布局与视觉降噪**: 实现了企业级响应式布局，Dashboard 看板的“四大能量”严格同步到顶部状态栏，高度一致性降低用户视觉负担。集成“小树成长系统”并实现跨终端（桌面/移动）全响应式交互，支持点击跳转至专属“小树养成花园”视图。
- [x] **体验护航级细节 (Resilient UX)**: 增加动态骨架屏 (Skeleton) 完美填补网络空白，部署全局 Toast 与拦截器 Error Boundary 保护断网、401、500 等异常体验。
- [x] **国际化 (i18n)**: 完美支持中、英、日三语无缝切换。

### 2.3 小树养成系统 (Tree Garden System) - 已完成
- [x] **组件开发**: 开发基于 Canvas 的程序化生成“逻各斯之树”组件。
- [x] **多端集成**: 桌面端集成于 sidebar 导航，移动端集成于看板顶部，全端点击可触达。
- [x] **专属界面**: 开发 `TreeGarden.vue` 实现维度协调性定量分析与养成反馈体验。

### 2.4 深度报表与成就徽章 (Reports & Badges) - 已完成
- [x] **API 对齐**: 完成深度报表 API 及返回结构的 `api-schema.json` 定义。
- [x] **成就系统 (Badges)**: 建成“成就/徽章解锁系统”，在经验结算环节静默判定徽章奖励并于 Profile 页面炫耀展示（包含向死而生、厚积薄发等深度徽章）。
- [x] **深度画像周报 (Weekly Soul Profile)**: 在 Reports 视图完成 AI 报告一键动态提取和生成，融入 ECharts 绘制能力。

### 2.5 生产环境 (Java Spring Boot) - 待本地重置
    - **数据库 Schema**: 完成 `schema.sql` 补全，涵盖用户偏好、看板（支持 `longterm` 维度与 `checkpoints` 关联表）、统计、聊天记录等核心表。
    - **实体类与仓库**: 完成 `UserStats`, `UserPreferences`, `BoardCard` (含 `Checkpoint` 嵌入类), `ChatMessage` 等实体类及 Repository 开发。
    - **业务逻辑**: 实现了 `ChatService` (AI 上下文记忆)、`BoardController` (支持里程碑更新与 EXP 结算逻辑) 及 `LogController` (情绪持久化)。
- [x] **AI 集成**: 深度集成 DeepSeek API，实现智能任务识别与最宽容理解结算引擎。

## 3. 后续开发计划 (Roadmap) - 本地迁移与深化

### 第四阶段：生产环境联调 (Phase 4: Migration to Local) 
- [x] **Java 数据模型与业务逻辑**: 已完成核心开发（请参阅现有业务逻辑）。
- [x] **后端重构铺垫**: 已经在根目录生成规范的 `api-schema.json`（OpenAPI 3.0标准）。直接导入以自动生成 Java 实体与接口代码，实现前后端无缝对齐。
- [ ] **本地 VS Code / IDEA 联调**: 
    - 导出项目代码压缩包，并在本地构建和运行。
    - 结合导出的前端和 Schema 框架，用 Spring Boot 替换 `server.ts` 临时中间件，实现全量真实持久化数据。

### 第五阶段：深度优化 (Phase 5: Advanced Options)
- [ ] **数据源拓展 (Data Sources)**: 考虑接入用户健康数据 (如 Apple Health, Google Fit) 作为被动精力来源。
- [ ] **性能优化**: 引入 Redis 缓存常用配置，优化 AI 响应速度。

## 4. 技术规范 (Technical Standards)
- **代码规范**: 遵循日企严谨开发标准，要求注释详尽，命名统一。
- **安全规范**: 严格处理用户隐私数据，密码必须使用 BCrypt 加密，API 必须经过 JWT 鉴权。
- **文档规范**: 保持 README 与 API 文档同步更新。

---
*最后更新时间：2026-04-17*
