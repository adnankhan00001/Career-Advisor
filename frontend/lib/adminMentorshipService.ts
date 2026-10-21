import { apiRequest } from "./apiClient";
import { API_ENDPOINTS } from "./config";
import { PaginatedResponse } from "./adminService";
import { MentorshipRequestStatus } from "./mentorService";

export interface MentorshipOversightSummary {
  totalRequests: number;
  pendingRequests: number;
  acceptedRequests: number;
  completedRequests: number;
  declinedRequests: number;
  cancelledRequests: number;
  terminatedRequests: number;
  activeMentorships: number;
}

export interface AdminMentorshipRequest {
  id: number;
  studentId: number;
  studentName: string;
  studentEmail: string;
  studentCareerGoal?: string | null;
  mentorProfileId: number;
  mentorUserId: number;
  mentorName: string;
  mentorEmail: string;
  mentorCompany: string;
  mentorJobTitle: string;
  goalDescription: string;
  status: MentorshipRequestStatus | "TERMINATED";
  conversationId?: number | null;
  lastActivityAt: string;
  responseNote?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminMentorshipDetail {
  id: number;
  studentId: number;
  studentName: string;
  studentEmail: string;
  studentCareerGoal?: string | null;
  studentUserLevel?: string | null;
  studentSkills: string[];
  mentorProfileId: number;
  mentorUserId: number;
  mentorName: string;
  mentorEmail: string;
  mentorCompany: string;
  mentorJobTitle: string;
  mentorYearsOfExperience?: number;
  mentorBio?: string | null;
  mentorExpertiseAreas?: string;
  mentorMaxMentees: number;
  mentorActiveMenteesCount: number;
  goalDescription: string;
  status: MentorshipRequestStatus | "TERMINATED";
  conversationId?: number | null;
  conversationTitle?: string | null;
  messageCount: number;
  lastActivityAt: string;
  responseNote?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminMentorshipQueryParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
  status?: string;
  fromDate?: string;
  toDate?: string;
}

export async function getMentorshipSummary(): Promise<MentorshipOversightSummary> {
  return apiRequest<MentorshipOversightSummary>(API_ENDPOINTS.ADMIN.MENTORSHIP_SUMMARY);
}

export async function getAdminMentorshipRequests(
  params?: AdminMentorshipQueryParams
): Promise<PaginatedResponse<AdminMentorshipRequest>> {
  const query = new URLSearchParams();

  if (params) {
    if (params.page !== undefined) query.set("page", params.page.toString());
    if (params.size !== undefined) query.set("size", params.size.toString());
    if (params.sort) query.set("sort", params.sort);
    if (params.search && params.search.trim()) query.set("search", params.search.trim());
    if (params.status && params.status !== "ALL") query.set("status", params.status);
    if (params.fromDate) query.set("fromDate", params.fromDate);
    if (params.toDate) query.set("toDate", params.toDate);
  }

  const qs = query.toString();
  const url = qs
    ? `${API_ENDPOINTS.ADMIN.MENTORSHIP_LIST}?${qs}`
    : API_ENDPOINTS.ADMIN.MENTORSHIP_LIST;

  return apiRequest<PaginatedResponse<AdminMentorshipRequest>>(url);
}

export async function getAdminMentorshipDetail(id: number): Promise<AdminMentorshipDetail> {
  return apiRequest<AdminMentorshipDetail>(API_ENDPOINTS.ADMIN.MENTORSHIP_DETAIL(id));
}

export async function terminateMentorship(
  id: number,
  reason: string
): Promise<AdminMentorshipDetail> {
  return apiRequest<AdminMentorshipDetail>(API_ENDPOINTS.ADMIN.MENTORSHIP_TERMINATE(id), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ reason }),
  });
}
