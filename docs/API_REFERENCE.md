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

---

## 12. AI Infrastructure & Personal AI Endpoints (`/api/ai`)

### 12.1 AI Infrastructure Health Check
- **Endpoint**: `GET /api/ai/health`
- **Access**: Public / Authenticated
- **Description**: Probes AI infrastructure availability, active provider mode (`mock`, `openai`, `gemini`), and configured model without exposing secrets.
- **Response** (`200 OK`):
  ```json
  {
    "enabled": true,
    "provider": "mock",
    "available": true,
    "model": "gemini-1.5-flash",
    "message": "AI service is active and available."
  }
  ```

### 12.2 Personal AI Context
- **Endpoint**: `GET /api/ai/context`
- **Access**: Authenticated (`ROLE_USER` or `ROLE_ADMIN`)
- **Description**: Aggregates the authoritative personal context for the authenticated user (profile, verified skills, resume summary, quiz scores, roadmap milestone progress, DSA problem metrics, mock interview history, and recommendation readiness score).
- **Security**: Derives identity exclusively from `@AuthenticationPrincipal CustomUserDetails`. Strictly omits passwords, hashes, salts, and secrets.
- **Response** (`200 OK`):
  ```json
  {
    "userProfile": {
      "id": 1,
      "name": "Jane Doe",
      "email": "jane.doe@example.com",
      "careerGoal": "Java Backend Developer",
      "userLevel": "Intermediate"
    },
    "targetCareerGoal": "Java Backend Developer",
    "verifiedSkills": ["Java", "Spring Boot", "MySQL"],
    "resumeSummary": {
      "resumeId": 4,
      "fileName": "Jane_Resume.pdf",
      "uploadTimestamp": "2026-08-22T19:30:00",
      "parsingStatus": "PARSED",
      "extractedSkills": ["Java", "Spring Boot", "Docker"]
    },
    "quizAssessment": {
      "score": 8,
      "totalQuestions": 10,
      "percentage": 80,
      "evaluatedLevel": "Intermediate",
      "recommendedCareer": "Java Backend Developer"
    },
    "roadmapProgress": {
      "careerGoal": "Java Backend Developer",
      "completedStepsCount": 3,
      "totalStepsCount": 12,
      "completionPercentage": 25,
      "nextRecommendedStep": "Spring Data JPA & Hibernate"
    },
    "dsaProgress": {
      "solvedCount": 5,
      "totalCount": 22,
      "completionPercentage": 22,
      "categoryDistribution": {"ARRAYS": 2, "TREES": 1}
    },
    "mockInterviewPerformance": {
      "totalSessions": 2,
      "completedSessions": 2,
      "averageScore": 75,
      "bestScore": 85,
      "strongAreas": ["Java Core", "OOP"],
      "weakAreas": ["Concurrency"]
    },
    "recommendations": {
      "overallReadinessScore": 68,
      "userLifecycleState": "INTERMEDIATE",
      "topCareerMatch": "Java Backend Developer",
      "topCareerMatchPercentage": 85,
      "missingSkills": ["Kubernetes", "AWS"]
    },
    "contextTimestamp": "2026-08-22T14:00:00Z"
  }
  ```
- **Error Codes**: `401 Unauthorized` (Unauthenticated).

### 12.3 Contextual AI Chat Completion
- **Endpoint**: `POST /api/ai/chat`
- **Access**: Authenticated
- **Request Body**:
  ```json
  {
    "message": "What should be my next focus to improve my backend interview readiness?",
    "includePersonalContext": true,
    "conversationId": "conv_123"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "response": "Based on your Career Advisor profile, focus on completing your next roadmap milestone...",
    "status": "SUCCESS",
    "provider": "mock",
    "model": "gemini-1.5-flash",
    "tokensUsed": 45,
    "latencyMs": 12,
    "conversationId": "conv_123",
    "timestamp": "2026-08-22T19:30:00"
  }
  ```
- **Error Codes**: `401 Unauthorized`, `400 Bad Request` (Empty message).

---

## 13. Persistent Personal AI Chatbot (`/api/conversations`)

### 13.1 Create Persistent AI Conversation
- **Endpoint**: `POST /api/conversations`
- **Access**: Authenticated (owner derived strictly from JWT)
- **Request Body** *(Optional)*:
  ```json
  {
    "title": "Spring Boot Guidance"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "id": 1,
    "title": "Spring Boot Guidance",
    "conversationType": "USER_TO_AI",
    "archived": false,
    "messageCount": 0,
    "createdAt": "2026-08-23T11:30:00",
    "updatedAt": "2026-08-23T11:30:00",
    "lastMessageAt": "2026-08-23T11:30:00",
    "messages": []
  }
  ```

