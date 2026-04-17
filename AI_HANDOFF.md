# 🤖 AI 接力与项目上下文文档 (AI Handoff & Context)

> **To the Next AI Assistant (Cursor / GitHub Copilot / Cursor / ChatGPT etc.):**
> Hello fellow AI! 当你读取到这份文档时，说明人类开发者正在依赖你进行本项目的全栈重构与后续开发。
> 目前项目（**Seichou-Logos / 成長の軌跡** —— 一款结合了 AI 情感计算的人生 RPG 任务集成分板）的**前端部分已达到生产级可用状态**。你们接下来的主要任务是：**将配套的后端从目前的 Node.js (Express) 内存级 Mock 环境，平滑重构至 Java Spring Boot + PostgreSQL 工业级持久化环境**。

---

## 🏗️ 架构现状与目标 (Architecture)

### 1. 前端 (Frontend) - **[已冻结并完善，非必要勿修改]**
- **核心栈**: Vue 3 (Composition API) + Vite + TypeScript.
- **状态与路由**: Pinia (多 Store 拆分管理) + Vue Router.
- **视觉与交互**: Tailwind CSS v4 + Lucide Icons，包含全局 Error Boundary (Toast) 容错拦截、Dynamic Skeletons (骨架屏)。
- **原生体验**: Vite PWA (支持离线与桌面安装)，Web Audio API (纯数学合成 UI 反馈音效，见 `src/utils/audio.ts`)。

### 2. 后端过渡目标 (Backend Migration Target)
- **当前状态**: 所有 API 请求（如 `/api/cards`、`/api/settle`）被代理到 `server.ts` 中，使用 Node.js + Express 搭配内存数组进行 Mock。
- **重构目标**: **Java (JDK 17+) + Spring Boot 3.x + Spring Data JPA + PostgreSQL**。
- **环境预热**: 根目录下已存在 `docker-compose.yml`，定义好了 Postgres 数据库与后端镜像编排，方便一键启动基础设施。

---

## 🔑 核心资产字典 (Crucial Assets for Migration)

请在生成 Spring Boot 代码时，**务必严格参考以下文件**，它们是全栈联调防错的绝对基石：

### 📜 1. 接口契约：`api-schema.json`
这是前后端通信的唯一真理（Source of Truth）。
- 里面定义了完整的 RESTful API 路径、请求体 (Request Body) 和响应结构 (Response)。
- **你的任务**：直接读取该 JSON 并利用你的能力，为开发者生成对应的 Java 代码（如 `CardController.java`、`SettleRequestDTO.java`、`SettleResponseDTO.java` 等）。只要字段与此 JSON 100% 对齐，前端就不需要修改哪怕一行代码。

### 🧠 2. AI 灵魂系统：`server.ts` (底部的 prompt 区域)
前端界面的游戏感和情绪共鸣，完全依赖于调用 DeepSeek API 时注入的优质 Prompt（提示词）。
- 在现有的 `server.ts` 里，我留下了 4 套经过反复实测调优的 Prompt 模板：
  1. `Settle Prompt` (日常结算，将行为转化为金币/经验，附带情绪疏导)
  2. `Low Battery Prompt` ("Compost" 低电量模式，提供温暖的安慰和低保经验)
  3. `Weekly Report Prompt` (每周灵魂画报，根据 TAG 和事件生成深度性格侧写)
  4. `Badge Unlock Logic` (徽章解锁判定，通过后端推断是否满足条件并返回)
- **你的任务**：在 Java 端重写服务时，请原封不动地将这些 Prompt Copy 到 Java 的常量类（如 `AiPromptConstants.java`）中，通过 Spring 的 `RestTemplate` 或 `WebClient` 调用 DeepSeek API。

---

## 🚀 后续开发路线图 (Future Roadmap)

建议你引导开发者按照以下顺序进行接下来的工作：

1. **DB 设计阶段**:
   读取 `api-schema.json` 中的 `BoardCard`、`Stats` 等结构，帮助开发者生成适用于 PostgreSQL 的 DDL (SQL 建表语句) 和 JPA Entity 实体类。
2. **鉴权系统 (Auth)**:
   目前的 `JWT` 是 `server.ts` 中写死的假 Token。需要在 Spring Boot 中引入 Spring Security，并实现真正的 JWT 颁发和验证。
3. **接口一对一迁移**:
   使用 `RestController` 一一实现 `api-schema.json` 里的接口，并注意处理跨域 (CORS) 以便前端 Vite (运行在 3000 或 5173 端口) 能够免障调用 (通常是 8080 端口)。
4. **外部数据扩展 (可选项)**:
   在此前规划中，我们准备接入 HealthKit / Google Fit API 读取步数与心率自动转化为 Vitality 经验值。这是前端页面已预留未来卡槽的领域。

---

**最终结语：**
这份前端代码倾注了细腻的情感设计，包含了流式打字效果、极美的暗色系 UI 以及细致的音效反馈。希望你能协助人类开发者顺利完成后端重铸，在这个数字生命记录仪中，让每一个微小的习惯都能开出花来！ ✨
