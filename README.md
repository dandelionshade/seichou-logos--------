# 🌟 Seichou-Logos (成長の軌跡 / 成长之理)

> **AI Quantification as the Core: A Life-RPG for Personal Growth.**
> 
> 「Seichou-Logos」是一款以 **AI 量化 (AI Quantification)** 为核心的人生养成系统。它与传统的“记录时间”类软件互补：时间记录软件关注你花了多少时间，而 Seichou-Logos 关注你**做了什么**以及这些经历**为你带来了多少成长价值**。
> 
> 这是一个由 AI 赋能的“人生养成游戏”与个人成长看板系统。其核心理念是将用户的日常生活、待办事项和遭遇的挫折，转化为多维度的“成长资产”。AI 作为极其宽容的“人生导师”，会在后台静默分析用户的记录，对负面情绪和失败给予最宽容的理解，并将其重构为“身体生理”、“精神心理”以及更多维度的量化数据。

![Vue.js](https://img.shields.io/badge/Vue%203-4FC08D?style=for-the-badge&logo=vue.js&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)
![DeepSeek API](https://img.shields.io/badge/AI-DeepSeek-blue?style=for-the-badge)

## ✨ 核心功能 (Core Features)

- 📋 **二维成长看板 (Growth Kanban)**: 
  - **横轴 (时间)**: 跨越短期到长期（每日 -> 每周 -> 每月），像待办事项一样记录生活。
  - **纵轴 (维度)**: 分为“身体生理 (Physical)”与“精神心理 (Mental)”两大部分。
- 🎮 **人生养成游戏化 (Life-RPG Gamification)**: 自定义标签记录事件，AI 后台静默计算经验值 (EXP) 和四大能量维度 (Vitality, Flow, Spark, Echo)，让用户通过“小树”系统直观监测生命状态。
- 🌳 **小树成长系统 (Tree of Logos)**: 一个视觉化的非数字化反馈系统，以程序化生成的一棵树为镜，实时反映用户的成长等级、维度平衡与精神状态。支持点击进入专属的“逻各斯花园 (Tree Garden)”养成界面，深度分析维度协调性与成长进展。
- 🧠 **AI 量化核心 (AI Quantification Core)**: 
  - **价值重构**: 不同于单纯的时间统计，AI 会分析事件背后的成长意义。
  - **极度宽容**: AI 遵循“最宽容理解”原则。即使是“今天什么都没做”，AI 也会将其量化为“有效的精力储备”。
- 💬 **智聊室 (Wisdom Chat Room)**: 
  - **AI 导师对话**: 像接入了 AI 的 Galgame 一样，你可以与你的 AI 导师进行深度对话。
  - **数据驱动**: AI 会基于你的看板数据和历史记录，为你提供个性化的建议和心理疏导。
- 🗺️ **行动轨迹 (Journey Log)**: 
  - **活跃度量化**: 通过热力图和趋势图展示行动的连续性与强度。
  - **时间轴回顾**: 结构化展示过往所有成长记录与 AI 洞察。
- 🌟 **成就与徽章系统 (Achievement System)**: 记录您的里程碑，解锁“向死而生”、“厚积薄发”等独特灵魂徽章，作为每次顿悟与抗争的图腾。
- 🎭 **灵魂画像 (Soul Profile) & 一键周报**: 
  - **周期性“灵魂画像”报告 (Weekly Soul Profile)**: 一键抓取历史片段，调用 AI 深度生成带情绪关键色、思维误区提醒的 Markdown 级精美周报。
  - **多维度平衡**: 通过雷达图展示生理、心理、社交、技能、韧性五个维度的平衡状态。
- 🌍 **国际化支持 (i18n)**: 完美支持英语 (English)、简体中文 (Chinese) 和日语 (Japanese) 的无缝切换。
- 🛡️ **优雅的前端护航保障 (Resilient UX)**: 
  - **动态骨架屏 (Skeleton Loading)**: 数据请求时告别白屏，提供精致的结构化占位动画。
  - **全局 Toast & Error Boundary**: 无缝拦截 API 错误（如 Token 过期、网络断开等），并以优雅悬浮窗（Toast）提示用户兜底。
- 🔒 **安全可靠**: 基于 Spring Security + JWT 的认证体系，使用 BCrypt 加密用户隐私数据。

## 🏗️ 架构说明 (Architecture)

本项目采用了**前后端分离**的架构，并已完成从 `localStorage` 到**真实 API 调用模式**的全面迁移：

1. **前端 (Frontend)**: `Vue 3` + `Vite` + `TypeScript` + `Tailwind CSS` + `Pinia` + `Vue Router`。
   - **API 驱动**: 所有 Store（Auth, Board, Log, Preference）均已重构，通过统一的 `apiFetch` 工具与后端通信。
   - **实时同步**: 用户偏好、看板任务、成长数据及聊天记录均实现云端持久化。
2. **生产后端 (Production Backend)**: 位于 `/backend` 目录，基于 `Java 21` + `Spring Boot 3` + `Spring Data JPA` + `PostgreSQL` 构建。
   - **Schema 补全**: 已完成包含用户统计、偏好、看板（支持长期目标与里程碑）、聊天记录及情绪分析在内的完整数据库设计。
3. **预览/开发代理 (Preview Proxy)**: 根目录下的 `server.ts` 是一个功能完备的 Node.js/Express 服务。
   - **Mock 数据库**: 在 AI Studio 预览环境中提供内存级数据持久化，模拟真实的 Java 后端行为，确保开发与演示的连贯性。
   - **AI 核心**: 集成了 DeepSeek API，处理情绪重构、多轮对话及游戏化结算逻辑。

## 🚀 本地开发指南 (Getting Started)

如果你想将本项目导出并在本地（如 VS Code / IntelliJ IDEA）继续开发，特别是使用 Java 重构后端，请参阅以下资料：

**📑 API 接口定义文档 (API Schema)**
我们在根目录生成了 `api-schema.json` 文件（符合 OpenAPI 3.0 规范）。这也是当前 Node.js `server.ts` 完整暴露的接口契约。
在你的 Java 项目中，可以直接导入此 JSON 并使用工具（如 Swagger Codegen 或 OpenAPI Generator）一键生成 Java DTO 和 Controller 接口代码。

### 1. 环境准备 (Prerequisites)
- [Node.js](https://nodejs.org/) (v20+)
- [Java JDK](https://adoptium.net/) (v21+)
- [Maven](https://maven.apache.org/) (v3.8+)
- [Docker](https://www.docker.com/) & Docker Compose

### 2. 环境变量配置 (Environment Variables)
在项目根目录创建 `.env` 文件，并填入你的 DeepSeek API Key：
```env
DEEPSEEK_API_KEY=your_api_key_here
```

#### 无 Key 降级模式说明 (No-Key Degraded Mode)
如果未配置 `DEEPSEEK_API_KEY`（或值为占位符），Node 预览后端仍可运行，但部分 AI 能力会进入降级模式：
- `POST /api/reframe`: 返回本地 fallback 的情绪分析结构（可用于前端联调）。
- `POST /api/chat`: 返回本地 fallback 的陪伴式回复，`action` 为 `null`。
- `POST /api/settle`: 返回 mock 结算结果。
- `POST /api/low-battery`: 返回 mock 安抚文案与经验值。
- `POST /api/reports/weekly`: 返回 mock Markdown 周报。

建议在联调/验收环境配置真实 Key，以获得完整 AI 质量与行为表现。

### 3. 启动数据库 (Start Database)
项目根目录提供了 `docker-compose.yml`，一键启动 PostgreSQL：
```bash
docker compose up -d
```

### 4. 启动后端服务 (Start Spring Boot Backend)
进入 `/backend` 目录，编译并运行 Java 项目：
```bash
cd backend
mvn clean install
mvn spring-boot:run
```
*后端服务将运行在 `http://localhost:8080`*

### 5. 启动本地开发服务 (Start Local Dev Server)
打开一个新的终端，在项目根目录安装依赖并启动 Node 预览后端（内置 Vite 中间件）：
```bash
npm install
npm run dev
```
*默认访问地址为 `http://localhost:3000`，如端口冲突可用 `PORT=3001 npm run dev` 覆盖端口。*

## ☁️ 推送到 GitHub 并在 PC + IntelliJ IDEA 继续开发（单开发者）

### 1. 推送前快速自检（本机）
```bash
git --no-pager status --short
npm run lint
npm run build
docker compose config
```

### 2. 首次绑定远程并推送（本机）
将 `<your-github-repo-url>` 替换为你的仓库地址：
```bash
git remote add origin <your-github-repo-url>
```

如果 `origin` 已存在，改用：
```bash
git remote set-url origin <your-github-repo-url>
```

然后执行：
```bash
git branch -M main
git add .
git commit -m "chore: pre-push stabilization and docs handoff"
git push -u origin main
```

### 3. 在 Windows PC + IntelliJ IDEA 接续开发
```bash
git clone <your-github-repo-url>
cd EnjoyLifeProject
npm install
npm run dev
```

复制环境变量模板可按你使用的终端选择：
```powershell
Copy-Item .env.example .env
```

```cmd
copy .env.example .env
```

建议在 IDEA 中打开项目后，先确认 Node/JDK 版本：Node `>=20`、Java `>=21`。`.env` 不会被提交；请在 PC 本地填入你自己的 `DEEPSEEK_API_KEY`。

## 🤝 参与贡献 (Contributing)

欢迎提交 Pull Request 或发布 Issue！
1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

## 📄 许可证 (License)

Distributed under the MIT License. See `LICENSE` for more information.