### 13.2 List User Conversations
- **Endpoint**: `GET /api/conversations`
- **Access**: Authenticated
- **Response** (`200 OK`): Array of user's `ConversationResponseDto` records ordered by `updatedAt DESC`.

### 13.3 Get Conversation & Message Stream
- **Endpoint**: `GET /api/conversations/{id}`
- **Access**: Authenticated (Owner only)
- **Response** (`200 OK`): `ConversationResponseDto` including chronological `messages` array.
- **Error Codes**: `404 Not Found` (Zero-Trust IDOR protection if not owned).

### 13.4 Send Message in Conversation
- **Endpoint**: `POST /api/conversations/{id}/messages`
- **Access**: Authenticated (Owner only)
- **Request Body**:
  ```json
  {
    "content": "What should I learn for Spring Boot to become a senior backend engineer?"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "conversationId": 1,
    "conversationTitle": "Spring Boot Guidance",
    "userMessage": {
      "id": 1,
      "conversationId": 1,
      "senderType": "USER",
      "senderName": "Alice Candidate",
      "content": "What should I learn for Spring Boot to become a senior backend engineer?",
      "sequenceNumber": 1,
      "status": "SENT",
      "createdAt": "2026-08-23T11:30:05"
    },
    "aiMessage": {
      "id": 2,
      "conversationId": 1,
      "senderType": "AI",
      "senderName": "OneStop AI Advisor",
      "content": "Based on your verified skills and roadmap progress...",
      "sequenceNumber": 2,
      "status": "DELIVERED",
      "createdAt": "2026-08-23T11:30:06"
    },
    "status": "SUCCESS",
    "provider": "mock",
    "model": "gemini-1.5-flash",
    "tokensUsed": 65,
    "latencyMs": 14
  }
  ```
- **Error Codes**: `400 Bad Request` (Blank, oversized >4000 chars, or archived conversation), `404 Not Found` (Unowned conversation).

### 13.5 Archive Conversation
- **Endpoint**: `POST /api/conversations/{id}/archive`
- **Access**: Authenticated (Owner only)
- **Response** (`200 OK`): Updated conversation object with `archived: true`. Subsequent writes are rejected with `400 Bad Request`.

### 13.6 Delete Conversation
- **Endpoint**: `DELETE /api/conversations/{id}`
- **Access**: Authenticated (Owner only)
- **Response** (`200 OK`): `{"message": "Conversation deleted successfully"}`. Deletes conversation and cascades message deletion.

---

## 14. Real-Time Human Communication Endpoints (`/api/conversations` & `/ws`)

### 14.1 Candidate Discovery Search
- **Endpoint**: `GET /api/users/search?q={query}`
- **Access**: Authenticated (`ROLE_USER` or `ROLE_ADMIN`)
- **Query Parameter**: `q` (minimum 2 characters).
- **Response** (`200 OK`): Safe candidate projection (omits password, email hash, contact details; includes `online` status; excludes caller).

### 14.2 Start Direct Peer Conversation
- **Endpoint**: `POST /api/conversations/user`
- **Access**: Authenticated (`ROLE_USER` or `ROLE_ADMIN`)
- **Request Body**:
  ```json
  {
    "targetUserId": 2,
    "initialMessage": "Hi Bob, let's practice system design!"
  }
  ```
- **Response** (`201 Created`): `USER_TO_USER` conversation metadata with 2 participants.

### 14.3 Start Admin Support Ticket
- **Endpoint**: `POST /api/conversations/admin`
- **Access**: Authenticated (`ROLE_USER`)
- **Request Body**:
  ```json
  {
    "subject": "Mock interview feedback inquiry",
    "initialMessage": "Can you explain question #3 evaluation?"
  }
  ```
- **Response** (`201 Created`): `USER_TO_ADMIN` support conversation metadata.

### 14.4 List Human Conversations
- **Endpoint**: `GET /api/conversations/human`
- **Access**: Authenticated (`ROLE_USER` or `ROLE_ADMIN`)
- **Response** (`200 OK`): List of user's active human conversations with participants and unread counters.

### 14.5 Send Human Message
- **Endpoint**: `POST /api/conversations/{id}/human-messages`
- **Access**: Authenticated (Participant only)
- **Request Body**: `{"content": "Sounds good, let's connect."}`
- **Response** (`200 OK`): Created message with `sequenceNumber`, `status: "SENT"`, and broadcasts event via STOMP broker.

### 14.6 Mark Conversation Read
- **Endpoint**: `POST /api/conversations/{id}/read`
- **Access**: Authenticated (Participant only)
- **Response** (`200 OK`): Updates `lastReadAt`, marks unread messages as `READ`, and broadcasts `MESSAGE_READ` event.

