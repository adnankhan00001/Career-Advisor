import { apiRequest } from "./apiClient";
import { API_ENDPOINTS } from "./config";

export interface TimeSeriesPoint {
  date: string;
  count: number;
}

export interface UserAnalytics {
  totalUsers: number;
  newUsersInPeriod: number;
  activeUsers: number;
  suspendedUsers: number;
  deactivatedUsers: number;
  roleDistribution: Record<string, number>;
  statusDistribution: Record<string, number>;
  registrationTrend: TimeSeriesPoint[];
}

export interface CareerPrepAnalytics {
  totalResumes: number;
  resumesUploadedInPeriod: number;
  resumeUploadTrend: TimeSeriesPoint[];
  totalQuizAttempts: number;
  totalSolvedProblems: number;
  totalMockInterviews: number;
  mockInterviewsInPeriod: number;
  completedInterviewsCount: number;
  mockInterviewTrend: TimeSeriesPoint[];
  careerGoalDistribution: Record<string, number>;
  userLevelDistribution: Record<string, number>;
}

export interface MentorshipAnalytics {
  totalRequests: number;
  requestsInPeriod: number;
  requestCreationTrend: TimeSeriesPoint[];
  statusDistribution: Record<string, number>;
  acceptanceRate: number;
  rejectionRate: number;
  completionRate: number;
  totalApprovedMentors: number;
  activeMentorsCount: number;
  totalCapacity: number;
  utilizedCapacity: number;
  averageResponseTimeHours: number | null;
}

export async function getUserAnalytics(days: number = 30): Promise<UserAnalytics> {
  return apiRequest<UserAnalytics>(`${API_ENDPOINTS.ADMIN.ANALYTICS.USERS}?days=${days}`);
}

export async function getCareerPrepAnalytics(days: number = 30): Promise<CareerPrepAnalytics> {
  return apiRequest<CareerPrepAnalytics>(
    `${API_ENDPOINTS.ADMIN.ANALYTICS.CAREER_PREPARATION}?days=${days}`
  );
}

export async function getMentorshipAnalytics(days: number = 30): Promise<MentorshipAnalytics> {
  return apiRequest<MentorshipAnalytics>(`${API_ENDPOINTS.ADMIN.ANALYTICS.MENTORSHIP}?days=${days}`);
}
