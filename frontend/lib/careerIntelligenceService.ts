import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";

export type DimensionType =
  | "SKILLS"
  | "RESUME"
  | "ASSESSMENT"
  | "ROADMAP"
  | "DSA"
  | "MOCK_INTERVIEW";

export type ReadinessState =
  | "BEGINNER"
  | "FOUNDATIONAL"
  | "DEVELOPING"
  | "ADVANCED"
  | "JOB_READY";

export type GapPriority = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";

export type ActionCategory =
  | "ROADMAP"
  | "DSA"
  | "RESUME"
  | "INTERVIEW"
  | "ASSESSMENT"
  | "SKILL";

export interface DimensionScore {
  dimension: DimensionType;
  displayName: string;
  score: number;
  weightPercentage: number;
  weightedContribution: number;
  attempted: boolean;
  statusDescription: string;
  detailMetric: string;
}

export interface CareerGap {
  skill: string;
  priority: GapPriority;
  source: string;
  evidence: string[];
  recommendedAction: string;
  relatedRoadmapTopic?: string;
}

export interface CareerAction {
  id: string;
  title: string;
  description: string;
  category: ActionCategory;
  priority: GapPriority;
  reasoning: string;
  estimatedImpactPoints: number;
  actionUrl: string;
  completed: boolean;
}

export interface CareerIntelligence {
  userId: number;
  userName: string;
  targetCareer: string;
  userLevel: string;
  overallReadinessScore: number;
  readinessState: ReadinessState;
  readinessStateDisplayName: string;
  summary: string;
  dimensionScores: Record<DimensionType, DimensionScore>;
  strongestDimensions: string[];
  weakestDimensions: string[];
  skillGaps: CareerGap[];
  prioritizedActions: CareerAction[];
  calculatedAt: string;
}

export const careerIntelligenceService = {
  async getCareerIntelligence(): Promise<CareerIntelligence | null> {
    try {
      return await apiRequest<CareerIntelligence>(API_ENDPOINTS.CAREER_INTELLIGENCE.BASE, {
        method: "GET",
      });
    } catch {
      return null;
    }
  },

  async getPrioritizedActions(): Promise<CareerAction[]> {
    try {
      return await apiRequest<CareerAction[]>(API_ENDPOINTS.CAREER_INTELLIGENCE.ACTIONS, {
        method: "GET",
      });
    } catch {
      return [];
    }
  },
};
