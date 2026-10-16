import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";

export type LearningPlanStatus = "ACTIVE" | "COMPLETED" | "ARCHIVED";
export type LearningPlanSource = "AI" | "DETERMINISTIC" | "HYBRID";
export type LearningTaskStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED" | "SKIPPED" | "OVERDUE";
export type LearningTaskCategory =
  | "ROADMAP"
  | "DSA"
  | "SKILL"
  | "QUIZ"
  | "MOCK_INTERVIEW"
  | "RESUME"
  | "PROJECT"
  | "AI_COACHING"
  | "REVIEW";
export type GapPriority = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
export type StudyWindow = "MORNING" | "AFTERNOON" | "EVENING" | "FLEXIBLE";

export interface PlannerPreference {
  dailyStudyMinutes: number;
  preferredStudyWindow: StudyWindow;
  preferredDays: string[];
  weeklyGoal: string;
}

export interface LearningTask {
  id: number;
  planId: number;
  title: string;
  description: string;
  category: LearningTaskCategory;
  priority: GapPriority;
  scheduledDate: string;
  estimatedMinutes: number;
  whyItMatters: string;
  expectedOutcome: string;
  actionUrl: string;
  status: LearningTaskStatus;
  sequenceNumber: number;
  completedAt?: string;
}

export interface DailySchedule {
  date: string;
  dayOfWeek: string;
  totalMinutesPlanned: number;
  totalMinutesCompleted: number;
  completionPercentage: number;
  isPreferredDay: boolean;
  tasks: LearningTask[];
}

export interface WeeklyPlan {
  id: number;
  userId: number;
  targetCareer: string;
  readinessScoreAtGeneration: number;
  status: LearningPlanStatus;
  source: LearningPlanSource;
  weeklyGoal: string;
  weekStartDate: string;
  weekEndDate: string;
  totalTasks: number;
  completedTasks: number;
  completionPercentage: number;
  totalMinutesPlanned: number;
  totalMinutesCompleted: number;
  currentStreak: number;
  days: DailySchedule[];
  generatedAt: string;
}

export interface PlannerProgress {
  activePlanId: number;
  weekStartDate: string;
  weekEndDate: string;
  totalTasks: number;
  completedTasks: number;
  pendingTasks: number;
  skippedTasks: number;
  overdueTasks: number;
  completionPercentage: number;
  weeklyMinutesPlanned: number;
  weeklyMinutesCompleted: number;
  currentStreak: number;
}

export const plannerService = {
  async getCurrentPlan(): Promise<WeeklyPlan | null> {
    try {
      return await apiRequest<WeeklyPlan>(API_ENDPOINTS.PLANNER.CURRENT, { method: "GET" });
    } catch {
      return null;
    }
  },

  async generatePlan(): Promise<WeeklyPlan | null> {
    try {
      return await apiRequest<WeeklyPlan>(API_ENDPOINTS.PLANNER.GENERATE, { method: "POST" });
    } catch {
      return null;
    }
  },

  async refreshPlan(): Promise<WeeklyPlan | null> {
    try {
      return await apiRequest<WeeklyPlan>(API_ENDPOINTS.PLANNER.REFRESH, { method: "POST" });
    } catch {
      return null;
    }
  },

  async getTodayTasks(): Promise<LearningTask[]> {
    try {
      return await apiRequest<LearningTask[]>(API_ENDPOINTS.PLANNER.TODAY, { method: "GET" });
    } catch {
      return [];
    }
  },

  async getWeeklySchedule(): Promise<DailySchedule[]> {
    try {
      return await apiRequest<DailySchedule[]>(API_ENDPOINTS.PLANNER.WEEK, { method: "GET" });
    } catch {
      return [];
    }
  },

  async getProgress(): Promise<PlannerProgress | null> {
    try {
      return await apiRequest<PlannerProgress>(API_ENDPOINTS.PLANNER.PROGRESS, { method: "GET" });
    } catch {
      return null;
    }
  },

  async getPreferences(): Promise<PlannerPreference | null> {
    try {
      return await apiRequest<PlannerPreference>(API_ENDPOINTS.PLANNER.PREFERENCES, { method: "GET" });
    } catch {
      return null;
    }
  },

  async updatePreferences(dto: PlannerPreference): Promise<PlannerPreference | null> {
    try {
      return await apiRequest<PlannerPreference>(API_ENDPOINTS.PLANNER.PREFERENCES, {
        method: "PUT",
        body: JSON.stringify(dto),
      });
    } catch {
      return null;
    }
  },

  async startTask(id: number): Promise<LearningTask | null> {
    try {
      return await apiRequest<LearningTask>(API_ENDPOINTS.PLANNER.START_TASK(id), { method: "POST" });
    } catch {
      return null;
    }
  },

  async completeTask(id: number): Promise<LearningTask | null> {
    try {
      return await apiRequest<LearningTask>(API_ENDPOINTS.PLANNER.COMPLETE_TASK(id), { method: "POST" });
    } catch {
      return null;
    }
  },

  async skipTask(id: number): Promise<LearningTask | null> {
    try {
      return await apiRequest<LearningTask>(API_ENDPOINTS.PLANNER.SKIP_TASK(id), { method: "POST" });
    } catch {
      return null;
    }
  },
};
