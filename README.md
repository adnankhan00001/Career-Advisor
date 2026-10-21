# OneStop Career Advisor

[![Backend CI / Tests](https://img.shields.io/badge/backend%20tests-130%20passed-brightgreen.svg)]()
[![Frontend Build](https://img.shields.io/badge/frontend%20build-Next.js%2016-blue.svg)]()
[![Java](https://img.shields.io/badge/java-17%20LTS-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/spring%20boot-3.3.4-green.svg)]()
[![React](https://img.shields.io/badge/react-19-cyan.svg)]()
[![TypeScript](https://img.shields.io/badge/typescript-5.0+-blue.svg)]()
[![Tailwind CSS](https://img.shields.io/badge/tailwind-v4-38bdf8.svg)]()
[![MySQL](https://img.shields.io/badge/mysql-8.0-4479A1.svg)]()

OneStop Career Advisor is an end-to-end, AI-enhanced platform designed to guide professionals and students through career path exploration, personalized skills roadmaps, coding practice, mock interviews, automated resume optimization, and direct verified mentorship.

---

## 1. Project Overview & Architecture

### Core Features
* **Career Explorer & Roadmaps:** Step-by-step career path progression tracking, milestone toggles, and customized career goals.
* **Practice Hub & Problem Solving:** Curated coding and system design problem sets with progress tracking and solutions.
* **Mock Interviews:** Behavioral and technical interview simulation with structured scoring, feedback reports, and history.
* **Resume Analyzer:** Resume parsing, automated skill extraction, gap analysis, and tailored career recommendations.
* **Mentorship Hub & Real-Time Messaging:** Verified mentor network with application review, mentee request workflows, dynamic capacity controls, and real-time STOMP messaging over WebSocket with WebRTC audio/video calling.
* **Full Administrative Governance:**
  1. **Admin Overview (`/admin`):** Telemetry dashboard with user KPIs, preparation distributions, and recent activity streams.
  2. **User Governance (`/admin/users`):** Paginated search, role filtering, account suspension/reactivation, self-suspension guard, and last-admin safety locks.
  3. **Mentor Governance (`/admin/mentors`):** Application review, credential verification, approval/rejection workflows, and suspension controls.
  4. **Mentorship Oversight (`/admin/mentorships`):** Global relationship tracking, lifecycle transitions, administrative termination with reason logging, and instant mentor capacity release.
  5. **Support Inbox (`/admin/messages`):** Real-time administrative support messaging with role-based thread isolation.
  6. **Audit Logging (`/admin/audit-logs`):** Immutable MySQL-backed audit trail with correlation IDs, client IP capture, and automated credential masking.
  7. **Platform Settings (`/admin/settings`):** Persistent configuration engine for mentorship capacity bounds (`min`, `max`, `default`), registration toggles, and runtime governance.

### Technology Stack
* **Frontend:** Next.js 16 (App Router), React 19, TypeScript 5, Tailwind CSS v4, Framer Motion, STOMP.js over SockJS.
* **Backend:** Java 17 LTS, Spring Boot 3.3.4, Spring Security 6, Spring Data JPA / Hibernate, Spring WebSocket (STOMP).
* **Database:** MySQL 8.0+ (InnoDB, `utf8mb4_unicode_ci`).
* **AI Infrastructure:** Hybrid multi-provider architecture supporting Gemini 1.5, OpenAI, or local Mock AI.
* **Hosting Platforms:**
  * **Frontend:** Vercel (Next.js serverless edge & static prerendering).
  * **Backend & Database:** Railway (Spring Boot container deployment & Railway Managed MySQL).

---

## 2. Prerequisites & Environment Requirements

Before running the application locally, ensure you have the following installed:

| Tool | Minimum Version | Recommended Version | Verification Command |
| :--- | :--- | :--- | :--- |
| **Java Development Kit (JDK)** | OpenJDK 17 LTS | OpenJDK 17 or Temurin 17 | `java -version` |
| **Node.js** | 18.18.0 LTS | Node.js 20.x LTS | `node -v` |
| **npm** | 10.0.0+ | npm 10.8+ | `npm -v` |
| **MySQL Server** | MySQL 8.0+ | MySQL 8.0 or 8.4 LTS | `mysql --version` |
| **Git** | 2.30+ | 2.40+ | `git --version` |

*Note: Maven is bundled via the Maven Wrapper (`./mvnw` or `.\mvnw.cmd`), so a standalone Maven installation is optional.*

---

## 3. Database Setup

1. Start your local MySQL service.
2. Log into MySQL as an administrative user and create the database schema:
   ```sql
   CREATE DATABASE career_advisor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. (Optional) Create a dedicated local application user:
   ```sql
   CREATE USER 'career_user'@'localhost' IDENTIFIED BY 'LocalPassword123!';
   GRANT ALL PRIVILEGES ON career_advisor.* TO 'career_user'@'localhost';
   FLUSH PRIVILEGES;
   ```
4. **Schema Initialization Strategy:**
   * The application uses Hibernate `spring.jpa.hibernate.ddl-auto=update`. On startup, all tables, columns, and foreign keys are created automatically without dropping existing data.
   * Platform settings and initial configuration are seeded automatically on startup via `@PostConstruct` into the `platform_settings` table.
   * Reference idempotent DDL scripts are available at:
     * `backend/src/main/resources/schema-platform-settings.sql`
     * `backend/src/main/resources/schema-audit-logs.sql`

---

## 4. Backend Setup & Local Execution

### 1. Configuration
Open `backend/src/main/resources/application-dev.properties` (or set environment variables matching `.env.example`).
Verify the database credentials:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/career_advisor?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

### 2. Start the Spring Boot Application
From the repository root:
* **Windows (PowerShell / Command Prompt):**
  ```powershell
  cd backend
  .\mvnw.cmd spring-boot:run
  ```
* **macOS / Linux:**
  ```bash
  cd backend
  chmod +x ./mvnw
  ./mvnw spring-boot:run
  ```
The backend API server will start on `http://localhost:8080`.

### 3. Run Backend Automated Tests
Execute the full 130-test suite with Surefire:
```bash
cd backend
./mvnw test        # macOS / Linux
.\mvnw.cmd test    # Windows
```

---

## 5. Frontend Setup & Local Execution

### 1. Install Dependencies
```bash
cd frontend
npm install
```

### 2. Configure Environment Variables
Copy the environment template:
```bash
cp .env.example .env.local
```
Inside `frontend/.env.local`, verify:
```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
NEXT_PUBLIC_WEBRTC_STUN_URL=stun:stun.l.google.com:19302
```

### 3. Start Next.js Development Server
```bash
npm run dev
```
The frontend user interface will be accessible at `http://localhost:3000`.

### 4. Build Optimized Production Assets
```bash
npm run build
```

---

## 6. Environment Variables Reference

| Variable | Scope | Description | Default / Example |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Backend | Active profile (`dev` or `prod`) | `dev` |
| `PORT` | Backend | HTTP port for Spring Boot | `8080` |
| `SPRING_DATASOURCE_URL` | Backend | JDBC connection string | `jdbc:mysql://localhost:3306/career_advisor` |
| `SPRING_DATASOURCE_USERNAME`| Backend | MySQL database user | `root` |
| `SPRING_DATASOURCE_PASSWORD`| Backend | MySQL database password | *(secret)* |
| `JWT_SECRET` | Backend | 256-bit cryptographic signing secret | `404E635266556A586E327235753878...` |
| `JWT_EXPIRATION_MS` | Backend | Token validity duration | `86400000` (24 hours) |
| `CORS_ALLOWED_ORIGINS` | Backend | Allowed CORS origins | `http://localhost:3000,http://127.0.0.1:3000` |
| `ADMIN_EMAIL` | Backend | Initial bootstrap administrator email | `admin@careeradvisor.dev` |
| `ADMIN_PASSWORD` | Backend | Initial bootstrap administrator password | `AdminPass123!` |
| `ADMIN_NAME` | Backend | Initial bootstrap administrator display name| `System Administrator` |
| `AI_ENABLED` | Backend | Enable AI features | `true` |
| `AI_PROVIDER` | Backend | AI provider (`mock`, `gemini`, `openai`) | `mock` |
| `AI_API_KEY` | Backend | Provider API key | *(optional in dev)* |
| `MENTOR_MIN_MENTEES` | Backend | Default minimum mentor capacity | `1` |
| `MENTOR_MAX_MENTEES` | Backend | Default maximum mentor capacity | `10` |
| `MENTOR_DEFAULT_MENTEES` | Backend | Default assigned mentor capacity | `3` |
| `NEXT_PUBLIC_API_URL` | Frontend | REST API base URL | `http://localhost:8080` |
| `NEXT_PUBLIC_WS_URL` | Frontend | WebSocket endpoint | `ws://localhost:8080/ws` |
| `NEXT_PUBLIC_WEBRTC_STUN_URL`| Frontend| STUN server for WebRTC | `stun:stun.l.google.com:19302` |

---

## 7. Authentication, Roles & Security Model

### User & Admin Roles
* **`ROLE_USER`:** Default role assigned upon registration at `/signup` (`POST /api/auth/register`). Grants access to career exploration, coding problem solving, resumes, quizzes, mock interviews, and applying for mentorship.
* **`ROLE_ADMIN`:** Administrative authority required for `/admin/**` frontend routes and `/api/admin/**` REST endpoints.

### Administrator Provisioning
There is **no public registration endpoint** for administrators. Administrative accounts are created in one of two secure ways:
1. **Automated Bootstrap:** On startup, `AdminDataInitializer.java` verifies if an account exists for `ADMIN_EMAIL`. If absent, it creates an active `ROLE_ADMIN` user with credentials hashed using BCrypt.
2. **Promotion by Existing Admin:** A logged-in administrator can promote or update user roles through the User Governance portal (`/admin/users`).

### Account Suspension & Real-Time Isolation
* **HTTP Layer:** `JwtAuthenticationFilter` validates user status against the live database on every request. Suspended or deactivated users immediately receive `403 Forbidden`.
* **WebSocket Inbound:** `WebSocketSecurityInterceptor` blocks `CONNECT`, `SUBSCRIBE`, and `SEND` STOMP frames from suspended or deactivated accounts.
* **WebSocket Outbound:** `WebSocketSecurityInterceptor` on `clientOutboundChannel` checks recipient status and drops `MESSAGE` frames to suspended users, preventing real-time data leaks.
* **Self-Suspension Guard:** Administrators cannot suspend their own accounts.
* **Last-Admin Guard:** The system rejects suspension or role demotion of the last remaining active administrator.

---

## 8. Deployment Blueprints (Railway & Vercel)

### Backend Deployment (Railway)
1. In the [Railway Dashboard](https://railway.app), create or link the project to this GitHub repository.
2. Configure a Managed MySQL service in Railway.
3. In the backend service settings:
   * **Root Directory:** `/` (or `/backend`)
   * **Build:** Utilizes `backend/Dockerfile`
4. Set Railway Environment Variables:
   * `SPRING_PROFILES_ACTIVE=prod`
   * `SPRING_DATASOURCE_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true`
   * `SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}`
   * `SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}`
   * `JWT_SECRET=<secure-random-256-bit-key>`
   * `CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app`
5. Verify deployment:
   ```bash
   curl -I https://your-backend.up.railway.app/api/auth/health
   # Returns HTTP/1.1 200 OK
   ```

### Frontend Deployment (Vercel)
1. Import the repository in [Vercel](https://vercel.com).
2. Set **Root Directory** to `frontend`.
3. Set **Framework Preset** to `Next.js`.
4. Configure Environment Variables in Vercel Project Settings:
   * `NEXT_PUBLIC_API_URL=https://your-backend.up.railway.app`
   * `NEXT_PUBLIC_API_BASE_URL=https://your-backend.up.railway.app`
   * `NEXT_PUBLIC_WS_URL=wss://your-backend.up.railway.app/ws`
5. Deploy. Vercel automatically runs `npm run build` and optimizes static assets.

---

## 9. Development Workflow & IDE Tips

### IntelliJ IDEA
1. Open the repository root or `backend` folder in IntelliJ IDEA.
2. Navigate to **Settings > Build, Execution, Deployment > Compiler > Annotation Processors**.
3. Enable **"Enable annotation processing"** to ensure Lombok annotations (`@Getter`, `@Builder`, etc.) generate code during compilation.
4. Set Project SDK to Java 17.

### Visual Studio Code
1. Open repository root in VS Code.
2. Install recommended extensions:
   * *Extension Pack for Java* (Microsoft)
   * *Tailwind CSS IntelliSense* (Tailwind Labs)
   * *ESLint* (Microsoft)
3. Ensure the Java extension points to your local JDK 17 installation.

### Git Branching & Contribution Workflow
1. Clone the repository:
   ```bash
   git clone https://github.com/adnankhan00001/Career-Advisor.git
   cd Career-Advisor
   ```
2. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Run verification before committing:
   ```bash
   cd backend && .\mvnw.cmd test
   cd ../frontend && npm run build
   ```
4. Commit and push:
   ```bash
   git add .
   git commit -m "feat(module): description of changes"
   git push origin feature/your-feature-name
   ```

---

## 10. Verification & Smoke Test Matrix

| Component | Target URL / Command | Expected Result |
| :--- | :--- | :--- |
| **Backend Root** | `GET /` | `HTTP 200 "Backend is running 🚀"` |
| **Auth Service Health** | `GET /api/auth/health` | `HTTP 200 {"status":"UP","message":"Authentication service is running"}` |
| **Admin API Health** | `GET /api/admin/health` (with Admin JWT) | `HTTP 200 {"status":"UP","service":"Admin Governance & Metrics Service"}` |
| **Admin Overview** | `GET /admin` (in browser) | Loads telemetry charts, KPI summary cards, and activity stream |
| **User Governance** | `GET /admin/users` (in browser) | Paginated table of users with role/status filters and status modal |
| **Audit Logs** | `GET /admin/audit-logs` (in browser) | Displays searchable, filterable administrative event log |
| **Platform Settings** | `GET /admin/settings` (in browser) | Renders persistent mentorship capacity configuration form |
| **WebSocket / STOMP** | `CONNECT /ws` with valid JWT | Connects STOMP session; rejects suspended or deactivated users |
