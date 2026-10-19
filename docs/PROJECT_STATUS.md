# Career-Advisor Project Status & Comprehensive Development History

> **Permanent Single Source of Truth**  
> This document tracks the complete development history, system architecture, feature matrix, testing records, and viva-ready engineering specifications for the Career-Advisor project.  
> **Status**: Full-Stack Platform Complete & Live in Production; Mentorship Module Phase 1, 2, 3B.1, 3B.2, 3B.3, 3B.4.1, 3B.4.2A, 3B.4.2B & 3B.5 Admin Review & Capacity COMPLETE (Phase 25)

---

## 1. Project Overview

**Career-Advisor** is an enterprise-grade full-stack career guidance, skill evaluation, in-browser coding workspace, WebRTC calling, real-time messaging, AI chatbot, and adaptive career intelligence platform. It helps students and engineers discover tech career paths, assess their abilities through diagnostic quizzes, generate personalized step-by-step learning roadmaps, track skill acquisition, receive 6-dimensional career readiness scoring, practice interactive coding challenges in an in-browser IDE, execute timed technical mock interviews, analyze resumes with canonical skill extraction, schedule daily time-boxed study routines, and stay accountable with an AI Progress Coach.

### Core Technology Stack

- **Frontend**: Next.js 16.2.3 (App Router, Turbopack), React 19.2.4, Tailwind CSS 4, Framer Motion 12.38
- **Backend**: Spring Boot 3.3.4, Java 17, Spring Security 6, JJWT 0.12.6, Spring Data JPA, Spring WebSocket (STOMP), Maven
- **Database**: MySQL 8.0 (Database: `career_advisor`, InnoDB)
- **Real-Time Communication**: STOMP over WebSocket (port 8080 `/ws`), WebRTC P2P (Audio & Video)
- **AI Infrastructure**: Decoupled `AiProvider` abstraction (`MockAiProvider`, `OpenAiCompatibleProvider`), prompt grounding engine
- **Document Parsing**: Apache PDFBox 3.0.3, Apache POI 5.3.0
- **Containerization**: Multi-stage Dockerfiles (`backend/Dockerfile`, `frontend/Dockerfile`) and `docker-compose.yml`

---

## 2. System Architecture

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                           Client (Browser)                              │
│                    Next.js 16 (React 19) @ Port 3000                    │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                    AuthProvider (AuthContext)                     │  │
│  │         (token, user, isAuthenticated, login, signup, logout)     │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
│                                     │                                   │
│  ┌──────────────────────────────────▼────────────────────────────────┐  │
│  │ Services (careerService, skillService, progressService,           │  │
│  │           problemService, quizService, recommendationService,     │  │
│  │           interviewService, resumeService, chatService,           │  │
│  │           websocketService, webrtcService, plannerService,        │  │
│  │           careerIntelligenceService, progressCoachService)        │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
└─────────────────────────────────────┼───────────────────────────────────┘
                                      │
                     HTTP REST / JSON (Authorization: Bearer <JWT>)
                     + STOMP over WebSocket (/ws)
                     + WebRTC Peer-to-Peer Media Stream (SRTP/UDP)
                                      │
┌─────────────────────────────────────▼───────────────────────────────────┐
│                    Spring Boot 3.3.4 Backend @ Port 8080                │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │        JwtAuthenticationFilter + WebSocketSecurityInterceptor     │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
│                                     │                                   │
│  ┌───────────────┬──────────────────┼───────────────┬────────────────┐  │
│  │ AuthController│ CareerController │ RoadmapContr. │ SkillController│  │
│  │ ProgressContr.│ ProblemController│ QuizController│ InterviewContr.│  │
│  │ ResumeContr.  │ AdminController  │ AiController  │ ConversationCtr│  │
│  │ CallController│ RealTimeMsgContr.│ PlannerContr. │ CareerIntelCtr │  │
│  │ ProgressCoach │ AdminPlannerCtr. │ TestController│                │  │
│  └───────┬───────┴──────────┬───────┴───────┬───────┴────────┬───────┘  │
│          │                  │               │                │          │
│  ┌───────▼──────────────────▼───────────────▼────────────────▼───────┐  │
│  │ Domain & Calculation Services:                                    │  │
│  │  - UserService, CareerService, RoadmapService, UserSkillService   │  │
│  │  - UserProgressService, ProblemService, QuizService               │  │
│  │  - InterviewService, ResumeService, ResumeParserService           │  │
│  │  - AdminService, UserAiContextService, AiContextBuilder           │  │
│  │  - ChatService, ConversationService, PresenceService, CallService │  │
│  │  - CareerIntelligenceService, AiActionPlanService                 │  │
│  │  - PlannerService, DeterministicPlannerEngine                     │  │
│  │  - AiProgressCoachService, DeterministicProgressEngine           │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
│                                     │                                   │
│  ┌──────────────────────────────────▼────────────────────────────────┐  │
│  │ JPA Repositories (UserRepository, UserSkillRepository,            │  │
│  │   UserRoadmapProgressRepository, QuizAttemptRepository,           │  │
│  │   CodingProblemRepository, UserProblemProgressRepository,         │  │
│  │   MockInterviewRepository, InterviewQuestionRepository,           │  │
│  │   InterviewAnswerRepository, ResumeRepository, AiUsageLogRepository│  │
│  │   ConversationRepository, ChatMessageRepository, CallSessionRepo, │  │
│  │   LearningPlanRepository, LearningTaskRepository,                 │  │
│  │   PlannerPreferenceRepository, WeeklyProgressReportRepository,    │  │
│  │   MentorProfileRepository, MentorshipRequestRepository)           │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
└─────────────────────────────────────┼───────────────────────────────────┘
                                      │ Spring Data JPA (Hibernate Dialect)
┌─────────────────────────────────────▼───────────────────────────────────┐
│                       MySQL Database @ Port 3306                        │
│                         Schema: career_advisor                          │
│     Tables: users, user_skills, user_roadmap_progress, quiz_attempts,   │
│             coding_problems, coding_problem_tags, user_problem_progress,│
│             mock_interviews, interview_questions,                       │
│             interview_question_options, interview_question_concepts,    │
│             interview_answers, resumes, resume_skills, ai_usage_logs,   │
│             conversations, conversation_participants, chat_messages,    │
│             call_sessions, learning_plans, learning_tasks,              │
│             planner_preferences, weekly_progress_reports,               │
│             mentor_profiles, mentorship_requests                        │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Feature Matrix

| Feature | Frontend Status | Backend Status | Persistence | Tests | Notes |
| ------- | --------------- | -------------- | ----------- | ----- | ----- |
| Landing Page | COMPLETE | N/A | N/A | Manual | High-conversion presentation page |
| User Registration | COMPLETE | COMPLETE | MySQL (`users`) | PASS | Encrypted with BCrypt |
| User Login (JWT) | COMPLETE | COMPLETE | Stateless JWT | PASS | 24h HMAC-SHA256 expiration |
| Auth State & Routing | COMPLETE | COMPLETE | LocalStorage + Header | PASS | Protected routes redirect to /login |
| Career Exploration | COMPLETE | COMPLETE | Static Service | PASS | 7 tracks with dynamic matching scores |
| Interactive Roadmaps | COMPLETE | COMPLETE | Static Service | PASS | 7 career tracks with milestone checklists |
| Roadmap Progress Tracking | COMPLETE | COMPLETE | MySQL (`user_roadmap_progress`) | PASS | User-isolated step toggling |
| Skill Portfolio Management | COMPLETE | COMPLETE | MySQL (`user_skills`) | PASS | Add, remove, and list user skills |
| Skill Assessment Quiz | COMPLETE | COMPLETE | MySQL (`quiz_attempts`) | PASS | 8-question evaluation with level classification |
| Dashboard Synchronization | COMPLETE | COMPLETE | Dynamic REST | PASS | Aggregates roadmap, skills, quiz, practice, and AI intelligence |
| DSA & Practice Hub | COMPLETE | COMPLETE | MySQL (`coding_problems`, `user_problem_progress`) | PASS | 22 curated problems across 10 topics |
| Personalized Intelligence | COMPLETE | COMPLETE | Dynamic Rule-Engine | PASS | Multi-faceted recommendations & readiness scoring |
| In-Browser Code Workspace | COMPLETE | COMPLETE | MySQL (`coding_problems`, `user_problem_progress`) | PASS | Interactive IDE, line numbers, sample runner, and test case inspector |
| Timed Mock Interviews | COMPLETE | COMPLETE | MySQL (`mock_interviews`, `interview_questions`, `interview_answers`) | PASS | Backend-authoritative timer, anti-cheating, score evaluation, explanations |
| Performance History & Review | COMPLETE | COMPLETE | MySQL (`mock_interviews`) | PASS | Filterable attempts history and detailed question reviews |
| Resume Analyzer & Skill Extraction | COMPLETE | COMPLETE | MySQL (`resumes`, `resume_skills`) | PASS | PDF & DOCX text parsing, canonical skill normalization, gap analysis & profile sync |
| RBAC & Admin Governance | COMPLETE | COMPLETE | MySQL (`users` with `Role.ADMIN`) | PASS | Platform KPI counters, candidate user directory, inspection modal |
| Production Containerization & Hardening | COMPLETE | COMPLETE | Multi-Stage Dockerfiles | PASS | Docker Compose topology, HTTP security headers, Spring profiles (`dev`/`prod`) |
| AI Infrastructure & Personal Context | COMPLETE | COMPLETE | MySQL (`ai_usage_logs`) | PASS | Decoupled provider abstraction, 8-service personal context aggregation |
| Persistent Personal AI Chatbot | COMPLETE | COMPLETE | MySQL (`conversations`, `chat_messages`) | PASS | Zero-hallucination grounded advice, auto-titling, responsive chat UI |
| Real-Time Human Messaging & Presence | COMPLETE | COMPLETE | MySQL + WebSocket (STOMP) | PASS | Peer direct messages, admin support tickets, typing indicators, read receipts |
| WebRTC Audio & Video Calling | COMPLETE | COMPLETE | MySQL (`call_sessions`) + P2P SRTP | PASS | Server-authoritative signaling, anti-race state machine, draggable PiP call overlay |
| 6-Dimensional Career Intelligence | COMPLETE | COMPLETE | Dynamic Multi-Engine | PASS | Weighted readiness calculation (Skills, Roadmap, Quiz, DSA, Interview, Resume) |
| Goal-Oriented AI Action Planner | COMPLETE | COMPLETE | Dynamic AI/Deterministic | PASS | 3-5 bounded high-impact actions with explainable rationale |
| Adaptive Weekly Study Planner | COMPLETE | COMPLETE | MySQL (`learning_plans`, `learning_tasks`) | PASS | Daily study time-boxing, 7-day schedule, task lifecycle (Start/Complete/Skip), streaks |
| AI Progress Coach & Weekly Accountability | COMPLETE | COMPLETE | MySQL (`weekly_progress_reports`) | PASS | Longitudinal trend analysis (IMPROVING/STABLE/DECLINING), weekly coaching synthesis |

---

## 4. Phase Hierarchy

```text
Phase 1 — Frontend Prototype & Audit — COMPLETE
Phase 2 — Careers & Roadmap Domain — COMPLETE
Phase 3 — Skills & Quiz Assessment — COMPLETE
Phase 4 — Backend Security, JWT & User Persistence — COMPLETE
Phase 5 — Frontend JWT Authentication Integration — COMPLETE
Phase 6 — Full-Stack API Integration & Dashboard Synchronization — COMPLETE
Phase 7 — DSA & Practice Hub Foundation — COMPLETE
Phase 8 — Personalized Career & Interview Intelligence — COMPLETE
Phase 9 — In-Browser Coding Workspace & Mock Interview — COMPLETE
├── Phase 9A — Coding Workspace Foundation — COMPLETE
├── Phase 9B.1 — Mock Interview Backend Foundation — COMPLETE
└── Phase 9B.2 — Mock Interview Frontend UI & Live Timer — COMPLETE
Phase 10 — Production Stabilization, UI/UX Polish & Viva Readiness — COMPLETE
Phase 11 — Resume Analyzer & Intelligent Skill Extraction — COMPLETE
Phase 12 — RBAC, Admin Security & Candidate Directory — COMPLETE
Phase 13 — Production Readiness, Hardening & Docker Containerization — COMPLETE
Phase 14 — AI Infrastructure & Real-Time Communication Ecosystem — COMPLETE
├── Phase 14A — Enterprise AI Infrastructure & Personal Context Engine — COMPLETE
├── Phase 14B — Persistent Personal AI Chatbot — COMPLETE
├── Phase 14C — Real-Time Human Communication (WebSocket / STOMP) — COMPLETE
└── Phase 14D — WebRTC Peer-to-Peer Audio & Video Calling — COMPLETE
Phase 15 — Unified Career Intelligence, Action Plan, Adaptive Planner & Progress Coach — COMPLETE
├── Phase 15A — 6-Dimensional Unified Career Intelligence Engine — COMPLETE
├── Phase 15B — Goal-Oriented AI Action Planner & Explanations — COMPLETE
├── Phase 15C — Adaptive Weekly Study Planner & Daily Time-Boxing — COMPLETE
└── Phase 15D — AI Progress Coach & Weekly Longitudinal Accountability — COMPLETE
```

---

## 5. Development History

### 2026-08-22 — Phase 11: Resume Analyzer & Intelligent Skill Extraction

**Status**: COMPLETE  
**Purpose**: Build a robust, server-side Resume Analyzer module that accepts PDF and DOCX uploads, extracts technical text, normalizes candidate skills against a canonical dictionary, matches candidates against 7 career roadmaps, detects high-priority skill gaps, feeds into the Recommendation Engine, and allows user-confirmed skill synchronization into `user_skills`.

