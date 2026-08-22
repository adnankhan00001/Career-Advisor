"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import {
  getAdminUsers,
  getAdminUserDetail,
  AdminUser,
  AdminUserDetail,
} from "@/lib/adminService";

export default function AdminUsersPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();

  const [users, setUsers] = useState<AdminUser[]>([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState<"ALL" | "USER" | "ADMIN">("ALL");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedUser, setSelectedUser] = useState<AdminUserDetail | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);

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

  useEffect(() => {
    if (user?.role === "ADMIN") {
      loadUsers();
    }
  }, [user]);

  const loadUsers = async (searchQuery: string = searchTerm) => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getAdminUsers(searchQuery);
      setUsers(data);
    } catch (err: any) {
      console.error("Failed to load users:", err);
      setError(err.message || "Failed to load candidate accounts.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    loadUsers(searchTerm);
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

  const filteredUsers = users.filter((u) => {
    if (roleFilter === "ALL") return true;
    return u.role === roleFilter;
  });

  if (isAuthLoading || (user && user.role !== "ADMIN")) {
    return (
      <div className="min-h-[50vh] flex flex-col items-center justify-center text-gray-500 gap-3">
        <div className="w-8 h-8 border-4 border-black border-t-transparent rounded-full animate-spin"></div>
        <p className="text-sm font-medium">Verifying administrator authorization...</p>
      </div>
    );
  }

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
            Inspect platform user profiles, verified skills, and practice metrics.
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

      {/* Filter and Search Bar */}
      <div className="bg-white rounded-2xl p-4 border border-gray-200 shadow-xs flex flex-col md:flex-row gap-4 justify-between items-center">
        <form onSubmit={handleSearchSubmit} className="flex-1 flex gap-2 w-full md:w-auto">
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search candidates by name or email..."
            className="flex-1 px-4 py-2 rounded-xl border border-gray-200 text-sm focus:outline-none focus:ring-2 focus:ring-black"
          />
          <button
            type="submit"
            className="px-4 py-2 rounded-xl bg-black text-white font-semibold text-xs hover:bg-gray-800 transition cursor-pointer"
          >
            Search
          </button>
        </form>

        <div className="flex items-center gap-2 w-full md:w-auto">
          <span className="text-xs font-bold text-gray-500">Role:</span>
          {(["ALL", "USER", "ADMIN"] as const).map((rf) => (
            <button
              key={rf}
              onClick={() => setRoleFilter(rf)}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer ${
                roleFilter === rf
                  ? "bg-purple-600 text-white shadow-xs"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {rf}
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

      {/* User Table */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-gray-50/80 border-b border-gray-200 text-gray-500 font-bold uppercase tracking-wider">
              <tr>
                <th className="px-6 py-4">User</th>
                <th className="px-4 py-4">Role</th>
                <th className="px-4 py-4">Target Career</th>
                <th className="px-4 py-4">Skills</th>
                <th className="px-4 py-4">Resume</th>
                <th className="px-4 py-4">Interviews</th>
                <th className="px-4 py-4">DSA Solved</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="px-6 py-12 text-center text-gray-400">
                    Loading users...
                  </td>
                </tr>
              ) : filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-6 py-12 text-center text-gray-400">
                    No candidates found matching your criteria.
                  </td>
                </tr>
              ) : (
                filteredUsers.map((u) => {
                  const isAdmin = u.role === "ADMIN";
                  return (
                    <tr key={u.id} className="hover:bg-gray-50/60 transition">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div
                            className={`w-8 h-8 rounded-lg flex items-center justify-center font-bold text-xs text-white ${
                              isAdmin ? "bg-purple-600 shadow-xs" : "bg-black"
                            }`}
                          >
                            {u.name ? u.name.charAt(0).toUpperCase() : "U"}
                          </div>
                          <div>
                            <p className="font-bold text-gray-900">{u.name}</p>
                            <p className="text-gray-400 text-[11px]">{u.email}</p>
                          </div>
                        </div>
                      </td>
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
                      <td className="px-4 py-4">
                        <span className="font-medium text-gray-800">
                          {u.careerGoal || "None"}
                        </span>
                      </td>
                      <td className="px-4 py-4">
                        <span className="font-semibold text-gray-700">
                          {u.skillCount} skill{u.skillCount === 1 ? "" : "s"}
                        </span>
                      </td>
                      <td className="px-4 py-4">
                        {u.resumePresent ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-700 border border-emerald-200 font-bold text-[11px]">
                            <span>📄</span> Uploaded
                          </span>
                        ) : (
                          <span className="text-gray-400 font-normal">None</span>
                        )}
                      </td>
                      <td className="px-4 py-4">
                        <span className="font-semibold text-gray-700">
                          {u.mockInterviewCount}
                        </span>
                      </td>
                      <td className="px-4 py-4">
                        <span className="font-semibold text-gray-700">
                          {u.solvedProblemsCount}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <button
                          onClick={() => handleInspectUser(u.id)}
                          className="px-3 py-1.5 rounded-lg bg-gray-100 hover:bg-gray-900 hover:text-white font-bold text-[11px] text-gray-800 transition cursor-pointer"
                        >
                          Inspect 🔍
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

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

            <div className="pt-4 border-t border-gray-100 flex justify-end">
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
