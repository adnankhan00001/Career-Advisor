import { apiRequest } from "./apiClient";
import { API_ENDPOINTS } from "./config";

export type MentorVerificationStatus = "APPLIED" | "APPROVED" | "REJECTED" | "SUSPENDED";

export type MentorshipRequestStatus = "PENDING" | "ACCEPTED" | "DECLINED" | "CANCELLED" | "COMPLETED";

export interface MentorApplicationDto {
  jobTitle: string;
  company: string;
  yearsOfExperience: number;
  bio?: string;
  expertiseAreas: string;
  maxMentees?: number;
  linkedinUrl?: string;
  githubUrl?: string;
  portfolioUrl?: string;
}

export interface MentorProfileUpdateDto {
  jobTitle?: string;
  company?: string;
  yearsOfExperience?: number;
  bio?: string;
  expertiseAreas?: string;
  maxMentees?: number;
  linkedinUrl?: string;
  githubUrl?: string;
  portfolioUrl?: string;
  isActive?: boolean;
}

export interface MentorProfileDto {
  id: number;
  userId: number;
  name: string;
  email: string;
  jobTitle: string;
  company: string;
  yearsOfExperience: number;
  bio: string | null;
  expertiseAreas: string;
  maxMentees: number;
  activeMenteesCount: number;
  availableSlots: number;
  verificationStatus: MentorVerificationStatus;
  isActive: boolean;
  rejectionReason: string | null;
  linkedinUrl: string | null;
  githubUrl: string | null;
  portfolioUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateMentorshipRequestDto {
  goalDescription: string;
}

export interface MentorshipRequestDto {
  id: number;
  studentId: number;
  studentName: string;
  studentEmail: string;
  mentorProfileId: number;
  mentorUserId: number;
  mentorName: string;
  mentorCompany: string;
  mentorJobTitle: string;
  goalDescription: string;
  status: MentorshipRequestStatus;
  conversationId: number | null;
  responseNote: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MentorshipActionDto {
  note?: string;
}

export interface MentorshipCapacityConfigDto {
  minMentees: number;
  maxMentees: number;
  defaultMentees: number;
}

export const mentorService = {
  // ================================================================
  // MENTOR PROFILE OPERATIONS
  // ================================================================

  async apply(data: MentorApplicationDto): Promise<MentorProfileDto> {
    return apiRequest<MentorProfileDto>(API_ENDPOINTS.MENTORS.APPLY, {
      method: "POST",
      body: JSON.stringify(data),
    });
  },

  async getMyProfile(): Promise<MentorProfileDto> {
    return apiRequest<MentorProfileDto>(API_ENDPOINTS.MENTORS.ME, {
      method: "GET",
    });
  },

  async updateMyProfile(data: MentorProfileUpdateDto): Promise<MentorProfileDto> {
    return apiRequest<MentorProfileDto>(API_ENDPOINTS.MENTORS.ME, {
      method: "PUT",
      body: JSON.stringify(data),
    });
  },

  async getApprovedMentors(params?: { query?: string; expertise?: string }): Promise<MentorProfileDto[]> {
    const queryParams = new URLSearchParams();
    if (params?.query && params.query.trim()) {
      queryParams.append("query", params.query.trim());
    }
    if (params?.expertise && params.expertise.trim()) {
      queryParams.append("expertise", params.expertise.trim());
    }
    const queryString = queryParams.toString();
    const url = queryString
      ? `${API_ENDPOINTS.MENTORS.LIST}?${queryString}`
      : API_ENDPOINTS.MENTORS.LIST;

    return apiRequest<MentorProfileDto[]>(url, {
      method: "GET",
    });
  },

  async getMentorById(mentorId: number | string): Promise<MentorProfileDto> {
    return apiRequest<MentorProfileDto>(API_ENDPOINTS.MENTORS.DETAIL(mentorId), {
      method: "GET",
    });
  },

  // ================================================================
  // MENTORSHIP REQUEST OPERATIONS
  // ================================================================

  async createRequest(mentorId: number | string, data: CreateMentorshipRequestDto): Promise<MentorshipRequestDto> {
    return apiRequest<MentorshipRequestDto>(API_ENDPOINTS.MENTORS.CREATE_REQUEST(mentorId), {
      method: "POST",
      body: JSON.stringify(data),
    });
  },

  async getSentRequests(): Promise<MentorshipRequestDto[]> {
    return apiRequest<MentorshipRequestDto[]>(API_ENDPOINTS.MENTORS.REQUESTS_SENT, {
      method: "GET",
    });
  },

  async getReceivedRequests(): Promise<MentorshipRequestDto[]> {
    return apiRequest<MentorshipRequestDto[]>(API_ENDPOINTS.MENTORS.REQUESTS_RECEIVED, {
      method: "GET",
    });
  },

  async acceptRequest(requestId: number | string, note?: string): Promise<MentorshipRequestDto> {
    const body = note && note.trim() ? JSON.stringify({ note: note.trim() }) : undefined;
    return apiRequest<MentorshipRequestDto>(API_ENDPOINTS.MENTORS.ACCEPT_REQUEST(requestId), {
      method: "PATCH",
      ...(body ? { body } : {}),
    });
  },

  async declineRequest(requestId: number | string, note?: string): Promise<MentorshipRequestDto> {
    const body = note && note.trim() ? JSON.stringify({ note: note.trim() }) : undefined;
    return apiRequest<MentorshipRequestDto>(API_ENDPOINTS.MENTORS.DECLINE_REQUEST(requestId), {
      method: "PATCH",
      ...(body ? { body } : {}),
    });
  },

  async cancelRequest(requestId: number | string): Promise<MentorshipRequestDto> {
    return apiRequest<MentorshipRequestDto>(API_ENDPOINTS.MENTORS.CANCEL_REQUEST(requestId), {
      method: "PATCH",
    });
  },

  async completeMentorship(requestId: number | string, note?: string): Promise<MentorshipRequestDto> {
    const body = note && note.trim() ? JSON.stringify({ note: note.trim() }) : undefined;
    return apiRequest<MentorshipRequestDto>(API_ENDPOINTS.MENTORS.COMPLETE_REQUEST(requestId), {
      method: "PATCH",
      ...(body ? { body } : {}),
    });
  },

  // ================================================================
  // ADMINISTRATIVE OPERATIONS (ROLE_ADMIN ONLY)
  // ================================================================

  async getPendingApplications(): Promise<MentorProfileDto[]> {
    return apiRequest<MentorProfileDto[]>(API_ENDPOINTS.MENTORS.ADMIN.PENDING, {
      method: "GET",
    });
  },

  async getAllMentors(status?: MentorVerificationStatus): Promise<MentorProfileDto[]> {
    const queryParams = new URLSearchParams();
    if (status) {
      queryParams.append("status", status);
    }
    const queryString = queryParams.toString();
    const url = queryString
      ? `${API_ENDPOINTS.MENTORS.ADMIN.LIST}?${queryString}`
      : API_ENDPOINTS.MENTORS.ADMIN.LIST;

    return apiRequest<MentorProfileDto[]>(url, {
      method: "GET",
    });
  },

  async getAdminMentorById(mentorId: number | string): Promise<MentorProfileDto> {
    return apiRequest<MentorProfileDto>(API_ENDPOINTS.MENTORS.ADMIN.DETAIL(mentorId), {
      method: "GET",
    });
  },

  async approveApplication(mentorId: number | string): Promise<MentorProfileDto> {
    return apiRequest<MentorProfileDto>(API_ENDPOINTS.MENTORS.ADMIN.APPROVE(mentorId), {
      method: "PATCH",
    });
  },

  async rejectApplication(mentorId: number | string, note?: string): Promise<MentorProfileDto> {
    const body = note && note.trim() ? JSON.stringify({ note: note.trim() }) : undefined;
    return apiRequest<MentorProfileDto>(API_ENDPOINTS.MENTORS.ADMIN.REJECT(mentorId), {
      method: "PATCH",
      ...(body ? { body } : {}),
    });
  },

  async suspendMentor(mentorId: number | string, note?: string): Promise<MentorProfileDto> {
    const body = note && note.trim() ? JSON.stringify({ note: note.trim() }) : undefined;
    return apiRequest<MentorProfileDto>(API_ENDPOINTS.MENTORS.ADMIN.SUSPEND(mentorId), {
      method: "PATCH",
      ...(body ? { body } : {}),
    });
  },

  async getCapacityConfig(): Promise<MentorshipCapacityConfigDto> {
    return apiRequest<MentorshipCapacityConfigDto>(API_ENDPOINTS.MENTORS.ADMIN.CONFIG, {
      method: "GET",
    });
  },

  async updateCapacityConfig(data: MentorshipCapacityConfigDto): Promise<MentorshipCapacityConfigDto> {
    return apiRequest<MentorshipCapacityConfigDto>(API_ENDPOINTS.MENTORS.ADMIN.CONFIG, {
      method: "PUT",
      body: JSON.stringify(data),
    });
  },
};
