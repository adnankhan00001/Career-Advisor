export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const WS_BASE_URL =
  process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:8080/ws";

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
