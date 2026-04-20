# AGENTS.md

## Scope and runtime reality
- This repo is a Vue 3 + Vite frontend with two backend modes: a Node preview backend (`server.ts`) and a Spring Boot backend (`backend/`).
- Day-to-day local dev defaults to the Node path: `npm run dev` runs `tsx server.ts` on port `3000` (not Vite dev server).
- Frontend API calls are relative (`/api`) via `src/api/index.ts`, so whichever backend process owns `/api` defines behavior.
- OpenAPI contract lives in `api-schema.json` and matches the Node preview API shape (`http://localhost:3000/api`).

## Architecture map
- Frontend bootstrap: `src/main.ts` wires Pinia, Vue Router, i18n, and PWA registration.
- UI routing and auth guard: `src/router/index.ts` (`requiresAuth` + token-based redirect to `/auth`).
- Domain state is store-centric: `src/store/*Store.ts` perform API I/O and views mostly orchestrate store actions.
- Production backend uses layered Spring structure (`controller` -> `service` -> `repository` -> `entity`) under `backend/src/main/java/com/seichou/logos/`.
- Security boundary is JWT-based in `backend/src/main/java/com/seichou/logos/security/SecurityConfig.java` + `JwtAuthenticationFilter.java`.

## Critical end-to-end flows
- Auth flow: `useAuthStore.login()` -> `apiFetch('/auth/login')` -> Node `server.ts` `/api/auth/login` OR Spring `AuthenticationController` `/api/auth/login`.
- Board settlement flow: `useBoardStore.settleExp()` -> `POST /api/settle` -> Node AI settlement in `server.ts` or deterministic fallback in Spring `GameplayController`.
- Emotion reframing flow: `useLogStore.reframeEmotion()` -> `POST /api/reframe` -> DeepSeek call in Node `server.ts` or Spring `EmotionReframingService`.
- Chat mentor flow: `src/views/ChatRoom.vue` `sendMessage()` -> `POST /api/chat` -> Node `server.ts` `/api/chat` OR Spring `ChatController` `@PostMapping("/api/chat")`.
- Weekly report flow: `src/views/Reports.vue` `generateWeeklyReport()` -> `POST /api/reports/weekly` -> markdown returned, then `marked` + `DOMPurify` render.

## Project-specific conventions to follow
- Keep new frontend data logic in stores, not in view components; mirror existing `fetchX`/`updateX` action style.
- Use `apiFetch` for all HTTP calls so token injection + toast error handling stays centralized.
- Current exception to preserve unless refactoring end-to-end: `src/views/ChatRoom.vue` uses direct `fetch('/api/chat')` (public endpoint in both backends).
- Maintain response compatibility with both backends when changing contracts (Node preview is currently the canonical dev surface).
- Chat action payloads are not fully aligned; handle both `action.data.category` (Node contract) and `action.data.dimension` (current frontend consumption path).
- Use existing category/status vocabulary exactly (`health|mind|career|social`, `todo|done|struggled|composted`).
- Persist user/session UX state the same way existing stores do (`localStorage` keys in `authStore`, `chatStore`, `preferenceStore`).

## Developer workflows
- Node preview/fullstack-in-one:
  - `npm install`
  - `npm run dev`
- Frontend build/typecheck:
  - `npm run build`
  - `npm run lint`
- Spring backend (from `backend/`):
  - `mvn clean install`
  - `mvn spring-boot:run`
- Infra: `docker compose up -d` starts PostgreSQL and backend service.
- Important mismatch: `package.json` `clean` script uses `rm -rf dist` (POSIX style) and may fail on Windows shells.

## Integrations and config touchpoints
- DeepSeek key is required for non-mock AI behavior: `.env` (`DEEPSEEK_API_KEY`) and `backend/src/main/resources/application.yml` (`deepseek.api.key`).
- Node preview fallback behavior is endpoint-specific when DeepSeek key is missing: `/api/settle`, `/api/low-battery`, `/api/reports/weekly` return mock responses; `/api/reframe` and `/api/chat` now also return local fallback responses for frontend联调.
- DB defaults are PostgreSQL (`seichou_logos`) in `docker-compose.yml` and `backend/src/main/resources/application.yml`.
- API and DB schema are evolving; compare `api-schema.json`, Spring controllers, and `backend/sql/schema.sql` before changing DTO/entity fields.
- Swagger is available on Spring backend (`/swagger-ui.html`) for current Java endpoint verification.

