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
  activeUsers?: number;
  approvedMentors?: number;
  pendingMentorApplications?: number;
  activeMentorships?: number;
  openSupportRequests?: number;
  totalResumes: number;
  totalQuizAttempts: number;
  totalSolvedProblems: number;
  totalMockInterviews: number;
  totalCompletedInterviews: number;
  totalCareerGoals: number;
  careerGoalsDistribution: Record<string, number>;
  userLevelDistribution: Record<string, number>;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AdminUserQueryParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
  role?: string;
  status?: string;
}

export interface AdminUser {
  id: number;
  name: string;
  email: string;
  role: "USER" | "ADMIN" | string;
  status: "ACTIVE" | "SUSPENDED" | "DEACTIVATED" | string;
  suspensionReason?: string;
  suspendedAt?: string;
  createdAt?: string;
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
  status: "ACTIVE" | "SUSPENDED" | "DEACTIVATED" | string;
  suspensionReason?: string;
  suspendedAt?: string;
  createdAt?: string;
  updatedAt?: string;
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

export interface UpdateUserStatusPayload {
  status: "ACTIVE" | "SUSPENDED" | "DEACTIVATED";
  reason?: string;
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

export async function getAdminUsers(
  params?: AdminUserQueryParams | string
): Promise<PaginatedResponse<AdminUser>> {
  const query = new URLSearchParams();

  if (typeof params === "string") {
    if (params.trim()) query.set("search", params.trim());
  } else if (params) {
    if (params.page !== undefined) query.set("page", params.page.toString());
    if (params.size !== undefined) query.set("size", params.size.toString());
    if (params.sort) query.set("sort", params.sort);
    if (params.search && params.search.trim()) query.set("search", params.search.trim());
    if (params.role && params.role !== "ALL") query.set("role", params.role);
    if (params.status && params.status !== "ALL") query.set("status", params.status);
  }

  const qs = query.toString();
  const url = qs ? `${API_ENDPOINTS.ADMIN.USERS}?${qs}` : API_ENDPOINTS.ADMIN.USERS;
  return apiRequest<PaginatedResponse<AdminUser>>(url);
}

export async function getAdminUserDetail(id: number): Promise<AdminUserDetail> {
  return apiRequest<AdminUserDetail>(API_ENDPOINTS.ADMIN.USER_DETAIL(id));
}

export async function updateUserStatus(
  id: number,
  payload: UpdateUserStatusPayload
): Promise<AdminUserDetail> {
  return apiRequest<AdminUserDetail>(API_ENDPOINTS.ADMIN.USER_STATUS(id), {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
}