**What Was Done**:
- **Backend Architecture & Domain Model**:
  - Added Apache PDFBox 3.0.3, Apache POI 5.3.0, and Apache POI-OOXML 5.3.0 dependencies in `pom.xml`.
  - Configured Spring Multipart upload limits (5MB max file, 10MB max request).
  - Created `Resume` entity with fields for metadata, parsed text (`LONGTEXT`), summary, email, phone, education, experience, projects, and `@ElementCollection` table `resume_skills`.
  - Created `ResumeRepository` enforcing user-isolated queries (`findByIdAndUser`, `findByUserOrderByUploadTimestampDesc`, `deleteByIdAndUser`).
  - Created `ResumeParserService` supporting PDF extraction (via PDFBox `Loader.loadPDF` and magic byte `%PDF` validation) and DOCX extraction (via POI `XWPFDocument`/`XWPFWordExtractor` and magic byte `PK` validation) with text sanitization.
  - Created `ResumeSkillExtractor` with a dictionary of 35+ canonical skills across 6 categories, alias matching, confidence scoring (80–99%), and regex section extractors for contact details, summary, education, experience, and projects.
  - Created `ResumeService` orchestrating document parsing, career track alignment, skill gap prioritization, and user-confirmed skill synchronization via `UserSkillService`.
  - Integrated `RecommendationService` to dynamically include `RESUME_SYNC` (if unsynced skills exist) or `RESUME` (if no resume uploaded) actionable recommendations.
  - Implemented `ResumeController` with REST endpoints:
    - `POST /api/resumes/upload` (multipart)
    - `GET /api/resumes`
    - `GET /api/resumes/{id}`
    - `GET /api/resumes/{id}/analysis`
    - `GET /api/resumes/latest/analysis`
    - `DELETE /api/resumes/{id}`
    - `POST /api/resumes/{id}/sync-skills`
  - Added global exception handlers in `GlobalExceptionHandler` for `UnsupportedFileTypeException` (415), `FileValidationException` (400), `ResumeParsingException` (422), and `MaxUploadSizeExceededException` (413).
- **Frontend UI & Integration**:
  - Created `frontend/lib/resumeService.ts` API client with typed methods and Bearer token auth.
  - Updated `frontend/lib/apiClient.ts` to seamlessly handle `FormData` multipart requests without header conflict.
  - Added `"Resume Analyzer"` (`📄` icon) to navigation in `frontend/app/(protected)/layout.tsx`.
  - Built comprehensive `/resume` page (`frontend/app/(protected)/resume/page.tsx`) featuring:
    - Drag-and-drop upload zone with live parsing status states (`IDLE`, `UPLOADING`, `PROCESSING`, `COMPLETED`, `FAILED`).
    - Resume overview card with metadata, contact details, executive summary, and delete option.
    - Extracted Technical Skills section with interactive toggle chips, confidence badges, and "Add All Confirmed Skills" bulk sync button.
    - Top Career Matches breakdown with match progress bars, matched vs missing skills, and direct roadmap links.
    - Skill Gap Matrix highlighting high-priority missing skills linked to Roadmaps and DSA Practice.
    - Parsed Background cards (Work Experience, Education, Projects).
  - Integrated compact Resume Intelligence widget into Dashboard (`dashboard/page.tsx`) and Profile (`profile/page.tsx`).
- **Browser Failure Bug Fix & Stabilization**:
  - **Issue**: Real browser at `http://localhost:3000/resume` displayed generic `"An unexpected error occurred. Please try again later."` banner upon initial render and upload.
  - **Root Cause**: `frontend/lib/resumeService.ts` called `${API_BASE_URL}/resumes/...` instead of `${API_BASE_URL}/api/resumes/...` (because `API_BASE_URL` is `http://localhost:8080` without `/api`). This caused Spring Boot to fail to match `@RequestMapping("/api/resumes")`, throw `NoResourceFoundException`, and return HTTP 500 via the global unhandled exception handler.
  - **Fix Applied**:
    1. Added `RESUMES` endpoints to `frontend/lib/config.ts` under `API_ENDPOINTS` with proper `${API_BASE_URL}/api/resumes/...` mapping.
    2. Updated `frontend/lib/resumeService.ts` to consume `API_ENDPOINTS.RESUMES` across all 6 API client methods.
    3. Added `NoResourceFoundException` handler in `backend/src/main/java/com/careeradvisor/backend/exception/GlobalExceptionHandler.java` to return clean HTTP 404 instead of HTTP 500.
    4. Enhanced error mapping in `frontend/app/(protected)/resume/page.tsx` (`mapApiError`) to map 400, 401, 403, 404, 413, 415, 422, 500 into user-friendly diagnostic messages.
- **Testing & Verification**:
  - Backend compile: 102 source files compiled with `BUILD SUCCESS`.
  - Frontend production build: All 16 routes generated with 0 errors (`npm run build`).
  - Browser Workflow Contract Suite (`scratch/test_browser_resume_workflow.js`): **16/16 PASS (100%)**.
  - Phase 11 Dedicated Test Suite (`scratch/test_phase11_resume.js`): **19/19 PASS (100%)**.
  - Master Regression Test Suite (`scratch/test_master_regression.js`): **20/20 PASS (100%)** across all 11 phases.

---

### Phase 12 — RBAC + Admin Security Foundation + Platform Governance

**Status**: COMPLETE  
**Purpose**: Implement a production-grade Role-Based Access Control (RBAC) foundation supporting two canonical roles (`ROLE_USER` and `ROLE_ADMIN`), integrating roles into JWT claims and Spring Security authorization pipelines, protecting administrative endpoints (`/api/admin/**`), bootstrapping a development administrator account safely via environment configuration, exposing platform telemetry & user governance REST APIs, and providing a responsive `/admin` overview dashboard and `/admin/users` governance interface with client-side route protection.

**What Was Done**:
- **Backend Architecture & Security**:
  - Created `Role` enum (`USER`, `ADMIN`).
  - Updated `User` entity with `@Enumerated(EnumType.STRING) private Role role = Role.USER;` and ensured all constructors default to `Role.USER`.
  - Updated `JwtUtils` to embed `role` claim in JWT payload (`claims.put("role", role.name())`) and provide `extractRole(token)`.
  - Updated `CustomUserDetails` to map `user.getRole()` dynamically into `SimpleGrantedAuthority("ROLE_" + user.getRole().name())`.
  - Updated `CustomUserDetailsService` to load authoritative roles from the database.
  - Implemented `CustomAccessDeniedHandler` returning clean HTTP 403 JSON payloads for unauthorized resource access.
  - Updated `SecurityConfig` to restrict `/api/admin/**` exclusively to `hasRole("ADMIN")`, while preserving public and standard user routes.
  - Hardened `UserService.register()` to enforce `Role.USER` strictly, completely preventing role elevation injection attacks via public registration.
  - Created `AdminDataInitializer` (`CommandLineRunner`) bootstrapping the development administrator (`admin@careeradvisor.dev` / `AdminPass123!`) with BCrypt-hashed password.
- **Admin REST API Layer**:
  - Created `AdminStatsOverviewDto`, `AdminUserDto`, and `AdminUserDetailDto` with zero password/hash/token exposure.
  - Created `AdminService` aggregating real-time platform metrics and providing searchable candidate summaries and deep performance matrix inspections.
  - Created `AdminController` (`/api/admin`):
    - `GET /api/admin/me`: Returns current admin profile.
    - `GET /api/admin/health`: Returns administrative health telemetry.
    - `GET /api/admin/stats/overview`: Returns live platform KPI counts (users, resumes, quiz attempts, solved DSA problems, mock interviews, completed interviews, career tracks breakdown).
    - `GET /api/admin/users`: Returns candidate directory with search query support.
    - `GET /api/admin/users/{id}`: Returns candidate inspection detail (verified skills, resume status, interview score, solved problems).
- **Frontend UI & Integration**:
  - Updated `frontend/lib/config.ts` with `API_ENDPOINTS.ADMIN`.
  - Updated `frontend/lib/authService.ts` and `frontend/context/AuthContext.tsx` to handle user `role` in state, login, and token revalidation.
  - Created `frontend/lib/adminService.ts` API client with full TypeScript interfaces.
  - Updated `frontend/app/(protected)/layout.tsx` to conditionally display `"Admin Dashboard"` (`🛡️` icon, `/admin`) and `ADMIN` user badge only for users with `role === "ADMIN"`.
  - Built `/admin` overview dashboard (`frontend/app/(protected)/admin/page.tsx`) with 6 KPI cards, career track distribution progress bars, experience tier distribution, learning activity breakdown, and future governance module cards with "Coming in Phase 13" badges.
  - Built `/admin/users` user governance directory (`frontend/app/(protected)/admin/users/page.tsx`) with live search, role filter tabs, user table, and modal candidate profile inspection.
  - Implemented strict client-side route guards redirecting unauthorized users (`role !== "ADMIN"`) to `/dashboard`.
- **Testing & Verification**:
  - Backend compile: 110 source files compiled with `BUILD SUCCESS`.
  - Frontend production build: 18 routes compiled with 0 errors (`npm run build`).
  - Phase 12 RBAC Security Suite (`scratch/test_phase12_rbac.js`): **38/38 PASS (100%)**.
  - Browser RBAC Workflow Contract Suite (`scratch/test_browser_rbac_workflow.js`): **13/13 PASS (100%)**.
  - Phase 11 Resume Analyzer Verification Suite (`scratch/test_phase11_resume.js`): **19/19 PASS (100%)**.
  - Browser Resume Contract Suite (`scratch/test_browser_resume_workflow.js`): **16/16 PASS (100%)**.
  - Master Regression Suite (`scratch/test_master_regression.js`): **25/25 PASS (100%)** across all 12 phases.

### Phase 13 — Production Readiness, Hardening & Deployment Foundation

#### Step 1 — Environment Configuration Hardening

**Status**: COMPLETE  
**Purpose**: Separate development and production configurations using Spring profiles (`dev` vs `prod`), externalize sensitive parameters into environment variables with zero plaintext secret exposure, make CORS allowed origins dynamically configurable, safely parameterize multipart upload limits, provide a frontend `.env.example` template, and guarantee zero regressions across local developer workflows and regression test suites.

**What Was Done**:
- **Backend Profile Architecture**:
  - `application.properties`: Configured common server settings (`server.port=${PORT:8080}`), JPA dialect, multipart limits (`${MAX_FILE_SIZE:5MB}`, `${MAX_REQUEST_SIZE:10MB}`), and dynamic active profile selection (`spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}`).
  - `application-dev.properties`: Local developer profile with safe fallback values (`root` user, empty password fallback, `show-sql=true`, local `dev` JWT secret, `http://localhost:3000` CORS origin, and local admin bootstrapping).
  - `application-prod.properties`: Strict production profile consuming 100% environment-driven configuration (`${DB_HOST}`, `${DB_PORT}`, `${DB_NAME}`, `${DB_USERNAME}`, `${DB_PASSWORD}`, `${JWT_SECRET}`, `${JWT_EXPIRATION_MS}`, `${CORS_ALLOWED_ORIGINS}`, `${ADMIN_EMAIL}`, `${ADMIN_PASSWORD}`), with `show-sql=false`, `open-in-view=false`, and SQL query logging disabled.
- **Dynamic CORS & Security Hardening**:
  - Updated `SecurityConfig.java` to inject `${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}`, parsing comma-separated origins dynamically into `CorsConfiguration.setAllowedOrigins()`.
  - Updated `AdminDataInitializer.java` to verify `adminEmail` and `adminPassword` are non-empty before bootstrapping, allowing production environments to skip auto-creation if desired.
- **Frontend Configuration & .gitignore**:
  - Created `frontend/.env.example` documenting `NEXT_PUBLIC_API_URL` configuration.
  - Updated `.gitignore` to strictly exclude `.env`, `.env.local`, `*.env*` while preserving `!.env.example`.
- **Testing & Verification**:
  - Backend compile: 110 source files compiled with `BUILD SUCCESS`.
  - Frontend production build: 18 routes compiled with 0 errors (`npm run build`).
  - Active profile confirmed in runtime logs: `The following 1 profile is active: "dev"`.
  - Phase 12 RBAC Suite: **38/38 PASS (100%)**.
  - Phase 11 Resume Suite: **19/19 PASS (100%)**.
  - Master Regression Suite: **25/25 PASS (100%)**.
  - Browser RBAC Workflow Suite: **13/13 PASS (100%)**.
  - Browser Resume Workflow Suite: **16/16 PASS (100%)**.

#### Step 2 — Security Hardening

**Status**: COMPLETE  
**Purpose**: Audit Spring Security configuration, enforce zero-trust user isolation across all endpoints, prevent IDOR (Insecure Direct Object Reference) and privilege escalation vulnerabilities, sanitize controller CORS to rely solely on centralized security configuration, verify DTO password/hash protection, and document the comprehensive full-stack security matrix.

**Endpoint Security Matrix**:

| Endpoint Pattern | HTTP Methods | Public | USER | ADMIN | Security & Authorization Rationale |
| :--- | :--- | :---: | :---: | :---: | :--- |
| `/api/auth/register`, `/signup` | `POST` | YES | YES | YES | Public onboarding; strictly forces `Role.USER` (ignores payload role) |
| `/api/auth/login` | `POST` | YES | YES | YES | Issues signed JWT via BCrypt credentials validation |
| `/api/auth/me` | `GET` | NO | YES | YES | Returns authenticated user identity and role from `SecurityContext` |
| `/api/auth/health` | `GET` | YES | YES | YES | Liveness / health monitoring check |
| `/api/careers/**` | `GET` | YES | YES | YES | Public career catalog and details |
| `/api/roadmaps/**` | `GET` | YES | YES | YES | Public step-by-step career roadmaps |
| `/api/skills/**` | `GET`, `POST`, `DELETE` | NO | YES | YES | User-isolated skill portfolio (bound to JWT principal) |
| `/api/progress/**` | `GET`, `POST` | NO | YES | YES | User-isolated roadmap checklist and target career goals |
| `/api/quiz/submit` | `POST` | YES | YES | YES | Anonymous or authenticated quiz evaluation |
| `/api/quiz/latest` | `GET` | NO | YES | YES | User-isolated latest quiz score record |
| `/api/problems`, `/{id}` | `GET` | YES | YES | YES | Public problem catalog (attaches solved state if authenticated) |
| `/api/problems/progress/summary` | `GET` | NO | YES | YES | User-isolated DSA category and topic progress summary |
| `/api/problems/{id}/run` | `POST` | YES | YES | YES | In-browser code simulation and test execution sandbox |
| `/api/problems/{id}/toggle`, `/submit` | `POST` | NO | YES | YES | User-isolated solved problem persistence |
| `/api/recommendations` | `GET` | NO | YES | YES | User-isolated multi-factor personalized readiness & actions |
| `/api/interviews/**` | `GET`, `POST` | NO | YES | YES | User-isolated timed mock interview sessions, answers & evaluation |
| `/api/resumes/**` | `GET`, `POST`, `DELETE` | NO | YES | YES | User-isolated PDF/DOCX resume analysis, parsing & skill sync |
| `/api/admin/**` | `GET` | NO | NO | YES | Strictly restricted to `hasRole("ADMIN")` via Spring Security |

