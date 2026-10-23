import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";

export type OnboardingState =
  | "NOT_STARTED"
  | "CAREER_SELECTED"
  | "TECHNOLOGIES_SELECTED"
  | "STUDY_PREFERENCES_CONFIGURED"
  | "ASSESSMENT_IN_PROGRESS"
  | "ASSESSMENT_COMPLETED"
  | "ASSESSMENT_REVIEWED"
  | "ROADMAP_GENERATED";

export type StudyWindow = "MORNING" | "AFTERNOON" | "EVENING" | "FLEXIBLE";

export interface CatalogTechnology {
  id: number;
  name: string;
  slug: string;
  category: string;
  icon: string;
  description: string;
  competencyCount: number;
}

export interface CatalogCareer {
  id: number;
  title: string;
  slug: string;
  description: string;
  overview: string;
  category: string;
  level: string;
  duration: string;
  salaryRange: string;
  technologies: CatalogTechnology[];
}

export interface OnboardingStudyPreferences {
  dailyStudyMinutes: number;
  preferredStudyWindow: StudyWindow;
  preferredDays: string[];
  targetCompletionDate?: string | null;
}

export interface OnboardingStateDto {
  state: OnboardingState;
  selectedCareer: CatalogCareer | null;
  selectedTechnologies: CatalogTechnology[];
  studyPreferences: OnboardingStudyPreferences;
  currentAssessmentSessionId: number | null;
  assessmentStatus: string | null;
  hasPersonalizedRoadmap: boolean;
  canResumeAssessment: boolean;
  canGenerateRoadmap: boolean;
}

export interface AssessmentQuestion {
  sessionQuestionId: number;
  questionId: number;
  technologyName: string;
  technologySlug: string;
  topic: string;
  difficulty: "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
  presentationOrder: number;
  questionText: string;
  options: string[];
  scoringWeight: number;
  selectedOptionIndex: number | null;
  status: "UNANSWERED" | "ANSWERED" | "SKIPPED";
  isCorrect?: boolean | null;
}

export interface AssessmentSession {
  sessionId: number;
  status: "IN_PROGRESS" | "SUBMITTED" | "TIMED_OUT" | "CANCELLED";
  startTime: string;
  expiresAt: string;
  remainingSeconds: number;
  timeLimitMinutes: number;
  totalQuestionsCount: number;
  answeredCount: number;
  skippedCount: number;
  currentOrder: number;
  selectedTechnologies: CatalogTechnology[];
  questions: AssessmentQuestion[];
}

export interface TechnologyResult {
  technologyId: number;
  technologyName: string;
  technologySlug: string;
  category: string;
  score: number;
  proficiencyLevel: "Beginner" | "Intermediate" | "Advanced" | "Expert";
  confidenceScore: number;
  totalQuestions: number;
  correctQuestions: number;
  assessedDifficulty: string;
  strengths: string[];
  weaknesses: string[];
  recommendedFocus: string[];
}

export interface QuestionReview {
  sessionQuestionId: number;
  technologyName: string;
  topic: string;
  difficulty: string;
  questionText: string;
  options: string[];
  correctOptionIndex: number;
  selectedOptionIndex: number | null;
  isCorrect: boolean;
  explanation: string;
  status: string;
}

export interface AssessmentReport {
  sessionId: number;
  status: string;
  startTime: string;
  submittedAt: string;
  overallScore: number;
  overallProficiency: string;
  technologyResults: TechnologyResult[];
  questionReviews: QuestionReview[];
}

export const onboardingService = {
  async getState(): Promise<OnboardingStateDto> {
    return apiRequest<OnboardingStateDto>(API_ENDPOINTS.ONBOARDING.STATE, {
      method: "GET",
    });
  },

  async selectCareer(careerSlug: string): Promise<OnboardingStateDto> {
    return apiRequest<OnboardingStateDto>(API_ENDPOINTS.ONBOARDING.SELECT_CAREER, {
      method: "POST",
      body: JSON.stringify({ careerSlug }),
    });
  },

  async selectTechnologies(technologySlugs: string[]): Promise<OnboardingStateDto> {
    return apiRequest<OnboardingStateDto>(API_ENDPOINTS.ONBOARDING.SELECT_TECHNOLOGIES, {
      method: "POST",
      body: JSON.stringify({ technologySlugs }),
    });
  },

  async saveStudyPreferences(preferences: OnboardingStudyPreferences): Promise<OnboardingStateDto> {
    return apiRequest<OnboardingStateDto>(API_ENDPOINTS.ONBOARDING.STUDY_PREFERENCES, {
      method: "POST",
      body: JSON.stringify(preferences),
    });
  },

  async startAssessment(): Promise<AssessmentSession> {
    return apiRequest<AssessmentSession>(API_ENDPOINTS.ONBOARDING.START_ASSESSMENT, {
      method: "POST",
    });
  },

  async reviewReport(): Promise<OnboardingStateDto> {
    return apiRequest<OnboardingStateDto>(API_ENDPOINTS.ONBOARDING.REVIEW_REPORT, {
      method: "POST",
    });
  },

  async complete(): Promise<OnboardingStateDto> {
    return apiRequest<OnboardingStateDto>(API_ENDPOINTS.ONBOARDING.COMPLETE, {
      method: "POST",
    });
  },

  async reset(): Promise<OnboardingStateDto> {
    return apiRequest<OnboardingStateDto>(API_ENDPOINTS.ONBOARDING.RESET, {
      method: "POST",
    });
  },

  async getCatalogCareers(): Promise<CatalogCareer[]> {
    return apiRequest<CatalogCareer[]>(API_ENDPOINTS.CATALOG.CAREERS, {
      method: "GET",
    });
  },
};
