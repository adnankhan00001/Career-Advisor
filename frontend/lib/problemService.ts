import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";

export type Difficulty = "EASY" | "MEDIUM" | "HARD";
export type ProblemCategory =
  | "DSA"
  | "JAVA"
  | "OOP"
  | "DBMS"
  | "OS"
  | "CN"
  | "SPRING_BOOT";

export interface CodingProblem {
  id: number;
  slug: string;
  title: string;
  description: string;
  difficulty: Difficulty;
  category: ProblemCategory;
  topic: string;
  externalUrl?: string;
  tags: string[];
  acceptanceRate?: string;
  orderIndex: number;
  starterCode?: string;
  sampleInput?: string;
  sampleOutput?: string;
  constraints?: string;
  explanation?: string;
  solved: boolean;
  solvedAt?: string;
}

export interface TopicProgressStat {
  topic: string;
  total: number;
  solved: number;
  percentage: number;
}

export interface CategoryProgressStat {
  category: ProblemCategory;
  displayName: string;
  total: number;
  solved: number;
  percentage: number;
}

export interface ProblemProgressSummary {
  totalProblems: number;
  solvedProblems: number;
  unsolvedProblems: number;
  completionPercentage: number;
  easyTotal: number;
  easySolved: number;
  mediumTotal: number;
  mediumSolved: number;
  hardTotal: number;
  hardSolved: number;
  topicStats: TopicProgressStat[];
  categoryStats: CategoryProgressStat[];
  nextRecommendedProblem: string;
}

export interface CodeSubmissionRequest {
  language: string;
  code: string;
  customInput?: string;
}

export interface CodeSubmissionResult {
  status: "ACCEPTED" | "WRONG_ANSWER" | "COMPILATION_ERROR" | "RUNTIME_ERROR" | "NOT_EXECUTED";
  output?: string;
  expectedOutput?: string;
  input?: string;
  executionTimeMs?: number;
  memoryKb?: number;
  testCasesPassed?: number;
  totalTestCases?: number;
  message?: string;
  sandboxInfo?: string;
}

export const problemService = {
  async getProblems(params?: {
    category?: ProblemCategory;
    topic?: string;
    difficulty?: Difficulty;
  }): Promise<CodingProblem[]> {
    try {
      const searchParams = new URLSearchParams();
      if (params?.category) searchParams.set("category", params.category);
      if (params?.topic) searchParams.set("topic", params.topic);
      if (params?.difficulty) searchParams.set("difficulty", params.difficulty);

      const qs = searchParams.toString();
      const url = qs
        ? `${API_ENDPOINTS.PROBLEMS.LIST}?${qs}`
        : API_ENDPOINTS.PROBLEMS.LIST;

      return await apiRequest<CodingProblem[]>(url);
    } catch {
      return [];
    }
  },

  async getProblem(idOrSlug: string): Promise<CodingProblem | null> {
    try {
      return await apiRequest<CodingProblem>(
        API_ENDPOINTS.PROBLEMS.DETAIL(idOrSlug)
      );
    } catch {
      return null;
    }
  },

  async toggleProblemSolved(id: number): Promise<CodingProblem | null> {
    try {
      return await apiRequest<CodingProblem>(API_ENDPOINTS.PROBLEMS.TOGGLE(id), {
        method: "POST",
      });
    } catch {
      return null;
    }
  },

  async runCode(
    id: number,
    request: CodeSubmissionRequest
  ): Promise<CodeSubmissionResult | null> {
    try {
      return await apiRequest<CodeSubmissionResult>(
        API_ENDPOINTS.PROBLEMS.RUN(id),
        {
          method: "POST",
          body: JSON.stringify(request),
        }
      );
    } catch {
      return {
        status: "NOT_EXECUTED",
        message: "Failed to connect to execution server.",
        sandboxInfo: "Evaluation Service Unavailable",
      };
    }
  },

  async submitCode(
    id: number,
    request: CodeSubmissionRequest
  ): Promise<CodeSubmissionResult | null> {
    try {
      return await apiRequest<CodeSubmissionResult>(
        API_ENDPOINTS.PROBLEMS.SUBMIT(id),
        {
          method: "POST",
          body: JSON.stringify(request),
        }
      );
    } catch {
      return {
        status: "NOT_EXECUTED",
        message: "Failed to submit code.",
        sandboxInfo: "Evaluation Service Unavailable",
      };
    }
  },

  async getProgressSummary(): Promise<ProblemProgressSummary | null> {
    try {
      return await apiRequest<ProblemProgressSummary>(
        API_ENDPOINTS.PROBLEMS.PROGRESS_SUMMARY
      );
    } catch {
      return null;
    }
  },
};
