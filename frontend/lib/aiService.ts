import { apiRequest } from "./apiClient";
import { API_ENDPOINTS } from "./config";

export interface AiHealth {
  enabled: boolean;
  provider: string;
  available: boolean;
  model: string;
  message: string;
}

export interface UserProfileSummary {
  id: number;
  name: string;
  email: string;
  careerGoal: string;
  userLevel: string;
}

export interface ResumeAiSummary {
  resumeId: number;
  fileName: string;
  uploadTimestamp: string;
  parsingStatus: string;
  extractedSkills: string[];
  topCareerMatches: string[];
  identifiedSkillGaps: string[];
  executiveSummarySnippet?: string;
}

export interface QuizAiSummary {
  score: number;
  totalQuestions: number;
  percentage: number;
  evaluatedLevel: string;
  recommendedCareer: string;
  attemptDate: string;
}

export interface RoadmapAiProgress {
  careerGoal: string;
  completedStepsCount: number;
  totalStepsCount: number;
  completionPercentage: number;
  completedStepTitles: string[];
  nextRecommendedStep: string;
}

export interface DsaAiProgress {
  solvedCount: number;
  totalCount: number;
  completionPercentage: number;
  categoryDistribution: Record<string, number>;
  nextRecommendedProblem?: string;
}

export interface MockInterviewAiSummary {
  totalSessions: number;
  completedSessions: number;
  averageScore: number;
  bestScore: number;
  strongAreas: string[];
  weakAreas: string[];
  latestScore?: number;
  latestCategory?: string;
}

export interface RecommendationAiSummary {
  overallReadinessScore: number;
  userLifecycleState: string;
  topCareerMatch: string;
  topCareerMatchPercentage: number;
  missingSkills: string[];
  topActionableRecommendations: string[];
}

export interface PersonalAiContext {
  userProfile: UserProfileSummary;
  targetCareerGoal: string;
  verifiedSkills: string[];
  resumeSummary?: ResumeAiSummary;
  quizAssessment?: QuizAiSummary;
  roadmapProgress?: RoadmapAiProgress;
  dsaProgress?: DsaAiProgress;
  mockInterviewPerformance?: MockInterviewAiSummary;
  recommendations?: RecommendationAiSummary;
  contextTimestamp: string;
}

export interface AiChatRequest {
  message: string;
  conversationId?: string;
  includePersonalContext?: boolean;
}

export interface AiChatResponse {
  response: string;
  status: string;
  provider: string;
  model: string;
  tokensUsed: number;
  latencyMs: number;
  conversationId?: string;
  timestamp: string;
}

/**
 * Checks AI service availability & infrastructure health.
 */
export async function getAiHealth(): Promise<AiHealth> {
  return apiRequest<AiHealth>(API_ENDPOINTS.AI.HEALTH);
}

/**
 * Retrieves the authoritative personal AI context for the authenticated user.
 */
export async function getPersonalAiContext(): Promise<PersonalAiContext> {
  return apiRequest<PersonalAiContext>(API_ENDPOINTS.AI.CONTEXT);
}

/**
 * Sends a message to the Career Advisor AI assistant.
 */
export async function sendAiChatMessage(request: AiChatRequest): Promise<AiChatResponse> {
  return apiRequest<AiChatResponse>(API_ENDPOINTS.AI.CHAT, {
    method: "POST",
    body: JSON.stringify(request),
  });
}
