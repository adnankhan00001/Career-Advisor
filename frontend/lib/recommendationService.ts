import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";

export interface RecommendationItem {
  id: string;
  type: "CAREER" | "SKILL_GAP" | "ROADMAP" | "PRACTICE" | "INTERVIEW" | "ONBOARDING";
  priority: "HIGH" | "MEDIUM" | "LOW";
  title: string;
  description: string;
  reason: string;
  actionText: string;
  actionUrl: string;
  score?: number;
}

export interface CareerMatch {
  title: string;
  category: string;
  matchScore: number;
  skillScore: number;
  goalScore: number;
  quizScore: number;
  matchedSkills: string[];
  missingSkills: string[];
  targetGoal: boolean;
}

export interface SkillGap {
  targetCareer: string;
  matchPercentage: number;
  acquiredSkills: string[];
  missingSkills: string[];
  highPriorityMissing: string[];
}

export interface RoadmapAction {
  careerTitle: string;
  nextTopic: string;
  sectionTitle: string;
  completedTopics: number;
  totalTopics: number;
  progressPercentage: number;
  reason: string;
}

export interface PracticeAction {
  recommendedTopic: string;
  problemSlug: string;
  problemTitle: string;
  difficulty: "EASY" | "MEDIUM" | "HARD";
  topicSolved: number;
  topicTotal: number;
  overallSolved: number;
  overallTotal: number;
  reason: string;
}

export interface InterviewFocus {
  subject: string;
  category: string;
  reason: string;
  scoreOrAccuracy: number;
  keyTopicsToReview: string[];
  suggestedPracticeProblemSlug?: string;
}

export interface PersonalizedIntelligence {
  userState: "ONBOARDING" | "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
  summaryHeadline: string;
  careerGoal: string;
  overallReadinessScore: number;
  careerMatches: CareerMatch[];
  skillGaps: SkillGap;
  nextRoadmapAction: RoadmapAction;
  practiceAction: PracticeAction;
  interviewFocusAction: InterviewFocus;
  actionPlan: RecommendationItem[];
}

export const recommendationService = {
  async getRecommendations(): Promise<PersonalizedIntelligence | null> {
    try {
      return await apiRequest<PersonalizedIntelligence>(
        API_ENDPOINTS.RECOMMENDATIONS.INTELLIGENCE
      );
    } catch {
      return null;
    }
  },
};
