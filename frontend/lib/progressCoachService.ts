import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";

export type TrendDirection = "IMPROVING" | "STABLE" | "DECLINING" | "INSUFFICIENT_DATA";

export interface ProgressAction {
  title: string;
  category: string;
  whyItMatters: string;
  actionUrl: string;
  estimatedEffort: string;
}

export interface WeeklyProgressReport {
  id?: number;
  userId: number;
  periodStart: string;
  periodEnd: string;
  overallProgressScore: number;
  careerReadinessScore: number;
  roadmapProgress: number;
  dsaProgress: number;
  dsaSolvedCount: number;
  dsaTotalCount: number;
  interviewProgress: number;
  interviewCompletedCount: number;
  plannerConsistency: number;
  completedTasks: number;
  skippedTasks: number;
  studyMinutes: number;
  streakDays: number;
  roadmapTrend: TrendDirection;
  dsaTrend: TrendDirection;
  interviewTrend: TrendDirection;
  plannerTrend: TrendDirection;
  overallTrend: TrendDirection;
  strengths: string[];
  weaknesses: string[];
  improvements: string[];
  topAchievements: string[];
  priorityAreas: string[];
  nextWeekFocus: ProgressAction[];
  coachSummary: string;
  source: string;
  generatedAt: string;
}

export const progressCoachService = {
  async getWeeklyReport(): Promise<WeeklyProgressReport | null> {
    try {
      return await apiRequest<WeeklyProgressReport>(API_ENDPOINTS.PROGRESS_COACH.WEEKLY, {
        method: "GET",
      });
    } catch {
      return null;
    }
  },

  async refreshWeeklyReport(): Promise<WeeklyProgressReport | null> {
    try {
      return await apiRequest<WeeklyProgressReport>(API_ENDPOINTS.PROGRESS_COACH.REFRESH, {
        method: "POST",
      });
    } catch {
      return null;
    }
  },

  async getReportHistory(): Promise<WeeklyProgressReport[]> {
    try {
      return await apiRequest<WeeklyProgressReport[]>(API_ENDPOINTS.PROGRESS_COACH.HISTORY, {
        method: "GET",
      });
    } catch {
      return [];
    }
  },
};
