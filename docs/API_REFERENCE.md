# Career-Advisor REST API Specification & Reference Manual

> **Base URL**: `/api` (or environment-configured `NEXT_PUBLIC_API_URL`)  
> **Authentication**: JSON Web Token (JWT) in `Authorization: Bearer <token>` header  
> **Data Format**: `application/json` (Multipart for resume upload: `multipart/form-data`)

---

## 1. Authentication Endpoints (`/api/auth`)

### 1.1 Register New Candidate
- **Endpoint**: `POST /api/auth/register` (or `POST /api/auth/signup`)
- **Access**: Public
- **Request Body**:
  ```json
  {
    "name": "Jane Doe",
    "email": "jane.doe@example.com",
    "password": "Password123!"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "id": 1,
    "name": "Jane Doe",
    "email": "jane.doe@example.com",
    "role": "USER"
  }
  ```
- **Error Codes**: `400 Bad Request` (Validation error), `409 Conflict` (Email already exists).

### 1.2 User Login
- **Endpoint**: `POST /api/auth/login`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "jane.doe@example.com",
    "password": "Password123!"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "id": 1,
    "name": "Jane Doe",
    "email": "jane.doe@example.com",
    "role": "USER"
  }
  ```
- **Error Codes**: `401 Unauthorized` (Invalid credentials).

### 1.3 Get Current Profile / Token Revalidation
- **Endpoint**: `GET /api/auth/me`
- **Access**: Authenticated (`ROLE_USER` or `ROLE_ADMIN`)
- **Response** (`200 OK`):
  ```json
  {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane.doe@example.com",
    "role": "USER"
  }
  ```
- **Error Codes**: `401 Unauthorized`.

### 1.4 Liveness Health Check
- **Endpoint**: `GET /api/auth/health`
- **Access**: Public
- **Response** (`200 OK`):
  ```json
  {
    "status": "UP",
    "message": "Authentication service is running"
  }
  ```

---

## 2. Careers Catalog (`/api/careers`)

### 2.1 List All Career Tracks
- **Endpoint**: `GET /api/careers`
- **Access**: Public
- **Response** (`200 OK`): Array of 7 career tracks (`id`, `title`, `description`, `requiredSkills`, `matchScore`).

### 2.2 Get Career Details
- **Endpoint**: `GET /api/careers/{idOrTitle}`
- **Access**: Public
- **Response** (`200 OK`): Single career track object.
- **Error Codes**: `404 Not Found`.

---

## 3. Roadmaps (`/api/roadmaps`)

### 3.1 Get All Roadmaps
- **Endpoint**: `GET /api/roadmaps`
- **Access**: Public
- **Response** (`200 OK`): Map of career track titles to section step arrays.

### 3.2 Get Roadmap for Specific Career
- **Endpoint**: `GET /api/roadmaps/{careerTitle}`
- **Access**: Public
- **Response** (`200 OK`): Array of `RoadmapSectionDto` objects.
- **Error Codes**: `404 Not Found`.

---

## 4. Skills Portfolio (`/api/skills`)

### 4.1 Get User Skills
- **Endpoint**: `GET /api/skills`
- **Access**: Authenticated (`ROLE_USER` or `ROLE_ADMIN`)
- **Response** (`200 OK`): `["Java", "Spring Boot", "MySQL", "Docker"]`

### 4.2 Add Skill to Portfolio
- **Endpoint**: `POST /api/skills`
- **Access**: Authenticated
- **Request Body**: `{"skill": "Kubernetes"}`
- **Response** (`200 OK`): Updated list of verified skills.

### 4.3 Remove Skill from Portfolio
- **Endpoint**: `DELETE /api/skills/{skillName}`
- **Access**: Authenticated
- **Response** (`200 OK`): Updated list of skills after deletion.

---

## 5. Learning Progress (`/api/progress`)

### 5.1 Get Progress Summary
- **Endpoint**: `GET /api/progress/summary`
- **Access**: Authenticated
- **Response** (`200 OK`):
  ```json
  {
    "careerGoal": "Java Backend Developer",
    "completedStepsCount": 4,
    "totalStepsCount": 24,
    "roadmapProgressPercentage": 17,
    "completedSteps": ["Java Syntax", "OOP Principles"],
    "verifiedSkills": ["Java", "SQL"],
    "overallReadinessScore": 35
  }
  ```

### 5.2 Toggle Roadmap Step Completion
- **Endpoint**: `POST /api/progress/roadmap/toggle`
- **Access**: Authenticated
- **Request Body**: `{"stepTitle": "REST API Architecture", "careerTitle": "Java Backend Developer"}`
- **Response** (`200 OK`): Updated list of completed step titles.

### 5.3 Update Target Career Goal
- **Endpoint**: `POST /api/progress/career-goal`
- **Access**: Authenticated
- **Request Body**: `{"careerGoal": "Full Stack Developer"}`
- **Response** (`200 OK`): `{"careerGoal": "Full Stack Developer", "message": "Career goal updated successfully"}`

### 5.4 Reset Roadmap Progress
- **Endpoint**: `POST /api/progress/reset`
- **Access**: Authenticated
- **Response** (`200 OK`): `{"message": "Roadmap progress reset successfully"}`

---

## 6. Quiz Assessment (`/api/quiz`)

### 6.1 Submit Quiz Assessment
- **Endpoint**: `POST /api/quiz/submit` (or `POST /quiz/submit`)
- **Access**: Public / Authenticated (associates attempt with candidate if authenticated)
- **Request Body**: `{"answers": {"1": "B", "2": "A", ...}}`
- **Response** (`200 OK`):
  ```json
  {
    "score": 8,
    "totalQuestions": 10,
    "percentage": 80,
    "level": "Intermediate",
    "recommendedCareer": "Java Backend Developer"
  }
  ```

### 6.2 Get Latest Quiz Result
- **Endpoint**: `GET /api/quiz/latest`
- **Access**: Authenticated
- **Response** (`200 OK`): Last recorded quiz attempt score and level.

---

## 7. DSA Practice & Coding Workspace (`/api/problems`)

### 7.1 List Problems
- **Endpoint**: `GET /api/problems`
- **Parameters**: `?category=ARRAYS&topic=TwoPointer&difficulty=MEDIUM`
- **Access**: Public (attaches user solved state if authenticated)
- **Response** (`200 OK`): Array of `CodingProblemDto` (22 curated problems).

### 7.2 Get Problem Detail
- **Endpoint**: `GET /api/problems/{idOrSlug}`
- **Access**: Public
- **Response** (`200 OK`): Problem description, constraints, starter code, sample input/output.

### 7.3 Run In-Browser Simulation
- **Endpoint**: `POST /api/problems/{id}/run`
- **Access**: Public
- **Request Body**: `{"code": "...", "language": "java"}`
- **Response** (`200 OK`): `{"status": "ACCEPTED", "passedCases": 5, "totalCases": 5, "output": "..."}`

### 7.4 Submit Final Solution
- **Endpoint**: `POST /api/problems/{id}/submit`
- **Access**: Authenticated
- **Response** (`200 OK`): Execution result + persists completion in `user_problem_progress`.

---

## 8. Personalized Intelligence (`/api/recommendations`)

### 8.1 Get Actionable Readiness & Priority Actions
- **Endpoint**: `GET /api/recommendations`
- **Access**: Authenticated
- **Response** (`200 OK`): Composite readiness score ($0-100\%$), user lifecycle state (`ONBOARDING`, `BEGINNER`, `INTERMEDIATE`, `JOB_READY`), high-priority missing skills, targeted practice actions, and resume sync prompts.

---

## 9. Timed Mock Interviews (`/api/interviews`)

### 9.1 Start Interview Session
- **Endpoint**: `POST /api/interviews`
- **Access**: Authenticated
- **Request Body**: `{"category": "JAVA_BACKEND", "difficulty": "MEDIUM", "durationMinutes": 10}`
- **Response** (`200 OK`): Active session object with 5 questions (answers masked).

### 9.2 Save Answer
- **Endpoint**: `POST /api/interviews/{id}/answers`
- **Access**: Authenticated
- **Request Body**: `{"questionId": 101, "selectedOption": "A"}`
- **Response** (`200 OK`): Updated session state.

### 9.3 Submit Interview for Evaluation
- **Endpoint**: `POST /api/interviews/{id}/submit`
- **Access**: Authenticated
- **Response** (`200 OK`): Evaluated score ($0-100\%$), accuracy, strong/weak areas, unlocked explanations.

### 9.4 Get Interview History & Summary
- **Endpoint**: `GET /api/interviews`, `GET /api/interviews/summary`
- **Access**: Authenticated
- **Response** (`200 OK`): Candidate interview session history and aggregate accuracy telemetry.

---

## 10. Resume Analyzer & Skill Extraction (`/api/resumes`)

### 10.1 Upload and Analyze Resume
- **Endpoint**: `POST /api/resumes/upload`
- **Content-Type**: `multipart/form-data`
- **Access**: Authenticated
- **Form Param**: `file` (PDF or DOCX, max 5MB)
- **Response** (`201 Created`):
  ```json
  {
    "resumeId": 1,
    "fileName": "Jane_Resume.pdf",
    "parsingStatus": "PARSED",
    "extractedContact": {"email": "jane@example.com", "phone": "+1-555-0199"},
    "extractedSkills": [{"skill": "Java", "normalizedName": "Java", "synced": false}],
    "careerMatches": [{"careerTitle": "Java Backend Developer", "matchPercentage": 85}],
    "skillGaps": [{"careerTitle": "Java Backend Developer", "missingSkills": ["Docker"]}]
  }
  ```
- **Error Codes**: `413 Payload Too Large` (>5MB), `415 Unsupported Media Type` (Invalid extension or magic bytes).

### 10.2 Sync Resume Skills to Profile
- **Endpoint**: `POST /api/resumes/{id}/sync-skills`
- **Access**: Authenticated
- **Request Body**: `{"skills": ["Java", "Docker"]}`
- **Response** (`200 OK`): List of newly synchronized skills.

### 10.3 Delete Resume
- **Endpoint**: `DELETE /api/resumes/{id}`
- **Access**: Authenticated
- **Response** (`200 OK`): `{"message": "Resume deleted successfully"}`

---

## 11. Platform Governance & Admin (`/api/admin`)

### 11.1 Admin Profile & Health Check
- **Endpoints**: `GET /api/admin/me`, `GET /api/admin/health`
- **Access**: `ROLE_ADMIN` strictly enforced
- **Error Codes**: `403 Forbidden` for standard candidates, `401 Unauthorized` for anonymous.

### 11.2 Platform KPI Overview Telemetry
- **Endpoint**: `GET /api/admin/stats/overview`
- **Access**: `ROLE_ADMIN` strictly enforced
- **Response** (`200 OK`):
  ```json
  {
    "totalUsers": 120,
    "totalResumes": 45,
    "totalQuizAttempts": 85,
    "totalSolvedProblems": 340,
    "totalMockInterviews": 90,
    "completedMockInterviews": 75,
    "careerGoalsDistribution": {"Java Backend Developer": 60, "Frontend Engineer": 35}
  }
  ```

### 11.3 Candidate Directory & Search
- **Endpoint**: `GET /api/admin/users?search=Jane`
- **Access**: `ROLE_ADMIN` strictly enforced
- **Response** (`200 OK`): List of `AdminUserDto` (passwords strictly omitted).

### 11.4 Candidate Deep Inspection
- **Endpoint**: `GET /api/admin/users/{id}`
- **Access**: `ROLE_ADMIN` strictly enforced
- **Response** (`200 OK`): Detailed candidate portfolio, verified skills, resume status, and interview metrics.
