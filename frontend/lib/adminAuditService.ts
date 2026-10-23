import { apiRequest } from "./apiClient";
import { API_ENDPOINTS } from "./config";
import { PaginatedResponse } from "./adminService";

export type AdminAuditAction =
  | "USER_SUSPENDED"
  | "USER_REACTIVATED"
  | "USER_DEACTIVATED"
  | "USER_ROLE_CHANGED"
  | "MENTOR_APPROVED"
  | "MENTOR_REJECTED"
  | "MENTOR_SUSPENDED"
  | "MENTOR_REACTIVATED"
  | "MENTOR_CAPACITY_UPDATED"
  | "MENTORSHIP_TERMINATED"
  | "PLATFORM_SETTING_UPDATED"
  | "SUPPORT_TICKET_RESOLVED"
  | "SUPPORT_TICKET_REOPENED"
  | "SUPPORT_REPLY_SENT";

export type AdminAuditTargetType =
  | "USER"
  | "MENTOR"
  | "MENTORSHIP_REQUEST"
  | "PLATFORM_SETTING"
  | "SUPPORT_CONVERSATION";

export interface AdminAuditLog {
  id: number;
  action: AdminAuditAction;
  adminUserId: number | null;
  adminEmail: string | null;
  adminName: string | null;
  targetType: AdminAuditTargetType;
  targetId: string | null;
  summary: string;
  detailsJson: string | null;
  ipAddress: string | null;
  correlationId: string | null;
  createdAt: string;
}

export interface AdminAuditSummary {
  totalEvents: number;
  todayEvents: number;
  mentorActionsCount: number;
  userActionsCount: number;
  mentorshipActionsCount: number;
  supportActionsCount: number;
  settingsActionsCount: number;
}

export interface AuditLogFilterParams {
  page?: number;
  size?: number;
  sort?: string;
  action?: AdminAuditAction | "";
  targetType?: AdminAuditTargetType | "";
  adminUserId?: number | string;
  targetId?: string;
  search?: string;
  startDate?: string;
  endDate?: string;
}

export async function getAuditLogs(
  params: AuditLogFilterParams = {}
): Promise<PaginatedResponse<AdminAuditLog>> {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set("page", params.page.toString());
  if (params.size !== undefined) query.set("size", params.size.toString());
  if (params.sort) query.set("sort", params.sort);
  if (params.action) query.set("action", params.action);
  if (params.targetType) query.set("targetType", params.targetType);
  if (params.adminUserId) query.set("adminUserId", params.adminUserId.toString());
  if (params.targetId) query.set("targetId", params.targetId);
  if (params.search) query.set("search", params.search);
  if (params.startDate) query.set("startDate", params.startDate);
  if (params.endDate) query.set("endDate", params.endDate);

  const qs = query.toString();
  const url = qs
    ? `${API_ENDPOINTS.ADMIN.AUDIT_LOGS}?${qs}`
    : API_ENDPOINTS.ADMIN.AUDIT_LOGS;
  return apiRequest<PaginatedResponse<AdminAuditLog>>(url);
}

export async function getAuditSummary(): Promise<AdminAuditSummary> {
  return apiRequest<AdminAuditSummary>(API_ENDPOINTS.ADMIN.AUDIT_SUMMARY);
}

export async function getAuditLogById(
  id: number | string
): Promise<AdminAuditLog> {
  return apiRequest<AdminAuditLog>(API_ENDPOINTS.ADMIN.AUDIT_DETAIL(id));
}
