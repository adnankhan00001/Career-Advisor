import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";

export type InterviewStatus = "IN_PROGRESS" | "COMPLETED" | "ABANDONED" | "EXPIRED";

export type InterviewCategory =
  | "JAVA"
  | "OOP"
  | "DBMS"
  | "SPRING_BOOT"
  | "DSA"
  | "OS"
  | "CN";

export type Difficulty = "EASY" | "MEDIUM" | "HARD";

export interface StartInterviewRequest {
  category?: InterviewCategory;
  difficulty?: Difficulty;
  durationMinutes?: number;
  questionCount?: number;
}

export interface InterviewQuestion {
  id: number;
  question: string;
  category: InterviewCategory;
  topic: string;
  difficulty: Difficulty;
  questionType: string;
  options: string[];
  expectedConcepts: string[];
  orderIndex: number;
  selectedAnswer?: string | null;
}

export interface MockInterviewSession {
  id: number;
  category: InterviewCategory;
  difficulty: Difficulty;
  status: InterviewStatus;
  startedAt: string;
  deadline: string;
  remainingSeconds: number;
  durationSeconds: number;
  totalQuestions: number;
  answeredCount: number;
  questions: InterviewQuestion[];
}

export interface InterviewQuestionReview {
  id: number;
  question: string;
  category: InterviewCategory;
  topic: string;
  difficulty: Difficulty;
  questionType: string;
  options: string[];
  userAnswer?: string | null;
  correctAnswer: string;
  isCorrect?: boolean | null;
  explanation?: string;
  expectedConcepts: string[];
}

export interface MockInterviewResult {
  id: number;
  category: InterviewCategory;
  difficulty: Difficulty;
  status: InterviewStatus;
  startedAt: string;
  completedAt?: string;
  timeTakenSeconds: number;
  durationSeconds: number;
  score: number; // 0 - 100%
  totalQuestions: number;
  correctCount: number;
  strongAreas: string[];
  weakAreas: string[];
  categoryBreakdown?: Record<string, number>;
  questionReviews: InterviewQuestionReview[];
  recommendation: string;
}

export interface MockInterviewHistoryItem {
  id: number;
  category: InterviewCategory;
  difficulty: Difficulty;
  status: InterviewStatus;
  score: number;
  totalQuestions: number;
  correctCount: number;
  durationSeconds: number;
  startedAt: string;
  completedAt?: string;
}

export interface InterviewSummary {
  totalInterviews: number;
  completedInterviews: number;
  averageScore: number;
  bestScore: number;
  latestScore: number;
  strongestCategory: string;
  weakestCategory: string;
  recentInterviews: MockInterviewHistoryItem[];
}

export const CATEGORY_LABELS: Record<InterviewCategory, string> = {
  JAVA: "Core Java & Concurrency",
  OOP: "Object-Oriented Design",
  DBMS: "DBMS, SQL & Indexing",
  SPRING_BOOT: "Spring Boot & Microservices",
  DSA: "Data Structures & Algorithms",
  OS: "Operating Systems",
  CN: "Computer Networks",
};

export const interviewService = {
  async startInterview(request: StartInterviewRequest): Promise<MockInterviewSession> {
    return apiRequest<MockInterviewSession>(API_ENDPOINTS.INTERVIEWS.BASE, {
      method: "POST",
      body: JSON.stringify(request),
    });
  },

  async getActiveSession(id: number | string): Promise<MockInterviewSession> {
    return apiRequest<MockInterviewSession>(API_ENDPOINTS.INTERVIEWS.SESSION(id));
  },

  async saveAnswer(
    id: number | string,
    questionId: number,
    answer: string
  ): Promise<MockInterviewSession> {
    return apiRequest<MockInterviewSession>(API_ENDPOINTS.INTERVIEWS.ANSWERS(id), {
      method: "POST",
      body: JSON.stringify({ questionId, answer }),
    });
  },

  async submitInterview(id: number | string): Promise<MockInterviewResult> {
    return apiRequest<MockInterviewResult>(API_ENDPOINTS.INTERVIEWS.SUBMIT(id), {
      method: "POST",
    });
  },

  async getInterviewResult(id: number | string): Promise<MockInterviewResult> {
    return apiRequest<MockInterviewResult>(API_ENDPOINTS.INTERVIEWS.RESULT(id));
  },

  async getInterviewHistory(): Promise<MockInterviewHistoryItem[]> {
    return apiRequest<MockInterviewHistoryItem[]>(API_ENDPOINTS.INTERVIEWS.BASE);
  },

  async getInterviewSummary(): Promise<InterviewSummary> {
    return apiRequest<InterviewSummary>(API_ENDPOINTS.INTERVIEWS.SUMMARY);
  },
};
