"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import {
  getAdminUsers,
  getAdminUserDetail,
  updateUserStatus,
  AdminUser,
  AdminUserDetail,
  AdminUserQueryParams,
} from "@/lib/adminService";

export default function AdminUsersPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();

  const [users, setUsers] = useState<AdminUser[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [sortField, setSortField] = useState<string>("createdAt");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");

  const [searchInput, setSearchInput] = useState("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<"ALL" | "USER" | "ADMIN">("ALL");
  const [statusFilter, setStatusFilter] = useState<"ALL" | "ACTIVE" | "SUSPENDED" | "DEACTIVATED">("ALL");

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; message: string } | null>(null);

  // Modals state
  const [selectedUser, setSelectedUser] = useState<AdminUserDetail | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);

  const [suspendTarget, setSuspendTarget] = useState<AdminUser | null>(null);
  const [suspensionReason, setSuspensionReason] = useState("");
  const [suspendError, setSuspendError] = useState<string | null>(null);

  const [reactivateTarget, setReactivateTarget] = useState<AdminUser | null>(null);
  const [isSubmittingStatus, setIsSubmittingStatus] = useState(false);

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

  const loadUsers = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const params: AdminUserQueryParams = {
        page,
        size,
        sort: `${sortField},${sortDirection}`,
        search: appliedSearch,
        role: roleFilter,
        status: statusFilter,
      };
      const response = await getAdminUsers(params);
      setUsers(response.content || []);
      setTotalPages(response.totalPages || 0);
      setTotalElements(response.totalElements || 0);
    } catch (err: any) {
      console.error("Failed to load users:", err);
      setError(err.message || "Failed to load candidate accounts.");
    } finally {
      setIsLoading(false);
    }
  }, [page, size, sortField, sortDirection, appliedSearch, roleFilter, statusFilter]);

  useEffect(() => {
    if (user?.role === "ADMIN") {
      loadUsers();
    }
  }, [user, loadUsers]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    setAppliedSearch(searchInput.trim());
  };

  const handleClearSearch = () => {
    setSearchInput("");
    setAppliedSearch("");
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

  const handleInspectUser = async (id: number) => {
    setIsLoadingDetail(true);
    try {
      const detail = await getAdminUserDetail(id);
      setSelectedUser(detail);
    } catch (err: any) {
      alert("Failed to load user details: " + err.message);
    } finally {
      setIsLoadingDetail(false);
    }
  };

  const handleOpenSuspendModal = (target: AdminUser) => {
    setSuspendTarget(target);
    setSuspensionReason("");
    setSuspendError(null);
  };

  const handleConfirmSuspend = async () => {
    if (!suspendTarget) return;
    if (!suspensionReason.trim()) {
      setSuspendError("Please specify a reason for suspending this account.");
      return;
    }

    setIsSubmittingStatus(true);
    setSuspendError(null);
    try {
      await updateUserStatus(suspendTarget.id, {
        status: "SUSPENDED",
        reason: suspensionReason.trim(),
      });
      setFeedback({
        type: "success",
        message: `Account for ${suspendTarget.name} has been suspended successfully. Active sessions are revoked immediately.`,
      });
      setSuspendTarget(null);
      loadUsers();
    } catch (err: any) {
      console.error("Failed to suspend account:", err);
      setSuspendError(err.message || "Failed to suspend account.");
    } finally {
      setIsSubmittingStatus(false);
    }
  };

  const handleConfirmReactivate = async () => {
    if (!reactivateTarget) return;

    setIsSubmittingStatus(true);
    try {
      await updateUserStatus(reactivateTarget.id, {
        status: "ACTIVE",
      });
      setFeedback({
        type: "success",
        message: `Account for ${reactivateTarget.name} has been reactivated successfully.`,
      });
      setReactivateTarget(null);
      loadUsers();
    } catch (err: any) {
      console.error("Failed to reactivate account:", err);
      setFeedback({
        type: "error",
        message: err.message || "Failed to reactivate account.",
      });
      setReactivateTarget(null);
    } finally {
      setIsSubmittingStatus(false);
    }
  };

  const formatDate = (isoString?: string) => {
    if (!isoString) return "N/A";
    try {
      const date = new Date(isoString);
      return date.toLocaleDateString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
      });
    } catch {
      return isoString;
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
            <span>User Governance</span>
          </div>
          <h1 className="text-2xl font-extrabold text-gray-900 tracking-tight">
            User Governance & Candidates
          </h1>
          <p className="text-xs text-gray-500">
            Inspect platform user profiles, status enforcement, verified skills, and practice metrics.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Link
            href="/admin"
            className="px-4 py-2 rounded-xl bg-gray-100 text-gray-700 hover:bg-gray-200 font-semibold text-xs transition"
          >
            ← Back to Overview
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
            className="text-xs font-bold px-2 py-1 rounded-lg hover:bg-black/5 cursor-pointer"
          >
            ✕
          </button>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="bg-white rounded-2xl p-4 border border-gray-200 shadow-xs flex flex-col lg:flex-row gap-4 justify-between items-stretch lg:items-center">
        {/* Search Input */}
        <form onSubmit={handleSearchSubmit} className="flex-1 flex gap-2">
          <div className="relative flex-1">
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search candidate name or email..."
              className="w-full pl-9 pr-8 py-2 rounded-xl border border-gray-200 text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-black"
            />
            <span className="absolute left-3 top-2.5 text-gray-400 text-xs">🔍</span>
            {searchInput && (
              <button
                type="button"
                onClick={handleClearSearch}
                className="absolute right-2.5 top-2 text-gray-400 hover:text-gray-700 text-xs cursor-pointer font-bold"
              >
                ✕
              </button>
            )}
          </div>
          <button
            type="submit"
            className="px-4 py-2 rounded-xl bg-black text-white font-semibold text-xs hover:bg-gray-800 transition cursor-pointer"
          >
            Search
          </button>
        </form>

        {/* Filter Controls */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Role Filter */}
          <div className="flex items-center gap-1.5 bg-gray-50 p-1 rounded-xl border border-gray-100">
            <span className="text-[11px] font-bold text-gray-500 pl-2">Role:</span>
            {(["ALL", "USER", "ADMIN"] as const).map((rf) => (
              <button
                key={rf}
                onClick={() => {
                  setRoleFilter(rf);
                  setPage(0);
                }}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition cursor-pointer ${
                  roleFilter === rf
                    ? "bg-purple-600 text-white shadow-xs"
                    : "text-gray-600 hover:bg-gray-200"
                }`}
              >
                {rf}
              </button>
            ))}
          </div>

          {/* Status Filter */}
          <div className="flex items-center gap-1.5 bg-gray-50 p-1 rounded-xl border border-gray-100">
            <span className="text-[11px] font-bold text-gray-500 pl-2">Status:</span>
            {(["ALL", "ACTIVE", "SUSPENDED", "DEACTIVATED"] as const).map((sf) => (
              <button
                key={sf}
                onClick={() => {
                  setStatusFilter(sf);
                  setPage(0);
                }}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition cursor-pointer ${
                  statusFilter === sf
                    ? sf === "SUSPENDED"
                      ? "bg-red-600 text-white shadow-xs"
                      : sf === "ACTIVE"
                      ? "bg-emerald-600 text-white shadow-xs"
                      : "bg-gray-800 text-white shadow-xs"
                    : "text-gray-600 hover:bg-gray-200"
                }`}
              >
                {sf}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-sm">
          ⚠️ {error}
        </div>
      )}

      {/* User Table */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-gray-50/80 border-b border-gray-200 text-gray-500 font-bold uppercase tracking-wider select-none">
              <tr>
                <th
                  onClick={() => handleSort("name")}
                  className="px-6 py-4 cursor-pointer hover:bg-gray-100 transition"
                >
                  <div className="flex items-center gap-1">
                    <span>User</span>
                    {sortField === "name" && (
                      <span>{sortDirection === "asc" ? "▲" : "▼"}</span>
                    )}
                  </div>
                </th>
                <th
                  onClick={() => handleSort("role")}
                  className="px-4 py-4 cursor-pointer hover:bg-gray-100 transition"
                >
                  <div className="flex items-center gap-1">
                    <span>Role</span>
                    {sortField === "role" && (
                      <span>{sortDirection === "asc" ? "▲" : "▼"}</span>
                    )}
                  </div>
                </th>
                <th
                  onClick={() => handleSort("status")}
                  className="px-4 py-4 cursor-pointer hover:bg-gray-100 transition"
                >
                  <div className="flex items-center gap-1">
                    <span>Status</span>
                    {sortField === "status" && (
                      <span>{sortDirection === "asc" ? "▲" : "▼"}</span>
                    )}
                  </div>
                </th>
                <th className="px-4 py-4">Target Career</th>
                <th className="px-4 py-4">Skills</th>
                <th className="px-4 py-4">Resume</th>
                <th className="px-4 py-4">Interviews</th>
                <th className="px-4 py-4">DSA Solved</th>
                <th
                  onClick={() => handleSort("createdAt")}
                  className="px-4 py-4 cursor-pointer hover:bg-gray-100 transition"
                >
                  <div className="flex items-center gap-1">
                    <span>Joined</span>
                    {sortField === "createdAt" && (
                      <span>{sortDirection === "asc" ? "▲" : "▼"}</span>
                    )}
                  </div>
                </th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {isLoading ? (
                <tr>
                  <td colSpan={10} className="px-6 py-12 text-center text-gray-400">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <div className="w-6 h-6 border-2 border-black border-t-transparent rounded-full animate-spin"></div>
                      <span>Loading candidate directory...</span>
                    </div>
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={10} className="px-6 py-12 text-center text-gray-400">
                    No candidates found matching your criteria.
                  </td>
                </tr>
              ) : (
                users.map((u) => {
                  const isAdmin = u.role === "ADMIN";
                  const isCurrentUser = user?.id === u.id;
                  const isSuspended = u.status === "SUSPENDED";
                  const isDeactivated = u.status === "DEACTIVATED";

                  return (
                    <tr
                      key={u.id}
                      className={`hover:bg-gray-50/60 transition ${
                        isSuspended ? "bg-red-50/20" : ""
                      }`}
                    >
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div
                            className={`w-8 h-8 rounded-lg flex items-center justify-center font-bold text-xs text-white ${
                              isAdmin
                                ? "bg-purple-600 shadow-xs"
                                : isSuspended
                                ? "bg-red-500"
                                : "bg-black"
                            }`}
                          >
                            {u.name ? u.name.charAt(0).toUpperCase() : "U"}
                          </div>
                          <div>
                            <div className="flex items-center gap-1.5">
                              <p className="font-bold text-gray-900">{u.name}</p>
                              {isCurrentUser && (
                                <span className="px-1.5 py-0.2 rounded text-[9px] font-bold bg-gray-200 text-gray-700">
                                  You
                                </span>
                              )}
                            </div>
                            <p className="text-gray-400 text-[11px]">{u.email}</p>
                          </div>
                        </div>
                      </td>

                      {/* Role */}
                      <td className="px-4 py-4">
                        <span
                          className={`px-2.5 py-1 rounded-full text-[10px] font-extrabold ${
                            isAdmin
                              ? "bg-purple-100 text-purple-700 border border-purple-200"
                              : "bg-blue-50 text-blue-700 border border-blue-200"
                          }`}
                        >
                          {u.role}
                        </span>
                      </td>

                      {/* Status */}
                      <td className="px-4 py-4">
                        {isSuspended ? (
                          <span
                            title={u.suspensionReason || "Account Suspended"}
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-red-100 text-red-700 border border-red-200"
                          >
                            <span className="w-1.5 h-1.5 rounded-full bg-red-600"></span>
                            SUSPENDED
                          </span>
                        ) : isDeactivated ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-gray-100 text-gray-600 border border-gray-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-gray-400"></span>
                            DEACTIVATED
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                            ACTIVE
                          </span>
                        )}
                      </td>

                      {/* Career Goal */}
                      <td className="px-4 py-4">
                        <span className="font-medium text-gray-800">
                          {u.careerGoal || "None"}
                        </span>
                      </td>

                      {/* Skills */}
                      <td className="px-4 py-4">
                        <span className="font-semibold text-gray-700">
                          {u.skillCount} skill{u.skillCount === 1 ? "" : "s"}
                        </span>
                      </td>

                      {/* Resume */}
                      <td className="px-4 py-4">
                        {u.resumePresent ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-700 border border-emerald-200 font-bold text-[11px]">
                            <span>📄</span> Uploaded
                          </span>
                        ) : (
                          <span className="text-gray-400 font-normal">None</span>
                        )}
                      </td>

                      {/* Interviews */}
                      <td className="px-4 py-4">
                        <span className="font-semibold text-gray-700">
                          {u.mockInterviewCount}
                        </span>
                      </td>

                      {/* DSA Solved */}
                      <td className="px-4 py-4">
                        <span className="font-semibold text-gray-700">
                          {u.solvedProblemsCount}
                        </span>
                      </td>

                      {/* Joined Date */}
                      <td className="px-4 py-4 text-gray-500 font-medium">
                        {formatDate(u.createdAt)}
                      </td>

                      {/* Actions */}
                      <td className="px-6 py-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => handleInspectUser(u.id)}
                            className="px-2.5 py-1 rounded-lg bg-gray-100 hover:bg-gray-900 hover:text-white font-bold text-[11px] text-gray-800 transition cursor-pointer"
                            title="Inspect user profile"
                          >
                            Inspect 🔍
                          </button>

                          {isSuspended ? (
                            <button
                              onClick={() => setReactivateTarget(u)}
                              disabled={isCurrentUser}
                              className={`px-2.5 py-1 rounded-lg font-bold text-[11px] transition ${
                                isCurrentUser
                                  ? "bg-gray-100 text-gray-400 cursor-not-allowed"
                                  : "bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-600 hover:text-white cursor-pointer"
                              }`}
                              title={
                                isCurrentUser
                                  ? "Cannot modify own account status"
                                  : "Reactivate account"
                              }
                            >
                              Reactivate
                            </button>
                          ) : (
                            <button
                              onClick={() => handleOpenSuspendModal(u)}
                              disabled={isCurrentUser}
                              className={`px-2.5 py-1 rounded-lg font-bold text-[11px] transition ${
                                isCurrentUser
                                  ? "bg-gray-100 text-gray-400 cursor-not-allowed"
                                  : "bg-red-50 text-red-700 border border-red-200 hover:bg-red-600 hover:text-white cursor-pointer"
                              }`}
                              title={
                                isCurrentUser
                                  ? "Cannot suspend your own admin account"
                                  : "Suspend account"
                              }
                            >
                              Suspend
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
              <strong className="text-gray-900">{totalElements}</strong> candidates
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

      {/* Suspend Confirmation Modal */}
      {suspendTarget && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl border border-gray-200 space-y-5 animate-in fade-in duration-200">
            <div className="flex items-start justify-between pb-3 border-b border-gray-100">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-red-100 text-red-600 flex items-center justify-center font-bold text-lg">
                  ⚠️
                </div>
                <div>
                  <h2 className="text-lg font-bold text-gray-900">Suspend Account</h2>
                  <p className="text-xs text-gray-500">
                    Immediately revoke platform access for {suspendTarget.name}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setSuspendTarget(null)}
                className="w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-600 flex items-center justify-center font-bold text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-900 space-y-1">
              <p className="font-bold">Important Notice:</p>
              <p>
                Suspending an account terminates ongoing sessions immediately via token validation and blocks any future login attempts.
              </p>
            </div>

            {suspendError && (
              <div className="p-3 rounded-xl bg-red-50 border border-red-200 text-xs text-red-700 font-medium">
                ⚠️ {suspendError}
              </div>
            )}

            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1.5">
                Reason for Suspension <span className="text-red-500">*</span>
              </label>
              <textarea
                value={suspensionReason}
                onChange={(e) => setSuspensionReason(e.target.value)}
                placeholder="Specify the policy violation or rationale (mandatory)..."
                rows={3}
                className="w-full px-3.5 py-2.5 rounded-xl border border-gray-200 text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-red-500"
              />
            </div>

            <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-100">
              <button
                type="button"
                onClick={() => setSuspendTarget(null)}
                disabled={isSubmittingStatus}
                className="px-4 py-2 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold text-xs cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmSuspend}
                disabled={isSubmittingStatus}
                className="px-5 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs transition cursor-pointer flex items-center gap-2"
              >
                {isSubmittingStatus ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Suspending...</span>
                  </>
                ) : (
                  <span>Confirm Suspension</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reactivate Confirmation Modal */}
      {reactivateTarget && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl border border-gray-200 space-y-5 animate-in fade-in duration-200">
            <div className="flex items-start justify-between pb-3 border-b border-gray-100">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center font-bold text-lg">
                  ✓
                </div>
                <div>
                  <h2 className="text-lg font-bold text-gray-900">Reactivate Account</h2>
                  <p className="text-xs text-gray-500">
                    Restore access for {reactivateTarget.name}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setReactivateTarget(null)}
                className="w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-600 flex items-center justify-center font-bold text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            <p className="text-xs text-gray-600">
              Are you sure you want to reactivate this account? The user will be permitted to log in and use platform features again.
            </p>

            <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-100">
              <button
                type="button"
                onClick={() => setReactivateTarget(null)}
                disabled={isSubmittingStatus}
                className="px-4 py-2 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold text-xs cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmReactivate}
                disabled={isSubmittingStatus}
                className="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs transition cursor-pointer flex items-center gap-2"
              >
                {isSubmittingStatus ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Reactivating...</span>
                  </>
                ) : (
                  <span>Confirm Reactivate</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* User Details Modal */}
      {selectedUser && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] overflow-y-auto p-6 sm:p-8 shadow-2xl border border-gray-200 space-y-6">
            <div className="flex items-center justify-between pb-4 border-b border-gray-100">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-purple-600 text-white flex items-center justify-center font-bold text-base shadow-sm">
                  {selectedUser.name ? selectedUser.name.charAt(0).toUpperCase() : "U"}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-lg font-bold text-gray-900">{selectedUser.name}</h2>
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-purple-100 text-purple-700 border border-purple-200">
                      {selectedUser.role}
                    </span>
                    {selectedUser.status === "SUSPENDED" ? (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-red-100 text-red-700 border border-red-200">
                        SUSPENDED
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-100 text-emerald-700 border border-emerald-200">
                        {selectedUser.status || "ACTIVE"}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-gray-500">{selectedUser.email}</p>
                </div>
              </div>

              <button
                onClick={() => setSelectedUser(null)}
                className="w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-600 flex items-center justify-center font-bold text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            {/* Suspension Banner if Suspended */}
            {selectedUser.status === "SUSPENDED" && (
              <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-900 space-y-1">
                <div className="flex items-center justify-between text-xs font-bold">
                  <span>⚠️ Account Suspended</span>
                  <span className="text-[11px] font-normal text-red-700">
                    Suspended At: {formatDate(selectedUser.suspendedAt)}
                  </span>
                </div>
                <p className="text-xs">
                  <strong>Reason:</strong> {selectedUser.suspensionReason || "No specific reason provided."}
                </p>
              </div>
            )}

            {/* Profile KPI Matrix */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
              <div className="bg-gray-50 rounded-2xl p-3 border border-gray-100">
                <p className="text-[10px] font-bold text-gray-400 uppercase">Target Career</p>
                <p className="text-xs font-bold text-gray-900 mt-1 truncate">
                  {selectedUser.careerGoal || "Not Set"}
                </p>
              </div>
              <div className="bg-gray-50 rounded-2xl p-3 border border-gray-100">
                <p className="text-[10px] font-bold text-gray-400 uppercase">Experience Level</p>
                <p className="text-xs font-bold text-gray-900 mt-1">
                  {selectedUser.userLevel || "Beginner"}
                </p>
              </div>
              <div className="bg-gray-50 rounded-2xl p-3 border border-gray-100">
                <p className="text-[10px] font-bold text-gray-400 uppercase">Roadmap Steps</p>
                <p className="text-xs font-bold text-gray-900 mt-1">
                  {selectedUser.completedRoadmapStepsCount} Done
                </p>
              </div>
              <div className="bg-gray-50 rounded-2xl p-3 border border-gray-100">
                <p className="text-[10px] font-bold text-gray-400 uppercase">DSA Solved</p>
                <p className="text-xs font-bold text-gray-900 mt-1">
                  {selectedUser.solvedProblemsCount} Problems
                </p>
              </div>
            </div>

            {/* Resume Details */}
            <div className="p-4 rounded-2xl bg-gray-50 border border-gray-200">
              <h3 className="text-xs font-bold text-gray-700 mb-2 flex items-center gap-1.5">
                <span>📄</span> Resume Intelligence Status
              </h3>
              {selectedUser.resumePresent ? (
                <div className="text-xs space-y-1 text-gray-600">
                  <p>
                    <span className="font-semibold text-gray-800">Original File:</span>{" "}
                    {selectedUser.resumeFileName}
                  </p>
                  <p>
                    <span className="font-semibold text-gray-800">Uploaded At:</span>{" "}
                    {selectedUser.resumeUploadTimestamp}
                  </p>
                </div>
              ) : (
                <p className="text-xs text-gray-400">No resume uploaded yet by this candidate.</p>
              )}
            </div>

            {/* Mock Interview Summary */}
            <div className="p-4 rounded-2xl bg-gray-50 border border-gray-200">
              <h3 className="text-xs font-bold text-gray-700 mb-2 flex items-center gap-1.5">
                <span>🎯</span> Mock Interview Performance
              </h3>
              <div className="flex justify-between items-center text-xs text-gray-600">
                <span>
                  Total Sessions:{" "}
                  <strong className="text-gray-900">{selectedUser.mockInterviewCount}</strong>
                </span>
                <span>
                  Average Score:{" "}
                  <strong className="text-purple-700">
                    {selectedUser.averageInterviewScore !== null && selectedUser.averageInterviewScore !== undefined
                      ? `${selectedUser.averageInterviewScore}%`
                      : "N/A"}
                  </strong>
                </span>
              </div>
            </div>

            {/* Verified Skills */}
            <div>
              <h3 className="text-xs font-bold text-gray-700 mb-2.5">
                Verified Skill Portfolio ({selectedUser.skills.length})
              </h3>
              <div className="flex flex-wrap gap-2">
                {selectedUser.skills.length > 0 ? (
                  selectedUser.skills.map((skill, idx) => (
                    <span
                      key={idx}
                      className="px-3 py-1 rounded-xl bg-blue-50 text-blue-700 border border-blue-200 text-xs font-semibold"
                    >
                      {skill}
                    </span>
                  ))
                ) : (
                  <p className="text-xs text-gray-400">No verified skills added.</p>
                )}
              </div>
            </div>

            {/* Account Metadata */}
            <div className="flex items-center justify-between text-[11px] text-gray-400 pt-2 border-t border-gray-100">
              <span>Account ID: {selectedUser.id}</span>
              <span>Member Since: {formatDate(selectedUser.createdAt)}</span>
            </div>

            <div className="pt-2 border-t border-gray-100 flex justify-end">
              <button
                onClick={() => setSelectedUser(null)}
                className="px-5 py-2 rounded-xl bg-black text-white font-bold text-xs hover:bg-gray-800 transition cursor-pointer"
              >
                Close Inspection
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
