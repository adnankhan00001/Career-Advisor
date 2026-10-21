import { apiRequest } from "./apiClient";
import { API_ENDPOINTS } from "./config";

export interface MentorshipCapacityConfig {
  minMentees: number;
  maxMentees: number;
  defaultMentees: number;
}

export interface PlatformSettings {
  mentorshipCapacity: MentorshipCapacityConfig;
  lastUpdatedAt: string | null;
  lastUpdatedBy: string | null;
}

export interface UpdatePlatformSettingsRequest {
  mentorshipCapacity: MentorshipCapacityConfig;
}

export async function getPlatformSettings(): Promise<PlatformSettings> {
  return apiRequest<PlatformSettings>(API_ENDPOINTS.ADMIN.SETTINGS);
}

export async function updatePlatformSettings(
  payload: UpdatePlatformSettingsRequest
): Promise<PlatformSettings> {
  return apiRequest<PlatformSettings>(API_ENDPOINTS.ADMIN.SETTINGS, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}
