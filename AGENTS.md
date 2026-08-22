# AGENTS.md — Agent & Developer Guidelines for Career-Advisor

This repository follows strict, permanent development rules. Every AI agent and human developer operating on this project MUST follow the guidelines defined here.

---

## 1. Permanent Rules & Workflow

The permanent single source of truth for project state, roadmap, features, architecture, and development history is:

```text
docs/PROJECT_STATUS.md
```

### Mandatory Workflow for Every Task

```text
1. Read AGENTS.md
        ↓
2. Read docs/PROJECT_STATUS.md
        ↓
3. Inspect the relevant existing code
        ↓
4. Understand current implementation
        ↓
5. Plan the change
        ↓
6. Implement the change
        ↓
7. Test the change
        ↓
8. Update docs/PROJECT_STATUS.md
        ↓
9. Report structured phase status to user
        ↓
10. Identify the next logical step
```

---

## 2. Repository Structure

```text
Career-Advisor/
├── backend/                      # Spring Boot 3.3.4 (Java 17) REST API
│   ├── src/main/java/            # Spring Boot controllers, services, models, DTOs, security
│   ├── src/main/resources/       # application.properties (DB, server port 8080)
│   ├── pom.xml                   # Maven dependencies (JPA, Security, MySQL, Validation)
│   └── mvnw / mvnw.cmd           # Maven wrapper scripts
├── frontend/                     # Next.js 16.2 (Turbopack) + React 19 + Tailwind CSS 4
│   ├── app/                      # App router pages (Landing, Login, Signup, Quiz, Protected routes)
│   │   ├── (protected)/          # Dashboard, Careers, Roadmap, Skills, Practice, Profile
│   │   ├── login/ & signup/      # Authentication pages
│   │   ├── quiz/                 # Career assessment quiz
│   │   └── page.tsx              # Landing page
│   ├── components/               # UI components (Navbar, CareerCard, ProblemModal, PersonalizedPlan)
│   ├── lib/                      # Business logic, static data, API services
│   └── package.json              # Next.js & React dependencies
├── docs/                         # Project documentation
│   └── PROJECT_STATUS.md         # Single source of truth for project state & history
└── AGENTS.md                     # This file
```

---

## 3. Technology Stack

- **Frontend**: Next.js 16.2.3 (App Router, Turbopack), React 19.2.4, Tailwind CSS 4, Framer Motion
- **Backend**: Spring Boot 3.3.4, Java 17, Spring Security 6, Spring Data JPA, JJWT 0.12.6, Maven
- **Database**: MySQL 8.0 (schema `career_advisor`)
- **Architecture**: Decoupled Frontend (port 3000) & Backend REST API (port 8080)

---

## 4. Documentation Maintenance Rules

1. **Automatic Updates**: Never wait for the user to prompt for a documentation update. Update `docs/PROJECT_STATUS.md` after EVERY meaningful development step.
2. **Never Fabricate Status**: If something is partial or broken, document it accurately. A feature is `COMPLETE` only when code exists, builds/tests pass, and behavior is verified.
3. **Preserve History**: Maintain chronological entries in `docs/PROJECT_STATUS.md` without overwriting past history.
4. **Track Tests, Bugs & Architecture**: Record test outputs, bug fixes, schema changes, and architectural decisions explicitly.

---

## 5. Mandatory Phase Status Reporting

At the end of **EVERY** meaningful development task or phase, after implementation, testing, and documentation updates, **ALWAYS** provide a structured status report to the user in your response.

### Required Report Structure

```markdown
# Phase X Status

## Completed
1. <feature/module completed>
2. <feature/module completed>
3. <feature/module completed>

## Tested
1. **Backend Compilation**
   - Command: `.\mvnw clean compile`
   - Result: PASS / FAIL
   - Number of Java source files compiled: <N>

2. **Backend Tests**
   - Test suite name: `<file/suite>`
   - Result: X/X PASS
   - Percentage: <N>%

3. **Frontend Production Build**
   - Command: `npm run build`
   - Result: PASS / FAIL
   - Number of routes generated: <N>

4. **End-to-End Testing**
   - Test suite name: `<file/suite>`
   - Result: X/X PASS
   - Percentage: <N>%

## Architecture Flow
Provide a concise ASCII architecture diagram showing:
Frontend → API Client → Spring Boot REST API → Security/JWT → Services → JPA/Hibernate → MySQL
(Including the newly implemented module and its connections).

## Files Changed / Added
Group files by:
### Backend
- file
### Frontend
- file
### Database / Entities
- file
### Documentation
- file

## API Endpoints
If APIs were created or modified, provide:
| Method | Endpoint | Authentication | Purpose | Status |
|---|---|---|---|---|
| GET/POST | /api/... | JWT / Public | ... | PASS |

*(If no APIs changed, explicitly state: `No API changes in this phase.`)*

## Database Changes
State new tables, modified tables, relationships, constraints, or seeded data.
*(If no database changes, explicitly state: `No database changes in this phase.`)*

## Security
Report authentication requirements, JWT usage, user isolation, authorization rules, and sensitive decisions.

## Known Issues
Explicitly state `None.` if there are genuinely no known issues.
Otherwise list every known issue with severity, description, and status.

## Documentation
Always state: `docs/PROJECT_STATUS.md — UPDATED` and summarize what was recorded.

## Current Phase
Use:
- `Phase X: <Name> — COMPLETE` (ONLY if all criteria passed)
- `Phase X: <Name> — PARTIALLY COMPLETE`
- `Phase X: <Name> — IN PROGRESS`

## Next Phase
State the next logical phase and briefly explain why it comes next. Do NOT start the next phase automatically.

## Completion Matrix
| Component | Status |
|---|---|
| Domain Model | PASS / FAIL / N/A |
| Repository | PASS / FAIL / N/A |
| Service | PASS / FAIL / N/A |
| Controller/API | PASS / FAIL / N/A |
| Database | PASS / FAIL / N/A |
| Security | PASS / FAIL / N/A |
| Frontend | PASS / FAIL / N/A |
| Testing | PASS / FAIL |
| Backend Build | PASS / FAIL |
| Frontend Build | PASS / FAIL |
| Documentation | UPDATED / PENDING |
```

---

## 6. Critical Operational Rules

1. **Non-Optional Reporting**: The status report is mandatory. The user should never have to prompt for status, test results, changed files, or next steps.
2. **Accuracy Rule**: Never fabricate test results. Only report `PASS` when tests or builds were actually executed successfully. Use `NOT TESTED`, `PARTIAL`, or `FAIL` where applicable. Never convert "implemented" into "tested".
3. **Documentation + Status Separation**: Updating `docs/PROJECT_STATUS.md` and providing the structured status report in the response are two separate, mandatory requirements. Doing one does not replace the other.