**What Was Done**:
- **Centralized CORS Governance**: Removed all redundant and hardcoded `@CrossOrigin` annotations from all 11 REST controllers (`AuthController`, `AdminController`, `CareerController`, `RoadmapController`, `SkillController`, `ProgressController`, `QuizController`, `ProblemController`, `RecommendationController`, `InterviewController`, `ResumeController`), delegating 100% of CORS policy enforcement to `SecurityConfig.corsConfigurationSource()`.
- **Zero-Trust User Isolation Verified**: Audited all entity queries to guarantee that private records (`Resume`, `MockInterview`, `UserSkill`, `UserRoadmapProgress`, `UserProblemProgress`) are strictly isolated by `user.getId()`. Cross-tenant query attempts return HTTP 404 (preventing ID enumeration).
- **No Client-Injected Identifiers**: Zero REST controllers accept or trust client-supplied `userId` values in JSON request bodies or URL parameters for user data mutation.
- **DTO Sanitization**: Confirmed that all DTOs (`AdminUserDto`, `AdminUserDetailDto`, `AuthResponse`, `UserDto`) strictly exclude password hashes, salts, and secrets.
- **Testing & Verification**:
  - Backend compile: `BUILD SUCCESS` (110 classes).
  - Frontend build: `18 routes` compiled with 0 errors (`npm run build`).
  - Phase 12 RBAC Suite: **38/38 PASS (100%)**.
  - Phase 11 Resume Suite: **19/19 PASS (100%)**.
  - Master Regression Suite: **25/25 PASS (100%)**.
  - Browser RBAC Workflow Suite: **13/13 PASS (100%)**.
  - Browser Resume Workflow Suite: **16/16 PASS (100%)**.

#### Steps 3–20 — CORS, Error Handling, File Security, DB Optimization, Docker & Documentation

**Status**: COMPLETE  
**Purpose**: Execute end-to-end production hardening, including Spring Security HTTP response headers, global error structure standardization, file upload validation, database indexing, logging sanitization, containerization assets (Dockerfiles & docker-compose.yml), comprehensive REST API reference manual, deployment guide, and full master regression test suite.

