import { apiRequest } from "./apiClient";
import { API_ENDPOINTS } from "./config";

export interface AdminProfile {
  id: number;
  name: string;
  email: string;
  role: "ADMIN" | string;
}

export interface AdminHealth {
  status: string;
  service: string;
  role: string;
  authenticatedUser?: string;
}

export interface AdminStatsOverview {
  totalUsers: number;
  totalResumes: number;
  totalQuizAttempts: number;
  totalSolvedProblems: number;
  totalMockInterviews: number;
  totalCompletedInterviews: number;
  totalCareerGoals: number;
  careerGoalsDistribution: Record<string, number>;
  userLevelDistribution: Record<string, number>;
}

export interface AdminUser {
  id: number;
  name: string;
  email: string;
  role: "USER" | "ADMIN" | string;
  careerGoal?: string;
  userLevel?: string;
  latestQuizScore?: string;
  skillCount: number;
  resumePresent: boolean;
  mockInterviewCount: number;
  solvedProblemsCount: number;
}

export interface AdminUserDetail {
  id: number;
  name: string;
  email: string;
  role: "USER" | "ADMIN" | string;
  careerGoal?: string;
  userLevel?: string;
  latestQuizScore?: string;
  skills: string[];
  resumePresent: boolean;
  resumeFileName?: string;
  resumeUploadTimestamp?: string;
  mockInterviewCount: number;
  averageInterviewScore?: number;
  solvedProblemsCount: number;
  completedRoadmapStepsCount: number;
}

export async function getAdminProfile(): Promise<AdminProfile> {
  return apiRequest<AdminProfile>(API_ENDPOINTS.ADMIN.ME);
}

export async function getAdminHealth(): Promise<AdminHealth> {
  return apiRequest<AdminHealth>(API_ENDPOINTS.ADMIN.HEALTH);
}

export async function getAdminStatsOverview(): Promise<AdminStatsOverview> {
  return apiRequest<AdminStatsOverview>(API_ENDPOINTS.ADMIN.STATS_OVERVIEW);
}

export async function getAdminUsers(search?: string): Promise<AdminUser[]> {
  const url = search
    ? `${API_ENDPOINTS.ADMIN.USERS}?search=${encodeURIComponent(search)}`
    : API_ENDPOINTS.ADMIN.USERS;
  return apiRequest<AdminUser[]>(url);
}

export async function getAdminUserDetail(id: number): Promise<AdminUserDetail> {
  return apiRequest<AdminUserDetail>(API_ENDPOINTS.ADMIN.USER_DETAIL(id));
}
