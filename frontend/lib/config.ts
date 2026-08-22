export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

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
};