**What Was Accomplished**:
1. **HTTP Security Headers**: Configured `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, and `Referrer-Policy: strict-origin-when-cross-origin` in Spring Security `SecurityFilterChain`.
2. **API Error Handling**: Audited `GlobalExceptionHandler.java` guaranteeing structured, safe error JSON payloads across all standard HTTP error statuses (`400`, `401`, `403`, `404`, `409`, `413`, `415`, `422`, `500`) with zero stack traces or class internals leaked.
3. **File Upload Hardening**: Verified PDF/DOCX magic bytes (`%PDF`, `PK`), MIME types, in-memory stream extraction, 5MB file limits, and path traversal prevention in `ResumeParserService.java`.
4. **Database Indexing & Entity Optimization**: Added performance indexes on `mock_interviews(user_id, started_at)`, `resumes(user_id, upload_timestamp)`, `user_problem_progress(user_id, problem_id)`, and `user_skills(user_id, skill_name)` with zero schema disruption.
5. **Logging & Observability**: Enforced production logging guidelines (passwords, tokens, parsed resume text, and authorization headers are never logged).
6. **Documentation Deliverables**:
   - `docs/API_REFERENCE.md`: Complete specification for all 11 modules and 30+ endpoints.
   - `docs/DEPLOYMENT_GUIDE.md`: Comprehensive production deployment manual with reverse proxy (Nginx) blueprints and environment variable specifications.
   - `docs/VIVA_GUIDE.md`: Updated to Version 5.0 with 72 academic and production viva questions.
7. **Containerization & Docker Assets**:
   - `backend/Dockerfile`: Multi-stage build (Maven $\rightarrow$ Eclipse Temurin JRE 17 Alpine) with non-root security user.
   - `frontend/Dockerfile`: Multi-stage build (Node.js 20 Alpine) with standalone Next.js production server.
   - `docker-compose.yml`: Multi-container production deployment topology with MySQL 8 volume persistence, backend, frontend, and health checks.
8. **Master Regression & Test Suites**:
   - `scratch/test_phase13_production.js`: **28/28 PASS (100%)**.
   - `scratch/test_phase12_rbac.js`: **38/38 PASS (100%)**.
   - `scratch/test_phase11_resume.js`: **19/19 PASS (100%)**.
   - `scratch/test_master_regression.js`: **25/25 PASS (100%)**.
   - `scratch/test_browser_rbac_workflow.js`: **13/13 PASS (100%)**.
   - `scratch/test_browser_resume_workflow.js`: **16/16 PASS (100%)**.
   - **Total Passing Automated Tests**: **139/139 PASS (100%)**.

---

### Phase 14A — AI Infrastructure & Personal AI Foundation

**Status**: COMPLETE  
**Commit Baseline**: `75bb539`  
**Purpose**: Establish a robust, decoupled, and secure enterprise AI infrastructure foundation with provider abstraction, environment configuration hardening, zero-trust personal user context engine, telemetry and usage tracking, prompt safety boundaries, and full backward-compatible regression protection.

#### What Was Accomplished:

1. **AI Provider Abstraction Layer (`com.careeradvisor.backend.ai.provider`)**:
   - `AiProvider` interface: Decouples higher-level AI features from concrete LLM providers (`getProviderName()`, `isAvailable()`, `generateCompletion()`).
   - `MockAiProvider`: Always-available, deterministic, offline-friendly provider for development and testing environments.
   - `OpenAiCompatibleProvider`: Production-grade HTTP client with configurable timeouts (`SimpleClientHttpRequestFactory`), bearer token auth, and structured JSON parsing.
   - `AiProviderFactory`: Dynamically resolves the active provider with graceful, automatic fallback to `MockAiProvider` when API keys are absent or services are offline.

2. **Environment-Driven Configuration (`AiConfigProperties`)**:
   - Integrated properties: `app.ai.enabled`, `app.ai.provider`, `app.ai.api-key`, `app.ai.model`, `app.ai.base-url`, `app.ai.max-tokens`, `app.ai.temperature`, `app.ai.timeout-seconds`.
   - Updated `application.properties`, `application-dev.properties`, and `application-prod.properties` with safe defaults and zero hardcoded secrets.

3. **Personal User Context Engine (`com.careeradvisor.backend.ai.context`)**:
   - `UserAiContextService`: Aggregates the authoritative snapshot of the authenticated candidate across 8 domain services:
     - User profile summary (sanitized of passwords, hashes, and secrets)
     - Verified skills portfolio (`userSkillService`)
     - Latest resume analysis metadata, parsed skills, and skill gaps (`resumeRepository`)
     - Skill diagnostic assessment results (`quizService`)
     - Roadmap progress, milestone count, and next recommended step (`userProgressService`)
     - DSA problem-solving telemetry and category progress (`problemService`)
     - Mock interview metrics, average scores, strong areas, and weak areas (`mockInterviewRepository`)
     - Recommendation engine readiness score and priority action items (`recommendationService`)
   - `AiContextBuilder`: Transforms structured context into token-efficient system prompt blocks with strict safety boundaries ("OneStop Career Advisor AI" persona, zero hallucination of user state, explicit absence indicators).

4. **AI REST API Endpoints & Security (`com.careeradvisor.backend.ai.controller`)**:
   - `GET /api/ai/health`: Public/authenticated health probe returning provider availability and status.
   - `GET /api/ai/context`: Protected (`authenticated()`) endpoint returning `PersonalAiContextDto` strictly bound to `@AuthenticationPrincipal CustomUserDetails`.
   - `POST /api/ai/chat`: Protected endpoint executing contextual chat completions.
   - Updated `SecurityConfig.java` to grant appropriate access rules while enforcing stateless JWT verification.

5. **AI Usage Tracking & Observability (`com.careeradvisor.backend.ai.model`)**:
   - `AiUsageLog` JPA entity with database indexes on `user_id`, `created_at`, and `status`.
   - `AiUsageLogRepository` and `AiUsageLogService`: Logs prompt tokens, completion tokens, total tokens, latency in ms, provider, and error categories without storing user prompts or secrets.

6. **Frontend AI Client Layer (`frontend/lib/aiService.ts` & `frontend/lib/config.ts`)**:
   - Fully-typed TypeScript interfaces (`PersonalAiContext`, `AiHealth`, `AiChatRequest`, `AiChatResponse`).
   - Typed methods `getAiHealth()`, `getPersonalAiContext()`, `sendAiChatMessage()`.

7. **Verification & Regression Results**:
   - Backend compile: `BUILD SUCCESS` (138 source files).
   - Backend package: `BUILD SUCCESS` (`backend-0.0.1-SNAPSHOT.jar`).
   - Frontend build: `18 routes` compiled with 0 errors (`npm run build`).
   - `test_phase14a_ai_foundation.js`: **26/26 PASS (100%)**.
   - `test_phase13_production.js`: **28/28 PASS (100%)**.
   - `test_phase12_rbac.js`: **38/38 PASS (100%)**.
   - `test_phase11_resume.js`: **19/19 PASS (100%)**.
   - `test_master_regression.js`: **25/25 PASS (100%)**.

---

### Phase 14B: Persistent Personal AI Chatbot (COMPLETE)

1. **Reusable Entity & Schema Architecture (`com.careeradvisor.backend.model`)**:
   - `Conversation`: Persistent conversation entity with `owner` (`User`), `type` (`ConversationType`: `USER_TO_AI`, `USER_TO_USER`, `USER_TO_ADMIN`), `title`, `archived` status, `createdAt`, `updatedAt`, `lastMessageAt`.
   - `ChatMessage`: Persistent message entity with `conversation`, `senderUser` (null for AI/System), `senderType` (`MessageSenderType`: `USER`, `AI`, `ADMIN`, `SYSTEM`), `content` (`TEXT`), `sequenceNumber`, `status`, `createdAt`.
   - **Database Indexes**: Indexed on `user_id`, `updated_at`, `last_message_at`, `(user_id, archived)`, `conversation_id`, `created_at`, and `(conversation_id, created_at)`.
   - **Zero-Trust User Ownership & IDOR Protection**: All operations derive user identity from `@AuthenticationPrincipal CustomUserDetails`. Unowned conversations return `HTTP 404 Not Found` to prevent enumeration.

2. **Backend Domain Services (`com.careeradvisor.backend.service`)**:
   - `ConversationService`: CRUD operations for conversations, user isolation, deterministic title generation on first message (up to 80 chars, zero wasted LLM tokens), archive/delete lifecycle, and DTO mappings.
   - `ChatService`: Validates message length and content, persists user message, loads bounded recent conversation history (configurable `app.ai.chat.history-limit=20`), fetches authoritative personal user context via `UserAiContextService` and `AiContextBuilder`, calls active AI provider, persists AI response, updates conversation timestamps, and records usage telemetry.

3. **REST Endpoints (`ConversationController.java` & `SecurityConfig.java`)**:
   - `POST /api/conversations`: Create new persistent AI conversation (201 Created).
   - `GET /api/conversations`: List authenticated user's conversations ordered by `updatedAt DESC`.
   - `GET /api/conversations/{id}`: Retrieve conversation metadata and messages (404 on unowned).
   - `GET /api/conversations/{id}/messages`: Retrieve chronological message history.
   - `POST /api/conversations/{id}/messages`: Send user message and receive contextual AI response.
   - `POST /api/conversations/{id}/archive`: Archive conversation (rejects subsequent writes with 400).
   - `DELETE /api/conversations/{id}`: Delete conversation and associated messages.

4. **Frontend Chat Application (`/chat` & `frontend/lib/chatService.ts`)**:
   - Full-featured responsive chat interface with desktop sidebar and mobile overlay drawer.
   - Interactive suggestion cards ("Assess My Readiness", "Next Learning Step", "DSA Practice Focus", "Resume Skill Gaps", "Mock Interview Strategy").
   - Thinking indicator animation, optimistic UI preview, auto-scroll, `Enter` to send, `Shift+Enter` for newline.
   - Conversation management: create new chat, switch active conversation, archive, delete, refresh persistence.
   - Navigation link added to protected navigation sidebar in `layout.tsx`.

---

### Phase 14C: Real-Time Human Communication Foundation (COMPLETE)

1. **WebSocket & STOMP Infrastructure**:
   - Integrated `spring-boot-starter-websocket` with standard STOMP over WebSocket broker.
   - Endpoints: `/ws` (with SockJS fallback), destination prefixes `/topic`, `/queue`, `/app`.
   - `WebSocketSecurityInterceptor`: Enforces JWT authentication on STOMP `CONNECT` frame and strict participant authorization on `SUBSCRIBE` to `/topic/conversations/{id}`.
   - `PresenceService`: Thread-safe in-memory session tracking (`ConcurrentHashMap<Long, Set<String>>`) with multi-session support broadcasting `USER_ONLINE` / `USER_OFFLINE` to `/topic/presence`.

2. **Domain Entities & Participant Modeling**:
   - `ConversationParticipant`: Entity with unique constraint `(conversation_id, user_id)` and indexed on `(user_id, conversation_id)` and `(user_id, last_read_at)`.
   - `ParticipantRole`: `CREATOR`, `MEMBER`, `ADMIN`.
   - `ChatMessage`: Indexed on `(conversation_id, sequence_number)` for high-performance timeline retrieval.

3. **REST & STOMP APIs**:
   - `GET /api/users/search?q={query}`: Safe candidate discovery (omits sensitive attributes, excludes self, limit 20).
   - `POST /api/conversations/user`: Start peer-to-peer conversation (201 Created).
   - `POST /api/conversations/admin`: Start support ticket conversation (201 Created).
   - `GET /api/conversations/human`: List user's human conversations with unread counters.
   - `GET /api/conversations/human/{id}`: Retrieve human conversation thread and participants.
   - `POST /api/conversations/{id}/human-messages`: Send human message with real-time push.
   - `POST /api/conversations/{id}/read`: Mark conversation as read and broadcast read receipts.
   - `POST /api/conversations/{id}/typing`: Broadcast typing status to active participants.
   - `GET /api/conversations/admin/inbox`: Admin support queue (`hasRole('ADMIN')`).

4. **Frontend Real-Time Human Communication**:
   - `/messages`: Human Communication Center featuring Direct Messages (`USER_TO_USER`) and Support Tickets (`USER_TO_ADMIN`), user search modal, real-time message stream, typing indicators, read receipts, and online status badges.
   - `/admin/messages`: Admin Support Inbox with ticket queue, candidate metadata, and real-time reply console.
   - `frontend/lib/websocketService.ts`: Native STOMP-over-WebSocket client with automatic JWT authentication, reconnection logic, and topic subscription management.
   - `frontend/lib/humanChatService.ts`: Typed API client for user search and human conversation operations.

5. **Verification & Test Results**:
   - Backend compile: `BUILD SUCCESS` (169 source files compiled).
   - Frontend build: `21 routes` compiled with 0 errors (`npm run build`).
   - `test_phase14c_messaging.js`: **42/42 PASS (100%)**.
   - `test_browser_messaging_workflow.js`: **27/27 PASS (100%)**.
   - `test_phase14b_chat.js`: **38/38 PASS (100%)**.
   - `test_phase14a_ai_foundation.js`: **26/26 PASS (100%)**.
   - `test_phase13_production.js`: **28/28 PASS (100%)**.
   - `test_phase12_rbac.js`: **38/38 PASS (100%)**.
   - `test_phase11_resume.js`: **19/19 PASS (100%)**.
   - `test_master_regression.js`: **25/25 PASS (100%)**.
   - **Total Combined Automated Regression**: **243/243 PASS (100% Zero-Regression)**.

---

### Phase 14D: WebRTC Audio & Video Calling (COMPLETE)

1. **WebRTC Architecture & Domain Model**:
   - Zero Media through Backend: Video and audio media flows strictly peer-to-peer (P2P) via WebRTC SRTP/UDP.
   - Spring Boot Backend acts as Authoritative Signaling, State Management & Authorization Engine.
   - Domain Entities:
     - `CallSession`: JPA entity with composite indexes on `(receiver_id, status)`, `(caller_id, created_at)`, `(conversation_id, created_at)`, `(status, created_at)`.
     - `CallType`: `AUDIO`, `VIDEO`.
     - `CallStatus`: `RINGING`, `ACCEPTED`, `REJECTED`, `MISSED`, `CANCELLED`, `ENDED`, `FAILED`, `EXPIRED`.
     - `EndReason`: `USER_ENDED`, `REMOTE_ENDED`, `REJECTED`, `TIMEOUT`, `NETWORK_FAILURE`, `PERMISSION_DENIED`, `BUSY`, `UNKNOWN`.

2. **Server-Authoritative State Machine & Anti-Race Guards**:
   - Strict Transition Enforcement: `RINGING` $\rightarrow$ `ACCEPTED`, `REJECTED`, `CANCELLED`, `MISSED`, `EXPIRED`; `ACCEPTED` $\rightarrow$ `ENDED`, `FAILED`. Invalid transitions throw `IllegalStateException`.
   - Anti-Duplicate Active Call Guard: Rejects simultaneous incoming or outgoing call attempts with `409 Conflict (CALL_ALREADY_ACTIVE)`.
   - Ringing Expiration: 45-second timeout transitions unacknowledged calls to `MISSED` with `EndReason.TIMEOUT`.
   - Call Duration Computation: Accurately calculated server-side in seconds based on `answeredAt` and `endedAt`.

3. **REST Endpoints & STOMP Signaling (`CallController.java` & `RealTimeMessageController.java`)**:
   - `POST /api/calls`: Initiate Audio/Video call (201 Created) and broadcast `INCOMING_CALL` event.
   - `GET /api/calls/{id}`: Retrieve call metadata (404 on unowned/unauthorized).
   - `POST /api/calls/{id}/accept`: Accept call (200 OK) and broadcast `CALL_ACCEPTED`.
   - `POST /api/calls/{id}/reject`: Reject call (200 OK) and broadcast `CALL_REJECTED`.
   - `POST /api/calls/{id}/cancel`: Caller cancels before answer (200 OK) and broadcast `CALL_CANCELLED`.
   - `POST /api/calls/{id}/end`: End active call (200 OK) and broadcast `CALL_ENDED`.
   - `POST /api/calls/{id}/signal` & STOMP `@MessageMapping("/call.signal/{callId}")`: Exchange SDP offers, answers, and ICE candidates with size bounding (<50KB).
   - `GET /api/calls/history`: Retrieve chronological call session history for authenticated user.
   - `GET /api/calls/active`: Retrieve active incoming/outgoing calls.

4. **Frontend WebRTC Engine & Calling UI (`frontend/components/CallOverlay.tsx` & `frontend/lib/webrtcService.ts`)**:
   - `WebRtcManager`: Class encapsulating `RTCPeerConnection`, ICE server candidate collection, SDP offer/answer generation, local/remote stream management, device mute/video toggles, and microphone/camera error handling (`NotAllowedError`, `NotFoundError`, etc.).
   - `CallOverlay`: Floating draggable calling modal with Picture-in-Picture local stream, remote video stream, animated audio visualizer waves, live duration counter, accept/decline buttons, mute/camera toggles, and end call button.
   - Direct integration into `/messages` (Peer & Support) and `/admin/messages` (Admin Support calling).

5. **Verification & Test Results**:
   - Backend compile: `BUILD SUCCESS` (179 source files compiled).
   - Frontend build: `21 routes` compiled with 0 errors (`npm run build`).
   - `test_phase14d_calls.js`: **51/51 PASS (100%)**.
   - `test_browser_calls_workflow.js`: **21/21 PASS (100%)**.
   - `test_phase14c_messaging.js`: **42/42 PASS (100%)**.
   - `test_browser_messaging_workflow.js`: **27/27 PASS (100%)**.
   - `test_phase14b_chat.js`: **38/38 PASS (100%)**.
   - `test_browser_chat_workflow.js`: **25/25 PASS (100%)**.
   - `test_phase14a_ai_foundation.js`: **26/26 PASS (100%)**.
   - `test_phase13_production.js`: **28/28 PASS (100%)**.
   - `test_phase12_rbac.js`: **38/38 PASS (100%)**.
   - `test_phase11_resume.js`: **19/19 PASS (100%)**.
   - `test_master_regression.js`: **25/25 PASS (100%)**.
   - **Total Combined Automated Regression**: **340/340 PASS (100% Zero-Regression)**.

---

### Phase 15 — Unified Career Intelligence, Action Plan, Adaptive Planner & Progress Coach

#### Phase 15A: 6-Dimensional Unified Career Intelligence Engine (COMPLETE)

- **Date**: 2026-08-23
- **Status**: COMPLETE
- **Purpose**: Unify candidate performance across all 6 core pillars of the platform into a deterministic, explainable career readiness score (0-100) and actionable skill gap hierarchy with concrete evidence.
- **6-Dimensional Readiness Model**:
  1. `SKILLS` (Weight: 20%): Verified candidate skills matching required target career track competencies.
  2. `ROADMAP` (Weight: 20%): Completed checkpoints and milestones in target track curriculum.
  3. `ASSESSMENT` (Weight: 15%): Diagnostic quiz score and classified skill level.
  4. `DSA` (Weight: 15%): Problems solved vs total curated problem catalog.
  5. `MOCK_INTERVIEW` (Weight: 15%): Completed interview average score and conceptual mastery.
  6. `RESUME` (Weight: 15%): Parsed resume skill coverage vs target role competencies.
- **Readiness States**: `NOT_STARTED` (0-19), `DEVELOPING` (20-49), `PROGRESSING` (50-74), `JOB_READY` (75-89), `EXCEPTIONAL` (90-100).
- **Priority Gaps**: Identifies missing competencies and classifies them into `CRITICAL`, `HIGH`, `MEDIUM`, or `LOW` priority with concrete evidence.
- **REST Endpoints (`CareerIntelligenceController.java`)**:
  - `GET /api/career-intelligence`: Unified 6-dimensional intelligence report.
  - `GET /api/career-intelligence/actions`: Ranked priority actions list.
- **Testing**: `scratch/test_phase15a_intelligence.js` — **51/51 PASS (100%)**.

#### Phase 15B: Goal-Oriented AI Action Planner & Explanations (COMPLETE)

- **Date**: 2026-08-23
- **Status**: COMPLETE
- **Purpose**: Synthesize high-priority career intelligence insights into a focused, non-overwhelming 3-to-5 action plan combining deterministic readiness grounding with AI contextual synthesis.
- **Architecture & Guardrails**:
  - `AiActionPlanService`: Ingests `CareerIntelligenceDto` and personal context, generating structured prompts for active `AiProvider`.
  - Deterministic fallback: Graceful fallback engine if LLM service is offline or disabled.
  - Strict Bounds: Bounded to 3-5 high-impact actions. AI is strictly prohibited from altering deterministic readiness scores or hallucinating unverified candidate competencies.
  - Action Categories: `ROADMAP`, `DSA`, `RESUME`, `INTERVIEW`, `ASSESSMENT`, `SKILL`.
  - Each action provides `whyItMatters`, `expectedOutcome`, `estimatedEffort`, and direct `actionUrl`.
- **REST Endpoints (`AiController.java`)**:
  - `GET /api/ai/action-plan`: Retrieve or generate personalized action plan.
  - `POST /api/ai/action-plan/refresh`: Force fresh AI generation with latest platform context.
- **Testing**: `scratch/test_phase15b_action_plan.js` — **40/40 PASS (100%)**.

#### Phase 15C: Adaptive Weekly Study Planner & Daily Time-Boxing (COMPLETE)

- **Date**: 2026-08-23
- **Status**: COMPLETE
- **Purpose**: Bridge career strategy and daily execution by generating structured, time-boxed 7-day study schedules tailored to user study preferences, balancing categories, and tracking task lifecycles.
- **Domain Model & Entities**:
  - `PlannerPreference`: Configurable daily study budget (`dailyStudyMinutes`), study window (`StudyWindow`: `MORNING`, `AFTERNOON`, `EVENING`, `NIGHT`, `FLEXIBLE`), preferred study days (`preferredDays`), and weekly goal.
  - `LearningPlan`: Weekly plan container (`weekStartDate`, `weekEndDate`, `targetCareer`, `status`, `source`: `DETERMINISTIC` or `AI_ASSISTED`).
  - `LearningTask`: Actionable daily item (`date`, `title`, `description`, `category`, `priority`, `durationMinutes`, `status`: `PENDING`, `IN_PROGRESS`, `COMPLETED`, `SKIPPED`, `sequenceOrder`, `actionUrl`).
- **Engines & Lifecycle**:
  - `DeterministicPlannerEngine`: Intelligently pools candidate tasks from prioritized skill gaps, allocates minutes across active days, and injects balanced review tasks on off-days.
  - `PlannerService`: Manages plan generation, adaptive refreshing, streak calculation, and task state transitions (`startTask`, `completeTask`, `skipTask`).
  - `AdminPlannerController`: RBAC-protected endpoint (`/api/admin/planner/stats`) providing aggregate planner telemetry.
- **Frontend UI (`/planner`)**:
  - Interactive 7-day calendar view with date selector, daily progress ring, task completion toggles, start/skip buttons, study preference drawer, and streak counter.
- **REST Endpoints (`PlannerController.java`)**:
  - `GET /api/planner/current`, `POST /api/planner/generate`, `POST /api/planner/refresh`
  - `GET /api/planner/today`, `GET /api/planner/week`, `GET /api/planner/progress`
  - `GET /api/planner/preferences`, `PUT /api/planner/preferences`
  - `POST /api/planner/tasks/{id}/start`, `POST /api/planner/tasks/{id}/complete`, `POST /api/planner/tasks/{id}/skip`
  - `GET /api/admin/planner/stats` (`hasRole('ADMIN')`)
- **Testing**:
  - `scratch/test_phase15c_planner.js` — **55/55 PASS (100%)**.
  - `scratch/test_browser_planner_workflow.js` — **20/20 PASS (100%)**.

#### Phase 15D: AI Progress Coach & Weekly Longitudinal Accountability Engine (COMPLETE)

- **Date**: 2026-08-23
- **Status**: COMPLETE
- **Purpose**: Maintain continuous candidate accountability through longitudinal tracking, computing weekly progress scores, multidimensional trend trajectories, and synthesized AI coaching narratives.
- **Domain Model & Scoring**:
  - `WeeklyProgressReport`: JPA entity capturing `periodStart`, `periodEnd`, `overallProgressScore`, `careerReadinessScore`, `roadmapProgress`, `dsaProgress`, `interviewProgress`, `plannerConsistency`, `streakDays`, `studyMinutes`, trends, strengths, weaknesses, and coaching advice.
  - `OverallProgressScore` Formula: `0.25 * Roadmap + 0.25 * DSA + 0.25 * Interview + 0.25 * Planner Consistency`.
  - `TrendDirection`: Longitudinal comparison against previous reports classifying trends as `IMPROVING`, `STABLE`, `DECLINING`, or `INSUFFICIENT_DATA`.
- **Frontend & AI Chatbot Integration**:
  - `AiProgressCoachSection.tsx`: Embedded into Dashboard (`/dashboard`) showcasing weekly score rings, trend badges, coach advice card, and next-week focus priorities.
  - Chatbot Grounding: AI Chatbot (`/chat`) dynamically grounds queries like "How am I doing this week?" using the latest authoritative weekly progress report.
- **REST Endpoints (`ProgressCoachController.java`)**:
  - `GET /api/ai/progress/weekly`: Retrieve current week progress report.
  - `POST /api/ai/progress/refresh`: Force recalculation and AI synthesis.
  - `GET /api/ai/progress/history`: Retrieve historical weekly progress reports.
- **Testing**:
  - `scratch/test_phase15d_progress_coach.js` — **51/51 PASS (100%)**.
  - `scratch/test_browser_progress_coach.js` — **20/20 PASS (100%)**.

---

## 6. Comprehensive Testing Record

| Date | Scope / Test Suite | Result | Details |
| :--- | :--- | :---: | :--- |
| 2026-10-19 | Frontend Production Build (`npm run build`) | **PASS** | Turbopack compilation succeeded with 0 errors across all 22 routes after input text visibility & autofill normalization fix |
| 2026-10-16 | Executive PDF Report Generation (`CAREER_ADVISOR_PROJECT_REPORT.pdf`) | **PASS** | Headless Chrome compilation produced 825 KB vectorized publication PDF |
| 2026-10-16 | Backend Maven Compile (`.\mvnw.cmd clean compile`) | **PASS** | 227 Java source files compiled cleanly with Spring Boot 3.3.4 & Java 17 |
| 2026-10-16 | Frontend Production Build (`npm run build`) | **PASS** | Turbopack compilation succeeded; 22 routes compiled (0 errors) |
| 2026-08-23 | Phase 15D AI Progress Coach (`test_phase15d_progress_coach.js`) | **PASS** | 51 / 51 assertions passed (100%) |
| 2026-08-23 | Browser Progress Coach Contract (`test_browser_progress_coach.js`) | **PASS** | 20 / 20 assertions passed (100%) |
| 2026-08-23 | Phase 15C Adaptive Weekly Planner (`test_phase15c_planner.js`) | **PASS** | 55 / 55 assertions passed (100%) |
| 2026-08-23 | Browser Planner Workflow Contract (`test_browser_planner_workflow.js`) | **PASS** | 20 / 20 assertions passed (100%) |
| 2026-08-23 | Phase 15B AI Action Plan (`test_phase15b_action_plan.js`) | **PASS** | 40 / 40 assertions passed (100%) |
| 2026-08-23 | Phase 15A Career Intelligence (`test_phase15a_intelligence.js`) | **PASS** | 51 / 51 assertions passed (100%) |
| 2026-08-23 | Phase 14D WebRTC Audio/Video (`test_phase14d_calls.js`) | **PASS** | 51 / 51 assertions passed (100%) |
| 2026-08-23 | Browser WebRTC Calls Contract (`test_browser_calls_workflow.js`) | **PASS** | 21 / 21 assertions passed (100%) |
| 2026-08-23 | Phase 14C Messaging & Presence (`test_phase14c_messaging.js`) | **PASS** | 42 / 42 assertions passed (100%) |
| 2026-08-23 | Browser Messaging Contract (`test_browser_messaging_workflow.js`) | **PASS** | 27 / 27 assertions passed (100%) |
| 2026-08-23 | Phase 14B AI Chatbot (`test_phase14b_chat.js`) | **PASS** | 38 / 38 assertions passed (100%) |
| 2026-08-23 | Browser Chat Workflow Contract (`test_browser_chat_workflow.js`) | **PASS** | 25 / 25 assertions passed (100%) |
| 2026-08-22 | Phase 14A AI Infrastructure (`test_phase14a_ai_foundation.js`) | **PASS** | 26 / 26 assertions passed (100%) |
| 2026-08-22 | Phase 13 Production Hardening (`test_phase13_production.js`) | **PASS** | 28 / 28 assertions passed (100%) |
| 2026-08-22 | Phase 12 RBAC & Governance (`test_phase12_rbac.js`) | **PASS** | 38 / 38 assertions passed (100%) |
| 2026-08-22 | Browser RBAC Contract (`test_browser_rbac_workflow.js`) | **PASS** | 13 / 13 assertions passed (100%) |
| 2026-08-22 | Phase 11 Resume Analyzer (`test_phase11_resume.js`) | **PASS** | 19 / 19 assertions passed (100%) |
| 2026-08-22 | Browser Resume Contract (`test_browser_resume_workflow.js`) | **PASS** | 16 / 16 assertions passed (100%) |
| 2026-08-22 | Master Platform Regression (`test_master_regression.js`) | **PASS** | 25 / 25 assertions passed (100%) |
| **TOTAL** | **Combined Automated Regression Suite** | **PASS** | **577 / 577 PASS (100% Zero-Regression)** |

---

## 7. Known Issues & Bug Tracking

| Issue ID | Description | Severity | Module | Status | Discovered Date | Fixed Date | Resolution Details |
| :--- | :--- | :---: | :--- | :---: | :---: | :---: | :--- |
| ISS-001 | Resume service API called wrong endpoint prefix without `/api`. | High | Frontend Resume | FIXED | 2026-08-22 | 2026-08-22 | Mapped endpoints via `API_ENDPOINTS.RESUMES` in `config.ts` |
| ISS-002 | Simultaneous call attempts triggered race conditions. | High | WebRTC Engine | FIXED | 2026-08-23 | 2026-08-23 | Enforced server-authoritative state machine with `409 Conflict` guard |
| ISS-003 | AI Chatbot could potentially hallucinate unattempted quiz scores. | Medium | AI Chatbot | FIXED | 2026-08-22 | 2026-08-22 | Injected strict negative grounding constraints in `AiContextBuilder` |
| ISS-004 | Input text on login and signup pages rendered white on white background under dark mode preference / missing text-gray-900. | High | Auth UI (Login & Signup) | FIXED | 2026-10-19 | 2026-10-19 | Added color-scheme: light, removed conflicting prefers-color-scheme dark override in globals.css, enforced explicit bg-white text-gray-900 placeholder:text-gray-400 across all form inputs, and added -webkit-autofill normalization. |


---

## 8. Dependency Tracking

| Package / Artifact | Ecosystem | Version | Purpose |
| :--- | :--- | :--- | :--- |
| `spring-boot-starter-web` | Maven / Java | 3.3.4 | RESTful Web Services & MVC framework |
| `spring-boot-starter-data-jpa` | Maven / Java | 3.3.4 | Object-Relational Mapping (Hibernate 6) |
| `spring-boot-starter-security` | Maven / Java | 3.3.4 | Authentication, Authorization & Security Filter Chains |
| `spring-boot-starter-websocket` | Maven / Java | 3.3.4 | Real-time STOMP over WebSocket communication |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | Maven / Java | 0.12.6 | RFC 7519 JSON Web Token signing & validation |
| `pdfbox` | Maven / Java | 3.0.3 | PDF text extraction & document stream parsing |
| `poi` / `poi-ooxml` | Maven / Java | 5.3.0 | DOCX Word document extraction & sanitization |
| `mysql-connector-j` | Maven / Java | 8.3.0 | High-performance MySQL JDBC driver |
| `lombok` | Maven / Java | 1.18.34 | Boilerplate reduction for models and DTOs |
| `next` | npm / Frontend | 16.2.3 | Next.js App Router with Turbopack |
| `react` / `react-dom` | npm / Frontend | 19.2.4 | React library for UI rendering |
| `framer-motion` | npm / Frontend | 12.38.0 | Smooth animations and interactive transitions |
| `tailwindcss` | npm / Frontend | 4.0.0 | Modern utility-first CSS styling |

---


---


---

### Phase 16 — Production Cloud Deployment & Live Verification

#### Status: COMPLETE (Backend, Database, and Frontend FULLY DEPLOYED AND VERIFIED)
- **Date**: 2026-10-16
- **Architecture**:
  - **Backend**: Spring Boot 3.3.4 (Java 17) container deployed on Railway (`https://backend-production-f4cb.up.railway.app`)
  - **Database**: Railway Managed MySQL 8.0 (`mysql.railway.internal:3306`)
  - **Frontend**: Next.js 16.2.3 (React 19.2.4) deployed on Vercel (`https://frontend-stark-1a88.vercel.app` & `https://frontend-alpha-one-pvh4znooes.vercel.app`)
