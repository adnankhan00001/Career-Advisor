"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import {
  getAuditLogs,
  getAuditSummary,
  AdminAuditLog,
  AdminAuditSummary,
  AdminAuditAction,
  AdminAuditTargetType,
  AuditLogFilterParams,
} from "@/lib/adminAuditService";

export default function AdminAuditLogsPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();

  // Summary KPIs
  const [summary, setSummary] = useState<AdminAuditSummary | null>(null);
  const [isLoadingSummary, setIsLoadingSummary] = useState(true);

  // Table & Pagination state
  const [logs, setLogs] = useState<AdminAuditLog[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [sortField, setSortField] = useState<string>("createdAt");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");

  // Filters
  const [searchInput, setSearchInput] = useState("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [actionFilter, setActionFilter] = useState<AdminAuditAction | "">("");
  const [targetTypeFilter, setTargetTypeFilter] = useState<AdminAuditTargetType | "">("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Detail Modal
  const [selectedLog, setSelectedLog] = useState<AdminAuditLog | null>(null);
  const [copiedJson, setCopiedJson] = useState(false);

  // Route guard
  useEffect(() => {
    if (!isAuthLoading) {
      if (!isAuthenticated) {
        router.replace("/login");
      } else if (user?.role !== "ADMIN") {
        router.replace("/dashboard");
      }
    }
  }, [isAuthLoading, isAuthenticated, user, router]);

  const loadSummary = async () => {
    setIsLoadingSummary(true);
    try {
      const data = await getAuditSummary();
      setSummary(data);
    } catch (err: any) {
      console.error("Failed to load audit summary:", err);
    } finally {
      setIsLoadingSummary(false);
    }
  };

  const loadLogs = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const params: AuditLogFilterParams = {
        page,
        size,
        sort: `${sortField},${sortDirection}`,
        search: appliedSearch || undefined,
        action: actionFilter || undefined,
        targetType: targetTypeFilter || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
      };
      const response = await getAuditLogs(params);
      setLogs(response.content || []);
      setTotalPages(response.totalPages || 0);
      setTotalElements(response.totalElements || 0);
    } catch (err: any) {
      console.error("Failed to load audit logs:", err);
      setError(err.message || "Failed to load audit logs.");
    } finally {
      setIsLoading(false);
    }
  }, [page, size, sortField, sortDirection, appliedSearch, actionFilter, targetTypeFilter, startDate, endDate]);

  useEffect(() => {
    if (isAuthenticated && user?.role === "ADMIN") {
      loadSummary();
    }
  }, [isAuthenticated, user]);

  useEffect(() => {
    if (isAuthenticated && user?.role === "ADMIN") {
      loadLogs();
    }
  }, [isAuthenticated, user, loadLogs]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    setAppliedSearch(searchInput.trim());
  };

  const handleResetFilters = () => {
    setSearchInput("");
    setAppliedSearch("");
    setActionFilter("");
    setTargetTypeFilter("");
    setStartDate("");
    setEndDate("");
    setPage(0);
  };

  const handleCopyJson = (jsonString: string) => {
    navigator.clipboard.writeText(jsonString);
    setCopiedJson(true);
    setTimeout(() => setCopiedJson(false), 2000);
  };

  const getActionBadgeClass = (action: AdminAuditAction) => {
    switch (action) {
      case "MENTOR_APPROVED":
      case "USER_REACTIVATED":
      case "MENTOR_REACTIVATED":
        return "bg-emerald-50 text-emerald-700 border-emerald-200";
      case "USER_SUSPENDED":
      case "USER_DEACTIVATED":
      case "MENTOR_REJECTED":
      case "MENTOR_SUSPENDED":
      case "MENTORSHIP_TERMINATED":
        return "bg-rose-50 text-rose-700 border-rose-200";
      case "MENTOR_CAPACITY_UPDATED":
      case "USER_ROLE_CHANGED":
      case "PLATFORM_SETTING_UPDATED":
        return "bg-blue-50 text-blue-700 border-blue-200";
      default:
        return "bg-gray-50 text-gray-700 border-gray-200";
    }
  };

  const formatTimestamp = (dateString: string) => {
    try {
      const d = new Date(dateString);
      return d.toLocaleString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      });
    } catch {
      return dateString;
    }
  };

  if (isAuthLoading) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center text-gray-500 gap-3">
        <div className="w-8 h-8 border-4 border-black border-t-transparent rounded-full animate-spin"></div>
        <p className="text-sm font-medium">Verifying administrator authorization...</p>
      </div>
    );
  }

  if (!isAuthenticated || user?.role !== "ADMIN") {
    return null;
  }

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-gray-200/80 shadow-sm">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200">
              Security & Compliance
            </span>
            <span className="text-xs text-gray-400">•</span>
            <span className="text-xs text-gray-500 font-medium">Immutable Audit Trail</span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-gray-900">
            Administrative Audit Logs
          </h1>
          <p className="text-sm text-gray-500 mt-1 max-w-2xl">
            Complete, chronological history of governance actions, account interventions, approvals, and system mutations.
          </p>
        </div>

        {/* Action Controls & Navigation */}
        <div className="flex flex-wrap items-center gap-2">
          <Link
            href="/admin"
            className="px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
          >
            ← Admin Dashboard
          </Link>
          <Link
            href="/admin/users"
            className="px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
          >
            User Governance
          </Link>
          <Link
            href="/admin/mentors"
            className="px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
          >
            Mentor Governance
          </Link>
          <button
            onClick={() => {
              loadSummary();
              loadLogs();
            }}
            disabled={isLoading}
            className="px-3 py-1.5 text-xs font-medium text-white bg-black hover:bg-gray-800 disabled:opacity-50 rounded-lg transition-colors flex items-center gap-1.5"
          >
            <span className={`inline-block ${isLoading ? "animate-spin" : ""}`}>🔄</span>
            Refresh
          </button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        <div className="bg-white p-4 rounded-xl border border-gray-200/80 shadow-sm">
          <div className="flex items-center justify-between text-gray-500 mb-2">
            <span className="text-xs font-medium">Total Events</span>
            <span className="text-base">📋</span>
          </div>
          <div className="text-2xl font-bold text-gray-900">
            {isLoadingSummary ? "..." : (summary?.totalEvents ?? 0).toLocaleString()}
          </div>
          <p className="text-xs text-gray-500 mt-1">All recorded actions</p>
        </div>

        <div className="bg-white p-4 rounded-xl border border-gray-200/80 shadow-sm">
          <div className="flex items-center justify-between text-gray-500 mb-2">
            <span className="text-xs font-medium">Today</span>
            <span className="text-base">⚡</span>
          </div>
          <div className="text-2xl font-bold text-indigo-600">
            {isLoadingSummary ? "..." : (summary?.todayEvents ?? 0).toLocaleString()}
          </div>
          <p className="text-xs text-gray-500 mt-1">Since midnight UTC</p>
        </div>

        <div className="bg-white p-4 rounded-xl border border-gray-200/80 shadow-sm">
          <div className="flex items-center justify-between text-gray-500 mb-2">
            <span className="text-xs font-medium">User Actions</span>
            <span className="text-base">👥</span>
          </div>
          <div className="text-2xl font-bold text-gray-900">
            {isLoadingSummary ? "..." : (summary?.userActionsCount ?? 0).toLocaleString()}
          </div>
          <p className="text-xs text-gray-500 mt-1">Suspensions & status</p>
        </div>

        <div className="bg-white p-4 rounded-xl border border-gray-200/80 shadow-sm">
          <div className="flex items-center justify-between text-gray-500 mb-2">
            <span className="text-xs font-medium">Mentor Actions</span>
            <span className="text-base">🎓</span>
          </div>
          <div className="text-2xl font-bold text-gray-900">
            {isLoadingSummary ? "..." : (summary?.mentorActionsCount ?? 0).toLocaleString()}
          </div>
          <p className="text-xs text-gray-500 mt-1">Approvals & status</p>
        </div>

        <div className="bg-white p-4 rounded-xl border border-gray-200/80 shadow-sm col-span-2 md:col-span-1">
          <div className="flex items-center justify-between text-gray-500 mb-2">
            <span className="text-xs font-medium">Mentorships</span>
            <span className="text-base">🤝</span>
          </div>
          <div className="text-2xl font-bold text-gray-900">
            {isLoadingSummary ? "..." : (summary?.mentorshipActionsCount ?? 0).toLocaleString()}
          </div>
          <p className="text-xs text-gray-500 mt-1">Terminations & oversight</p>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white p-4 rounded-xl border border-gray-200/80 shadow-sm space-y-4">
        <form onSubmit={handleSearchSubmit} className="flex flex-col md:flex-row gap-3">
          <div className="flex-1 relative">
            <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400 text-sm">
              🔍
            </span>
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search by summary, admin email, or target ID..."
              className="w-full pl-9 pr-4 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-black/10 focus:border-black"
            />
          </div>
          <button
            type="submit"
            className="px-4 py-2 text-sm font-medium text-white bg-black hover:bg-gray-800 rounded-lg transition-colors whitespace-nowrap"
          >
            Search
          </button>
        </form>

        <div className="flex flex-wrap items-center gap-3 pt-2 border-t border-gray-100">
          {/* Action Filter */}
          <div className="flex items-center gap-1.5">
            <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
              Action:
            </label>
            <select
              value={actionFilter}
              onChange={(e) => {
                setActionFilter(e.target.value as AdminAuditAction | "");
                setPage(0);
              }}
              className="text-xs border border-gray-200 rounded-lg px-2.5 py-1.5 bg-white text-gray-700 focus:outline-none focus:ring-1 focus:ring-black"
            >
              <option value="">All Actions</option>
              <optgroup label="User Governance">
                <option value="USER_SUSPENDED">User Suspended</option>
                <option value="USER_REACTIVATED">User Reactivated</option>
                <option value="USER_DEACTIVATED">User Deactivated</option>
                <option value="USER_ROLE_CHANGED">User Role Changed</option>
              </optgroup>
              <optgroup label="Mentor Governance">
                <option value="MENTOR_APPROVED">Mentor Approved</option>
                <option value="MENTOR_REJECTED">Mentor Rejected</option>
                <option value="MENTOR_SUSPENDED">Mentor Suspended</option>
                <option value="MENTOR_REACTIVATED">Mentor Reactivated</option>
                <option value="MENTOR_CAPACITY_UPDATED">Capacity Updated</option>
              </optgroup>
              <optgroup label="Mentorship & System">
                <option value="MENTORSHIP_TERMINATED">Mentorship Terminated</option>
                <option value="PLATFORM_SETTING_UPDATED">Setting Updated</option>
              </optgroup>
            </select>
          </div>

          {/* Target Type Filter */}
          <div className="flex items-center gap-1.5">
            <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
              Target:
            </label>
            <select
              value={targetTypeFilter}
              onChange={(e) => {
                setTargetTypeFilter(e.target.value as AdminAuditTargetType | "");
                setPage(0);
              }}
              className="text-xs border border-gray-200 rounded-lg px-2.5 py-1.5 bg-white text-gray-700 focus:outline-none focus:ring-1 focus:ring-black"
            >
              <option value="">All Targets</option>
              <option value="USER">User</option>
              <option value="MENTOR">Mentor</option>
              <option value="MENTORSHIP_REQUEST">Mentorship Request</option>
              <option value="PLATFORM_SETTING">Platform Setting</option>
            </select>
          </div>

          {/* Date Range */}
          <div className="flex items-center gap-1.5">
            <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
              From:
            </label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value);
                setPage(0);
              }}
              className="text-xs border border-gray-200 rounded-lg px-2 py-1 bg-white text-gray-700 focus:outline-none focus:ring-1 focus:ring-black"
            />
          </div>

          <div className="flex items-center gap-1.5">
            <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
              To:
            </label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value);
                setPage(0);
              }}
              className="text-xs border border-gray-200 rounded-lg px-2 py-1 bg-white text-gray-700 focus:outline-none focus:ring-1 focus:ring-black"
            />
          </div>

          {/* Reset button */}
          {(appliedSearch || actionFilter || targetTypeFilter || startDate || endDate) && (
            <button
              onClick={handleResetFilters}
              className="text-xs font-medium text-rose-600 hover:text-rose-700 ml-auto flex items-center gap-1"
            >
              ✕ Clear Filters
            </button>
          )}
        </div>
      </div>

      {/* Audit Log Table */}
      <div className="bg-white rounded-xl border border-gray-200/80 shadow-sm overflow-hidden">
        {error ? (
          <div className="p-8 text-center">
            <div className="inline-flex p-3 rounded-full bg-rose-50 text-rose-600 mb-3">
              ⚠️
            </div>
            <h3 className="text-base font-semibold text-gray-900">Failed to load audit records</h3>
            <p className="text-sm text-gray-500 mt-1 max-w-md mx-auto">{error}</p>
            <button
              onClick={loadLogs}
              className="mt-4 px-4 py-2 text-xs font-medium text-white bg-black hover:bg-gray-800 rounded-lg transition-colors"
            >
              Retry
            </button>
          </div>
        ) : isLoading ? (
          <div className="p-12 text-center text-gray-500">
            <div className="w-8 h-8 border-4 border-black border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
            <p className="text-sm font-medium">Fetching audit trail records...</p>
          </div>
        ) : logs.length === 0 ? (
          <div className="p-12 text-center">
            <div className="text-4xl mb-3">📋</div>
            <h3 className="text-base font-semibold text-gray-900">No audit records found</h3>
            <p className="text-sm text-gray-500 mt-1 max-w-md mx-auto">
              No audit logs match the current search filters. Administrative actions will automatically be recorded here.
            </p>
            {(appliedSearch || actionFilter || targetTypeFilter || startDate || endDate) && (
              <button
                onClick={handleResetFilters}
                className="mt-4 px-4 py-2 text-xs font-medium text-black bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
              >
                Reset Filters
              </button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50/75 border-b border-gray-200 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Timestamp</th>
                  <th className="py-3 px-4">Administrator</th>
                  <th className="py-3 px-4">Action</th>
                  <th className="py-3 px-4">Target</th>
                  <th className="py-3 px-4">Summary</th>
                  <th className="py-3 px-4">Network IP</th>
                  <th className="py-3 px-4 text-right">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50/50 transition-colors">
                    {/* Timestamp */}
                    <td className="py-3.5 px-4 whitespace-nowrap text-xs text-gray-600">
                      {formatTimestamp(log.createdAt)}
                    </td>

                    {/* Administrator */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full bg-gray-200 text-gray-700 font-bold text-xs flex items-center justify-center">
                          {(log.adminName || log.adminEmail || "S").charAt(0).toUpperCase()}
                        </div>
                        <div>
                          <div className="font-medium text-gray-900 text-xs">
                            {log.adminName || "System Admin"}
                          </div>
                          <div className="text-gray-400 text-[11px]">
                            {log.adminEmail || "system@internal"}
                          </div>
                        </div>
                      </div>
                    </td>

                    {/* Action Badge */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span
                        className={`inline-block px-2.5 py-1 rounded-full text-xs font-semibold border ${getActionBadgeClass(
                          log.action
                        )}`}
                      >
                        {log.action.replace(/_/g, " ")}
                      </span>
                    </td>

                    {/* Target */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <div className="text-xs font-medium text-gray-800">
                        {log.targetType}
                      </div>
                      {log.targetId && (
                        <div className="text-[11px] font-mono text-gray-500">
                          ID: #{log.targetId}
                        </div>
                      )}
                    </td>

                    {/* Summary */}
                    <td className="py-3.5 px-4">
                      <div className="text-xs text-gray-800 max-w-xs md:max-w-md truncate" title={log.summary}>
                        {log.summary}
                      </div>
                    </td>

                    {/* IP Address */}
                    <td className="py-3.5 px-4 whitespace-nowrap text-xs font-mono text-gray-500">
                      {log.ipAddress || "—"}
                    </td>

                    {/* Inspect Button */}
                    <td className="py-3.5 px-4 whitespace-nowrap text-right">
                      <button
                        onClick={() => setSelectedLog(log)}
                        className="px-2.5 py-1 text-xs font-medium text-indigo-600 hover:text-indigo-800 bg-indigo-50 hover:bg-indigo-100 rounded-md transition-colors"
                      >
                        Inspect
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Footer */}
        {totalElements > 0 && (
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3 px-4 py-3 border-t border-gray-100 bg-gray-50/50">
            <div className="text-xs text-gray-500">
              Showing{" "}
              <span className="font-semibold text-gray-700">
                {page * size + 1}
              </span>{" "}
              to{" "}
              <span className="font-semibold text-gray-700">
                {Math.min((page + 1) * size, totalElements)}
              </span>{" "}
              of <span className="font-semibold text-gray-700">{totalElements}</span> entries
            </div>

            <div className="flex items-center gap-2">
              <div className="flex items-center gap-1 mr-2">
                <span className="text-xs text-gray-500">Rows:</span>
                <select
                  value={size}
                  onChange={(e) => {
                    setSize(Number(e.target.value));
                    setPage(0);
                  }}
                  className="text-xs border border-gray-200 rounded px-2 py-1 bg-white text-gray-700 focus:outline-none"
                >
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                  <option value={100}>100</option>
                </select>
              </div>

              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0 || isLoading}
                className="px-3 py-1 text-xs font-medium rounded-lg border border-gray-200 bg-white text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              >
                Previous
              </button>
              <span className="text-xs text-gray-600 font-medium px-1">
                {page + 1} / {Math.max(1, totalPages)}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1 || isLoading}
                className="px-3 py-1 text-xs font-medium rounded-lg border border-gray-200 bg-white text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Inspection Modal */}
      {selectedLog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-2xl w-full border border-gray-200 shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
            {/* Modal Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100 bg-gray-50/50">
              <div className="flex items-center gap-2.5">
                <span className="text-lg">📋</span>
                <div>
                  <h3 className="text-base font-bold text-gray-900">Audit Record Details</h3>
                  <p className="text-xs text-gray-500">Record ID #{selectedLog.id}</p>
                </div>
              </div>
              <button
                onClick={() => setSelectedLog(null)}
                className="w-8 h-8 rounded-full flex items-center justify-center text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
              >
                ✕
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6 overflow-y-auto space-y-5 text-sm">
              {/* Event Attributes Grid */}
              <div className="grid grid-cols-2 gap-4 p-4 rounded-xl bg-gray-50/75 border border-gray-100">
                <div>
                  <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider block">
                    Action
                  </span>
                  <span
                    className={`inline-block mt-1 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${getActionBadgeClass(
                      selectedLog.action
                    )}`}
                  >
                    {selectedLog.action}
                  </span>
                </div>

                <div>
                  <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider block">
                    Timestamp
                  </span>
                  <span className="text-xs text-gray-800 font-medium block mt-1">
                    {formatTimestamp(selectedLog.createdAt)}
                  </span>
                </div>

                <div>
                  <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider block">
                    Administrator
                  </span>
                  <div className="text-xs text-gray-900 font-medium mt-1">
                    {selectedLog.adminName || "System"} (ID: {selectedLog.adminUserId ?? "—"})
                  </div>
                  <div className="text-[11px] text-gray-500 font-mono">
                    {selectedLog.adminEmail || "system@internal"}
                  </div>
                </div>

                <div>
                  <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider block">
                    Target
                  </span>
                  <div className="text-xs text-gray-900 font-medium mt-1">
                    {selectedLog.targetType}
                  </div>
                  <div className="text-[11px] text-gray-500 font-mono">
                    Target ID: {selectedLog.targetId || "—"}
                  </div>
                </div>

                <div>
                  <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider block">
                    Client IP Address
                  </span>
                  <span className="text-xs font-mono text-gray-700 mt-1 block">
                    {selectedLog.ipAddress || "Unknown"}
                  </span>
                </div>

                <div>
                  <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider block">
                    Correlation ID
                  </span>
                  <span className="text-xs font-mono text-gray-700 mt-1 block truncate" title={selectedLog.correlationId || "None"}>
                    {selectedLog.correlationId || "None"}
                  </span>
                </div>
              </div>

              {/* Summary */}
              <div>
                <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider block mb-1">
                  Summary
                </span>
                <p className="text-sm text-gray-800 bg-white p-3 rounded-lg border border-gray-200">
                  {selectedLog.summary}
                </p>
              </div>

              {/* Structured Details JSON */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
                    Audit Payload (Sanitized JSON)
                  </span>
                  {selectedLog.detailsJson && (
                    <button
                      onClick={() => handleCopyJson(selectedLog.detailsJson!)}
                      className="text-xs text-indigo-600 hover:text-indigo-800 font-medium flex items-center gap-1"
                    >
                      {copiedJson ? "✓ Copied!" : "📋 Copy JSON"}
                    </button>
                  )}
                </div>

                {selectedLog.detailsJson ? (
                  <pre className="p-3.5 bg-gray-900 text-gray-100 rounded-xl text-xs font-mono overflow-x-auto max-h-60 leading-relaxed">
                    {(() => {
                      try {
                        return JSON.stringify(JSON.parse(selectedLog.detailsJson), null, 2);
                      } catch {
                        return selectedLog.detailsJson;
                      }
                    })()}
                  </pre>
                ) : (
                  <p className="text-xs text-gray-400 italic p-3 bg-gray-50 rounded-lg border border-dashed border-gray-200">
                    No additional metadata attached to this audit entry.
                  </p>
                )}
              </div>
            </div>

            {/* Modal Footer */}
            <div className="flex items-center justify-end px-6 py-3 border-t border-gray-100 bg-gray-50">
              <button
                onClick={() => setSelectedLog(null)}
                className="px-4 py-2 text-xs font-medium text-gray-700 hover:text-gray-900 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
