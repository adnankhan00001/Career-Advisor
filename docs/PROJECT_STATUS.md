# Career-Advisor Project Status & Comprehensive Development History

> **Permanent Single Source of Truth**  
> This document tracks the complete development history, system architecture, feature matrix, testing records, and viva-ready engineering specifications for the Career-Advisor project.  
> **Status**: Production Stabilized & Viva Ready (Phase 12 COMPLETE)

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

---

## 6. Immediate Next Step

### Task
Phase 12 (RBAC + Admin Security Foundation + Platform Governance) is 100% COMPLETE and thoroughly verified. Awaiting user review and instructions for future phases.