### 14.7 Emit Typing Indicator
- **Endpoint**: `POST /api/conversations/{id}/typing`
- **Access**: Authenticated (Participant only)
- **Request Body**: `{"typing": true}`
- **Response** (`200 OK`): Broadcasts `TYPING_STARTED` or `TYPING_STOPPED` to active conversation room.

### 14.8 Admin Support Inbox
- **Endpoint**: `GET /api/conversations/admin/inbox`
- **Access**: Authenticated (`ROLE_ADMIN` strictly enforced)
- **Response** (`200 OK`): All `USER_TO_ADMIN` support tickets with candidate information and message history.

### 14.9 STOMP WebSocket Messaging Reference
- **WebSocket Endpoint**: `/ws` (supports SockJS fallback)
- **CONNECT Frame Authentication**: `Authorization: Bearer <jwt_token>` header required.
- **Topic Subscriptions**:
  - `/topic/conversations/{id}`: Real-time message push, read receipts, typing indicators, and WebRTC calling signaling (Participant authorization validated on subscribe).
  - `/topic/presence`: Real-time candidate online/offline status updates (`USER_ONLINE`, `USER_OFFLINE`).

---

## 15. WebRTC Audio & Video Calling Endpoints (`/api/calls` & STOMP Signaling)

### 15.1 Initiate Call
- **Endpoint**: `POST /api/calls`
- **Access**: Authenticated (`ROLE_USER` or `ROLE_ADMIN`)
- **Request Body**:
  ```json
  {
    "conversationId": 1,
    "callType": "AUDIO"
  }
  ```
  *(Supported `callType`: `AUDIO`, `VIDEO`)*
- **Response** (`201 Created`):
  ```json
  {
    "id": 10,
    "conversationId": 1,
    "callerId": 5,
    "callerName": "Alice",
    "receiverId": 8,
    "receiverName": "Bob",
    "callType": "AUDIO",
    "status": "RINGING",
    "startedAt": "2026-08-23T18:00:00Z",
    "durationSeconds": 0
  }
  ```
- **Error Codes**: `400 Bad Request` (Invalid conversation / AI conversation / self call), `404 Not Found` (Conversation not found), `409 Conflict` (`CALL_ALREADY_ACTIVE`).

### 15.2 Accept Call
- **Endpoint**: `POST /api/calls/{id}/accept`
- **Access**: Authenticated (Receiver only)
- **Response** (`200 OK`): Status transitions to `ACCEPTED`, records `answeredAt`, broadcasts `CALL_ACCEPTED` STOMP event.

### 15.3 Reject Call
- **Endpoint**: `POST /api/calls/{id}/reject`
- **Access**: Authenticated (Receiver only)
- **Request Body** *(Optional)*: `{"reason": "REJECTED"}`
- **Response** (`200 OK`): Status transitions to `REJECTED`, broadcasts `CALL_REJECTED` STOMP event.

### 15.4 Cancel Call
- **Endpoint**: `POST /api/calls/{id}/cancel`
- **Access**: Authenticated (Caller only)
- **Response** (`200 OK`): Status transitions to `CANCELLED`, broadcasts `CALL_CANCELLED` STOMP event.

### 15.5 End Call
- **Endpoint**: `POST /api/calls/{id}/end`
- **Access**: Authenticated (Caller or Receiver)
- **Request Body** *(Optional)*: `{"reason": "USER_ENDED"}`
- **Response** (`200 OK`): Status transitions to `ENDED`, calculates `durationSeconds`, broadcasts `CALL_ENDED` STOMP event.

### 15.6 Send WebRTC Signal
- **Endpoint**: `POST /api/calls/{id}/signal`
- **Access**: Authenticated (Active Call Participant only)
- **Request Body**:
  ```json
  {
    "type": "WEBRTC_OFFER",
    "sdp": "v=0\r\no=alice...",
    "candidate": null
  }
  ```
  *(Supported `type`: `WEBRTC_OFFER`, `WEBRTC_ANSWER`, `WEBRTC_ICE_CANDIDATE`)*
- **Response** (`200 OK`): Broadcasts WebRTC signal payload to destination conversation channel `/topic/conversations/{conversationId}`.
- **Error Codes**: `400 Bad Request` (Payload size > 50KB or invalid signal type), `404 Not Found` (Call not found or unauthorized).

### 15.7 Get Active Calls
- **Endpoint**: `GET /api/calls/active`
- **Access**: Authenticated
- **Response** (`200 OK`): Array of active incoming/outgoing `RINGING` or `ACCEPTED` call sessions for the authenticated user.

### 15.8 Get Call History
- **Endpoint**: `GET /api/calls/history`
- **Access**: Authenticated
- **Response** (`200 OK`): Chronological list of past call sessions involving the user, with durations, status, and termination reasons.
