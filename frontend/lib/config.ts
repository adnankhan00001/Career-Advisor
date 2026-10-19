export const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_BASE_URL ||
  process.env.NEXT_PUBLIC_API_URL ||
  "http://localhost:8080"
).replace(/\/+$/, "");

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
      CONFIG: `${API_BASE_URL}/api/admin/mentors/config`,
    },
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
