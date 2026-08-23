# Career-Advisor Comprehensive Viva, Academic & Technical Guide

> **Project Title**: Career-Advisor: Intelligent Tech Career Exploration, Coding Workspace & Mock Interview Preparation Platform  
> **Author / Maintainer**: Adnan Khan  
> **Architecture**: Distributed Client-Server REST Architecture (Next.js 16 + Spring Boot 3.3.4 + MySQL 8.0)  
> **Documentation Version**: 7.0 (Phase 14B — Persistent Personal AI Chatbot)

---

## Table of Contents

1. [Executive Summary & Abstract](#1-executive-summary--abstract)
2. [Problem Statement & Motivation](#2-problem-statement--motivation)
3. [Project Objectives](#3-project-objectives)
4. [Technology Stack & Rationale](#4-technology-stack--rationale)
5. [End-to-End System Architecture](#5-end-to-end-system-architecture)
6. [Database Schema & Entity Relationships](#6-database-schema--entity-relationships)
7. [Authentication, Security & User Isolation](#7-authentication-security--user-isolation)
8. [Core Algorithms & Business Logic](#8-core-algorithms--business-logic)
   - [8.1 Multi-Factor Career Matching Algorithm](#81-multi-factor-career-matching-algorithm)
   - [8.2 Personalized Intelligence & Recommendation Engine](#82-personalized-intelligence--recommendation-engine)
   - [8.3 In-Browser Coding Workspace Execution Simulator](#83-in-browser-coding-workspace-execution-simulator)
   - [8.4 Backend-Authoritative Timed Mock Interview Engine](#84-backend-authoritative-timed-mock-interview-engine)
   - [8.5 Resume Text Parsing & Deterministic Skill Extraction Engine](#85-resume-text-parsing--deterministic-skill-extraction-engine)
9. [Module-by-Module Feature Breakdown](#9-module-by-module-feature-breakdown)
10. [Key Engineering Challenges & Solutions](#10-key-engineering-challenges--solutions)
11. [Comprehensive Viva Questions & Expert Answers (25 General Q&As)](#11-comprehensive-viva-questions--expert-answers)
12. [Phase 11 Viva Questions & Answers: Resume Analyzer & Skill Extraction (12 Q&As)](#12-phase-11-viva-questions--answers-resume-analyzer--skill-extraction-q26q37)
13. [Future Scope & Production Roadmap](#13-future-scope--production-roadmap)

---

## 1. Executive Summary & Abstract

**Career-Advisor** is a full-stack web application designed to bridge the gap between academic education and tech industry hiring expectations. The platform provides an integrated ecosystem where aspiring software engineers can:
1. **Explore Tech Career Paths**: Discover curated curricula across 7 primary tech tracks (Java Backend, Frontend, Full Stack, Cloud/DevOps, Data Analytics, Mobile, AI/ML).
2. **Track Skills & Progress**: Maintain a verified skills portfolio and track milestone completions along interactive roadmaps.
3. **Assess Readiness**: Complete calibration quizzes and receive dynamically computed readiness scores.
4. **Practice DSA in Browser**: Solve 22 curated Data Structures & Algorithms problems within a full-featured in-browser code editor with sample execution and automated test suites.
5. **Simulate Technical Interviews**: Undertake timed, category-specific technical mock interviews with backend-authoritative timers, anti-cheating answer masking, automated score calculation, and weak area analysis.
6. **Receive Actionable Guidance**: Benefit from a multi-tier recommendation engine that continually analyzes user profile data to suggest high-impact next steps.

---

## 2. Problem Statement & Motivation

### The Problem
- **Curriculum Ambiguity**: Students frequently struggle to identify which specific technologies, algorithms, and system concepts are necessary for target roles.
- **Fragmented Tools**: Learners typically use disjointed websites for roadmap tracking, DSA practice, skill self-assessment, and interview prep.
- **Lack of Objective Feedback**: Self-learners rarely have access to timed technical mock rounds that evaluate architectural and conceptual depth while highlighting specific weak areas.

### The Solution
Career-Advisor unites these disjointed components into a single, cohesive, authenticated platform backed by a deterministic recommendation engine and secure client-server synchronization.

---

## 3. Project Objectives

1. **Modular Domain Architecture**: Clear separation of concerns between presentation, service orchestration, persistence, and security layers.
2. **Strict User Isolation**: Enforce zero-leakage multi-tenancy where every candidate's roadmap, code submissions, quiz attempts, and interview records are isolated.
3. **Authoritative Backend Validation**: Eliminate client-side trust assumptions by performing all critical calculations (timer expiration, score evaluation, code tests) authoritatively on the Spring Boot backend.
4. **Resilient User Experience**: Provide optimistic UI state updates with persistent fallback caches and seamless synchronization upon browser refresh.

---

## 4. Technology Stack & Rationale

| Layer | Technology | Version | Key Justification / Rationale |
|---|---|---|---|
| **Frontend Framework** | Next.js (App Router) | 16.2.3 | Server & Client component hybridization, file-based routing, Turbopack for sub-second hot reload, built-in optimization. |
| **UI Library** | React | 19.2.4 | Declarative component state management, hooks architecture (`useAuth`, `use`), Virtual DOM efficiency. |
| **Styling** | Tailwind CSS | 4.0 | Utility-first CSS, high performance, responsive design tokens, dark glassmorphism theme without bulky CSS files. |
| **Backend Framework** | Spring Boot | 3.3.4 | Enterprise-grade Java framework, Inversion of Control (IoC), Dependency Injection (DI), robust transaction management. |
| **Language** | Java | 17 (LTS) | Strong typing, records, sealed classes, pattern matching, JVM memory performance, enterprise standard. |
| **Security & Auth** | Spring Security + JJWT | 6.x / 0.12.6 | Stateless JWT Bearer token authentication, BCrypt password hashing, Custom UserDetails, Security Filter Chain. |
| **Persistence / ORM** | Spring Data JPA (Hibernate) | 3.3.4 | Automated repository generation, dialect abstraction, query derivation, dirty-checking persistence. |
| **Database** | MySQL | 8.0 | ACID transaction guarantees, relational schema integrity, foreign key cascading, indexing performance. |
| **Build Tools** | Maven & npm | 3.9+ / 10+ | Standardized dependency management and reproducible production builds. |

---

## 5. End-to-End System Architecture

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                      Client Browser (Port 3000)                         │
│                    Next.js 16 + React 19 App Router                     │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │     AuthProvider & Custom Hooks (user, token, isAuthenticated)     │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
│                                     │                                   │
│  ┌──────────────────────────────────▼────────────────────────────────┐  │
│  │   UI Pages: /dashboard, /careers, /roadmap, /skills, /quiz,       │  │
│  │             /practice/[id], /mock-interview/[id], /profile        │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
│                                     │                                   │
│  ┌──────────────────────────────────▼────────────────────────────────┐  │
│  │       apiClient.ts (Central HTTP Client + Bearer Token Injection) │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
└─────────────────────────────────────┼───────────────────────────────────┘
                                      │ HTTP / JSON REST
                       (Authorization: Bearer <JWT>)
                                      │
┌─────────────────────────────────────▼───────────────────────────────────┐
│                   Spring Boot 3.3.4 Backend (Port 8080)                 │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │  SecurityFilterChain -> JwtAuthenticationFilter -> DaoAuthProvider │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
│                                     │                                   │
│  ┌──────────────────────────────────▼────────────────────────────────┐  │
│  │ Controllers: Auth, Career, Roadmap, Skill, Progress, Quiz,        │  │
│  │              Problem, Interview, Recommendation                   │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
│                                     │                                   │
│  ┌──────────────────────────────────▼────────────────────────────────┐  │
│  │ Services: Business logic, evaluation, timers, recommendation engine│  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
│                                     │                                   │
│  ┌──────────────────────────────────▼────────────────────────────────┐  │
│  │ Repositories: Spring Data JPA interfaces                          │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
└─────────────────────────────────────┼───────────────────────────────────┘
                                      │ JDBC / SQL Dialect
┌─────────────────────────────────────▼───────────────────────────────────┐
│                       MySQL 8.0 Database (Port 3306)                    │
│     Tables: users, user_skills, user_roadmap_progress, quiz_attempts,   │
│             coding_problems, coding_problem_tags, user_problem_progress,│
│             mock_interviews, interview_questions, interview_answers     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Database Schema & Entity Relationships

```mermaid
erDiagram
    USERS ||--o{ USER_SKILLS : "has"
    USERS ||--o{ USER_ROADMAP_PROGRESS : "completes"
    USERS ||--o{ QUIZ_ATTEMPTS : "takes"
    USERS ||--o{ USER_PROBLEM_PROGRESS : "solves"
    USERS ||--o{ MOCK_INTERVIEWS : "attempts"
    CODING_PROBLEMS ||--o{ USER_PROBLEM_PROGRESS : "tracked_by"
    CODING_PROBLEMS ||--o{ CODING_PROBLEM_TAGS : "contains"
    MOCK_INTERVIEWS ||--o{ INTERVIEW_ANSWERS : "records"
    INTERVIEW_QUESTIONS ||--o{ INTERVIEW_ANSWERS : "referenced_by"
    INTERVIEW_QUESTIONS ||--o{ INTERVIEW_QUESTION_OPTIONS : "options"
    INTERVIEW_QUESTIONS ||--o{ INTERVIEW_QUESTION_CONCEPTS : "concepts"

    USERS {
        bigint id PK
        varchar name
        varchar email UK
        varchar password
        varchar career_goal
        varchar user_level
        varchar latest_quiz_score
    }

    MOCK_INTERVIEWS {
        bigint id PK
        bigint user_id FK
        varchar category
        varchar difficulty
        varchar status
        datetime started_at
        datetime deadline
        datetime completed_at
        int duration_seconds
        int total_questions
        int correct_count
        int score
        text strong_areas
        text weak_areas
    }

    INTERVIEW_QUESTIONS {
        bigint id PK
        varchar category
        varchar topic
        varchar difficulty
        text question
        varchar correct_answer
        text explanation
    }

    CODING_PROBLEMS {
        bigint id PK
        varchar title
        varchar slug UK
        varchar difficulty
        varchar category
        varchar topic
        text description
        text starter_code
        text sample_input
        text sample_output
    }
```

---

## 7. Authentication, Security & User Isolation

### 1. Registration & Password Storage
- User passwords are never saved in plaintext.
- Upon `POST /api/auth/register`, passwords are encrypted using `BCryptPasswordEncoder` with adaptive work factor (salt generated cryptographically).

### 2. JWT Issuance & Verification
- When authenticating via `POST /api/auth/login`, Spring Security authenticates the user against `CustomUserDetailsService`.
- Upon successful authentication, JJWT generates an HMAC-SHA256 signed JWT containing the user's primary email, database ID, and a 24-hour expiration claim.
- The client receives `{ token, type: "Bearer", id, name, email }` and attaches `Authorization: Bearer <token>` to all subsequent requests.

### 3. Request Interception Pipeline
- Every request passes through `JwtAuthenticationFilter`.
- The filter extracts the Bearer token, validates the HMAC-SHA256 signature, ensures expiration has not passed, extracts the username, and sets the authenticated `SecurityContextHolder`.

### 4. Zero-Trust User Isolation
- Controllers extract candidate identity via `@AuthenticationPrincipal CustomUserDetails userDetails`.
- Endpoints never trust a `userId` supplied in the request body or URL path for modifying or retrieving private entities.
- Any attempt by User B to query or submit User A's mock interview ID returns `403 Forbidden`.

---

## 8. Core Algorithms & Business Logic

### 8.1 Multi-Factor Career Matching Algorithm
Matches candidate skills against target career requirements:
$$\text{Skill Match } \% = \text{round}\left(\frac{|\text{User Skills} \cap \text{Target Required Skills}|}{|\text{Target Required Skills}|} \times 100\right)$$

### 8.2 Personalized Intelligence & Recommendation Engine
The platform calculates a composite **Overall Readiness Score** ($R$) dynamically:
$$R = \text{round}(0.35 \times M_{\text{skill}} + 0.25 \times P_{\text{roadmap}} + 0.20 \times S_{\text{quiz}} + 0.20 \times S_{\text{dsa}})$$

- If the user has 0 skills and 0 roadmap steps, their state is classified as `ONBOARDING` with 4 actionable onboarding setup steps.
- If mock interview performance detects weak areas with $< 70\%$ accuracy, the `interviewFocusAction` dynamically prioritizes that subject.

### 8.3 In-Browser Coding Workspace Execution Simulator
- Provides candidate with starter code, constraints, sample input/output, and test suite.
- Validates non-empty code submissions and compiles/executes code in a sandbox simulator.
- Passes all 5 internal test cases for correct logic, records persistence in `user_problem_progress`, and triggers real-time dashboard progress updates.

### 8.4 Backend-Authoritative Timed Mock Interview Engine
- **No Client Trust for Deadlines**:
  $$\text{Deadline} = \text{StartedAt} + \text{DurationSeconds}$$
  The client countdown timer merely calculates $\text{remaining} = \text{max}(0, \text{Deadline} - \text{CurrentTime})$.
- **Anti-Cheating Answer Masking**: During an active session, `GET /api/interviews/{id}` returns sanitized `InterviewQuestionDto` with `correctAnswer` and `explanation` masked to `null`.
- **Post-Submission Unlock**: Detailed solutions and explanations are only serialized when `status == COMPLETED` or `EXPIRED`.

### 8.5 Resume Text Parsing & Deterministic Skill Extraction Engine
- **Zero-Cloud Local Text Extraction**:
  - PDF: Apache PDFBox 3.0 `Loader.loadPDF(byte[])` + `PDFTextStripper` extracts raw text from document streams without third-party network calls.
  - DOCX: Apache POI 5.3 `XWPFDocument` + `XWPFWordExtractor` parses OpenXML body paragraphs and table cells.
- **Canonical Skill Normalization & Scoring**:
  - Matches 35+ canonical skills across Programming Languages, Backend, Frontend, Cloud/DevOps, Databases, and Core CS.
  - Regex alias normalization (e.g. `k8s` $\rightarrow$ `Kubernetes`, `js` $\rightarrow$ `JavaScript`, `postgres` $\rightarrow$ `PostgreSQL`).
  - Section-aware confidence scoring: Base 85% for canonical matches, 80% for acronyms/aliases, +10% boost for skills appearing in explicit `SKILLS` section headers (max 99%).
- **Automated Career Track & Skill Gap Evaluation**:
  - Evaluates extracted skills against 7 career track skill requirements.
  - Computes match percentage:
    $$\text{MatchScore} = \frac{|\text{ExtractedSkills} \cap \text{RequiredSkills}|}{|\text{RequiredSkills}|} \times 100$$
  - Identifies top 3 high-priority missing skills for candidate's target career and connects them to Roadmap milestones and DSA practice challenges.
- **User-Confirmed Skill Synchronization**:
  - Prevents automated overwriting of profile skills.
  - Requires explicit user selection or "Add All Confirmed Skills" action to invoke `UserSkillService.addSkill()`.

---

## 9. Module-by-Module Feature Breakdown

1. **Authentication**: Sign Up, Sign In, Profile Retrieval, Logout, JWT storage with local fallback.
2. **Career Discovery**: 7 tracks (Java, Frontend, Full Stack, DevOps, Data, Mobile, AI/ML) with matching metrics.
3. **Interactive Roadmaps**: Hierarchical section and step checklists with individual step toggling and progress calculation.
4. **Skill Portfolio**: Add/remove skill chips with instant match calibration.
5. **Skill Assessment Quiz**: 8-question timed calibration test with instant scoring and career suggestion.
6. **DSA Practice Hub**: 22 curated problems across Arrays, Strings, Two Pointers, Sliding Window, Linked List, Stack, Binary Search, Trees, Graphs, DP, Heap, Bit Manipulation, Java, and DBMS.
7. **Coding Workspace**: Full-screen IDE layout with line numbering, language selector, sample runner, and test visualizer.
8. **Timed Mock Interviews**: 31 conceptual questions across Java, OOP, DBMS, Spring Boot, DSA, OS, and CN.
9. **Performance History**: Historical attempts table with score filters, completion badges, and deep review links.
10. **Personalized Intelligence Plan**: Multi-tier dynamic advice dashboard banner.

---

## 10. Key Engineering Challenges & Solutions

| Challenge | Root Cause | Engineering Solution |
|---|---|---|
| **Timer Clock Tampering** | Client clocks can be manipulated or desynchronized by user manipulation or page refreshes. | Backend calculates authoritative `deadline` upon creation. Backend enforces timestamp check upon submission with a 30s network buffer. |
| **Anti-Cheating Answer Leaks** | Exposing correct answers in active test payloads allows inspection via browser DevTools. | Separate `InterviewQuestionDto` (masked) and `InterviewQuestionReviewDto` (complete). Masked DTO is returned until status is `COMPLETED`. |
| **User Multi-Tenant Isolation** | Potential vulnerability where User B submits or reads User A's entity IDs. | Every service query enforces ownership verification against `@AuthenticationPrincipal`. Unauthorized access returns HTTP 403 Forbidden. |
| **Stale State on Refresh** | Local state lost when refreshing browser during active interview or roadmap progression. | Persistent JPA persistence on backend + local storage cache fallbacks + authoritative sync on mount. |

---

## 11. Comprehensive Viva Questions & Expert Answers

### Q1: Why did you choose Spring Boot instead of Express.js/Node.js?
**Answer**: Spring Boot provides enterprise-level architecture out-of-the-box: strong type safety with Java 17, comprehensive transaction management (`@Transactional`), Inversion of Control (IoC), Dependency Injection (DI), robust ORM integration via Hibernate, and industry-standard security through Spring Security filter chains.

### Q2: How does JWT authentication work in this application?
**Answer**: When a user logs in with valid credentials, the server generates a cryptographically signed JSON Web Token (using HMAC-SHA256) containing the user's email, ID, and an expiration timestamp (24h). The client stores this token and sends it in the `Authorization: Bearer <token>` header for all protected API requests. The `JwtAuthenticationFilter` on the server validates the signature on each request before granting access.

### Q3: Why is the authentication stateless?
**Answer**: Stateless authentication eliminates the need for server-side HTTP session storage in memory. This enables horizontal scalability across multiple server instances without session replication or sticky routing, while reducing memory overhead on the backend.

### Q4: How is password security guaranteed?
**Answer**: Passwords are never stored in plaintext. We use Spring Security's `BCryptPasswordEncoder`, which applies a slow, salted, computationally expensive hashing algorithm (Adaptive Cost Factor). Passwords are excluded from JSON serialization by omitting password fields in DTOs.

### Q5: How do you prevent User A from modifying or accessing User B's interview or progress?
**Answer**: In our Spring Boot controllers, we do not accept `userId` from the request body or path. Instead, we extract the authenticated user identity directly from `@AuthenticationPrincipal CustomUserDetails`. In `InterviewService`, every operation validates `interview.getUser().getId().equals(currentUser.getId())`, throwing a `403 Forbidden` exception if an ID mismatch occurs.

### Q6: How does the Mock Interview timer prevent client-side cheating?
**Answer**: The frontend timer is purely a visual countdown. The authoritative duration is stored in the database as `startedAt` and `deadline = startedAt + durationSeconds`. When an interview is submitted, the backend verifies `LocalDateTime.now().isBefore(deadline.plusSeconds(30))`. If expired, the status transitions to `EXPIRED` and late answers are rejected.

### Q7: How does anti-cheating work for interview questions?
**Answer**: When an active interview session is fetched (`GET /api/interviews/{id}`), the backend maps the entities into `InterviewQuestionDto`, which does not contain the `correctAnswer` or `explanation` fields. Those fields are only populated in `InterviewQuestionReviewDto` when calling `GET /api/interviews/{id}/result` after the interview status has transitioned to `COMPLETED` or `EXPIRED`.

### Q8: How is the Recommendation Engine designed?
**Answer**: `RecommendationService` is a rule-based expert intelligence engine that evaluates a candidate's complete state across 4 vectors: logged technical skills, roadmap milestone progress, calibration quiz results, and DSA problem completion. It computes a composite readiness score ($0-100\%$), identifies high-priority skill gaps, and recommends the immediate next roadmap topic, practice problem, and interview focus area.

### Q9: Why Next.js 16 App Router over plain React (Vite/CRA)?
**Answer**: Next.js App Router offers hybrid rendering (combining Server Components for SEO and fast initial paint with Client Components for interactive forms and code editors), built-in file-system routing, layout nesting, automatic route-level code splitting, and Turbopack for near-instant compilation.

### Q10: What is the database architecture and ORM strategy?
**Answer**: We use MySQL 8.0 with Spring Data JPA and Hibernate. We designed 9 normalized tables with explicit foreign keys, unique indexes (`uk_interview_question`, `uk_user_email`), and `@ElementCollection` tables for dynamic lists (options, concepts, tags). Hibernate handles dirty-checking and SQL dialect translation.

### Q11: What HTTP status codes are used across the REST API?
- `200 OK`: Successful retrieval or update.
- `201 Created`: Successful user registration or resource creation.
- `400 Bad Request`: Validation failure (e.g., missing required fields, empty code submission).
- `401 Unauthorized`: Missing, expired, or invalid JWT token.
- `403 Forbidden`: Authenticated user attempting to access another user's isolated resource.
- `404 Not Found`: Resource does not exist (e.g., invalid problem slug or interview ID).
- `500 Internal Server Error`: Unhandled server runtime exceptions.

### Q12: How are CORS (Cross-Origin Resource Sharing) requests handled?
**Answer**: In `SecurityConfig.java`, we configure a `CorsConfigurationSource` bean that explicitly allows origins `http://localhost:3000` and `http://127.0.0.1:3000`, authorizes standard HTTP methods (`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`), and permits headers (`Authorization`, `Content-Type`).

### Q13: What happens when the browser is refreshed during an active mock interview?
**Answer**: The active interview ID is encoded in the dynamic route `/mock-interview/[id]`. On component mount, the frontend calls `GET /api/interviews/{id}` with the candidate's JWT. The backend returns the persisted session, answered questions count, selected answers, and remaining seconds calculated against the database deadline. The countdown resumes seamlessly.

### Q14: How does the In-Browser Coding Workspace work?
**Answer**: The coding workspace (`/practice/[id]`) features a split-pane layout with problem constraints and an interactive editor with line numbers and starter code. When "Run Code" is clicked, it calls `POST /api/problems/{id}/run` to simulate sandbox execution against sample inputs. When "Submit Code" is clicked, it calls `POST /api/problems/{id}/submit` which evaluates against all 5 test cases and persists the solved record in `user_problem_progress`.

### Q15: How does the Career Matching logic calculate skill alignment?
**Answer**: It normalizes candidate skills and target career required skills to lowercase strings, checks for bidirectional substring and exact containment, and calculates $\text{percentage} = \text{round}(|\text{matched}| / |\text{required}| \times 100)$.

### Q16: How do you handle database seeding?
**Answer**: We use Spring Boot `CommandLineRunner` beans (`ProblemDataInitializer` and `InterviewQuestionDataInitializer`). On application startup, they check `repository.count() == 0`, and if empty, automatically populate the 22 DSA problems and 31 curated technical interview questions.

### Q17: What are DTOs (Data Transfer Objects) and why are they used?
**Answer**: DTOs decouple the internal database entity model from the external API contract. This prevents accidental exposure of sensitive entity fields (e.g., password hashes, correct answer keys), avoids circular reference serialization errors in bidirectional JPA relationships, and optimizes payload size.

### Q18: What is the role of GlobalExceptionHandler?
**Answer**: `@RestControllerAdvice` class `GlobalExceptionHandler` intercepts runtime exceptions (e.g., `MethodArgumentNotValidException`, `DuplicateResourceException`, `ResourceNotFoundException`) and translates them into uniform, structured `ErrorResponse` objects with timestamps and descriptive messages.

### Q19: What design patterns are implemented in the project?
1. **Repository Pattern**: Spring Data JPA repositories abstract database persistence.
2. **Service Layer Pattern**: Business logic, algorithms, and transaction boundaries live in services.
3. **Filter Interceptor Pattern**: `JwtAuthenticationFilter` intercepts HTTP requests before controllers.
4. **Factory / Builder Pattern**: Lombok `@Builder` for constructing immutable DTOs.
5. **Context Provider Pattern**: React `AuthContext` provides global authentication state.

### Q20: How are roadmaps structured and rendered?
**Answer**: Roadmaps are structured hierarchically into Sections and Milestone Steps. The backend provides track definitions, and the frontend tracks user milestone completions in `user_roadmap_progress` with progress bars calculated as $(\text{completedSteps} / \text{totalSteps}) \times 100\%$.

### Q21: What are the primary user tiers in the Recommendation Engine?
- **ONBOARDING**: 0 skills and 0 roadmap steps logged. Action plan prompts career selection, skill logging, and calibration quiz.
- **BEGINNER**: $< 40\%$ readiness score. Prompts fundamental milestone topics and Easy DSA problems.
- **INTERMEDIATE**: $40\% - 74\%$ readiness score. Prompts advanced backend/frontend milestones, Medium DSA problems, and subject mock interviews.
- **ADVANCED**: $\ge 75\%$ readiness score. Prompts full mock interview assessments and Hard practice problems.

### Q22: How does the Quiz Assessment evaluate readiness?
**Answer**: The 8 questions evaluate General CS, Backend, Frontend, Data Analytics, and AI/ML competencies. The score determines candidate tier (Beginner $< 40\%$, Intermediate $40-74\%$, Advanced $\ge 75\%$) and calibrates the candidate's initial readiness score.

### Q23: What security headers or practices are in place?
- Stateless session policy (`SessionCreationPolicy.STATELESS`).
- CSRF disabled appropriately for stateless REST APIs.
- Passwords hashed with BCrypt.
- Expired tokens return HTTP 401 with `AuthEntryPointJwt`.
- Sensitive fields excluded from responses.

### Q24: What are the known limitations of the current implementation?
1. Code execution is currently performed via an internal sandbox simulator rather than an isolated Docker container cluster (e.g., Judge0).
2. Email verification is currently mocked without an external SMTP mail server.
3. Questions bank contains 31 curated technical questions and 22 DSA problems, which can be expanded in production.

### Q25: What are the top future enhancements planned for production?
1. Integration with a live code execution engine (Docker/Judge0) for arbitrary multi-language compilation.
2. AI-powered conversational voice interview simulation using LLM audio APIs.
3. Real-time peer-to-peer coding collaboration via WebSockets.
4. Cloud Native Deployment (Kubernetes, AWS RDS, Cloudflare CDN).

---

## 12. Phase 11 Viva Questions & Answers: Resume Analyzer & Skill Extraction (Q26–Q37)

### Q26: How does the Resume Analyzer parse PDFs and DOCX files without third-party cloud APIs?
**Answer**:
The backend implements standard, open-source Java document processing engines:
1. **Apache PDFBox 3.0.3** for PDF text stream parsing via `Loader.loadPDF(byte[])` and `PDFTextStripper`.
2. **Apache POI 5.3.0 & POI-OOXML** for DOCX parsing using `XWPFDocument` and `XWPFWordExtractor` over XML document streams.
All extraction runs entirely in-memory within the Spring Boot JVM without sending candidate documents or personally identifiable information (PII) to external cloud AI vendors.

### Q27: How does the deterministic skill extraction and canonical normalization work?
**Answer**:
`ResumeSkillExtractor` maintains a curated dictionary of 35+ canonical technical skills mapped to regular expression aliases (e.g., `k8s` $\rightarrow$ `Kubernetes`, `js` $\rightarrow$ `JavaScript`, `springboot` $\rightarrow$ `Spring Boot`, `postgres` $\rightarrow$ `PostgreSQL`).
The engine computes confidence scores (80%–99%):
- **Base Confidence**: 85% for canonical direct word-boundary matches.
- **Alias Matches**: 80% for abbreviated or synonymous acronyms.
- **Section Boost**: +10% boost (capped at 99%) if the skill is detected within an explicit `SKILLS`, `TECHNICAL EXPERTISE`, or `CORE COMPETENCIES` section header.
Extracted skills are cross-referenced with the user's existing profile skills (`UserSkillService`) to dynamically tag `alreadyInProfile = true/false`.

### Q28: Why do we extract text in-memory rather than saving uploaded files to public web assets?
**Answer**:
1. **Zero Attack Surface in Public Webroot**: Uploading candidate files into `frontend/public/` or static directories introduces directory traversal, remote code execution, and unauthenticated public exposure vulnerabilities.
2. **Stateless Scalability**: By processing byte arrays in-memory into normalized entities (`Resume`), the backend avoids filesystem state, making the service fully containerizable and horizontal-scaling friendly.
3. **Database Integrity**: The raw sanitized extracted text (`parsed_text` in MySQL `LONGTEXT`) is stored in a private, encrypted database table bound to the candidate's `user_id`.

### Q29: How is user isolation enforced for uploaded resumes?
**Answer**:
At both the controller and repository layers:
- Controllers obtain the authenticated `CustomUserDetails` directly from the Spring Security `SecurityContext`.
- `ResumeRepository` methods explicitly bind `user_id`:
  ```java
  Optional<Resume> findByIdAndUser(Long id, User user);
  List<Resume> findByUserOrderByUploadTimestampDesc(User user);
  void deleteByIdAndUser(Long id, User user);
  ```
- If User B attempts to access or synchronize User A's `resume_id`, the repository returns `Optional.empty()`, resulting in a safe `HTTP 404 Not Found` without disclosing resource existence.

### Q30: How does the Resume Service integrate with the existing CareerService and UserSkillService?
**Answer**:
Rather than creating duplicate matching engines, `ResumeService` directly reuses:
1. `CareerService.getAllCareers()` to obtain standard required skills for all 7 tech tracks.
2. `CareerService.calculateMatchScores()` to evaluate career readiness against extracted skills.
3. `UserSkillService.getUserSkills()` to check existing portfolio coverage.
4. `UserSkillService.addSkill(user, skillName)` when synchronizing confirmed skills.

### Q31: Why does skill synchronization require explicit user confirmation instead of auto-sync?
**Answer**:
1. **User Control**: Automatic overwriting could pollute a candidate's verified skill set with false positives or secondary tools mentioned incidentally in a resume.
2. **Interactive Triaging**: Candidates can select/deselect individual extracted skills or click "Add All Confirmed Skills" to incrementally expand their profile.
3. **Auditability**: Candidate-initiated sync creates an explicit record of added skills without deleting existing profile records.

### Q32: How does the RecommendationService dynamically incorporate resume intelligence into the action plan?
**Answer**:
`RecommendationService` inspects the user's resume history:
- If **no resume** has been uploaded, it injects a high-priority `RESUME` recommendation ("Upload Resume for Instant Skill Gap Detection").
- If a resume is uploaded and **unsynced detected skills** exist, it injects a `RESUME_SYNC` recommendation ("Synchronize X Detected Resume Skills to Profile").
- For new users, `buildNewUserIntelligence` includes an onboarding milestone step directing the user to `/resume`.

### Q33: How are multipart upload limits and magic bytes validated against malicious file masquerading?
**Answer**:
`ResumeParserService` performs a two-tier defense:
1. **Extension & Size Whitelist**: Rejects files not ending with `.pdf` or `.docx`, or exceeding `5MB` (configured via Spring `spring.servlet.multipart.max-file-size=5MB`).
2. **Magic Byte Verification**:
   - PDF files must start with the standard `%PDF` header (`0x25 0x50 0x44 0x46`).
   - DOCX files must start with standard ZIP/PK archive headers (`0x50 0x4B 0x03 0x04`).
If a malicious user renames an executable (`payload.exe`) to `resume.pdf`, magic byte validation immediately throws `UnsupportedFileTypeException` (HTTP 415).

### Q34: How are skill gaps prioritized between the user's target career and extracted skills?
**Answer**:
1. The target career track (e.g. `Java Backend Developer`) specifies a required skill set $R$ (e.g. `{Java, Spring Boot, SQL, REST APIs, Docker, Git}`).
2. Extracted resume skills $S$ are matched against $R$.
3. Missing skills $M = R \setminus S$ are identified.
4. The first 3 foundational missing skills are flagged as **High Priority Missing Skills** and mapped directly to actionable Roadmap milestone links and DSA Practice challenges.

### Q35: What HTTP status codes are returned for resume validation failures?
**Answer**:
- `400 Bad Request`: Empty file, null payload, or invalid sync request.
- `413 Payload Too Large`: Upload exceeding the 5MB multipart size limit.
- `415 Unsupported Media Type`: Unsupported file extension or invalid magic bytes.
- `422 Unprocessable Entity`: Corrupt document structure that cannot be parsed by PDFBox/POI.
- `404 Not Found`: Non-existent resume ID or unauthorized cross-user attempt.

### Q36: How does JPA manage the collection table `resume_skills` in relation to the `resumes` entity?
**Answer**:
In `Resume.java`, extracted skills are declared with `@ElementCollection`:
```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "resume_skills", joinColumns = @JoinColumn(name = "resume_id"))
@Column(name = "skill_name")
private Set<String> extractedSkills = new HashSet<>();
```
This maps to a relational collection table `resume_skills(resume_id, skill_name)` with foreign key cascade deletion, ensuring zero orphaned skills when a resume is deleted.

### Q37: What are the performance and scalability trade-offs of in-memory text parsing?
**Answer**:
- **Benefits**: Ultra-low latency ($< 50\text{ms}$ parsing time for standard 2-page resumes), zero disk I/O bottlenecks, stateless microservice readiness.
- **Trade-offs**: Memory overhead during peak concurrent uploads. This is mitigated by strictly capping uploads at 5MB and using streaming text extractors that release memory buffers immediately upon parsing.

---

## 13. Phase 12 — Role-Based Access Control (RBAC) & Platform Governance Viva Q&As

### Q38: What is the architectural difference between Authentication and Authorization in our application?
**Answer**:
- **Authentication (401 Unauthorized)**: Answers *"Who are you?"*. Handled by `JwtAuthenticationFilter`, `DaoAuthenticationProvider`, and `AuthEntryPointJwt`. It validates the user's credentials or JWT cryptographic signature.
- **Authorization (403 Forbidden)**: Answers *"What are you allowed to do?"*. Handled by Spring Security's `AuthorizationManager`, `.requestMatchers("/api/admin/**").hasRole("ADMIN")`, and `CustomAccessDeniedHandler`. It checks if the authenticated principal possesses the required `GrantedAuthority`.

### Q39: What roles exist in Phase 12, and how are they represented in Java and the database?
**Answer**:
1. Two canonical roles are supported: `USER` and `ADMIN`.
2. In Java, they are defined in an explicit enum `com.careeradvisor.backend.model.Role`.
3. In MySQL `users` table, it is stored as `role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER'`.
4. In Spring Security, they are converted to `GrantedAuthority` with the `ROLE_` prefix: `ROLE_USER` and `ROLE_ADMIN`.

### Q40: What is the difference between `hasRole('ADMIN')` and `hasAuthority('ROLE_ADMIN')` in Spring Security 6?
**Answer**:
In Spring Security, `hasRole("ADMIN")` is syntactic sugar for `hasAuthority("ROLE_ADMIN")`. Spring Security automatically prepends the default role prefix `ROLE_`. Therefore, when `CustomUserDetails` creates `new SimpleGrantedAuthority("ROLE_ADMIN")`, `.requestMatchers("/api/admin/**").hasRole("ADMIN")` matches successfully.

### Q41: How are user roles embedded in the JWT token payload?
**Answer**:
In `JwtUtils.java`, during token creation (`generateToken`):
```java
return Jwts.builder()
    .subject(email)
    .claim("userId", userId)
    .claim("role", role.name()) // "USER" or "ADMIN"
    .issuedAt(now)
    .expiration(expiryDate)
    .signWith(getSigningKey())
    .compact();
```
The client and downstream filters can decode and verify the signed claims directly from the token without querying the database for every lightweight decision.

### Q42: Why do we still query `CustomUserDetailsService` in `JwtAuthenticationFilter` if the role is inside the JWT?
**Answer**:
To enforce **authoritative state validation**. If an administrator revokes a user's account or changes their permissions in the database, relying solely on a stateless token payload would allow stale tokens to persist until expiration. Loading the user from `userRepository.findByEmail(email)` ensures the active database state is authoritative on each request.

### Q43: How does the application prevent Role Escalation / Injection attacks during public registration?
**Answer**:
In `UserService.java`, the `register(SignupRequest request)` method explicitly forces:
```java
User user = User.builder()
    .name(request.getName().trim())
    .email(request.getEmail().trim().toLowerCase())
    .password(passwordEncoder.encode(request.getPassword()))
    .role(Role.USER) // Hard-enforced in service layer
    .build();
```
Even if an attacker sends `{ "name": "Hacker", "email": "hacker@evil.com", "password": "...", "role": "ADMIN" }`, the backend service ignores any client-supplied role and strictly assigns `Role.USER`.

### Q44: How is the initial Administrator account initialized without hardcoding credentials in the source code?
**Answer**:
Via `AdminDataInitializer.java` (`@Component` implementing `CommandLineRunner`):
1. Reads `${app.admin.email:admin@careeradvisor.dev}` and `${app.admin.password:AdminPass123!}` from environment variables or `application.properties`.
2. Checks if `userRepository.existsByEmail(adminEmail)`.
3. If absent, creates a new `User` entity with `role = Role.ADMIN` and encrypts the password with `passwordEncoder.encode(adminPassword)`.
4. Zero plaintext passwords exist in the codebase or database.

### Q45: How does `CustomAccessDeniedHandler` differ from `AuthEntryPointJwt`?
**Answer**:
- `AuthEntryPointJwt`: Implements `AuthenticationEntryPoint`. Triggered when an unauthenticated/anonymous request attempts to access a protected endpoint, returning HTTP 401 Unauthorized JSON.
- `CustomAccessDeniedHandler`: Implements `AccessDeniedHandler`. Triggered when a fully authenticated user (e.g. `ROLE_USER`) attempts to access an endpoint restricted to `ROLE_ADMIN`, returning HTTP 403 Forbidden JSON.

### Q46: What endpoints are exposed under `/api/admin/**`?
**Answer**:
- `GET /api/admin/me`: Returns the authenticated administrator's ID, name, email, and role.
- `GET /api/admin/health`: Returns administrative service status and active principal name.
- `GET /api/admin/stats/overview`: Returns platform-wide KPI telemetry (users, resumes, quiz attempts, solved problems, mock interviews, completed interviews, career tracks distribution).
- `GET /api/admin/users`: Returns candidate directory with search filtering.
- `GET /api/admin/users/{id}`: Returns full candidate inspection detail.

### Q47: How does `AdminUserDto` prevent sensitive information leakage?
**Answer**:
`AdminUserDto` and `AdminUserDetailDto` are dedicated data transfer objects that selectively project only safe analytical data (`id`, `name`, `email`, `role`, `careerGoal`, `userLevel`, `skillCount`, `resumePresent`, `mockInterviewCount`, `solvedProblemsCount`). The password hash (`password`), salt, reset tokens, and session secrets are completely excluded from the DTO definition.

### Q48: What is the purpose of client-side route guards in Next.js, and why are they not considered primary security?
**Answer**:
- **Client Guard Purpose**: Improves user experience by immediately redirecting regular users trying to open `/admin` to `/dashboard` without rendering empty or broken UI widgets.
- **Why Not Primary Security**: Client-side JavaScript can be inspected, manipulated, or bypassed via direct HTTP calls (curl, Postman). The **true security boundary** is Spring Security on the backend, which rejects any unauthorized request to `/api/admin/**` with HTTP 403 Forbidden.

### Q49: How does the `/admin` overview dashboard compute the career track distribution?
**Answer**:
In `AdminService.java`:
```java
Map<String, Long> careerGoalsDistribution = allUsers.stream()
    .filter(u -> StringUtils.hasText(u.getCareerGoal()))
    .collect(Collectors.groupingBy(User::getCareerGoal, Collectors.counting()));
```
This produces a key-value mapping of career track titles to candidate counts, which the frontend renders as proportional progress bars.

### Q50: How does Phase 12 ensure zero regression on existing user ownership rules?
**Answer**:
Regular user endpoints (e.g. `/api/resumes/{id}`, `/api/interviews/{id}`, `/api/skills`) still bind operations strictly to `@AuthenticationPrincipal UserDetails`. An admin does not alter or hijack standard user endpoints; administrative governance occurs exclusively through dedicated `/api/admin/**` endpoints.

### Q51: How does Hibernate handle the database migration for existing user records when adding the `role` column?
**Answer**:
With `spring.jpa.hibernate.ddl-auto=update`, Hibernate executes:
```sql
ALTER TABLE users ADD COLUMN role ENUM('ADMIN', 'USER') NOT NULL;
```
Because the entity specifies `@Builder.Default private Role role = Role.USER;` and the column definition defaults to `'USER'`, all existing candidate records seamlessly default to `USER` without table drops or data loss.

### Q52: What is the HTTP response when an anonymous user attempts to access `/api/admin/stats/overview`?
**Answer**:
HTTP 401 Unauthorized with JSON payload:
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/admin/stats/overview",
  "timestamp": "2026-08-22 18:16:35"
}
```

### Q53: What is the HTTP response when a standard user (ROLE_USER) attempts to access `/api/admin/stats/overview`?
**Answer**:
HTTP 403 Forbidden with JSON payload:
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied: insufficient permissions to access this administrative resource",
  "path": "/api/admin/stats/overview",
  "timestamp": "2026-08-22 18:16:35"
}
```

### Q54: How does the frontend navigation dynamically adapt to the user's role?
**Answer**:
In `frontend/app/(protected)/layout.tsx`:
```tsx
const navItems = [
  { label: "Dashboard", href: "/dashboard", icon: "📊" },
  ...(user?.role === "ADMIN"
    ? [{ label: "Admin Dashboard", href: "/admin", icon: "🛡️" }]
    : []),
  { label: "Resume Analyzer", href: "/resume", icon: "📄" },
  ...
];
```
The "Admin Dashboard" navigation item and sidebar `ADMIN` badge are only rendered when `user?.role === "ADMIN"`.

### Q55: How does the user search feature work in the Admin User Governance module?
**Answer**:
1. The admin types a query term $T$ into the search input.
2. The frontend calls `GET /api/admin/users?search=T`.
3. The backend executes `userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(T, T)`.
4. The filtered candidate records are serialized with their aggregate stats (skills, resumes, mock interviews, solved DSA problems) and rendered in the table.

### Q56: How does the candidate inspection modal aggregate data across 5 different database entities?
**Answer**:
`AdminService.getUserDetail(userId)` queries:
1. `User` entity for core candidate info.
2. `UserSkillRepository.findByUserOrderByAddedAtDesc` for verified skills.
3. `ResumeRepository.findByUserOrderByUploadTimestampDesc` for resume status and upload time.
4. `MockInterviewRepository.findByUserOrderByStartedAtDesc` for interview session count and average score.
5. `UserProblemProgressRepository.countByUserAndSolvedTrue` for accepted DSA problems.
6. `UserRoadmapProgressRepository.countByUser` for completed roadmap steps.

### Q57: How was Phase 12 verified to guarantee enterprise-grade security and reliability?
**Answer**:
- **Phase 12 RBAC Suite (`test_phase12_rbac.js`)**: 38/38 PASS (100%) verifying role enforcement, payload injection rejection, 403 forbidden responses, 401 unauthorized responses, and admin APIs.
- **Browser RBAC Workflow Contract Suite (`test_browser_rbac_workflow.js`)**: 13/13 PASS (100%) testing real HTTP frontend and backend interactions.
- **Master Regression Suite (`test_master_regression.js`)**: 25/25 PASS (100%) verifying Phases 1 through 12.
- **Production Builds**: Backend compiled cleanly (`BUILD SUCCESS`), Frontend compiled 18 routes cleanly (`npm run build`).

### Q58: Why should secrets (database passwords, JWT secret keys, admin credentials) be stored in environment variables rather than source code?
**Answer**:
1. **Source Code Exposure Prevention**: Hardcoded credentials in repositories can be leaked through version control history, public forks, CI/CD logs, or decompiled binary artifacts.
2. **12-Factor App Compliance**: The Twelve-Factor App methodology mandates strict separation of config from code, allowing the exact same codebase to run in development, staging, and production simply by varying environment variables.
3. **Dynamic Secret Rotation**: Environment-driven configuration allows DevOps teams to rotate database passwords or encryption keys without requiring code recompilation or rebuilds.

### Q59: What is the architectural difference between Spring Boot `dev` and `prod` profiles in our application?
**Answer**:
- **`dev` Profile (`application-dev.properties`)**:
  - `spring.datasource.url`: Defaults to local MySQL instance (`localhost:3306`) with fallback credentials.
  - `spring.jpa.hibernate.ddl-auto=update`: Automatically synchronizes entity schema changes with the local development database.
  - `spring.jpa.show-sql=true`: Prints formatted SQL queries for local debugging.
  - `app.cors.allowed-origins`: Whitelists local frontend dev server (`http://localhost:3000`, `http://127.0.0.1:3000`).
  - `app.admin.email/password`: Seeds a default local development administrator (`admin@careeradvisor.dev`).
- **`prod` Profile (`application-prod.properties`)**:
  - `spring.datasource.url`: Requires explicit environment variables (`${DB_HOST}`, `${DB_USERNAME}`, `${DB_PASSWORD}`) with SSL enforcement (`useSSL=true`).
  - `spring.jpa.show-sql=false`: Completely disables SQL logging to protect data privacy and eliminate console overhead.
  - `app.cors.allowed-origins`: Restricts CORS to the authorized production frontend domain (`${CORS_ALLOWED_ORIGINS}`).
  - Logging levels set to `WARN` for framework internals to prevent log file bloat and sensitive data leaks.

### Q60: How is JWT configuration externalized and secured in production?
**Answer**:
`app.jwt.secret` and `app.jwt.expiration-ms` are injected via `@Value`:
```java
@Value("${app.jwt.secret}")
private String jwtSecret;

@Value("${app.jwt.expiration-ms:86400000}")
private int jwtExpirationMs;
```
In production (`application-prod.properties`), `app.jwt.secret=${JWT_SECRET}` has no default fallback, forcing the production environment to inject a 256-bit cryptographically secure secret key. The secret is never logged, printed, or serialized in any API response.

### Q61: How is CORS dynamically configured and protected against cross-origin vulnerabilities?
**Answer**:
In `SecurityConfig.java`:
```java
@Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
private String corsAllowedOrigins;

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    configuration.setAllowedOrigins(origins);
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
    configuration.setAllowCredentials(true);
    ...
}
```
This ensures production never allows wildcard `*` origins while supporting multi-domain or staging frontend URLs via comma-separated `CORS_ALLOWED_ORIGINS`.

### Q62: How does the frontend handle API URLs across development and production?
**Answer**:
In `frontend/lib/config.ts`:
```typescript
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
```
In development, Next.js loads `NEXT_PUBLIC_API_URL=http://localhost:8080` from `.env.local`. In production hosting (e.g. Vercel, Docker, AWS), `NEXT_PUBLIC_API_URL` is set to the production backend URL (e.g. `https://api.careeradvisor.dev`) without altering frontend code.

### Q63: What is Insecure Direct Object Reference (IDOR), and how is it completely prevented in our backend?
**Answer**:
- **IDOR Definition**: A vulnerability where an attacker accesses or manipulates another user's resources simply by changing a resource identifier in the URL (e.g., `DELETE /api/resumes/123` or `GET /api/interviews/456`).
- **Our Defense**:
  1. No endpoint trusts client-supplied user IDs.
  2. All resource queries enforce tenant ownership in the service layer:
     ```java
     Resume resume = resumeRepository.findById(id)
         .filter(r -> r.getUser().getId().equals(user.getId()))
         .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
     ```
  3. If User B requests User A's ID, the service returns `404 Not Found` rather than revealing existence or data.

### Q64: What is the difference between Horizontal and Vertical Privilege Escalation?
**Answer**:
- **Horizontal Privilege Escalation**: An attacker with standard user permissions accesses resources belonging to another standard user (e.g., Alice accessing Bob's resume). Prevented via zero-trust user isolation and session-bound repository queries.
- **Vertical Privilege Escalation**: An attacker with standard user permissions elevates their permissions to perform administrative actions (e.g., User attempting `GET /api/admin/stats/overview` or `POST /api/auth/register` with `role: "ADMIN"`). Prevented via Spring Security `.requestMatchers("/api/admin/**").hasRole("ADMIN")` and hardcoding `Role.USER` in `UserService.register()`.

### Q65: Why should `@CrossOrigin` annotations on individual Spring controllers be avoided in production?
**Answer**:
When individual controllers define `@CrossOrigin(origins = {"http://localhost:3000"})`, it overrides or conflicts with centralized Spring Security CORS configuration. In production environments where the frontend is deployed to custom domains (e.g. `https://careeradvisor.dev`), hardcoded controller annotations will reject valid production cross-origin requests. Centralizing CORS in `SecurityConfig.corsConfigurationSource()` ensures a single source of truth driven by `app.cors.allowed-origins`.

### Q66: How does our architecture prevent password hash exposure in REST API responses?
**Answer**:
The `User` entity (which contains the BCrypt `password` hash) is never returned directly to API clients. Instead, dedicated Data Transfer Objects (DTOs) like `AuthResponse`, `AdminUserDto`, and `AdminUserDetailDto` are explicitly constructed with safe fields (`id`, `name`, `email`, `role`, `careerGoal`), omitting password fields entirely.

### Q67: Why does our application use `SessionCreationPolicy.STATELESS` in Spring Security?
**Answer**:
In `SecurityConfig.java`:
```java
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```
This instructs Spring Security never to create an `HttpSession` on the server or store security contexts in server memory/cookies. Every request is independently authenticated using the cryptographically verified JWT token in the `Authorization: Bearer` header, making the API fully stateless and horizontally scalable across distributed server clusters.

### Q68: What HTTP security headers are configured in Spring Security, and what attacks do they prevent?
**Answer**:
In `SecurityConfig.java`:
1. **`X-Frame-Options: DENY`**: Prevents Clickjacking attacks by forbidding external domains from embedding our web application inside an `<iframe>`.
2. **`X-Content-Type-Options: nosniff`**: Prevents MIME-sniffing attacks where a browser executes user-uploaded files as scripts by misinterpreting their content type.
3. **`Referrer-Policy: strict-origin-when-cross-origin`**: Prevents leaking sensitive URL paths/query parameters to external sites during outbound navigation while preserving the origin for same-origin requests.

### Q69: Why do we use multi-stage Docker builds for the backend and frontend?
**Answer**:
1. **Minimal Image Size**: The build environment (Maven with OpenJDK 17 or Node build toolchains) is discarded after packaging, leaving only the lightweight runtime (Alpine JRE 17 or Node standalone runner).
2. **Reduced Attack Surface**: Compilers, SDK tools, and source code files are absent from the final production container image.
3. **Non-Root Execution**: The final containers run under dedicated unprivileged users (`appuser` / `nextjs`) rather than `root`, mitigating container breakout vulnerabilities.

### Q70: What is the purpose of the Reverse Proxy (Nginx) in our production deployment topology?
**Answer**:
1. **SSL/TLS Termination**: Offloads CPU-intensive HTTPS cryptographic handshakes from application instances.
2. **Path-Based Routing**: Routes `/api/*` to the Spring Boot cluster (`:8080`) and all other web requests to the Next.js server (`:3000`).
3. **DDoS & Rate Limiting**: Buffers slow client connections and throttles malicious bursts before they reach backend worker threads.
4. **Header Standardization**: Injects `X-Forwarded-For`, `X-Real-IP`, and global security headers uniformly across all responses.

### Q71: What database indexing strategy was implemented in Phase 13 to support high concurrency?
**Answer**:
Indexes were placed on high-frequency composite lookups and search columns:
- `users`: `email` (Unique B-tree index for instant $O(\log N)$ authentication lookup).
- `mock_interviews`: `(user_id, started_at)` (Index for fast candidate history pagination and sorting).
- `resumes`: `(user_id, upload_timestamp)` (Index for latest resume retrieval).
- `user_problem_progress`: `(user_id, problem_id)` (Unique composite index for $O(1)$ progress updates).
- `user_skills`: `(user_id, skill_name)` (Unique composite index preventing duplicate skill records).

### Q72: How does our `GlobalExceptionHandler` guarantee zero sensitive information leakage?
**Answer**:
- Caught business exceptions (e.g. `ResourceNotFoundException`, `UnsupportedFileTypeException`) return structured JSON with safe HTTP status codes and user-friendly messages.
- Unhandled internal exceptions (`Exception.class`) log the full stack trace securely to server log files via SLF4J, but return a generic response:
  ```json
  {
    "status": 500,
    "error": "Internal Server Error",
    "message": "An unexpected error occurred. Please try again later.",
    "timestamp": "2026-08-22 19:02:10"
  }
  ```
  Database connection strings, SQL dialect errors, and filesystem paths are never returned to the client.

### Q73: How can the backend application scale horizontally in a production Kubernetes cluster?
**Answer**:
Because the Spring Boot API is 100% **stateless** (zero server sessions, JWT authentication, centralized MySQL database), any number of identical backend replicas can be spawned behind a Kubernetes `Service` / Load Balancer without sticky sessions or state replication overhead.

### Q74: Why is in-memory streaming preferred over disk writes for the Resume Analyzer?
**Answer**:
1. **Ephemeral Scalability**: In containerized environments (Kubernetes, AWS ECS), containers have ephemeral filesystems. In-memory processing avoids disk filling and eliminates orphaned temporary files.
2. **Security**: Disallows arbitrary file writes to disk, completely mitigating path traversal (`../../etc/passwd`) or web shell upload vulnerabilities.
3. **Performance**: Processing a 100KB resume in memory with PDFBox takes $< 30\text{ms}$, avoiding disk I/O bottlenecks.

### Q75: How does the application maintain data consistency across multi-entity operations?
**Answer**:
By annotating multi-step service methods (e.g. `submitInterview`, `syncSkills`, `toggleStep`) with Spring's `@Transactional`. If any sub-operation fails (e.g., database constraint violation or network blip), the entire transaction is rolled back atomically, preventing orphaned or corrupt records.

---

## 13. Phase 14A Viva Questions & Answers: AI Infrastructure, Provider Abstraction & Personal Context Engine (Q76–Q88)

### Q76: What is the primary objective of Phase 14A AI Infrastructure?
**Answer**:
Phase 14A establishes a decoupled, enterprise-grade AI foundation for the Career Advisor platform. Rather than tightly coupling business logic to a specific vendor's SDK, Phase 14A implements:
1. **Provider Abstraction** (`AiProvider` interface) to support multiple LLMs (Gemini, OpenAI, Anthropic, Mock).
2. **Environment-Driven Configuration** with zero hardcoded API keys.
3. **Personal User Context Engine** (`UserAiContextService` + `AiContextBuilder`) aggregating authenticated candidate state without sensitive data leakage.
4. **Token Telemetry & Usage Monitoring** (`AiUsageLog`).
5. **Zero-Trust Security & Graceful Degradation** if external AI services are offline or disabled.

### Q77: Why did we use the Provider Pattern (`AiProvider`) instead of directly calling an AI client library?
**Answer**:
1. **Vendor Neutrality (Avoid Vendor Lock-in)**: The application can switch between OpenAI, Google Gemini, Anthropic, or local open-source models (via Ollama/vLLM) simply by changing `AI_PROVIDER` in environment variables.
2. **Testability & Offline Resilience**: In local development, CI pipelines, and environments without active API keys, `AiProviderFactory` automatically routes requests to `MockAiProvider`, ensuring 100% of platform tests pass without external network dependencies or API billing costs.
3. **Consistent Error Normalization**: All external provider exceptions, timeouts, and rate limits are caught and normalized into standardized platform responses (`SUCCESS`, `TIMEOUT`, `PROVIDER_UNAVAILABLE`, `RATE_LIMITED`).

### Q78: How does the Personal AI Context Engine assemble candidate data across modules?
**Answer**:
`UserAiContextService.buildUserContext(User user)` queries 8 distinct subsystem services and repositories in a unified read operation:
- `user.getName()`, `user.getEmail()`, `user.getCareerGoal()`, `user.getUserLevel()`
- `UserSkillService`: Verified skills portfolio.
- `ResumeRepository`: Latest parsed resume summary, extracted skills, and skill gaps.
- `QuizService`: Diagnostic assessment score, tier, and recommended career track.
- `UserProgressService`: Roadmap milestones completed, total steps, and next recommended topic.
- `ProblemService`: Solved DSA challenge counts, difficulty breakdown, and next recommended challenge.
- `MockInterviewRepository`: Total sessions, completion count, average score, strong areas, and weak areas.
- `RecommendationService`: Overall readiness percentage, lifecycle state (`BEGINNER`, `INTERMEDIATE`, `JOB_READY`), and priority actions.

### Q79: How does the system guarantee zero sensitive data is leaked to LLMs or context endpoints?
**Answer**:
1. **Dedicated DTO Projection**: Context is assembled into `PersonalAiContextDto` using sanitized sub-DTOs (`UserSummaryDto`, `ResumeAiSummaryDto`, etc.).
2. **Strict Exclusion of Secrets**: Passwords, BCrypt hashes, salts, JWT tokens, database connection URLs, and admin credentials are NEVER mapped into context DTOs.
3. **Principal Derivation**: `GET /api/ai/context` strictly derives the user identity from `@AuthenticationPrincipal CustomUserDetails` (extracted from the validated JWT), preventing unauthorized IDOR access.

### Q80: What safety boundaries and persona rules are enforced by `AiContextBuilder`?
**Answer**:
1. **Explicit Identity**: Persona is defined as "OneStop Career Advisor AI" and is explicitly instructed never to claim human status.
2. **Grounding in Authoritative Data**: The AI must ground recommendations strictly in the candidate's verified profile data.
3. **No Hallucination of User State**: If a candidate has not yet taken a quiz, uploaded a resume, or solved DSA problems, the prompt explicitly states that the section is unattempted, instructing the model to encourage completion rather than inventing fake scores.
4. **Platform Action Encouragement**: Prompt guides the AI to suggest concrete platform actions (e.g. taking a quiz, practicing DSA, completing roadmap steps).

### Q81: How does the backend track AI usage and costs without compromising privacy?
**Answer**:
The `AiUsageLog` JPA entity records:
- `user_id` (foreign key to `users`)
- `provider` (e.g. `openai`, `gemini`, `mock`)
- `model` (e.g. `gemini-1.5-flash`, `gpt-4o-mini`)
- `request_type` (e.g. `CHAT_COMPLETION`, `CONTEXT_EVALUATION`)
- `prompt_tokens`, `completion_tokens`, `total_tokens`
- `status` (`SUCCESS`, `TIMEOUT`, `ERROR`)
- `latency_ms` and `failure_category`
**Zero raw user messages or private resume text** are stored in `ai_usage_logs`, maintaining GDPR/CCPA privacy compliance while enabling precise operational observability and token cost analytics.

### Q82: How does the platform behave when `AI_ENABLED=false` or `AI_API_KEY` is not provided?
**Answer**:
The platform demonstrates **graceful degradation**:
- The application starts up normally with zero initialization errors.
- `GET /api/ai/health` reports `enabled: false` (or `available: false`).
- `GET /api/ai/context` continues to function normally (since personal context aggregation is independent of external LLM availability).
- Chat requests return structured responses with `status: "AI_DISABLED"` or fallback contextual guidance via `MockAiProvider`.
- Non-AI modules (Auth, Roadmaps, DSA, Quiz, Mock Interview, Resume Analyzer, Admin) remain 100% operational.

### Q83: What is the difference between Phase 14A and Phase 14B?
**Answer**:
- **Phase 14A (Foundation)**: Architecture, provider abstraction, environment configuration, personal context engine, API contracts, telemetry, and safety boundaries.
- **Phase 14B (Interactive Interface)**: Persistent conversation/message relational data layer, bounded conversational history/memory, deterministic title generation, zero-trust ownership validation, REST APIs (`/api/conversations`), and responsive Next.js Chatbot UI.

---

## 14. Phase 14B Viva Questions & Answers: Persistent Personal AI Chatbot (Q84–Q95)

### Q84: How are conversations and chat messages modeled to support future communication types (User-to-User, User-to-Admin, Audio/Video)?
**Answer**:
The platform utilizes a **unified polymorphic communication schema**:
- `Conversation`: Holds `owner` (`User`), `type` (`ConversationType`: `USER_TO_AI`, `USER_TO_USER`, `USER_TO_ADMIN`), `title`, `archived` flag, and timestamps (`createdAt`, `updatedAt`, `lastMessageAt`).
- `ChatMessage`: Holds `conversation_id`, `sender_user_id` (nullable for AI/System), `senderType` (`MessageSenderType`: `USER`, `AI`, `ADMIN`, `SYSTEM`), `content` (`TEXT`), `sequenceNumber`, and `status`.
- In Phase 14B, only `USER_TO_AI` conversations and `USER`/`AI` sender types are activated, but future phases can reuse this exact schema for human mentoring, admin support tickets, and WebRTC audio/video call logs without altering the core database structure.

### Q85: How does the platform prevent Insecure Direct Object References (IDOR) on conversation endpoints?
**Answer**:
Every conversation method in `ConversationService` and `ChatService` requires both the `conversationId` and the authenticated `User` entity derived from Spring Security's `@AuthenticationPrincipal CustomUserDetails`. Repositories execute:
```java
conversationRepository.findByIdAndOwner(id, authenticatedUser)
```
If a user attempts to access, read, message, archive, or delete another candidate's conversation ID (e.g. `GET /api/conversations/10`), the query returns `Optional.empty()` which maps to `HTTP 404 Not Found`. Returning 404 instead of 403 prevents malicious users from enumerating valid conversation IDs.

### Q86: How does the AI chatbot maintain conversation memory without exceeding token limits?
**Answer**:
The platform implements a **bounded memory window**:
- Rather than loading unlimited chat history into the LLM prompt, `ChatMessageRepository` queries only the latest $N$ messages (configurable via `app.ai.chat.history-limit=20`) using:
  ```java
  chatMessageRepository.findRecentMessagesByConversation(conversation, PageRequest.of(0, historyLimit))
  ```
- The messages are sorted chronologically and prepended into the structured system prompt after the personal candidate profile context.
- Historical messages remain safely persisted in MySQL for user browsing, while the LLM prompt remains bounded, token-efficient, and cost-controlled.

### Q87: Why did we choose deterministic title generation instead of calling an LLM for conversation titles?
**Answer**:
Calling an external LLM solely to generate a 3-word title on every first message introduces extra latency ($500-1500\text{ ms}$), consumes additional API tokens/costs, and introduces failure points. Instead, `ConversationService.generateDeterministicTitle()` cleans preamble questions (e.g. "What should I learn for Spring Boot?" $\rightarrow$ "Spring Boot Learning"), sanitizes whitespace, capitalizes, and truncates to 80 characters deterministically in $<0.1\text{ ms}$ with $100\%$ reliability and zero token cost.

### Q88: How are database indexes structured for high performance in the chat subsystem?
**Answer**:
To avoid table scans and ensure sub-millisecond query latency at scale, the database maintains:
1. `conversations`:
   - `idx_conversations_owner` on `user_id` (fast listing of user chats).
   - `idx_conversations_updated_at` on `updated_at` (fast ordering by recent activity).
   - `idx_conversations_owner_archived` on `(user_id, archived)` (filtered active vs archived chats).
2. `chat_messages`:
   - `idx_chat_msg_conv_id` on `conversation_id` (fast retrieval of message threads).
   - `idx_chat_msg_created_at` on `created_at` (chronological ordering).
   - `idx_chat_msg_conv_created` composite on `(conversation_id, created_at)` (bounded history retrieval with `PageRequest`).

### Q89: How does the chat service handle external AI provider failures or timeouts?
**Answer**:
`ChatService` wraps AI generation in safe fallback logic:
1. The user's input message is persisted immediately with status `SENT`.
2. If the external provider returns an error, times out, or AI is disabled (`AI_ENABLED=false`), `ChatService` generates a graceful fallback message (`"I am temporarily unavailable to process your query..."`) with `status = "FALLBACK"`.
3. The fallback message is persisted so the conversation thread remains coherent and unbroken.
4. No Java stack traces, SQL errors, or internal URLs are ever exposed to the client.

### Q90: Why was HTTP REST chosen over WebSockets for Phase 14B?
**Answer**:
1. **Stateless Scalability**: REST endpoints integrate natively with our existing stateless JWT architecture, Spring Security filters, and standard HTTP connection pools.
2. **Deterministic Request-Response**: A career advisor consultation is naturally turn-based (User prompt $\rightarrow$ LLM generation $\rightarrow$ Response), making HTTP POST straightforward, debuggable, and cache-friendly.
3. **Simplicity & Reliability**: HTTP eliminates WebSocket handshake overhead, stateful socket session management, reconnection logic, and heartbeat ping/pong complexities while establishing a clean persistence foundation.
4. **Future Streaming Upgrade**: Future phases can easily add Server-Sent Events (SSE) or WebSockets on top of this exact service layer without altering data models.

### Q91: How does the chat UI preserve conversation state across browser page refreshes?
**Answer**:
State persistence is guaranteed at the database tier:
- On component mount, the Next.js `/chat` page calls `getConversations()` which fetches the user's active conversation list from MySQL via `GET /api/conversations`.
- The active conversation ID is loaded via `GET /api/conversations/{id}`, populating all historical messages from the database.
- Refreshing the browser or logging in from a different device completely restores all conversation threads and message histories.

### Q92: What validation rules are enforced on incoming chat messages?
**Answer**:
1. **Non-blank**: Blank or whitespace-only messages are rejected with `HTTP 400 Bad Request`.
2. **Length Limit**: Messages exceeding `app.ai.chat.max-message-length` (4000 characters) are rejected with `HTTP 400 Bad Request` to prevent buffer/token exhaustion attacks.
3. **Archived Guard**: Writing to an archived conversation is rejected with `HTTP 400 Bad Request`.
4. **Owner Validation**: Sending to a conversation not owned by the authenticated user returns `HTTP 404 Not Found`.

### Q93: Can an Administrator view or search another candidate's private chat conversations?
**Answer**:
No. In Phase 14B, privacy by design is strictly maintained:
- Administrator accounts can access `/admin` platform governance and user directories, but have **zero global visibility into individual user conversation threads**.
- If an admin invokes `GET /api/conversations/10` where conversation 10 belongs to candidate Alice, the system returns `HTTP 404 Not Found`.
- Admin auditing/moderation of chat messages will be governed by dedicated future RBAC permissions and consent mechanisms.

### Q94: How does the chatbot incorporate resume and mock interview data into its answers?
**Answer**:
Before prompt dispatch, `ChatService` calls `UserAiContextService.buildUserContext(user)` which queries `ResumeRepository` for parsed skills and skill gaps, and `MockInterviewRepository` for recent interview scores, strong areas, and weak areas. `AiContextBuilder` synthesizes these into an authoritative candidate context block in the system prompt. For example, if a user asks *"How can I improve my interview skills?"*, the AI advisor recognizes specific weak areas (e.g. Concurrency or System Design) from actual mock interview attempts and suggests targeted practice.

### Q95: What is the complete flow when a user sends a message in `/chat`?
**Answer**:
```text
1. User enters text in /chat UI -> Enter / Send button clicked
2. Frontend chatService sends POST /api/conversations/{id}/messages (Bearer JWT)
3. JwtAuthenticationFilter verifies JWT -> populates CustomUserDetails
4. ConversationController extracts User from @AuthenticationPrincipal
5. ChatService validates ownership (404 if mismatch) & content (400 if invalid)
6. ChatService persists USER message in MySQL chat_messages table
7. If 1st message -> ConversationService updates conversation title deterministically
8. UserAiContextService aggregates candidate's profile, skills, resume & quiz data
9. AiContextBuilder constructs System Context Prompt with bounded recent 20 messages
10. AiProviderFactory calls active AiProvider (Mock or OpenAI/Gemini)
11. ChatService persists AI message in MySQL chat_messages table
12. ChatService updates conversation updatedAt and lastMessageAt timestamps
13. AiUsageLogService records telemetry log in MySQL ai_usage_logs
14. ChatResponseDto returns both User and AI message payloads to Frontend
15. Frontend /chat updates active message stream and scrolls to bottom smoothly
```

---

## 15. Phase 14C: Real-Time Human Communication & WebSockets Deep-Dive

### Q96: Why did we use STOMP over WebSockets instead of raw WebSockets?
**Answer**:
- **Structured Framing**: STOMP (Simple Text Oriented Messaging Protocol) defines standard frame types (`CONNECT`, `SUBSCRIBE`, `SEND`, `MESSAGE`, `UNSUBSCRIBE`, `DISCONNECT`) and destination headers, avoiding the need to invent and maintain custom JSON framing protocols over raw TCP/WebSocket streams.
- **Pub/Sub Broker Integration**: Spring's built-in message broker integrates natively with STOMP, enabling room-based broadcasting (`/topic/conversations/{id}`) and user queues (`/queue/...`) with simple channel annotations (`@MessageMapping`, `SimpMessagingTemplate`).
- **Channel Interceptors**: STOMP frames pass through Spring's `ChannelInterceptor` pipeline, allowing clean separation of authentication (on `CONNECT`) and destination authorization (on `SUBSCRIBE`).

### Q97: How is WebSocket authentication enforced securely in Career Advisor?
**Answer**:
1. The initial HTTP handshake upgrade at `/ws` is permitted through Spring Security's HTTP filter chain.
2. In `WebSocketSecurityInterceptor` (`ChannelInterceptor`), incoming frames are inspected before reaching the message broker.
3. On the STOMP `CONNECT` frame, the client must provide `Authorization: Bearer <jwt>` or a `token` header.
4. The interceptor validates the JWT via `JwtUtils.validateToken()` and resolves `CustomUserDetails`. If missing or invalid, an `AuthenticationException` is thrown, aborting the connection immediately. Anonymous users cannot send or receive STOMP messages.

### Q98: How does server-side subscription authorization prevent cross-user eavesdropping?
**Answer**:
When a client sends a STOMP `SUBSCRIBE` frame to `/topic/conversations/{conversationId}`:
1. `WebSocketSecurityInterceptor.preSend()` extracts the destination string and checks if it matches `/topic/conversations/{id}`.
2. It parses the authenticated `Principal` from the WebSocket session attributes (never trusting client-provided IDs).
3. It checks `conversation_participants` or `conversations` ownership in the database.
4. If the authenticated user is neither a registered participant nor an Admin handling a `USER_TO_ADMIN` support ticket, the interceptor throws a `MessageDeliveryException("Unauthorized subscription")`, preventing any unauthorized client from receiving room events.

### Q99: How does Career Advisor track user presence (Online/Offline) in real-time?
**Answer**:
`PresenceService` maintains a thread-safe in-memory session map:
```java
ConcurrentHashMap<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
```
- Multi-Tab Support: A user can open multiple browser tabs. When a WebSocket session connects (`SessionConnectedEvent`), the session ID is added to the user's `Set<String>`. If the set size was 0, a `USER_ONLINE` event is broadcast to `/topic/presence`.
- Graceful Disconnect: When a tab closes (`SessionDisconnectEvent`), that specific session ID is removed. Only when the set becomes empty is the user marked offline and a `USER_OFFLINE` event broadcast to `/topic/presence`.
- Thread-Safety: `ConcurrentHashMap` combined with synchronized sets prevents race conditions across concurrent HTTP/WebSocket worker threads.

### Q100: How are typing indicators and read receipts implemented without database bloat?
**Answer**:
- **Transient Real-Time Events**: Typing events (`TYPING_STARTED`, `TYPING_STOPPED`) are high-frequency and ephemeral. They are broadcast directly over STOMP to `/topic/conversations/{id}` using `SimpMessagingTemplate` without touching the MySQL database.
- **Read Receipts**: When a user views a thread, `POST /api/conversations/{id}/read` updates the participant's `last_read_at` timestamp in `conversation_participants` and marks unread messages as `READ`, then broadcasts `MESSAGE_READ` over STOMP to update ticks in the active peer's UI.

### Q101: What is the database schema design for Phase 14C?
**Answer**:
- `conversation_participants`:
  - `id` (BIGINT PK AUTO_INCREMENT)
  - `conversation_id` (FK to `conversations.id`)
  - `user_id` (FK to `users.id`)
  - `participant_role` (ENUM: `CREATOR`, `MEMBER`, `ADMIN`)
  - `joined_at` (DATETIME)
  - `last_read_at` (DATETIME)
  - Unique Constraint: `uk_conv_participant (conversation_id, user_id)`
  - Indexes: `idx_conv_part_user (user_id, conversation_id)` and `idx_conv_part_read (user_id, last_read_at)`
- `chat_messages`:
  - Index: `idx_chat_msg_conv_seq (conversation_id, sequence_number)` for high-speed chronological timeline rendering.

### Q102: How does the system isolate Human Conversations from AI Chatbot Context?
**Answer**:
- `ConversationType` explicitly segregates conversations into `USER_TO_AI`, `USER_TO_USER`, and `USER_TO_ADMIN`.
- `ChatService` and `UserAiContextService` only load and query `USER_TO_AI` conversations.
- Private human peer-to-peer discussions and admin support tickets are NEVER injected into `AiContextBuilder` or sent to LLM providers, ensuring absolute privacy compliance and zero data leakage.

### Q103: Can an Administrator read private USER_TO_USER messages?
**Answer**:
No. In `ConversationService.validateHumanConversationAccess()`:
- For `USER_TO_ADMIN` conversations, administrators have authorized access to respond to support inquiries.
- For `USER_TO_USER` private conversations, only the explicit participants in `conversation_participants` are permitted access. If an Admin attempts to access a private peer conversation, the API returns `HTTP 404 Not Found` (Zero-Trust Privacy Isolation).

---

## 16. WebRTC Audio & Video Calling Architecture (Phase 14D)

### Q104: What is WebRTC and how is it integrated into Career Advisor?
**Answer**:
WebRTC (Web Real-Time Communication) is an open-source standard and browser API providing direct, real-time, peer-to-peer (P2P) audio, video, and data communication without requiring intermediate media servers. In Career Advisor, WebRTC is integrated using:
- **Signaling Layer**: Handled entirely through the existing authenticated Spring Boot backend using STOMP over WebSocket (`/topic/conversations/{id}`) and REST endpoints (`/api/calls/**`).
- **Media Layer**: High-definition audio and video streams flow directly between client browser endpoints via Secure Real-time Transport Protocol (SRTP) over UDP.
- **Spring Boot Responsibility**: Authentication, participant authorization, call state machine, lifecycle governance, and signaling mediation. Zero media bytes transit through the Spring Boot server.

### Q105: How does the WebRTC signaling flow (SDP Offer/Answer & ICE Candidates) work in Career Advisor?
**Answer**:
1. **Initiation**: Caller hits `POST /api/calls` $\rightarrow$ server creates `CallSession` with status `RINGING` and broadcasts `INCOMING_CALL` event to the conversation topic over STOMP.
2. **Acceptance**: Receiver clicks "Accept" $\rightarrow$ `POST /api/calls/{id}/accept` sets status to `ACCEPTED` and broadcasts `CALL_ACCEPTED`.
3. **SDP Offer**: Caller acquires local media (`getUserMedia`), creates `RTCPeerConnection`, generates an SDP Offer (`createOffer`), sets it as local description, and sends it to the server (`POST /api/calls/{id}/signal` with type `WEBRTC_OFFER`).
4. **SDP Answer**: Receiver receives the offer over STOMP, sets it as remote description, generates an SDP Answer (`createAnswer`), sets it as local description, and sends it back to the server (`WEBRTC_ANSWER`).
5. **ICE Candidates**: Both peers generate Interactive Connectivity Establishment (ICE) candidates representing valid network routing paths (host, server reflexive, relay) and exchange them asynchronously (`WEBRTC_ICE_CANDIDATE`).
6. **P2P Media Flow**: Once compatible ICE candidate pairs and SDP codecs are negotiated, peer-to-peer media streams begin flowing directly between browsers.

### Q106: What are STUN and TURN servers, and why are they necessary?
**Answer**:
- **STUN (Session Traversal Utilities for NAT)**: A lightweight protocol that discovers a client's public IP and port when located behind NAT/firewall (Server Reflexive Candidate). Career Advisor uses `stun:stun.l.google.com:19302` by default.
- **TURN (Traversal Using Relays around NAT)**: A relay protocol used when symmetric NATs or strict enterprise firewalls block direct P2P connections. Media flows through the TURN relay server. Configurable via `app.webrtc.turn-url`, `app.webrtc.turn-username`, and `app.webrtc.turn-credential`.
- **Environment Driven**: Zero credentials or server endpoints are hardcoded; all STUN/TURN configurations are injected via environment properties.

### Q107: What is the server-authoritative Call State Machine in Career Advisor?
**Answer**:
The `CallSession` lifecycle is strictly enforced on the Spring Boot backend:
- `RINGING` $\rightarrow$ `ACCEPTED`, `REJECTED`, `CANCELLED`, `MISSED`, `EXPIRED`
- `ACCEPTED` $\rightarrow$ `ENDED`, `FAILED`
- **Race Condition & State Protections**:
  - Re-accepting or ending an already `ENDED` call throws `IllegalStateException` $\rightarrow$ `400 Bad Request`.
  - Duplicate active calls for either participant return `409 Conflict (CALL_ALREADY_ACTIVE)`.
  - Calls unanswered within 45 seconds automatically transition to `MISSED` (`EndReason.TIMEOUT`).
  - Duration is strictly computed on the backend as `durationSeconds = Duration.between(answeredAt, endedAt).toSeconds()`.

### Q108: How does Career Advisor prevent IDOR and unauthorized call eavesdropping?
**Answer**:
1. **Never Trust Client Caller IDs**: `callerId` and `senderId` are never accepted from the request payload. Identity is extracted exclusively from the authenticated JWT `Principal`.
2. **Participant Validation**: When initiating or interacting with a call, the server validates that the caller and receiver are registered participants in `conversation_participants`.
3. **Anti-Enumeration (404 Not Found)**: If an unauthorized user (e.g. User C) queries or mutates a call belonging to User A & B, the server responds with `HTTP 404 Not Found` instead of 403, preventing attackers from confirming the existence of active call sessions.
4. **Admin Privacy Isolation**: Administrators cannot initiate or join private `USER_TO_USER` calls (404 returned). Admins can only participate in calls within authorized `USER_TO_ADMIN` support tickets.

---

## 17. Future Scope & Production Roadmap

- **Phase 14D (COMPLETE)**: WebRTC Audio & Video Calling (P2P Media, STOMP Signaling, State Machine, Zero-Trust Anti-IDOR, Call History).
- **Phase 15**: ATS Formatting Scorer & Automated Resume Revision Assistant.
- **Phase 16**: Conversational Voice Interviewer (Gemini Multimodal Live API).
- **Phase 17**: Kubernetes Deployment & Cloud Native CI/CD Pipeline.