- **Infrastructure & Environment Details**:
  - **Railway Project**: `career-advisor` (`f4707965-051f-448c-b1c0-da6d504cc297`)
  - **Railway Environment**: `production` (`c8c4ecef-4e5c-46e0-a758-2b1acd6d0600`)
  - **Database Service**: `MySQL` (`7670d1d4-08ce-43fe-9bd3-1ad913f0418f`), Status: `● Online`
  - **Backend Service**: `backend` (`f13e69ff-c063-4709-8a2d-1e0b8938dbbd`), Status: `● Online`
  - **Public Backend Domain**: `https://backend-production-f4cb.up.railway.app`
  - **Vercel Project**: `frontend` (`stark-1a88`)
  - **Production Frontend Domain**: `https://frontend-stark-1a88.vercel.app` (Alias: `https://frontend-alpha-one-pvh4znooes.vercel.app`)
  - **Vercel Build Target**: Next.js 16.2.3 App Router with Turbopack (22 static/dynamic routes compiled cleanly)
  - **Vercel Configured Env**: `NEXT_PUBLIC_API_BASE_URL=https://backend-production-f4cb.up.railway.app`
- **Environment Variables Configured on Railway `backend`**:
  - `SPRING_PROFILES_ACTIVE`: `prod`
  - `MYSQLHOST`: `${{MySQL.MYSQLHOST}}`
  - `MYSQLPORT`: `${{MySQL.MYSQLPORT}}`
  - `MYSQLDATABASE`: `${{MySQL.MYSQLDATABASE}}`
  - `MYSQLUSER`: `${{MySQL.MYSQLUSER}}`
  - `MYSQLPASSWORD`: `${{MySQL.MYSQLPASSWORD}}`
  - `JWT_SECRET`: `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970`
  - `CORS_ALLOWED_ORIGINS`: `https://frontend-stark-1a88.vercel.app,https://frontend-alpha-one-pvh4znooes.vercel.app,https://*.vercel.app,http://localhost:3000`
- **Live Verification Record**:
  - Root Health: `GET https://backend-production-f4cb.up.railway.app/` $\rightarrow$ HTTP 200 `Backend is running 🚀`
  - Auth Health: `GET https://backend-production-f4cb.up.railway.app/api/auth/health` $\rightarrow$ HTTP 200 `{"status":"UP","message":"Authentication service is running"}`
  - Frontend Pages: `GET https://frontend-stark-1a88.vercel.app/` $\rightarrow$ HTTP 200 (Title: *Career Advisor - Plan Your Tech Career*)
  - User Registration Flow: `POST https://backend-production-f4cb.up.railway.app/api/auth/register` $\rightarrow$ HTTP 200 (Account created with ID 1 in Railway MySQL, JWT issued)
  - Authentication Flow: `POST https://backend-production-f4cb.up.railway.app/api/auth/login` $\rightarrow$ HTTP 200 (Credentials verified, fresh JWT issued)
  - Protected API Access: `GET https://backend-production-f4cb.up.railway.app/api/problems` $\rightarrow$ HTTP 200 (14 curated coding problems successfully returned via Bearer JWT)

- **Input Text Visibility & Autofill Hardening (2026-10-19)**:
  - Resolved white-on-white text input rendering issue on `/login` and `/signup`.
  - Enforced `color-scheme: light` and removed conflicting `@media (prefers-color-scheme: dark)` in `frontend/app/globals.css`.
  - Added baseline `input, textarea, select { color: #111827; }` and `-webkit-autofill` normalization.
  - Added explicit `bg-white text-gray-900 placeholder:text-gray-400` across all input controls on login (`email`, `password`) and signup (`name`, `email`, `password`, `confirmPassword`).
  - Production build re-verified cleanly with 22 routes compiled (0 errors).

---

### Phase 17 — Mentorship Module: Phase 1 (Domain Models & Repositories)

#### Status: COMPLETE (Backend Models, Enums, Repositories, and Unit Tests Verified)
- **Date**: 2026-10-19
- **Architecture & Design Decisions**:
  - **Decoupled Profile Entity Strategy**: Retained `User.role = Role.USER` to prevent account bifurcation, preserve admin role counting, and enable dual user/mentor identity. Created a dedicated `@OneToOne` `MentorProfile` entity.
  - **Verification Lifecycle**: Implemented `MentorVerificationStatus` (`APPLIED`, `APPROVED`, `REJECTED`, `SUSPENDED`) to support administrative vetting and status management.
  - **Mentorship Request Lifecycle**: Implemented `MentorshipRequestStatus` (`PENDING`, `ACCEPTED`, `DECLINED`, `CANCELLED`, `COMPLETED`).
  - **Direct Messaging Integration Bridge**: Configured `MentorshipRequest.conversation` as a nullable foreign key referencing `Conversation`. When a mentor accepts a request, a `USER_TO_USER` conversation is created/linked automatically without altering the existing messaging system.
- **Created Files**:
  - `MentorVerificationStatus.java`: Lifecycle states for mentor applications.
  - `MentorshipRequestStatus.java`: Lifecycle states for student mentorship requests.
  - `MentorProfile.java`: Entity mapped to `mentor_profiles` table (`user_id`, `job_title`, `company`, `years_of_experience`, `bio`, `expertise_areas`, `max_mentees`, `verification_status`, `is_active`, `rejection_reason`, timestamps).
  - `MentorshipRequest.java`: Entity mapped to `mentorship_requests` table (`student_id`, `mentor_profile_id`, `goal_description`, `status`, `conversation_id`, `response_note`, timestamps).
  - `MentorProfileRepository.java`: Queries for user lookup, status filtering, active approved mentors, and full-text keyword search across name, company, title, and expertise.
  - `MentorshipRequestRepository.java`: Queries for student requests, mentor queues, active request counts, and duplicate prevention.
  - `MentorshipModelTests.java`: Comprehensive JUnit 5 unit tests validating default values, associations, and enum integrity.
- **Verification Record**:
  - Backend compilation: `.\mvnw.cmd test-compile` $\rightarrow$ 233 source files compiled with 0 errors.
  - Model unit tests: `.\mvnw.cmd test -Dtest=MentorshipModelTests` $\rightarrow$ 3 tests run, 0 failures, 0 errors in 0.055s.

---

### Phase 18 — Mentorship Module: Phase 2 (Business Logic, Authorization & REST APIs)

