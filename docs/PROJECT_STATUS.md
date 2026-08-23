# Career-Advisor Project Status & Comprehensive Development History

> **Permanent Single Source of Truth**  
> This document tracks the complete development history, system architecture, feature matrix, testing records, and viva-ready engineering specifications for the Career-Advisor project.  
> **Status**: Production Ready, Hardened & Deployed Foundation (Phase 13 COMPLETE)

---

## 1. Project Overview

**Career-Advisor** is a full-stack career guidance, skill evaluation, in-browser coding workspace, and interview preparation platform designed to help students and developers discover tech career paths, assess their skills through interactive quizzes, generate personalized step-by-step learning roadmaps, track skill acquisition, receive personalized AI/rule-driven career & interview intelligence, practice interactive coding challenges in an in-browser IDE, and complete timed technical mock interviews.

### Core Technology Stack

- **Frontend**: Next.js 16.2.3 (App Router, Turbopack), React 19.2.4, Tailwind CSS 4, Framer Motion
- **Backend**: Spring Boot 3.3.4, Java 17, Spring Security 6, JJWT 0.12.6, Spring Data JPA, Maven
- **Database**: MySQL 8.0 (Database: `career_advisor`)
- **Communication**: REST APIs (JSON) over HTTP/HTTPS with CORS configuration for `http://localhost:3000`

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
│  │           interviewService) — apiClient with Bearer Token         │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
└─────────────────────────────────────┼───────────────────────────────────┘
                                      │
                     HTTP POST / GET / DELETE (JSON REST)
                    (Authorization: Bearer <JWT>)
                                      │
┌─────────────────────────────────────▼───────────────────────────────────┐
│                    Spring Boot 3.3.4 Backend @ Port 8080                │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                     JwtAuthenticationFilter                       │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
│                                     │                                   │
│  ┌───────────────┬──────────────────┼───────────────┬────────────────┐  │
│  │ AuthContr.    │ CareerController │ RoadmapContr. │ SkillContr.    │  │
│  │ (/api/auth/**)│ (/api/careers/**)│ (/api/roadmaps│ (/api/skills)  │  │
│  └───────┬───────┴──────────┬───────┴───────┬───────┴────────┬───────┘  │
│          │                  │               │                │          │
│          │       ┌──────────┴───────────────┴────────┐       │          │
│          │       │ ProgressContr. (/api/progress/**) │       │          │
│          │       │ ProblemContr.  (/api/problems/**) │       │          │
│          │       │ QuizController (/api/quiz/**)     │       │          │
│          │       │ RecommendationContr. (/api/recom.)│       │          │
│          │       │ InterviewController (/api/interv.)│       │          │
│          │       └──────────┬────────────────────────┘       │          │
│          │                  │                                │          │
│  ┌───────▼───────┐   ┌──────▼───────────────┐   ┌────────────▼───────┐  │
│  │  UserService  │   │ UserProgressService  │   │  UserSkillService  │  │
│  │ CareerService │   │    ProblemService    │   │    QuizService     │  │
│  │ RoadmapService│   │RecommendationService │   │  InterviewService  │  │
│  └───────┬───────┘   └──────┬───────────────┘   └────────────┬───────┘  │
│          │                  │                                │          │
│  ┌───────▼──────────────────▼────────────────────────────────▼───────┐  │
│  │ Repositories (UserRepository, UserSkillRepository,                │  │
│  │   UserRoadmapProgressRepository, QuizAttemptRepository,           │  │
│  │   CodingProblemRepository, UserProblemProgressRepository,         │  │
│  │   MockInterviewRepository, InterviewQuestionRepository,           │  │
│  │   InterviewAnswerRepository)                                      │  │
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
│             interview_answers                                           │
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

## 6. Immediate Next Step

### Task
Phase 14D (WebRTC Audio & Video Calling) is 100% COMPLETE and fully verified across all 340 automated tests. Ready for pre-commit review upon user instruction.
