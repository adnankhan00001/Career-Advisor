import { apiRequest } from "./apiClient";
import { API_ENDPOINTS } from "./config";

export interface MentorshipCapacityConfig {
  minMentees: number;
  maxMentees: number;
  defaultMentees: number;
}

export interface PlatformConfig {
  aiEnabled: boolean;
  aiProvider: string;
  aiModel: string;
  maxTokens: number;
  temperature: number;
  chatMaxMessageLength: number;
  chatHistoryLimit: number;
  chatTitleMaxLength: number;
  chatEnabled: boolean;
}

export interface SecurityPolicyConfig {
  authenticationMethod: string;
  tokenExpiration: string;
  rbacHierarchy: string;
  adminSafeguard: string;
  selfStatusModification: string;
  auditTrailImmutability: string;
  credentialSanitization: string;
}

export interface SystemInfoConfig {
  applicationName: string;
  serviceName: string;
  databaseEngine: string;
  databaseDialect: string;
  javaVersion: string;
  operatingSystem: string;
  serverStatus: string;
  mentorshipConstraintInvariant: string;
}

export interface PlatformSettings {
  mentorshipCapacity: MentorshipCapacityConfig;
  platformConfig?: PlatformConfig;
  securityPolicy?: SecurityPolicyConfig;
  systemInfo?: SystemInfoConfig;
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