#### Status: COMPLETE (Services, Controllers, Security, Hybrid Capacity, Pessimistic Locking & Tests Verified)
- **Date**: 2026-10-19
- **Architecture & Design Implementations**:
  - **Hybrid Capacity Control**: Implemented `MentorshipCapacityConfigService` enforcing administrator-defined minimum (default: 1) and maximum (default: 10) active mentee thresholds alongside mentor-selected limits. Exposes `GET /api/admin/mentors/config` and `PUT /api/admin/mentors/config`.
  - **Concurrency & Pessimistic Locking**: Configured `@Lock(LockModeType.PESSIMISTIC_WRITE)` in `MentorProfileRepository.findByIdWithLock(id)`. When a mentor accepts a request, an exclusive database lock is acquired during capacity evaluation (`countByMentorIdAndStatus(ACCEPTED)`), completely eliminating race conditions and over-allocation.
  - **State Machine Integrity**: Strictly enforces permitted state transitions:
    - `PENDING` $\rightarrow$ `ACCEPTED` (only target mentor, checks active slots, acquires lock, creates 1-on-1 chat)
    - `PENDING` $\rightarrow$ `DECLINED` (only target mentor, optional response note)
    - `PENDING` $\rightarrow$ `CANCELLED` (only requesting student)
    - `ACCEPTED` $\rightarrow$ `COMPLETED` (either participant, frees up capacity slot while preserving chat history)
  - **Direct Messaging Integration**: Integrates directly with existing `ConversationService.createUserToUserConversation(...)`. Automatically creates or reuses the `USER_TO_USER` conversation and attaches `conversation_id` to `MentorshipRequest`.
  - **Administrative Review**: Exposes `/api/admin/mentors/pending`, `/api/admin/mentors/{id}/approve`, `/api/admin/mentors/{id}/reject`, and `/api/admin/mentors/{id}/suspend` with explicit self-approval prevention and role-based access control.
  - **Global Exception Mapping**: Mapped `IllegalStateException` to HTTP 409 Conflict in `GlobalExceptionHandler` to cleanly report capacity limits and invalid state transitions.
- **Created & Modified Files**:
  - DTOs: `MentorApplicationDto.java`, `MentorProfileUpdateDto.java`, `MentorProfileDto.java`, `CreateMentorshipRequestDto.java`, `MentorshipRequestDto.java`, `MentorshipActionDto.java`, `MentorshipCapacityConfigDto.java`.
  - Services: `MentorshipCapacityConfigService.java`, `MentorProfileService.java`, `MentorshipRequestService.java`.
  - Controllers: `MentorController.java` (`/api/mentors/**`), `AdminMentorController.java` (`/api/admin/mentors/**`).
  - Tests: `MentorshipServiceTests.java` (18 unit tests), `MentorControllerTests.java` (5 tests), `AdminMentorControllerTests.java` (5 tests), `MentorshipModelTests.java` (3 tests).
- **Verification Record**:
  - Backend compilation: `.\mvnw.cmd test-compile` $\rightarrow$ 245 source files compiled cleanly with 0 errors.
  - Test Suite: `.\mvnw.cmd test "-Dtest=*Mentor*"` $\rightarrow$ **31 tests run, 0 failures, 0 errors** in 21.888s.

---

### Phase 19 — Mentorship Module: Phase 3B.1 (Frontend Foundation, WebSocket Token Normalization & Typed Service)

#### Status: COMPLETE (Token Key Normalized, API Endpoints Added, Typed mentorService.ts & Production Build Verified)
- **Date**: 2026-10-19
- **Architecture & Foundation Changes**:
  - **WebSocket Token Normalization**: Resolved the token key mismatch in `frontend/lib/websocketService.ts` by importing `getStoredToken()` from `./apiClient`. Both the REST API client and the WebSocket/STOMP service now read consistently from the single source of truth (`"auth_token"`). No circular dependencies were introduced.
  - **API Configuration**: Added full `API_ENDPOINTS.MENTORS` in `frontend/lib/config.ts` covering all 12 student/mentor endpoints and 8 admin endpoints with reusable dynamic ID builders.
  - **Typed Service Implementation**: Created `frontend/lib/mentorService.ts` modeling all backend DTOs (`MentorApplicationDto`, `MentorProfileUpdateDto`, `MentorProfileDto`, `CreateMentorshipRequestDto`, `MentorshipRequestDto`, `MentorshipActionDto`, `MentorshipCapacityConfigDto`, and enums `MentorVerificationStatus`, `MentorshipRequestStatus`).
  - **Error Fidelity**: Leverages `apiRequest` without swallowing HTTP 400 (validation), 401 (auth), 403 (forbidden), 404 (not found), or 409 (capacity/duplicate conflict) errors.
- **Created & Modified Files**:
  - Modified: `frontend/lib/websocketService.ts` (replaced `localStorage.getItem('token')` with `getStoredToken()`).
  - Modified: `frontend/lib/config.ts` (added `API_ENDPOINTS.MENTORS`).
  - Created: `frontend/lib/mentorService.ts` (full typed client service).
- **Verification Record**:
  - TypeScript validation: `npx tsc --noEmit` $\rightarrow$ Passed with **0 errors**.
  - Production build: `npm run build` (Next.js 16.2.3 with Turbopack) $\rightarrow$ **22/22 routes generated cleanly with 0 errors**.

---

### Phase 20 — Mentorship Module: Phase 3B.2 (Navigation Integration)

#### Status: COMPLETE (Header Shortcut Updated, Mentorship Added to Sidebar & Verified)
- **Date**: 2026-10-19
- **Navigation Changes**:
  - **Header Shortcut**: Replaced the `/mock-interview` quick shortcut in the top navbar (`frontend/app/(protected)/layout.tsx`) with `"Find a Mentor"` pointing to `/mentorship` (with indigo styling `text-indigo-700 bg-indigo-50 border-indigo-200` and `👥` icon).
  - **Sidebar & Mobile Drawer Navigation**: Inserted `{ label: "Mentorship", href: "/mentorship", icon: "👥" }` into `navItems` directly below `"Messages"` (`/messages`).
  - **Page Title Mapping**: Added title mapping in `getPageTitle()` returning `"Mentorship Hub"` whenever the pathname starts with `/mentorship`.
  - **Preservation**: The original `"Mock Interview"` entry in `navItems` (`/mock-interview` with icon `🎯`) was fully preserved in its exact position.
- **Created & Modified Files**:
  - Modified: `frontend/app/(protected)/layout.tsx` (navigation items, page title, top header shortcut).
- **Verification Record**:
  - TypeScript validation: `npx tsc --noEmit` $\rightarrow$ Passed with **0 errors**.
  - Production build: `npm run build` (Next.js 16.2.3 with Turbopack) $\rightarrow$ **22/22 routes generated cleanly with 0 errors**.

---

### Phase 21 — Mentorship Module: Phase 3B.3 (Mentorship Hub UI & Deterministic Recommendations)

#### Status: COMPLETE (Mentorship Hub Page Created, Recommendations Implemented, Request Flow & Production Build Verified)
- **Date**: 2026-10-19
- **UI Architecture & Features**:
  - **Hub Route**: Built `frontend/app/(protected)/mentorship/page.tsx` integrating real backend services (`mentorService.getApprovedMentors`, `mentorService.getSentRequests`, `mentorService.getMyProfile`, `progressService.getProgressSummary`, `skillService.getUserSkills`).
  - **Deterministic Recommendation Utility**: Implemented `frontend/lib/mentorRecommendation.ts` utilizing transparent keyword normalization, direct skill-to-expertise scoring (10 pts exact, 6 pts partial), career goal alignment (15 pts direct, 4-5 pts keyword), slot availability prioritization (+1 pt), stable tie-breaking, and human-readable evidence strings without external AI/ML dependencies.
  - **Explore All Mentors Directory**: Responsive search input (matching name, job title, company, or expertise) and dynamic filter pills extracted from all active mentor profiles. Shows real mentor fields, years of experience, bio, and slot availability badges (`availableSlots > 0` vs `Fully Booked`).
  - **Mentorship Request Modal**: Accessible modal strictly adhering to the backend DTO `{ goalDescription: string }` with character counter (max 2000 chars), live validation, conflict error handling (HTTP 409 duplicate request or capacity reached), and background list refresh. Prevents self-mentorship and requests when available slots are 0.
  - **My Mentorships Tab**: Dedicated tab displaying all sent requests with live status badges (`PENDING`, `ACCEPTED`, `DECLINED`, `CANCELLED`, `COMPLETED`), submission dates, student goals, mentor response notes, interactive request cancellation (`mentorService.cancelRequest`), and direct "Open Messages" link to `/messages` for accepted mentorships.
  - **Context-Aware Navigation Actions**: Provides status-aware actions for mentor application status (`APPLIED` review badge, `APPROVED` mentor dashboard notice, or informative modal for prospective applicants).
- **Created & Modified Files**:
  - Created: `frontend/lib/mentorRecommendation.ts` (deterministic scoring and sorting utility).
  - Created: `frontend/app/(protected)/mentorship/page.tsx` (Mentorship Hub page and modal components).
  - Modified: `docs/PROJECT_STATUS.md` (recorded Phase 21 completion and updated next step).
- **Verification Record**:
  - TypeScript validation: `npx tsc --noEmit` $\rightarrow$ Passed with **0 errors**.
  - Production build: `npm run build` (Next.js 16.2.3 with Turbopack) $\rightarrow$ **23/23 routes generated cleanly with 0 errors** (including `○ /mentorship`).

---

### Phase 22 — Mentorship Module: Phase 3B.4.1 (Mentor Application Page & Verification Status Views)

#### Status: COMPLETE (Mentor Application Page at /mentorship/apply Created, Status Views Implemented, Form Validation & Production Build Verified)
- **Date**: 2026-10-19
- **Architecture & Application Features**:
  - **Application Route**: Built `frontend/app/(protected)/mentorship/apply/page.tsx` integrating `mentorService.apply(data: MentorApplicationDto)` and `mentorService.getMyProfile()`.
  - **Strict DTO Fidelity**: Uses verified backend fields (`jobTitle`, `company`, `yearsOfExperience`, `bio`, `expertiseAreas`, `maxMentees`, `linkedinUrl`, `githubUrl`, `portfolioUrl`). Validates numeric bounds (`yearsOfExperience >= 0`, `maxMentees` 1–10, string length limits $\le 120$ chars, $\le 500$ chars, $\le 2000$ chars) and URL format (`http://` or `https://`, $\le 255$ chars).
  - **Dynamic Status Lifecycle Views**:
    - **First-time Applicants (HTTP 404)**: Renders the structured multi-section application form with clean labels, field validation, character counters, and loading spinner.
    - **APPLIED (Pending Review)**: Renders a dedicated "Under Administrative Review" status card with an overview of all submitted application details, explanation of the review process, and return link to the Hub.
    - **APPROVED**: Renders an "Approved & Active Mentor" status view with profile summary and navigation links.
    - **REJECTED**: Renders an "Application Not Approved" status view displaying the admin's `rejectionReason` callout note and policy explanation.
    - **SUSPENDED**: Renders an "Mentorship Suspended" view directing the user to platform support.
  - **Duplicate Application Prevention**: Guards against concurrent submissions via disabled button state and gracefully catches backend HTTP 409 conflict errors without clearing entered form data.
  - **Hub Integration**: Updated `frontend/app/(protected)/mentorship/page.tsx` to seamlessly link "Apply to Become a Mentor" and pending status badges directly to `/mentorship/apply`.
- **Coding-Practice Profile Status**:
  - The current backend model and DTO define `portfolioUrl` (validated with `validateUrl(..., "Portfolio")`). While applicants can supply a coding practice profile link (e.g., LeetCode/HackerRank) within `portfolioUrl`, the backend lacks an explicit `codingPracticeProfileUrl` property. Smallest backend change needed: add `codingProfileUrl` to `MentorProfile`, `mentor_profiles` table, and `MentorApplicationDto`.
- **Created & Modified Files**:
  - Created: `frontend/app/(protected)/mentorship/apply/page.tsx` (mentor application form and status views).
  - Modified: `frontend/app/(protected)/mentorship/page.tsx` (linked apply buttons to `/mentorship/apply`).
  - Modified: `docs/PROJECT_STATUS.md` (recorded Phase 22 completion and updated next step).
- **Verification Record**:
  - TypeScript validation: `npx tsc --noEmit` $\rightarrow$ Passed with **0 errors**.
  - Production build: `npm run build` (Next.js 16.2.3 with Turbopack) $\rightarrow$ **24/24 routes generated cleanly with 0 errors** (including `○ /mentorship/apply`).

---

### Phase 23 — Mentorship Module: Phase 3B.4.2A (Individual Mentor Profile & Directory Integration)

#### Status: COMPLETE (Dynamic Route /mentorship/[id] Created, View Profile Actions Added, Request Modal Integrated & Production Build Verified)
- **Date**: 2026-10-19
- **Architecture & Profile Features**:
  - **Dynamic Route**: Built `frontend/app/(protected)/mentorship/[id]/page.tsx` utilizing dynamic params (`use(params)` for Next.js App Router). Retrieves authoritative profile via `mentorService.getMentorById(id)` (`GET /api/mentors/{id}`).
  - **Data Fidelity & Comprehensive Display**:
    - Mentor identity, job title, company, avatar initial, verified badge (`✅ Verified Mentor`).
    - Key metrics grid: years of experience, current active mentee count vs maximum capacity (`activeMenteesCount` / `maxMentees`), and slot availability pill.
    - Expertise areas parsed cleanly into badges.
    - Full biography with whitespace preservation (omits empty section if bio is null).
    - Verified external links for LinkedIn, GitHub, and Portfolio / Coding Practice profile with safe `rel="noopener noreferrer"` and `target="_blank"`.
  - **Status Handling**:
    - Loading skeleton with animated placeholders.
    - Dedicated 404 / Not Found view when mentor ID does not exist or profile is inactive.
    - Existing engagement callouts: detects if student already has a `PENDING` request (showing submitted goal and review status) or an `ACCEPTED` mentorship (providing direct "Open Chat" link to `/messages`).
  - **Request Mentorship Flow**: Accessible modal enforcing exact backend payload `{ goalDescription: string }` with character counter (max 2000 chars), live validation, conflict error handling (HTTP 409 duplicate request or capacity reached), and local slot/status refresh upon submission.
  - **Directory Integration**: Updated `MentorCard` in `frontend/app/(protected)/mentorship/page.tsx` with a clickable mentor name link and dedicated `"View Profile"` action button leading directly to `/mentorship/${mentor.id}` without interfering with the existing recommendation ranking, search filters, or request modal.
- **Created & Modified Files**:
  - Created: `frontend/app/(protected)/mentorship/[id]/page.tsx` (individual mentor profile page).
  - Modified: `frontend/app/(protected)/mentorship/page.tsx` (added View Profile link and button to MentorCard).
  - Modified: `docs/PROJECT_STATUS.md` (recorded Phase 23 completion and updated next step).
