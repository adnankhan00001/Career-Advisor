import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";
import { AssessmentSession, AssessmentQuestion, AssessmentReport } from "./onboardingService";

export interface StartAssessmentRequest {
  technologySlugs: string[];
  timeLimitMinutes?: number;
  minQuestionsPerTechnology?: number;
  maxQuestionsPerTechnology?: number;
}

export interface SubmitAnswerRequest {
  sessionQuestionId: number;
  selectedOptionIndex: number;
}

export interface AssessmentSummary {
  sessionId: number;
  status: string;
  startTime: string;
  submittedAt: string | null;
  overallScore: number | null;
  overallProficiency: string | null;
  assessedTechnologies: string[];
}

export const assessmentService = {
  async startAssessment(request: StartAssessmentRequest): Promise<AssessmentSession> {
    return apiRequest<AssessmentSession>(API_ENDPOINTS.ASSESSMENTS.START, {
      method: "POST",
      body: JSON.stringify(request),
    });
  },

  async getActiveAssessment(): Promise<AssessmentSession | null> {
    try {
      return await apiRequest<AssessmentSession>(API_ENDPOINTS.ASSESSMENTS.ACTIVE, {
        method: "GET",
      });
    } catch {
      return null;
    }
  },

  async getSessionState(sessionId: number): Promise<AssessmentSession> {
    return apiRequest<AssessmentSession>(API_ENDPOINTS.ASSESSMENTS.SESSION(sessionId), {
      method: "GET",
    });
  },

  async submitAnswer(sessionId: number, request: SubmitAnswerRequest): Promise<AssessmentQuestion> {
    return apiRequest<AssessmentQuestion>(API_ENDPOINTS.ASSESSMENTS.ANSWER(sessionId), {
      method: "POST",
      body: JSON.stringify(request),
    });
  },

  async skipQuestion(sessionId: number, sessionQuestionId: number): Promise<AssessmentQuestion> {
    return apiRequest<AssessmentQuestion>(API_ENDPOINTS.ASSESSMENTS.SKIP(sessionId), {
      method: "POST",
      body: JSON.stringify({ sessionQuestionId }),
    });
  },

  async submitAssessment(sessionId: number): Promise<AssessmentReport> {
    return apiRequest<AssessmentReport>(API_ENDPOINTS.ASSESSMENTS.SUBMIT(sessionId), {
      method: "POST",
    });
  },

  async getReport(sessionId: number): Promise<AssessmentReport> {
    return apiRequest<AssessmentReport>(API_ENDPOINTS.ASSESSMENTS.REPORT(sessionId), {
      method: "GET",
    });
  },

  async getHistory(): Promise<AssessmentSummary[]> {
    return apiRequest<AssessmentSummary[]>(API_ENDPOINTS.ASSESSMENTS.HISTORY, {
      method: "GET",
    });
  },
};
