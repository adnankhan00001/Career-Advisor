"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import {
  getMentorshipSummary,
  getAdminMentorshipRequests,
  getAdminMentorshipDetail,
  terminateMentorship,
  MentorshipOversightSummary,
  AdminMentorshipRequest,
  AdminMentorshipDetail,
  AdminMentorshipQueryParams,
} from "@/lib/adminMentorshipService";

export default function AdminMentorshipsPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();

  // Summary KPIs
  const [summary, setSummary] = useState<MentorshipOversightSummary | null>(null);
  const [isLoadingSummary, setIsLoadingSummary] = useState(true);

  // Table & Pagination state
  const [requests, setRequests] = useState<AdminMentorshipRequest[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [sortField, setSortField] = useState<string>("createdAt");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");

  // Filters
  const [searchInput, setSearchInput] = useState("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; message: string } | null>(null);

  // Inspect Modal
  const [selectedRequest, setSelectedRequest] = useState<AdminMentorshipDetail | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);

  // Terminate Modal
  const [terminateTarget, setTerminateTarget] = useState<AdminMentorshipRequest | null>(null);
  const [terminationReason, setTerminationReason] = useState("");
  const [terminateError, setTerminateError] = useState<string | null>(null);
  const [isSubmittingTerminate, setIsSubmittingTerminate] = useState(false);

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

  // Handle ESC key for closing modals
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        if (terminateTarget) {
          setTerminateTarget(null);
        } else if (selectedRequest) {
          setSelectedRequest(null);
        }
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [terminateTarget, selectedRequest]);

  const loadSummary = async () => {
    setIsLoadingSummary(true);
    try {
      const data = await getMentorshipSummary();
      setSummary(data);
    } catch (err: any) {
      console.error("Failed to load mentorship summary:", err);
    } finally {
      setIsLoadingSummary(false);
    }
  };

  const loadRequests = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const params: AdminMentorshipQueryParams = {
        page,
        size,
        sort: `${sortField},${sortDirection}`,
        search: appliedSearch,
        status: statusFilter,
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
      };
      const response = await getAdminMentorshipRequests(params);
      setRequests(response.content || []);
      setTotalPages(response.totalPages || 0);
      setTotalElements(response.totalElements || 0);
    } catch (err: any) {
      console.error("Failed to load mentorship requests:", err);
      setError(err.message || "Failed to load mentorship records.");
    } finally {
      setIsLoading(false);
    }
  }, [page, size, sortField, sortDirection, appliedSearch, statusFilter, fromDate, toDate]);

  useEffect(() => {
    if (user?.role === "ADMIN") {
      loadSummary();
      loadRequests();
    }
  }, [user, loadRequests]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    setAppliedSearch(searchInput.trim());
  };

  const handleClearFilters = () => {
    setSearchInput("");
    setAppliedSearch("");
    setStatusFilter("ALL");
    setFromDate("");
    setToDate("");
    setPage(0);
  };

  const handleSort = (field: string) => {
    if (sortField === field) {
      setSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setSortField(field);
      setSortDirection("asc");
    }
    setPage(0);
  };

  const handleInspect = async (id: number) => {
    setIsLoadingDetail(true);
    try {
      const detail = await getAdminMentorshipDetail(id);
      setSelectedRequest(detail);
    } catch (err: any) {
      alert("Failed to load request details: " + err.message);
    } finally {
      setIsLoadingDetail(false);
    }
  };

  const handleOpenTerminate = (req: AdminMentorshipRequest) => {
    setTerminateTarget(req);
    setTerminationReason("");
    setTerminateError(null);
  };

  const handleConfirmTerminate = async () => {
    if (!terminateTarget) return;
    if (!terminationReason.trim()) {
      setTerminateError("Please enter a meaningful reason for terminating this mentorship.");
      return;
    }

    setIsSubmittingTerminate(true);
    setTerminateError(null);
    try {
      await terminateMentorship(terminateTarget.id, terminationReason.trim());
      setFeedback({
        type: "success",
        message: `Mentorship request #${terminateTarget.id} was terminated successfully. Historical records have been preserved.`,
      });
      setTerminateTarget(null);
      loadSummary();
      loadRequests();
    } catch (err: any) {
      console.error("Failed to terminate mentorship:", err);
      setTerminateError(err.message || "Failed to terminate mentorship request.");
    } finally {
      setIsSubmittingTerminate(false);
    }
  };

  const formatDate = (isoString?: string) => {
    if (!isoString) return "N/A";
    try {
      const date = new Date(isoString);
      return date.toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
        year: "numeric",
      });
    } catch {
      return isoString;
    }
  };

  const formatDateTime = (isoString?: string) => {
    if (!isoString) return "N/A";
    try {
      const date = new Date(isoString);
      return date.toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return isoString;
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "ACCEPTED":
        return "bg-emerald-50 text-emerald-700 border-emerald-200";
      case "COMPLETED":
        return "bg-blue-50 text-blue-700 border-blue-200";
      case "PENDING":
        return "bg-amber-50 text-amber-700 border-amber-200";
      case "DECLINED":
        return "bg-rose-50 text-rose-700 border-rose-200";
      case "CANCELLED":
        return "bg-gray-100 text-gray-600 border-gray-200";
      case "TERMINATED":
        return "bg-purple-100 text-purple-700 border-purple-200";
      default:
        return "bg-gray-100 text-gray-700 border-gray-200";
    }
  };

  if (isAuthLoading || (user && user.role !== "ADMIN")) {
    return (
      <div className="min-h-[50vh] flex flex-col items-center justify-center text-gray-500 gap-3">
        <div className="w-8 h-8 border-4 border-black border-t-transparent rounded-full animate-spin"></div>
        <p className="text-sm font-medium">Verifying administrator authorization...</p>
      </div>
    );
  }

  const startRecord = totalElements > 0 ? page * size + 1 : 0;
  const endRecord = Math.min((page + 1) * size, totalElements);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-xs font-semibold text-gray-500 mb-1">
            <Link href="/admin" className="hover:text-black transition">
              Admin Dashboard
            </Link>
            <span>›</span>
            <span>Mentorship Oversight</span>
          </div>
          <h1 className="text-2xl font-extrabold text-gray-900 tracking-tight">
            Mentorship Oversight & Lifecycle Records
          </h1>
          <p className="text-xs text-gray-500">
            Monitor mentorship relations, inspect active conversations, and manage administrative interventions.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Link
            href="/admin/mentors"
            className="px-4 py-2 rounded-xl bg-purple-50 text-purple-700 hover:bg-purple-100 font-semibold text-xs border border-purple-200 transition"
          >
            🎓 Mentor Governance
          </Link>
          <Link
            href="/admin"
            className="px-4 py-2 rounded-xl bg-gray-100 text-gray-700 hover:bg-gray-200 font-semibold text-xs transition"
          >
            ← Overview
          </Link>
        </div>
      </div>

      {/* Feedback Alert */}
      {feedback && (
        <div
          className={`p-4 rounded-2xl flex items-center justify-between text-sm ${
            feedback.type === "success"
              ? "bg-emerald-50 border border-emerald-200 text-emerald-800"
              : "bg-red-50 border border-red-200 text-red-800"
          }`}
        >
          <span>{feedback.type === "success" ? "✓" : "⚠️"} {feedback.message}</span>
          <button
            onClick={() => setFeedback(null)}
            aria-label="Dismiss feedback message"
            className="text-xs font-bold px-2 py-1 rounded-lg hover:bg-black/5 cursor-pointer focus:outline-none focus:ring-2 focus:ring-black"
          >
            ✕
          </button>
        </div>
      )}

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-3">
        <div className="bg-white p-4 rounded-2xl border border-gray-200 shadow-xs">
          <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Total</p>
          <p className="text-xl font-extrabold text-gray-900 mt-1">
            {isLoadingSummary ? "..." : summary?.totalRequests ?? 0}
          </p>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-amber-200/80 shadow-xs">
          <p className="text-[10px] font-bold text-amber-600 uppercase tracking-wider">Pending</p>
          <p className="text-xl font-extrabold text-amber-700 mt-1">
            {isLoadingSummary ? "..." : summary?.pendingRequests ?? 0}
          </p>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-emerald-200/80 shadow-xs">
          <p className="text-[10px] font-bold text-emerald-600 uppercase tracking-wider">Active</p>
          <p className="text-xl font-extrabold text-emerald-700 mt-1">
            {isLoadingSummary ? "..." : summary?.activeMentorships ?? 0}
          </p>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-blue-200/80 shadow-xs">
          <p className="text-[10px] font-bold text-blue-600 uppercase tracking-wider">Completed</p>
          <p className="text-xl font-extrabold text-blue-700 mt-1">
            {isLoadingSummary ? "..." : summary?.completedRequests ?? 0}
          </p>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-rose-200/80 shadow-xs">
          <p className="text-[10px] font-bold text-rose-600 uppercase tracking-wider">Declined</p>
          <p className="text-xl font-extrabold text-rose-700 mt-1">
            {isLoadingSummary ? "..." : summary?.declinedRequests ?? 0}
          </p>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-gray-200 shadow-xs">
          <p className="text-[10px] font-bold text-gray-500 uppercase tracking-wider">Cancelled</p>
          <p className="text-xl font-extrabold text-gray-700 mt-1">
            {isLoadingSummary ? "..." : summary?.cancelledRequests ?? 0}
          </p>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-purple-200/80 shadow-xs">
          <p className="text-[10px] font-bold text-purple-600 uppercase tracking-wider">Terminated</p>
          <p className="text-xl font-extrabold text-purple-700 mt-1">
            {isLoadingSummary ? "..." : summary?.terminatedRequests ?? 0}
          </p>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white rounded-2xl p-4 border border-gray-200 shadow-xs space-y-4">
        <div className="flex flex-col md:flex-row gap-3 items-stretch md:items-center justify-between">
          {/* Search Input */}
          <form onSubmit={handleSearchSubmit} className="flex-1 flex gap-2">
            <div className="relative flex-1">
              <input
                type="text"
                id="mentorship-search-input"
                aria-label="Search by student, mentor, or company"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Search student, mentor, or company..."
                className="w-full pl-9 pr-8 py-2 rounded-xl border border-gray-200 text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-black"
              />
              <span className="absolute left-3 top-2.5 text-gray-400 text-xs" aria-hidden="true">🔍</span>
              {searchInput && (
                <button
                  type="button"
                  onClick={() => {
                    setSearchInput("");
                    setAppliedSearch("");
                    setPage(0);
                  }}
                  aria-label="Clear mentorship search"
                  className="absolute right-2.5 top-2 text-gray-400 hover:text-gray-700 text-xs font-bold"
                >
                  ✕
                </button>
              )}
            </div>
            <button
              type="submit"
              className="px-4 py-2 rounded-xl bg-black text-white font-semibold text-xs hover:bg-gray-800 transition cursor-pointer focus:outline-none focus:ring-2 focus:ring-black"
            >
              Search
            </button>
          </form>

          {/* Date Range Inputs */}
          <div className="flex items-center gap-2 text-xs">
            <div className="flex items-center gap-1 bg-gray-50 px-2 py-1 rounded-xl border border-gray-200">
              <span className="text-gray-400">From:</span>
              <input
                type="date"
                aria-label="Filter from date"
                value={fromDate}
                onChange={(e) => {
                  setFromDate(e.target.value);
                  setPage(0);
                }}
                className="bg-transparent text-gray-700 text-xs focus:outline-none"
              />
            </div>
            <div className="flex items-center gap-1 bg-gray-50 px-2 py-1 rounded-xl border border-gray-200">
              <span className="text-gray-400">To:</span>
              <input
                type="date"
                aria-label="Filter to date"
                value={toDate}
                onChange={(e) => {
                  setToDate(e.target.value);
                  setPage(0);
                }}
                className="bg-transparent text-gray-700 text-xs focus:outline-none"
              />
            </div>
            {(appliedSearch || statusFilter !== "ALL" || fromDate || toDate) && (
              <button
                type="button"
                onClick={handleClearFilters}
                className="px-3 py-1.5 rounded-lg bg-gray-100 hover:bg-gray-200 text-gray-600 font-bold text-xs cursor-pointer focus:outline-none focus:ring-2 focus:ring-black"
              >
                Clear
              </button>
            )}
          </div>
        </div>

        {/* Status Filter Tabs */}
        <div className="flex flex-wrap items-center gap-1.5 pt-2 border-t border-gray-100">
          <span className="text-[11px] font-bold text-gray-400 pr-1">Status:</span>
          {["ALL", "PENDING", "ACCEPTED", "COMPLETED", "DECLINED", "CANCELLED", "TERMINATED"].map((st) => (
            <button
              key={st}
              onClick={() => {
                setStatusFilter(st);
                setPage(0);
              }}
              className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition cursor-pointer focus:outline-none focus:ring-2 focus:ring-purple-400 ${
                statusFilter === st
                  ? "bg-purple-600 text-white shadow-xs"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {st}
            </button>
          ))}
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-sm">
          ⚠️ {error}
        </div>
      )}

      {/* Mentorship Records Table */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-gray-50/80 border-b border-gray-200 text-gray-500 font-bold uppercase tracking-wider select-none">
              <tr>
                <th
                  onClick={() => handleSort("id")}
                  aria-sort={sortField === "id" ? (sortDirection === "asc" ? "ascending" : "descending") : "none"}
                  className="px-4 py-4 cursor-pointer hover:bg-gray-100 transition"
                >
                  <div className="flex items-center gap-1">
                    <span>ID</span>
                    {sortField === "id" && <span aria-hidden="true">{sortDirection === "asc" ? "▲" : "▼"}</span>}
                  </div>
                </th>
                <th className="px-6 py-4">Student</th>
                <th className="px-6 py-4">Mentor</th>
                <th className="px-4 py-4">Topic / Goal</th>
                <th
                  onClick={() => handleSort("status")}
                  aria-sort={sortField === "status" ? (sortDirection === "asc" ? "ascending" : "descending") : "none"}
                  className="px-4 py-4 cursor-pointer hover:bg-gray-100 transition"
                >
                  <div className="flex items-center gap-1">
                    <span>Status</span>
                    {sortField === "status" && <span aria-hidden="true">{sortDirection === "asc" ? "▲" : "▼"}</span>}
                  </div>
                </th>
                <th
                  onClick={() => handleSort("createdAt")}
                  aria-sort={sortField === "createdAt" ? (sortDirection === "asc" ? "ascending" : "descending") : "none"}
                  className="px-4 py-4 cursor-pointer hover:bg-gray-100 transition"
                >
                  <div className="flex items-center gap-1">
                    <span>Requested</span>
                    {sortField === "createdAt" && <span aria-hidden="true">{sortDirection === "asc" ? "▲" : "▼"}</span>}
                  </div>
                </th>
                <th
                  onClick={() => handleSort("updatedAt")}
                  aria-sort={sortField === "updatedAt" ? (sortDirection === "asc" ? "ascending" : "descending") : "none"}
                  className="px-4 py-4 cursor-pointer hover:bg-gray-100 transition"
                >
                  <div className="flex items-center gap-1">
                    <span>Last Activity</span>
                    {sortField === "updatedAt" && <span aria-hidden="true">{sortDirection === "asc" ? "▲" : "▼"}</span>}
                  </div>
                </th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="px-6 py-12 text-center text-gray-400">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <div className="w-6 h-6 border-2 border-black border-t-transparent rounded-full animate-spin"></div>
                      <span>Loading mentorship requests...</span>
                    </div>
                  </td>
                </tr>
              ) : requests.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-6 py-12 text-center text-gray-400">
                    No mentorship records found matching criteria.
                  </td>
                </tr>
              ) : (
                requests.map((r) => {
                  const canTerminate = r.status === "ACCEPTED" || r.status === "PENDING";
                  return (
                    <tr key={r.id} className="hover:bg-gray-50/60 transition">
                      <td className="px-4 py-4 font-bold text-gray-400">#{r.id}</td>
                      <td className="px-6 py-4">
                        <div>
                          <p className="font-bold text-gray-900">{r.studentName}</p>
                          <p className="text-[11px] text-gray-400">{r.studentEmail}</p>
                          {r.studentCareerGoal && (
                            <span className="inline-block px-1.5 py-0.5 rounded text-[9px] font-semibold bg-gray-100 text-gray-600 mt-0.5">
                              {r.studentCareerGoal}
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div>
                          <p className="font-bold text-gray-900">{r.mentorName}</p>
                          <p className="text-[11px] text-gray-500">
                            {r.mentorJobTitle} at {r.mentorCompany}
                          </p>
                        </div>
                      </td>
                      <td className="px-4 py-4 max-w-xs">
                        <p className="text-gray-700 truncate font-medium">
                          {r.goalDescription}
                        </p>
                      </td>
                      <td className="px-4 py-4">
                        <span
                          className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-extrabold border ${getStatusBadge(
                            r.status
                          )}`}
                        >
                          {r.status}
                        </span>
                      </td>
                      <td className="px-4 py-4 text-gray-500 font-medium">
                        {formatDate(r.createdAt)}
                      </td>
                      <td className="px-4 py-4 text-gray-500 font-medium">
                        {formatDateTime(r.lastActivityAt)}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => handleInspect(r.id)}
                            className="px-2.5 py-1 rounded-lg bg-gray-100 hover:bg-gray-900 hover:text-white font-bold text-[11px] text-gray-800 transition cursor-pointer"
                          >
                            Inspect 🔍
                          </button>
                          {canTerminate && (
                            <button
                              onClick={() => handleOpenTerminate(r)}
                              className="px-2.5 py-1 rounded-lg bg-red-50 hover:bg-red-600 hover:text-white text-red-700 border border-red-200 font-bold text-[11px] transition cursor-pointer"
                              title="Administrative termination"
                            >
                              Terminate
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Controls */}
        <div className="px-6 py-4 bg-gray-50/80 border-t border-gray-200 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-gray-600">
          <div className="flex items-center gap-3">
            <span>
              Showing <strong className="text-gray-900">{startRecord}</strong> to{" "}
              <strong className="text-gray-900">{endRecord}</strong> of{" "}
              <strong className="text-gray-900">{totalElements}</strong> mentorship requests
            </span>

            <div className="flex items-center gap-1.5 ml-2">
              <span className="text-gray-400">Rows per page:</span>
              <select
                value={size}
                onChange={(e) => {
                  setSize(Number(e.target.value));
                  setPage(0);
                }}
                className="px-2 py-1 rounded-lg border border-gray-200 bg-white font-semibold text-gray-700 focus:outline-none"
              >
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
              </select>
            </div>
          </div>

          <div className="flex items-center gap-1">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0 || isLoading}
              className={`px-3 py-1.5 rounded-lg font-bold transition ${
                page === 0 || isLoading
                  ? "bg-gray-100 text-gray-400 cursor-not-allowed"
                  : "bg-white border border-gray-200 text-gray-700 hover:bg-gray-100 cursor-pointer"
              }`}
            >
              ← Previous
            </button>

            <span className="px-3 py-1.5 text-xs font-bold text-gray-800">
              Page {page + 1} of {Math.max(1, totalPages)}
            </span>

            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1 || isLoading}
              className={`px-3 py-1.5 rounded-lg font-bold transition ${
                page >= totalPages - 1 || isLoading
                  ? "bg-gray-100 text-gray-400 cursor-not-allowed"
                  : "bg-white border border-gray-200 text-gray-700 hover:bg-gray-100 cursor-pointer"
              }`}
            >
              Next →
            </button>
          </div>
        </div>
      </div>

      {/* Inspect Modal */}
      {selectedRequest && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="mentorship-detail-title"
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] overflow-y-auto p-6 sm:p-8 shadow-2xl border border-gray-200 space-y-6">
            <div className="flex items-center justify-between pb-4 border-b border-gray-100">
              <div>
                <div className="flex items-center gap-2">
                  <h2 id="mentorship-detail-title" className="text-lg font-bold text-gray-900">
                    Mentorship Request #{selectedRequest.id}
                  </h2>
                  <span
                    className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold border ${getStatusBadge(
                      selectedRequest.status
                    )}`}
                  >
                    {selectedRequest.status}
                  </span>
                </div>
                <p className="text-xs text-gray-500">
                  Requested on {formatDate(selectedRequest.createdAt)} • Last updated{" "}
                  {formatDateTime(selectedRequest.updatedAt)}
                </p>
              </div>

              <button
                onClick={() => setSelectedRequest(null)}
                aria-label="Close mentorship details modal"
                className="w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-600 flex items-center justify-center font-bold text-sm cursor-pointer focus:outline-none focus:ring-2 focus:ring-black"
              >
                ✕
              </button>
            </div>

            {/* Participants Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Student Details */}
              <div className="p-4 rounded-2xl bg-gray-50 border border-gray-200 space-y-2">
                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">
                  Student Participant
                </span>
                <div>
                  <p className="text-sm font-bold text-gray-900">{selectedRequest.studentName}</p>
                  <p className="text-xs text-gray-500">{selectedRequest.studentEmail}</p>
                </div>
                <div className="text-xs text-gray-600 space-y-1 pt-1 border-t border-gray-200/60">
                  <p>
                    <strong>Target Career:</strong> {selectedRequest.studentCareerGoal || "Not specified"}
                  </p>
                  <p>
                    <strong>Experience Level:</strong> {selectedRequest.studentUserLevel || "Beginner"}
                  </p>
                  <div>
                    <strong>Skills:</strong>{" "}
                    {selectedRequest.studentSkills.length > 0 ? (
                      <span className="text-gray-700">{selectedRequest.studentSkills.join(", ")}</span>
                    ) : (
                      <span className="text-gray-400">None declared</span>
                    )}
                  </div>
                </div>
              </div>

              {/* Mentor Details */}
              <div className="p-4 rounded-2xl bg-gray-50 border border-gray-200 space-y-2">
                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">
                  Assigned Mentor
                </span>
                <div>
                  <p className="text-sm font-bold text-gray-900">{selectedRequest.mentorName}</p>
                  <p className="text-xs text-gray-500">{selectedRequest.mentorEmail}</p>
                </div>
                <div className="text-xs text-gray-600 space-y-1 pt-1 border-t border-gray-200/60">
                  <p>
                    <strong>Position:</strong> {selectedRequest.mentorJobTitle} at{" "}
                    {selectedRequest.mentorCompany}
                  </p>
                  <p>
                    <strong>Experience:</strong> {selectedRequest.mentorYearsOfExperience ?? "N/A"} years
                  </p>
                  <p>
                    <strong>Capacity:</strong> {selectedRequest.mentorActiveMenteesCount} /{" "}
                    {selectedRequest.mentorMaxMentees} active mentees
                  </p>
                </div>
              </div>
            </div>

            {/* Goal Description */}
            <div className="p-4 rounded-2xl bg-gray-50 border border-gray-200 space-y-1">
              <span className="text-[10px] font-bold text-gray-500 uppercase tracking-wider">
                Mentorship Goal & Context
              </span>
              <p className="text-xs text-gray-800 leading-relaxed font-medium">
                {selectedRequest.goalDescription}
              </p>
            </div>

            {/* Conversation Metadata */}
            <div className="p-4 rounded-2xl bg-purple-50/50 border border-purple-200/80 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold text-purple-700 uppercase tracking-wider">
                  💬 Direct Communication Channel
                </span>
                {selectedRequest.conversationId && (
                  <Link
                    href={`/messages?conversationId=${selectedRequest.conversationId}`}
                    className="text-xs font-bold text-purple-700 hover:text-purple-900 underline"
                  >
                    Open Messages ↗
                  </Link>
                )}
              </div>
              {selectedRequest.conversationId ? (
                <div className="text-xs text-purple-900 space-y-1">
                  <p>
                    <strong>Conversation ID:</strong> #{selectedRequest.conversationId}
                  </p>
                  <p>
                    <strong>Total Exchanged Messages:</strong> {selectedRequest.messageCount}
                  </p>
                  <p>
                    <strong>Last Activity Recorded:</strong> {formatDateTime(selectedRequest.lastActivityAt)}
                  </p>
                </div>
              ) : (
                <p className="text-xs text-gray-500">
                  No conversation created yet. (A direct chat channel is initialized when the mentor accepts the request).
                </p>
              )}
            </div>

            {/* Lifecycle Notes */}
            {selectedRequest.responseNote && (
              <div className="p-4 rounded-2xl bg-amber-50/70 border border-amber-200 text-xs text-amber-950 space-y-1">
                <p className="font-bold">Lifecycle & Audit Notes:</p>
                <p className="font-mono text-[11px] leading-relaxed">{selectedRequest.responseNote}</p>
              </div>
            )}

            <div className="pt-2 border-t border-gray-100 flex items-center justify-between">
              {(selectedRequest.status === "ACCEPTED" || selectedRequest.status === "PENDING") && (
                <button
                  onClick={() => {
                    handleOpenTerminate(selectedRequest as any);
                    setSelectedRequest(null);
                  }}
                  className="px-4 py-2 rounded-xl bg-red-50 hover:bg-red-600 hover:text-white text-red-700 border border-red-200 font-bold text-xs transition cursor-pointer"
                >
                  Terminate Mentorship
                </button>
              )}
              <div className="ml-auto">
                <button
                  onClick={() => setSelectedRequest(null)}
                  className="px-5 py-2 rounded-xl bg-black text-white font-bold text-xs hover:bg-gray-800 transition cursor-pointer"
                >
                  Close Inspection
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Terminate Modal */}
      {terminateTarget && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="terminate-mentorship-title"
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl border border-gray-200 space-y-5 animate-in fade-in duration-200">
            <div className="flex items-start justify-between pb-3 border-b border-gray-100">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-red-100 text-red-600 flex items-center justify-center font-bold text-lg" aria-hidden="true">
                  ⚠️
                </div>
                <div>
                  <h2 id="terminate-mentorship-title" className="text-lg font-bold text-gray-900">Administrative Termination</h2>
                  <p className="text-xs text-gray-500">
                    Terminate Mentorship Request #{terminateTarget.id}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setTerminateTarget(null)}
                aria-label="Close termination modal"
                className="w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-600 flex items-center justify-center font-bold text-sm cursor-pointer focus:outline-none focus:ring-2 focus:ring-black"
              >
                ✕
              </button>
            </div>

            <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-900 space-y-1">
              <p className="font-bold">Intervention Safeguards:</p>
              <p>
                Terminating this mentorship transitions the record to <code>TERMINATED</code>, frees the mentor&apos;s allocated capacity slot, and permanently preserves historical messages and conversation context.
              </p>
            </div>

            {terminateError && (
              <div className="p-3 rounded-xl bg-red-50 border border-red-200 text-xs text-red-700 font-medium">
                ⚠️ {terminateError}
              </div>
            )}

            <div>
              <label htmlFor="termination-reason-input" className="block text-xs font-bold text-gray-700 mb-1.5">
                Termination Reason <span className="text-red-500">*</span>
              </label>
              <textarea
                id="termination-reason-input"
                value={terminationReason}
                onChange={(e) => setTerminationReason(e.target.value)}
                placeholder="Specify the administrative intervention reason (mandatory)..."
                rows={3}
                className="w-full px-3.5 py-2.5 rounded-xl border border-gray-200 text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-red-500"
              />
            </div>

            <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-100">
              <button
                type="button"
                onClick={() => setTerminateTarget(null)}
                disabled={isSubmittingTerminate}
                className="px-4 py-2 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold text-xs cursor-pointer focus:outline-none focus:ring-2 focus:ring-gray-400"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmTerminate}
                disabled={isSubmittingTerminate}
                className="px-5 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs transition cursor-pointer flex items-center gap-2 focus:outline-none focus:ring-2 focus:ring-red-500"
              >
                {isSubmittingTerminate ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Terminating...</span>
                  </>
                ) : (
                  <span>Confirm Termination</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