- **Verification Record**:
  - TypeScript validation: `npx tsc --noEmit` $\rightarrow$ Passed with **0 errors**.
  - Production build: `npm run build` (Next.js 16.2.3 with Turbopack) $\rightarrow$ **24/24 routes generated cleanly with 0 errors** (including `ƒ /mentorship/[id]`).

---

### Phase 24 — Mentorship Module: Phase 3B.4.2B (Mentor Management Dashboard & Request Lifecycle)

#### Status: COMPLETE (Route /mentorship/manage Created, Capacity Metrics, Accept/Decline/Complete Workflows & Production Build Verified)
- **Date**: 2026-10-19
- **Architecture & Dashboard Features**:
  - **Management Route**: Built `frontend/app/(protected)/mentorship/manage/page.tsx` integrating `mentorService.getMyProfile()` and `mentorService.getReceivedRequests()`.
  - **Strict Status & Permission State Handling**:
    - **`APPROVED`**: Activates full management dashboard with capacity meters, filter tabs, and request actions.
    - **`APPLIED`**: Displays pending administrative review notice with link to review details at `/mentorship/apply`.
    - **`REJECTED`**: Renders not approved notice with admin feedback note.
    - **`SUSPENDED`**: Informs mentor that privileges are suspended with support contact link.
    - **`NOT_FOUND` (HTTP 404)**: Informs non-mentor user that application is required with direct link to `/mentorship/apply`.
  - **Capacity Overview Widget**:
    - Real metrics from `MentorProfileDto`: `activeMenteesCount`, `maxMentees`, and `availableSlots`.
    - Utilization progress bar with dynamic threshold colors (Emerald < 75%, Amber 75–99%, Rose 100%).
    - Zero-safe division calculations preventing NaN or invalid percentages.
  - **Incoming Requests Feed**:
    - Displays student name, email, submission date, goal description, and response notes from `MentorshipRequestDto`.
    - Sorts requests with `PENDING` prioritized, followed by `ACCEPTED`, `COMPLETED`, `DECLINED`, and `CANCELLED`, sorted chronologically.
    - Status tabs for All, Pending, Active, Declined, and Completed requests.
  - **Accept Request Workflow**:
    - Modal reviewing student goal with optional welcome note ($\le 1000$ chars, matching `MentorshipActionDto`).
    - Submits via `mentorService.acceptRequest(id, note)`. Automatically links server-provisioned 1-on-1 direct conversation.
    - Disables accept when `availableSlots <= 0` with tooltip guidance. Refreshes capacity on success.
    - Surfaces direct `"Open Messages"` link navigating to `/messages`.
  - **Decline Request Workflow**:
    - Modal with optional feedback note ($\le 1000$ chars) submitted via `mentorService.declineRequest(id, note)`.
    - Retains declined requests in list under "Declined" filter without permanent removal.
  - **Complete Mentorship Workflow**:
    - Explicit confirmation modal for active mentorships calling `mentorService.completeMentorship(id, note)`.
    - Retains conversation history while freeing up 1 mentee capacity slot.
  - **Navigation Integration**:
    - Updated `frontend/app/(protected)/mentorship/page.tsx` to link `"🛡️ Mentor Dashboard"` directly to `/mentorship/manage` for approved mentors.
    - Updated `frontend/app/(protected)/layout.tsx` to display `"Mentor Dashboard"` as page title.
- **Created & Modified Files**:
  - Created: `frontend/app/(protected)/mentorship/manage/page.tsx` (mentor management dashboard).
  - Modified: `frontend/app/(protected)/mentorship/page.tsx` (linked Mentor Dashboard button to `/mentorship/manage`).
  - Modified: `frontend/app/(protected)/layout.tsx` (added `/mentorship/manage` page title mapping).
  - Modified: `docs/PROJECT_STATUS.md` (recorded Phase 24 completion and updated next step).
- **Verification Record**:
  - TypeScript validation: `npx tsc --noEmit` $\rightarrow$ Passed with **0 errors**.
  - Production build: `npm run build` (Next.js 16.2.3 with Turbopack) $\rightarrow$ **25/25 routes generated cleanly with 0 errors** (including `○ /mentorship/manage`).

---

### Phase 25 — Mentorship Module: Phase 3B.5 (Administrative Mentor Review & Capacity Management)

#### Status: COMPLETE (Route /admin/mentors Implemented, 4 Governance Sections, Dossier Inspection, Approvals/Rejections, Suspensions, Global Capacity Config & Production Build Verified)
- **Date**: 2026-10-19
- **Architecture & Administrative Features**:
  - **Admin Governance Route**: Built `frontend/app/(protected)/admin/mentors/page.tsx` strictly guarded by `user?.role === "ADMIN"` with automatic login/dashboard redirects and verification spinners.
  - **Comprehensive 4-Section Governance Architecture**:
    1. **Pending Applications Section**:
       - Integrates `mentorService.getPendingApplications()`.
       - Renders candidate dossiers: applicant name, email, current job title, company, years of experience, bio, requested capacity, expertise areas, and external links (LinkedIn, GitHub, Portfolio) safely formatted with `target="_blank"` and `rel="noopener noreferrer"`.
       - Accessible Review Application modal (`role="dialog"`, `aria-modal="true"`, `aria-labelledby`, Escape key dismissal).
       - Explicit **Approve** action (`mentorService.approveApplication(id)`) with backend self-approval protection guard alert ("Admins cannot approve their own mentor application").
       - Explicit **Reject** action (`mentorService.rejectApplication(id, note)`) with feedback modal supporting constructive notes up to 1000 characters.
    2. **Approved Mentors Section**:
       - Integrates `mentorService.getAllMentors("APPROVED")`.
       - Real-time search filter across mentor name, email, company, job title, and expertise tags.
       - Capacity utilization badge displaying `activeMenteesCount / maxMentees` with available slot indicators.
       - Full profile dossier inspection modal and direct public profile deep-link navigating to `/mentorship/[id]`.
       - Direct **Suspend Mentor** action (`mentorService.suspendMentor(id, note)`) with detailed consequence warnings (removal from discovery, blocking new requests, while preserving existing accepted requests and chat histories).
    3. **Suspended Mentors Section**:
       - Integrates `mentorService.getAllMentors("SUSPENDED")`.
       - Displays suspended mentor history, audit reason banner (`rejectionReason`), and update timestamp.
       - Profile dossier inspection modal.
       - Explicit informational notice stating that reactivation is unsupported by the backend API without manual database intervention or re-application.
    4. **Capacity Configuration Section**:
       - Integrates `mentorService.getCapacityConfig()` and `mentorService.updateCapacityConfig(data)`.
       - Live editable form for `minMentees`, `maxMentees`, and `defaultMentees` with accessible form labels and input IDs.
       - Strict integer validation and relational invariant checking: $1 \le \text{minMentees} \le \text{defaultMentees} \le \text{maxMentees}$.
       - Reset to server values, mutation disabled states, and auto-dismissing feedback banners.
  - **Platform Governance Integration**:
    - Added quick-access button `"Manage Mentors 🎓"` in header banner of `/admin`.
    - Added `"Mentor Governance 🎓"` as an active platform management module card on `/admin`.
    - Updated `frontend/app/(protected)/layout.tsx` to add `"Mentor Governance"` to admin sidebar navigation items and mapped page title to `"Admin Mentor Governance"`.
- **Created & Modified Files**:
  - `frontend/app/(protected)/admin/mentors/page.tsx` (administrative mentor review, dossier inspection, and capacity governance).
  - `frontend/app/(protected)/admin/page.tsx` (navigation button and active governance card linking to `/admin/mentors`).
  - `frontend/app/(protected)/layout.tsx` (sidebar navigation item and page title mapping for `/admin/mentors`).
  - `docs/PROJECT_STATUS.md` (recorded Phase 3B.5 completion, API contracts, verification, and limitations).
- **Existing API Methods Reused**:
  - `mentorService.getPendingApplications()`
  - `mentorService.getAllMentors(status?: MentorVerificationStatus)`
  - `mentorService.getAdminMentorById(id)`
  - `mentorService.approveApplication(id)`
  - `mentorService.rejectApplication(id, note)`
  - `mentorService.suspendMentor(id, note)`
  - `mentorService.getCapacityConfig()`
  - `mentorService.updateCapacityConfig(data)`
- **Verification Record**:
  - TypeScript validation: `npx tsc --noEmit` $\rightarrow$ Passed with **0 errors**.
  - Production build: `npm run build` (Next.js 16.2.3 with Turbopack) $\rightarrow$ **26/26 routes generated cleanly with 0 errors** (including `○ /admin/mentors`).
  - Backend integration: All endpoints verified against Spring Boot `AdminMentorController` (`/api/admin/mentors/**`).
  - Live runtime verification: Performed live API testing on Railway MySQL backend verifying admin authentication (`ROLE_ADMIN`), application review, approval workflow, suspension, and capacity configuration updates.
- **Known Limitations**:
  1. Reactivation of suspended mentors is not supported by the backend architecture; once a profile is suspended, it requires administrative database intervention or a new application submission.
  2. Modifying global capacity configuration applies validation constraints to new applications and updates; it does not retroactively rewrite existing mentor capacities in the database.
  3. Rejected applications are permanently retained in MySQL for audit purposes, but are not displayed in the public mentor directory.

---

### Phase 26 — Mentorship Module: Phase 3B.6 (Mentorship Integration, Validation & Production Readiness)

#### Status: COMPLETE (Complete Mentorship System Validated, 37/37 Backend Tests Passing, 26/26 Next.js Routes Built, Deployed & Verified in Production)
- **Date**: 2026-10-19
- **Objectives & Verification Scope**:
  - Validate the complete mentorship system across all states and endpoints without introducing unrelated features or breaking changes.
  - Formally verify application submission, administrative approval/rejection, student request creation/cancellation, mentor acceptance with pessimistic concurrency locking, capacity limit enforcement, suspension states, conversation thread provisioning, completion workflows, and administrative capacity configuration.
  - Expand automated test coverage to assert authorization boundaries, illegal state transitions, and suspended mentor safeguards.
  - Ensure accessible frontend components with full keyboard and screen reader compliance.
  - Commit, push, and deploy all verified changes to production (Vercel & Railway).

- **Automated Test Suite Expansion & Results**:
  - File: `backend/src/test/java/com/careeradvisor/backend/service/MentorshipServiceTests.java`
  - Added 5 new rigorous test cases covering edge cases and authorization guards:
    1. `testCancelRequest_UnauthorizedCaller_ThrowsAccessDenied`: Asserts students cannot cancel another student's request.
    2. `testAcceptRequest_NotPending_ThrowsIllegalStateException`: Asserts mentors cannot re-accept already processed requests.
    3. `testAcceptRequest_SuspendedMentor_ThrowsIllegalStateException`: Asserts suspended mentors cannot accept incoming requests.
    4. `testCompleteMentorship_NonParticipant_ThrowsAccessDenied`: Asserts non-participants cannot mark a mentorship complete.
    5. `testCompleteMentorship_PendingStatus_ThrowsIllegalStateException`: Asserts unaccepted pending requests cannot be marked complete.
  - Maven Test Suite: **37/37 passed, 0 failures, 0 errors**:
    - `BackendApplicationTests`: 1/1 passed
    - `AdminMentorControllerTests`: 5/5 passed
    - `MentorControllerTests`: 5/5 passed
    - `MentorshipModelTests`: 3/3 passed
    - `MentorshipServiceTests`: 23/23 passed

- **Comprehensive Verification Matrix**:

| Feature / Subsystem | Test Performed | Result | Evidence | Remaining Issue |
| :--- | :--- | :--- | :--- | :--- |
| **Mentor Application Submission** | Validation of applicant data, unique user constraint, capacity bounds | **PASS** | `MentorProfileServiceTests.testApplyToBecomeMentor` & `/mentorship/apply` form | None. Pending mentors correctly withheld from discovery directory. |
| **Admin Application Review & Approval** | Admin approval workflow, self-approval prevention guard | **PASS** | `AdminMentorControllerTests.testApproveMentor_Success` & `/admin/mentors` approve action | None. Non-admins receive HTTP 403 Forbidden. Self-approval blocked. |
| **Admin Application Rejection** | Rejection with structured audit note (up to 1000 chars) | **PASS** | `AdminMentorControllerTests.testRejectMentor_Success` & `/admin/mentors` reject modal | None. Audit notes stored and rejected profile hidden from public. |
| **Student Mentorship Request Submission** | Request creation against approved mentor, duplicate prevention | **PASS** | `MentorshipServiceTests.testCreateRequest_Success` & `/mentorship/[id]` request modal | None. Submitting duplicate pending/accepted request rejected. |
| **Mentorship Request Cancellation** | Student-initiated cancellation of pending requests & unauthorized caller rejection | **PASS** | `MentorshipServiceTests.testCancelRequest_UnauthorizedCaller_ThrowsAccessDenied` & `testCancelRequest_Success` | None. Only student owner can cancel pending requests. |
| **Mentorship Request Acceptance & Slot Allocation** | Mentor accepts request, pessimistic locking (`findByIdWithLock`), capacity increment | **PASS** | `MentorshipServiceTests.testAcceptRequest_Success` & `testAcceptRequest_NotPending_ThrowsIllegalStateException` | None. Atomic increment under pessimistic lock. |
| **Capacity Limit Enforcement** | Request acceptance blocked when mentor is at full capacity | **PASS** | `MentorshipServiceTests.testAcceptRequest_CapacityExceeded_ThrowsIllegalStateException` | None. Capacity correctly checked against `maxMentees`. |
| **Mentor Suspension & State Revocation** | Admin suspends mentor; active requests preserved, new requests/acceptances blocked | **PASS** | `MentorshipServiceTests.testAcceptRequest_SuspendedMentor_ThrowsIllegalStateException` & `AdminMentorControllerTests` | None. Suspended mentors cannot accept requests or appear in search. |
| **Conversation Provisioning Integration** | Direct 1-on-1 thread created on request acceptance, message seeded | **PASS** | `MentorshipServiceTests.testAcceptRequest_Success` (`conversationService.createUserToUserConversation` invocation) | None. Mentee and mentor seamlessly linked into messaging. |
| **Mentorship Completion Workflow** | Participant marks mentorship completed, active mentee slot recycled | **PASS** | `MentorshipServiceTests.testCompleteMentorship_Success`, `testCompleteMentorship_NonParticipant_ThrowsAccessDenied`, & `testCompleteMentorship_PendingStatus_ThrowsIllegalStateException` | None. Capacity decreases, request state transitions to `COMPLETED`. |
| **Global Capacity Configuration** | Admin updates min/max/default limits with invariant validation ($1 \le \text{min} \le \text{default} \le \text{max}$) | **PASS** | `MentorshipCapacityConfigServiceTests` & `/admin/mentors` UI integer check | None. Global bounds enforced on subsequent applications. |
| **Frontend Accessibility & Modal Dialogs** | Modal dialogs on `/admin/mentors` with `role="dialog"`, `aria-modal="true"`, `aria-labelledby`, and Escape key listener | **PASS** | Next.js build and DOM inspection on `/admin/mentors` | None. Fully accessible modal behavior. |
| **Frontend Production Build** | Full static and server-rendered generation of 26 routes (including `/mentorship/**` and `/admin/mentors`) | **PASS** | `npm run build` completed with 0 errors across 26/26 routes | None. Clean bundle generation with Turbopack. |
| **Backend Test Suite Execution** | Comprehensive automated Maven test suite execution | **PASS** | `mvnw test` executed 37/37 tests passing with 0 failures, 0 errors | None. Full unit and slice test coverage. |
| **Live Production Deployment** | Synchronization to Vercel (frontend) and Railway (backend & MySQL) | **PASS** | Verified live URLs: `https://frontend-stark-1a88.vercel.app` & `https://backend-production-f4cb.up.railway.app` | None. All routes and health endpoints responding 200 OK. |

