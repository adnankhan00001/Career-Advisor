export const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_BASE_URL ||
  process.env.NEXT_PUBLIC_API_URL ||
  "http://localhost:8080"
).replace(/\/+$/, "");

if (
  typeof window !== "undefined" &&
  window.location.hostname !== "localhost" &&
  window.location.hostname !== "127.0.0.1" &&
  (API_BASE_URL.includes("localhost") || API_BASE_URL.includes("127.0.0.1"))
) {
  console.error(
    `[CareerAdvisor Config] CRITICAL: App is loaded on remote host '${window.location.hostname}', but API_BASE_URL is defaulting to localhost ('${API_BASE_URL}'). Set NEXT_PUBLIC_API_URL in your hosting provider environment settings.`
  );
}

export const WS_BASE_URL =
  process.env.NEXT_PUBLIC_WS_URL ||
  (API_BASE_URL.startsWith("https://")
    ? `wss://${API_BASE_URL.slice(8)}/ws`
    : API_BASE_URL.startsWith("http://")
    ? `ws://${API_BASE_URL.slice(7)}/ws`
    : "ws://localhost:8080/ws");

export const API_ENDPOINTS = {
  AUTH: {
    REGISTER: `${API_BASE_URL}/api/auth/register`,
    LOGIN: `${API_BASE_URL}/api/auth/login`,
    ME: `${API_BASE_URL}/api/auth/me`,
    HEALTH: `${API_BASE_URL}/api/auth/health`,
  },
  CAREERS: {
    LIST: `${API_BASE_URL}/api/careers`,
    DETAIL: (idOrTitle: string) =>
      `${API_BASE_URL}/api/careers/${encodeURIComponent(idOrTitle)}`,
  },
  ROADMAPS: {
    LIST: `${API_BASE_URL}/api/roadmaps`,
    DETAIL: (careerTitle: string) =>
      `${API_BASE_URL}/api/roadmaps/${encodeURIComponent(careerTitle)}`,
  },
  SKILLS: {
    BASE: `${API_BASE_URL}/api/skills`,
    DELETE: (skillName: string) =>
      `${API_BASE_URL}/api/skills/${encodeURIComponent(skillName)}`,
  },
  PROGRESS: {
    SUMMARY: `${API_BASE_URL}/api/progress/summary`,
    RECENT_ACTIVITY: `${API_BASE_URL}/api/progress/recent-activity`,
    ROADMAP: `${API_BASE_URL}/api/progress/roadmap`,
    TOGGLE: `${API_BASE_URL}/api/progress/roadmap/toggle`,
    CAREER_GOAL: `${API_BASE_URL}/api/progress/career-goal`,
    RESET: `${API_BASE_URL}/api/progress/reset`,
  },
  PROBLEMS: {
    LIST: `${API_BASE_URL}/api/problems`,
    DETAIL: (idOrSlug: string) =>
      `${API_BASE_URL}/api/problems/${encodeURIComponent(idOrSlug)}`,
    TOGGLE: (id: number) => `${API_BASE_URL}/api/problems/${id}/toggle`,
    RUN: (id: number) => `${API_BASE_URL}/api/problems/${id}/run`,
    SUBMIT: (id: number) => `${API_BASE_URL}/api/problems/${id}/submit`,
    PROGRESS_SUMMARY: `${API_BASE_URL}/api/problems/progress/summary`,
    CATEGORIES: `${API_BASE_URL}/api/problems/categories`,
  },
  RECOMMENDATIONS: {
    INTELLIGENCE: `${API_BASE_URL}/api/recommendations`,
  },
  INTERVIEWS: {
    BASE: `${API_BASE_URL}/api/interviews`,
    SESSION: (id: number | string) => `${API_BASE_URL}/api/interviews/${id}`,
    ANSWERS: (id: number | string) => `${API_BASE_URL}/api/interviews/${id}/answers`,
    SUBMIT: (id: number | string) => `${API_BASE_URL}/api/interviews/${id}/submit`,
    RESULT: (id: number | string) => `${API_BASE_URL}/api/interviews/${id}/result`,
    SUMMARY: `${API_BASE_URL}/api/interviews/summary`,
  },
  QUIZ: {
    SUBMIT: `${API_BASE_URL}/api/quiz/submit`,
    LATEST: `${API_BASE_URL}/api/quiz/latest`,
  },
  RESUMES: {
    BASE: `${API_BASE_URL}/api/resumes`,
    UPLOAD: `${API_BASE_URL}/api/resumes/upload`,
    ANALYSIS: (id: number | string) => `${API_BASE_URL}/api/resumes/${id}/analysis`,
    LATEST_ANALYSIS: `${API_BASE_URL}/api/resumes/latest/analysis`,
    DETAIL: (id: number | string) => `${API_BASE_URL}/api/resumes/${id}`,
    SYNC_SKILLS: (id: number | string) => `${API_BASE_URL}/api/resumes/${id}/sync-skills`,
  },
  ADMIN: {
    ME: `${API_BASE_URL}/api/admin/me`,
    HEALTH: `${API_BASE_URL}/api/admin/health`,
    STATS_OVERVIEW: `${API_BASE_URL}/api/admin/stats/overview`,
    USERS: `${API_BASE_URL}/api/admin/users`,
    USER_DETAIL: (id: number | string) => `${API_BASE_URL}/api/admin/users/${id}`,
    USER_STATUS: (id: number | string) => `${API_BASE_URL}/api/admin/users/${id}/status`,
    MENTORSHIP_SUMMARY: `${API_BASE_URL}/api/admin/mentorships/summary`,
    MENTORSHIP_LIST: `${API_BASE_URL}/api/admin/mentorships`,
    MENTORSHIP_DETAIL: (id: number | string) => `${API_BASE_URL}/api/admin/mentorships/${id}`,
    MENTORSHIP_TERMINATE: (id: number | string) => `${API_BASE_URL}/api/admin/mentorships/${id}/terminate`,
    ANALYTICS: {
      USERS: `${API_BASE_URL}/api/admin/analytics/users`,
      CAREER_PREPARATION: `${API_BASE_URL}/api/admin/analytics/career-preparation`,
      MENTORSHIP: `${API_BASE_URL}/api/admin/analytics/mentorship`,
    },
    AUDIT_LOGS: `${API_BASE_URL}/api/admin/audit-logs`,
    AUDIT_SUMMARY: `${API_BASE_URL}/api/admin/audit-logs/summary`,
    AUDIT_DETAIL: (id: number | string) => `${API_BASE_URL}/api/admin/audit-logs/${id}`,
    SETTINGS: `${API_BASE_URL}/api/admin/settings`,
  },
  AI: {
    HEALTH: `${API_BASE_URL}/api/ai/health`,
    CONTEXT: `${API_BASE_URL}/api/ai/context`,
    CHAT: `${API_BASE_URL}/api/ai/chat`,
    ACTION_PLAN: `${API_BASE_URL}/api/ai/action-plan`,
    ACTION_PLAN_REFRESH: `${API_BASE_URL}/api/ai/action-plan/refresh`,
  },
  USERS: {
    SEARCH: `${API_BASE_URL}/api/users/search`,
  },
  CONVERSATIONS: {
    BASE: `${API_BASE_URL}/api/conversations`,
    DETAIL: (id: number | string) => `${API_BASE_URL}/api/conversations/${id}`,
    MESSAGES: (id: number | string) => `${API_BASE_URL}/api/conversations/${id}/messages`,
    ARCHIVE: (id: number | string) => `${API_BASE_URL}/api/conversations/${id}/archive`,
    USER: `${API_BASE_URL}/api/conversations/user`,
    ADMIN: `${API_BASE_URL}/api/conversations/admin`,
    HUMAN_LIST: `${API_BASE_URL}/api/conversations/human`,
    HUMAN_DETAIL: (id: number | string) => `${API_BASE_URL}/api/conversations/human/${id}`,
    HUMAN_MESSAGES: (id: number | string) => `${API_BASE_URL}/api/conversations/${id}/human-messages`,
    READ: (id: number | string) => `${API_BASE_URL}/api/conversations/${id}/read`,
    TYPING: (id: number | string) => `${API_BASE_URL}/api/conversations/${id}/typing`,
    ADMIN_INBOX: `${API_BASE_URL}/api/conversations/admin/inbox`,
    ADMIN_STATUS: (id: number | string) => `${API_BASE_URL}/api/conversations/admin/${id}/status`,
  },
    PLANNER: {
    CURRENT: `${API_BASE_URL}/api/planner/current`,
    GENERATE: `${API_BASE_URL}/api/planner/generate`,
    REFRESH: `${API_BASE_URL}/api/planner/refresh`,
    TODAY: `${API_BASE_URL}/api/planner/today`,
    WEEK: `${API_BASE_URL}/api/planner/week`,
    PROGRESS: `${API_BASE_URL}/api/planner/progress`,
    PREFERENCES: `${API_BASE_URL}/api/planner/preferences`,
    START_TASK: (id: number | string) => `${API_BASE_URL}/api/planner/tasks/${id}/start`,
    COMPLETE_TASK: (id: number | string) => `${API_BASE_URL}/api/planner/tasks/${id}/complete`,
    SKIP_TASK: (id: number | string) => `${API_BASE_URL}/api/planner/tasks/${id}/skip`,
    ADMIN_STATS: `${API_BASE_URL}/api/admin/planner/stats`,
  },
  CAREER_INTELLIGENCE: {
    BASE: `${API_BASE_URL}/api/career-intelligence`,
    ACTIONS: `${API_BASE_URL}/api/career-intelligence/actions`,
  },
  PROGRESS_COACH: {
    WEEKLY: `${API_BASE_URL}/api/ai/progress/weekly`,
    REFRESH: `${API_BASE_URL}/api/ai/progress/refresh`,
    HISTORY: `${API_BASE_URL}/api/ai/progress/history`,
  },
  CALLS: {
    BASE: `${API_BASE_URL}/api/calls`,
    DETAIL: (id: number | string) => `${API_BASE_URL}/api/calls/${id}`,
    ACCEPT: (id: number | string) => `${API_BASE_URL}/api/calls/${id}/accept`,
    REJECT: (id: number | string) => `${API_BASE_URL}/api/calls/${id}/reject`,
    CANCEL: (id: number | string) => `${API_BASE_URL}/api/calls/${id}/cancel`,
    END: (id: number | string) => `${API_BASE_URL}/api/calls/${id}/end`,
    SIGNAL: (id: number | string) => `${API_BASE_URL}/api/calls/${id}/signal`,
    HISTORY: `${API_BASE_URL}/api/calls/history`,
    ACTIVE: `${API_BASE_URL}/api/calls/active`,
  },
  MENTORS: {
    APPLY: `${API_BASE_URL}/api/mentors/apply`,
    ME: `${API_BASE_URL}/api/mentors/me`,
    LIST: `${API_BASE_URL}/api/mentors`,
    DETAIL: (id: number | string) => `${API_BASE_URL}/api/mentors/${id}`,
    CREATE_REQUEST: (mentorId: number | string) => `${API_BASE_URL}/api/mentors/${mentorId}/requests`,
    REQUESTS_SENT: `${API_BASE_URL}/api/mentors/requests/sent`,
    REQUESTS_RECEIVED: `${API_BASE_URL}/api/mentors/requests/received`,
    ACCEPT_REQUEST: (requestId: number | string) => `${API_BASE_URL}/api/mentors/requests/${requestId}/accept`,
    DECLINE_REQUEST: (requestId: number | string) => `${API_BASE_URL}/api/mentors/requests/${requestId}/decline`,
    CANCEL_REQUEST: (requestId: number | string) => `${API_BASE_URL}/api/mentors/requests/${requestId}/cancel`,
    COMPLETE_REQUEST: (requestId: number | string) => `${API_BASE_URL}/api/mentors/requests/${requestId}/complete`,
    ADMIN: {
      PENDING: `${API_BASE_URL}/api/admin/mentors/pending`,
      LIST: `${API_BASE_URL}/api/admin/mentors`,
      DETAIL: (id: number | string) => `${API_BASE_URL}/api/admin/mentors/${id}`,
      APPROVE: (id: number | string) => `${API_BASE_URL}/api/admin/mentors/${id}/approve`,
      REJECT: (id: number | string) => `${API_BASE_URL}/api/admin/mentors/${id}/reject`,
      SUSPEND: (id: number | string) => `${API_BASE_URL}/api/admin/mentors/${id}/suspend`,
      REACTIVATE: (id: number | string) => `${API_BASE_URL}/api/admin/mentors/${id}/reactivate`,
      CONFIG: `${API_BASE_URL}/api/admin/mentors/config`,
    },
  },
  CATALOG: {
    CAREERS: `${API_BASE_URL}/api/catalog/careers`,
    CAREER: (slug: string) => `${API_BASE_URL}/api/catalog/careers/${encodeURIComponent(slug)}`,
    TECHNOLOGIES: `${API_BASE_URL}/api/catalog/technologies`,
    TECHNOLOGY: (slug: string) => `${API_BASE_URL}/api/catalog/technologies/${encodeURIComponent(slug)}`,
    COMPETENCIES: (slug: string) => `${API_BASE_URL}/api/catalog/technologies/${encodeURIComponent(slug)}/competencies`,
  },
  ASSESSMENTS: {
    START: `${API_BASE_URL}/api/assessments/start`,
    ACTIVE: `${API_BASE_URL}/api/assessments/active`,
    SESSION: (id: number | string) => `${API_BASE_URL}/api/assessments/${id}`,
    ANSWER: (id: number | string) => `${API_BASE_URL}/api/assessments/${id}/answer`,
    SKIP: (id: number | string) => `${API_BASE_URL}/api/assessments/${id}/skip`,
    SUBMIT: (id: number | string) => `${API_BASE_URL}/api/assessments/${id}/submit`,
    REPORT: (id: number | string) => `${API_BASE_URL}/api/assessments/${id}/report`,
    HISTORY: `${API_BASE_URL}/api/assessments/history`,
    REASSESSMENTS: `${API_BASE_URL}/api/assessments/reassessments`,
    REASSESSMENT: (id: number | string) => `${API_BASE_URL}/api/assessments/reassessments/${id}`,
    REASSESSMENT_SUBMIT: (id: number | string) => `${API_BASE_URL}/api/assessments/reassessments/${id}/submit`,
    REASSESSMENT_COMPARISON: (id: number | string, baselineId?: number | string) =>
      `${API_BASE_URL}/api/assessments/reassessments/${id}/comparison${baselineId ? `?baselineId=${baselineId}` : ""}`,
  },
  ONBOARDING: {
    STATE: `${API_BASE_URL}/api/onboarding/state`,
    SELECT_CAREER: `${API_BASE_URL}/api/onboarding/select-career`,
    SELECT_TECHNOLOGIES: `${API_BASE_URL}/api/onboarding/select-technologies`,
    STUDY_PREFERENCES: `${API_BASE_URL}/api/onboarding/study-preferences`,
    START_ASSESSMENT: `${API_BASE_URL}/api/onboarding/start-assessment`,
    REVIEW_REPORT: `${API_BASE_URL}/api/onboarding/review-report`,
    COMPLETE: `${API_BASE_URL}/api/onboarding/complete`,
    RESET: `${API_BASE_URL}/api/onboarding/reset`,
  },
  PERSONALIZED_ROADMAP: {
    GET: `${API_BASE_URL}/api/roadmaps/personalized`,
    GENERATE: `${API_BASE_URL}/api/roadmaps/personalized/generate`,
    TOGGLE_STEP: (stepId: number | string) => `${API_BASE_URL}/api/roadmaps/personalized/steps/${stepId}/toggle`,
    STEP_STATE: (stepId: number | string) => `${API_BASE_URL}/api/roadmaps/personalized/steps/${stepId}/state`,
    STEP_PRIORITY: (stepId: number | string) => `${API_BASE_URL}/api/roadmaps/personalized/steps/${stepId}/priority`,
    ADD_CUSTOM_MILESTONE: `${API_BASE_URL}/api/roadmaps/personalized/milestones/custom`,
    UPDATE_MILESTONE: (milestoneId: number | string) => `${API_BASE_URL}/api/roadmaps/personalized/milestones/${milestoneId}`,
    DELETE_CUSTOM_MILESTONE: (milestoneId: number | string) => `${API_BASE_URL}/api/roadmaps/personalized/milestones/${milestoneId}`,
    SKIP_MILESTONE: (milestoneId: number | string) => `${API_BASE_URL}/api/roadmaps/personalized/milestones/${milestoneId}/skip`,
    REORDER_MILESTONES: `${API_BASE_URL}/api/roadmaps/personalized/milestones/reorder`,
    SUBMIT_EVIDENCE: (milestoneId: number | string) => `${API_BASE_URL}/api/roadmaps/personalized/milestones/${milestoneId}/evidence`,
    MILESTONE_EVIDENCE: (milestoneId: number | string) => `${API_BASE_URL}/api/roadmaps/personalized/milestones/${milestoneId}/evidence`,
    ALL_EVIDENCE: `${API_BASE_URL}/api/roadmaps/personalized/evidence`,
    UPDATE_SCHEDULE: `${API_BASE_URL}/api/roadmaps/personalized/schedule`,
    REVISIONS: `${API_BASE_URL}/api/roadmaps/personalized/revisions`,
    ADAPTIVE_UPDATE_PREVIEW: `${API_BASE_URL}/api/roadmaps/personalized/adaptive-updates/preview`,
    ADAPTIVE_UPDATE_PROPOSAL: (id: number | string) => `${API_BASE_URL}/api/roadmaps/personalized/adaptive-updates/${id}`,
    ADAPTIVE_UPDATE_APPLY: (id: number | string) => `${API_BASE_URL}/api/roadmaps/personalized/adaptive-updates/${id}/apply`,
    ADAPTIVE_UPDATE_REJECT: (id: number | string) => `${API_BASE_URL}/api/roadmaps/personalized/adaptive-updates/${id}/reject`,
    ADAPTIVE_UPDATE_HISTORY: `${API_BASE_URL}/api/roadmaps/personalized/adaptive-updates/history`,
  },
};

export const WEBRTC_CONFIG = {
  ICE_SERVERS: [
    { urls: process.env.NEXT_PUBLIC_WEBRTC_STUN_URL || "stun:stun.l.google.com:19302" },
    ...(process.env.NEXT_PUBLIC_WEBRTC_TURN_URL
      ? [
          {
            urls: process.env.NEXT_PUBLIC_WEBRTC_TURN_URL,
            username: process.env.NEXT_PUBLIC_WEBRTC_TURN_USERNAME || "",
            credential: process.env.NEXT_PUBLIC_WEBRTC_TURN_CREDENTIAL || "",
          },
        ]
      : []),
  ],
};
