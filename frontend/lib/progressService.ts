import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";

export interface ProgressSummary {
  careerGoal: string;
  roadmapPercent: number;
  completedStepsCount: number;
  totalStepsCount: number;
  skillsCount: number;
  skills: string[];
  completedSteps: string[];
  userLevel: string;
  latestQuizScore: string | null;
  nextTopicToLearn: string;
  skillMatchPercentage: number;
}

export const progressService = {
  async getProgressSummary(): Promise<ProgressSummary | null> {
    try {
      const summary = await apiRequest<ProgressSummary>(
        API_ENDPOINTS.PROGRESS.SUMMARY
      );
      if (summary) {
        if (typeof window !== "undefined") {
          localStorage.setItem("careerGoal", summary.careerGoal);
          localStorage.setItem("skills", JSON.stringify(summary.skills));
          localStorage.setItem(
            "completedSteps",
            JSON.stringify(summary.completedSteps)
          );
          if (summary.userLevel) {
            localStorage.setItem("userLevel", summary.userLevel);
          }
          if (summary.latestQuizScore) {
            localStorage.setItem("quizScore", summary.latestQuizScore);
          }
        }
        return summary;
      }
    } catch {
      // Fallback to local computation
    }
    return null;
  },

  async getCompletedSteps(): Promise<string[]> {
    try {
      const steps = await apiRequest<string[]>(API_ENDPOINTS.PROGRESS.ROADMAP);
      if (Array.isArray(steps)) {
        if (typeof window !== "undefined") {
          localStorage.setItem("completedSteps", JSON.stringify(steps));
        }
        return steps;
      }
    } catch {
      // Fallback
    }

    if (typeof window !== "undefined") {
      try {
        return JSON.parse(localStorage.getItem("completedSteps") || "[]");
      } catch {
        return [];
      }
    }
    return [];
  },

  async toggleStep(stepTitle: string, careerTitle?: string): Promise<string[]> {
    try {
      const steps = await apiRequest<string[]>(API_ENDPOINTS.PROGRESS.TOGGLE, {
        method: "POST",
        body: JSON.stringify({ stepTitle, careerTitle }),
      });
      if (Array.isArray(steps)) {
        if (typeof window !== "undefined") {
          localStorage.setItem("completedSteps", JSON.stringify(steps));
        }
        return steps;
      }
    } catch {
      // Fallback local toggle
    }

    if (typeof window !== "undefined") {
      const current: string[] = JSON.parse(
        localStorage.getItem("completedSteps") || "[]"
      );
      const next = current.includes(stepTitle)
        ? current.filter((s) => s !== stepTitle)
        : [...current, stepTitle];
      localStorage.setItem("completedSteps", JSON.stringify(next));
      return next;
    }
    return [];
  },

  async updateCareerGoal(careerGoal: string): Promise<string> {
    try {
      await apiRequest(API_ENDPOINTS.PROGRESS.CAREER_GOAL, {
        method: "POST",
        body: JSON.stringify({ careerGoal }),
      });
    } catch {
      // Fallback local update
    }

    if (typeof window !== "undefined") {
      localStorage.setItem("careerGoal", careerGoal);
      localStorage.setItem("selectedCareer", careerGoal);
    }
    return careerGoal;
  },

  async resetProgress(): Promise<void> {
    try {
      await apiRequest(API_ENDPOINTS.PROGRESS.RESET, {
        method: "POST",
      });
    } catch {
      // Fallback
    }

    if (typeof window !== "undefined") {
      localStorage.removeItem("completedSteps");
    }
  },
};