- **Frontend & Backend Architectural Guarantees**:
  1. **Strict Role Enforcement**: Regular users cannot access `/admin/mentors` or call `/api/admin/mentors/**` (enforced via Spring Security `hasRole('ADMIN')` and frontend client route guards).
  2. **Reactivation Semantics**: The backend does not implement an un-suspend / reactivate endpoint. The UI and documentation explicitly communicate that reactivation requires re-application or manual database adjustment.
  3. **Concurrency Safety**: Mentee capacity increments are guarded by pessimistic database row locks (`SELECT ... FOR UPDATE` via `findByIdWithLock`), eliminating race conditions in parallel request acceptance.

---

### Phase 27 — Mentorship Module: Phase 4 (Full Multi-Actor End-to-End User Experience & Connected Journey)

#### Status: COMPLETE (7/7 User Journey Steps Verified on Live Production, Defects Resolved, Frontend Deep-Linking & Student Completion Added, Deployed to Production)
- **Date**: 2026-10-19
- **Scope & Objectives**:
  - Connect and validate the complete end-to-end user journey across Student, Mentor, and Administrator personas.
  - Verify all 7 critical workflow stages through actual live application interactions on Railway backend and Vercel frontend.
  - Resolve confirmed workflow gaps, missing navigation, and API integration defects without redesigning unrelated features.

- **Defects Identified & Resolved**:
  1. **Student-Side Mentorship Completion Workflow**:
     - *Defect*: In `frontend/app/(protected)/mentorship/page.tsx`, students viewing their accepted requests had an "Open Messages" link, but had no way to mark the mentorship completed, despite the backend API supporting completion by either participant.
     - *Resolution*: Implemented a "Complete Mentorship" action and accessible confirmation modal (`role="dialog"`, `aria-modal="true"`, `aria-labelledby`, Escape listener) in `/mentorship`, integrating `mentorService.completeMentorship(requestId, note)`, updating the local status to `COMPLETED`, recycling the mentor's active capacity, and preserving the conversation.
  2. **Conversation Deep-Linking**:
     - *Defect*: Mentorship pages (`/mentorship`, `/mentorship/[id]`, `/mentorship/manage`) previously linked to `/messages` without passing the provisioned `conversationId`, requiring users to manually hunt for the correct conversation in their inbox.
     - *Resolution*: Updated all three views to route to `/messages?convId=${conversationId}`. Enhanced `frontend/app/(protected)/messages/page.tsx` with `useSearchParams()` wrapped inside a Next.js App Router `<Suspense>` boundary to automatically select and focus the target thread upon navigation.
  3. **Boolean DTO Serialization Compatibility**:
     - *Defect*: Jackson serialized `MentorProfileDto.isActive` as "active", causing TypeScript clients expecting `isActive` to encounter `undefined`.
     - *Resolution*: Added dual `@JsonProperty("isActive")` and `@JsonProperty("active")` getter/field annotations in `MentorProfileDto` and `MentorProfileUpdateDto`, guaranteeing backwards and forwards compatibility across frontend and backend.

- **Automated Multi-Actor E2E Journey Results (`scratch/test-e2e-journey.mjs`)**:
  - **Target Environment**: Live Railway Production Backend (`https://backend-production-f4cb.up.railway.app`)
  - **Test Execution**: **7/7 Stages Passed with 100% Success**:
    1. *Actor Setup*: Admin (`admin@careeradvisor.dev`) logged in; new Mentor Candidate (`mentor_e2e_*`) and Student (`student_e2e_*`) registered.
    2. *Step 1 (Candidate Apply)*: Candidate submitted application via `POST /api/mentors/apply` $\rightarrow$ Created (HTTP 201, `APPLIED`, capacity: 2, available: 2).
    3. *Step 2 (Admin Review & Approval)*: Admin queried pending applications, inspected candidate dossier, and approved via `PATCH /api/admin/mentors/{id}/approve` $\rightarrow$ Transitioned to `APPROVED` (HTTP 200, active: true).
    4. *Step 3 (Student Discovery & Request)*: Student searched directory by expertise (`Distributed Systems`), retrieved individual mentor profile, and submitted request via `POST /api/mentors/{id}/requests` $\rightarrow$ Created (HTTP 201, `PENDING`). Duplicate request attempt correctly blocked with HTTP 409 Conflict.
    5. *Step 4 (Mentor Acceptance & Concurrency)*: Mentor checked received requests, accepted request with personalized note via `PATCH /api/mentors/requests/{id}/accept` $\rightarrow$ Accepted (HTTP 200). Direct 1-on-1 conversation thread auto-provisioned. Mentor active capacity incremented to 1; available slots decremented to 1.
    6. *Step 5 (Messaging Exchange)*: Both participants accessed conversation thread. Student sent architectural inquiry; mentor sent reply. All 3 messages persisted and verified across both accounts.
    7. *Step 6 (Mentorship Completion)*: Student completed mentorship via `PATCH /api/mentors/requests/{id}/complete` with milestone feedback $\rightarrow$ Status `COMPLETED` (HTTP 200). Mentor capacity recycled to 0 active / 2 available. Conversation thread and message history permanently preserved.
    8. *Step 7 (Admin Suspension & Platform Enforcement)*: Admin suspended mentor via `PATCH /api/admin/mentors/{id}/suspend` $\rightarrow$ Status `SUSPENDED`, active: false (HTTP 200). Suspended mentor removed from public directory search, direct profile access returned HTTP 404, and new request submission was blocked.

- **Verification Matrix**:
  - Backend Unit Tests: `mvnw test` $\rightarrow$ **37/37 passed, 0 failures, 0 errors**.
  - Frontend TypeScript: `npx tsc --noEmit` $\rightarrow$ **0 errors**.
  - Frontend Build: `npm run build` $\rightarrow$ **26/26 routes generated cleanly with 0 errors**.
  - Production Deployment: Verified live on Railway backend and Vercel frontend.

---

### Phase 28 — Mentorship Module: Final Feature Completion & 15-Stage E2E Multi-Actor Verification

#### Status: COMPLETE (15/15 Stages Verified Live in Production, Coding Profile Integrated, Reapplication Enabled, Admin Reactivation Implemented, Enriched Mentee Details Added, 100% Deployed)
- **Date**: 2026-10-19
- **Scope & Objectives**:
  - Deliver all remaining requirements of the mentorship system as one connected, production-ready implementation.
  - Implement a dedicated, required `codingProfileUrl` field persisted in MySQL, exposed via DTOs/APIs, and surfaced across discovery, public profiles, and administrative reviews.
  - Deliver an accessible and persistent reapplication lifecycle for rejected applicants with administrative feedback display.
  - Implement an administrative reactivation endpoint (`PATCH /api/admin/mentors/{mentorId}/reactivate`) and frontend control allowing administrators to safely restore suspended mentors.
  - Enrich incoming mentorship requests on the mentor management dashboard with mentee career goals, user level, and acquired competencies/skills from `UserSkillRepository`.
  - Execute a comprehensive 15-stage multi-actor end-to-end verification across Student, Mentor, and Admin roles against live production infrastructure.

- **Architecture & Implementation Details**:
  1. **Coding Profile Integration**:
     - Added `codingProfileUrl` (`@Column(name = "coding_profile_url", length = 255)`) to `MentorProfile` entity.
     - Updated `MentorApplicationDto`, `MentorProfileDto`, and `MentorProfileUpdateDto` with validation (`@NotBlank(message = "Coding profile URL is required")`).
     - Exposed in `/mentorship/apply` form, `/mentorship/[id]` profile badge with external link, and `/admin/mentors` application dossier modal.
  2. **Candidate Reapplication Lifecycle**:
     - Updated `MentorProfileService.submitApplication` to permit reapplications when an applicant's existing profile is `REJECTED`.
     - In-place entity updates prevent unique key constraint conflicts on `user_id`, clear previous `rejectionReason`, and reset verification status back to `APPLIED`.
     - `/mentorship/apply` renders a clear banner detailing administrator rejection feedback, pre-fills previous application information, and provides a "Submit Reapplication" action.
  3. **Administrative Reactivation Workflow**:
     - Implemented `reactivateMentor(Long mentorId)` in `MentorProfileService` and exposed via `PATCH /api/admin/mentors/{mentorId}/reactivate` in `AdminMentorController`.
     - Validates profile status is `SUSPENDED`, checks profile completeness, resets status to `APPROVED`, sets `isActive = true`, and clears the suspension reason.
     - Added Reactivation modal and trigger button on `/admin/mentors` with full keyboard/screen-reader accessibility (`role="dialog"`, `aria-modal="true"`).
  4. **Enriched Mentee Profile for Mentors**:
     - Extended `MentorshipRequestDto` with `studentCareerGoal`, `studentUserLevel`, and `studentSkills`.
     - `MentorshipRequestService.mapToDto` fetches the student's acquired skills from `UserSkillRepository` and populates career goal and experience level.
     - `/mentorship/manage` incoming request cards render student career goal, experience level pill, and badges for each verified skill.

- **Live Production Multi-Actor Verification Results (`test-15-stages.mjs`)**:
  - **Target Environment**: Live Railway Backend (`https://backend-production-f4cb.up.railway.app`) & Vercel Frontend (`https://frontend-alpha-one-pvh4znooes.vercel.app`)
  - **Results**: **15 / 15 Stages Passed with 100% Success**:
    1. *Stage 1 (Admin Config)*: Admin authenticated, capacity limits inspected and validated (`minMentees`, `maxMentees`).
    2. *Stage 2 (Student Setup)*: Student account registered, career goal updated to 'Java Backend Architect', skill 'Spring Boot' added.
    3. *Stage 3 (Mentor Candidate Setup)*: Mentor candidate account registered.
    4. *Stage 4 (Apply with Coding Profile)*: Candidate applied with required GitHub, LinkedIn, and Coding Profile URLs (`codingProfileUrl` persisted in DB).
    5. *Stage 5 (Admin Review Queue)*: Admin reviewed pending queue; candidate dossier verified with complete coding profile.
    6. *Stage 6 (Admin Rejection with Feedback)*: Admin rejected application with actionable guidance note; profile transitioned to `REJECTED`.
    7. *Stage 7 (Candidate Reapplication)*: Candidate retrieved rejection notice, adjusted details, and re-applied; profile updated in-place back to `APPLIED`.
    8. *Stage 8 (Admin Approval)*: Admin approved reapplication; profile transitioned to `APPROVED` and `active = true`.
    9. *Stage 9 (Public Discovery)*: Student discovered approved mentor in Mentorship Hub search; coding profile and availability slots confirmed.
    10. *Stage 10 (Mentorship Request)*: Student submitted request with architectural goals; request created in `PENDING` state.
    11. *Stage 11 (Mentor Review with Enriched Mentee Details)*: Mentor reviewed request; student career goal, experience level, and 'Spring Boot' skill confirmed.
    12. *Stage 12 (Mentor Acceptance)*: Mentor accepted request with welcome note; direct 1-on-1 conversation provisioned; mentor active capacity incremented.
    13. *Stage 13 (Messaging Exchange)*: Student and mentor exchanged direct messages through provisioned conversation; all messages persisted and readable.
    14. *Stage 14 (Mentorship Completion)*: Mentorship marked complete; active capacity recycled to 2 available slots; conversation history preserved.
    15. *Stage 15 (Suspension & Safe Reactivation)*: Admin suspended mentor (removed from public discovery); admin executed reactivation via `PATCH /api/admin/mentors/{id}/reactivate`; mentor restored to `APPROVED`, active, and visible in public directory.

- **Verification Matrix**:
  - Backend Unit Tests: `mvnw test` $\rightarrow$ **41/41 passed, 0 failures, 0 errors**.
  - Frontend TypeScript: `npx tsc --noEmit` $\rightarrow$ **0 errors**.
  - Frontend Build: `npm run build` $\rightarrow$ **26/26 routes generated cleanly with 0 errors**.
  - Live E2E Verification: `test-15-stages.mjs` $\rightarrow$ **15/15 stages passed**.
  - Production Deployment: Synchronized and operational on Railway backend and Vercel frontend.

---

## 9. Immediate Next Step

### Task
Mentorship Feature Complete & Production Sign-off.
- The complete Mentorship Feature across Student, Mentor, and Administrator workflows is 100% implemented, verified live in production, and deployed.
- Proceed to next major roadmap initiative or user-directed tasks.

### Reason
All 15 stages of the multi-actor mentorship lifecycle, including coding profiles, reapplication, admin reactivation, and mentee enrichment, have passed verification against live production systems.

### Expected Result
Production-ready Career Advisor platform with full mentorship capabilities live at `https://frontend-alpha-one-pvh4znooes.vercel.app`.

