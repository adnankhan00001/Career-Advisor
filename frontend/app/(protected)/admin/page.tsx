"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { getAdminStatsOverview, AdminStatsOverview } from "@/lib/adminService";

export default function AdminDashboardPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();

  const [stats, setStats] = useState<AdminStatsOverview | null>(null);
  const [isLoadingStats, setIsLoadingStats] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Client-side route guard
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
      loadStats();
    }
  }, [user]);

  const loadStats = async () => {
    setIsLoadingStats(true);
    setError(null);
    try {
      const data = await getAdminStatsOverview();
      setStats(data);
    } catch (err: any) {
      console.error("Failed to load admin stats:", err);
      setError(err.message || "Failed to load platform statistics.");
    } finally {
      setIsLoadingStats(false);
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

  const kpis = [
    {
      title: "Registered Users",
      value: stats?.totalUsers ?? 0,
      icon: "👥",
      color: "bg-blue-500/10 text-blue-600 border-blue-200",
      desc: "Total platform candidates",
    },
    {
      title: "Resumes Analyzed",
      value: stats?.totalResumes ?? 0,
      icon: "📄",
      color: "bg-emerald-500/10 text-emerald-600 border-emerald-200",
      desc: "Parsed PDF & DOCX resumes",
    },
    {
      title: "Quiz Assessments",
      value: stats?.totalQuizAttempts ?? 0,
      icon: "📝",
      color: "bg-amber-500/10 text-amber-600 border-amber-200",
      desc: "Skill calibration tests",
    },
    {
      title: "DSA Problems Solved",
      value: stats?.totalSolvedProblems ?? 0,
      icon: "💡",
      color: "bg-purple-500/10 text-purple-600 border-purple-200",
      desc: "Accepted code submissions",
    },
    {
      title: "Mock Interviews",
      value: stats?.totalMockInterviews ?? 0,
      icon: "🎯",
      color: "bg-rose-500/10 text-rose-600 border-rose-200",
      desc: "Initialized interview rounds",
    },
    {
      title: "Completed Interviews",
      value: stats?.totalCompletedInterviews ?? 0,
      icon: "✅",
      desc: "Submitted & evaluated rounds",
      color: "bg-indigo-500/10 text-indigo-600 border-indigo-200",
    },
  ];

  return (
    <div className="space-y-8">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-gray-900 via-purple-950 to-gray-900 text-white rounded-3xl p-6 sm:p-8 shadow-xl relative overflow-hidden">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-500/20 border border-purple-400/30 text-purple-300 text-xs font-bold uppercase tracking-wider">
              <span>🛡️ Platform Governance</span>
              <span>•</span>
              <span>Phase 12 RBAC</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
              Administrative Control Center
            </h1>
            <p className="text-gray-300 text-sm max-w-2xl">
              System governance, live platform telemetry, and user management for the OneStop Career Advisor ecosystem.
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <Link
              href="/admin/users"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-white text-gray-900 font-bold text-sm hover:bg-gray-100 shadow-md transition cursor-pointer"
            >
              <span>Manage Users</span>
              <span>👥</span>
            </Link>
            <button
              onClick={loadStats}
              disabled={isLoadingStats}
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white/10 text-white font-semibold text-sm hover:bg-white/20 border border-white/20 transition cursor-pointer"
            >
              <span>Refresh Stats</span>
              <span className={isLoadingStats ? "animate-spin" : ""}>🔄</span>
            </button>
          </div>
        </div>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-sm flex items-center justify-between">
          <p className="font-medium">⚠️ {error}</p>
          <button
            onClick={loadStats}
            className="text-xs font-bold underline hover:text-red-900 cursor-pointer"
          >
            Retry
          </button>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {kpis.map((kpi, idx) => (
          <div
            key={idx}
            className="bg-white rounded-2xl p-6 border border-gray-200 shadow-xs flex flex-col justify-between hover:border-gray-300 transition"
          >
            <div className="flex items-center justify-between mb-4">
              <span className="text-xs font-bold uppercase tracking-wider text-gray-500">
                {kpi.title}
              </span>
              <span
                className={`w-10 h-10 rounded-xl flex items-center justify-center text-lg border ${kpi.color}`}
              >
                {kpi.icon}
              </span>
            </div>
            <div>
              <p className="text-3xl font-extrabold text-gray-900 tracking-tight">
                {isLoadingStats ? "..." : kpi.value.toLocaleString()}
              </p>
              <p className="text-xs text-gray-500 mt-1">{kpi.desc}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Analytics Breakdown Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Career Goals Distribution */}
        <div className="bg-white rounded-2xl p-6 border border-gray-200 shadow-xs">
          <div className="flex items-center justify-between mb-6 pb-4 border-b border-gray-100">
            <div>
              <h2 className="text-lg font-bold text-gray-900">Career Track Distribution</h2>
              <p className="text-xs text-gray-500">Target tracks selected by candidates</p>
            </div>
            <span className="text-xs font-bold text-blue-600 bg-blue-50 px-2.5 py-1 rounded-lg">
              {stats?.totalCareerGoals ?? 0} Goals Set
            </span>
          </div>

          <div className="space-y-4">
            {stats && stats.careerGoalsDistribution && Object.keys(stats.careerGoalsDistribution).length > 0 ? (
              Object.entries(stats.careerGoalsDistribution)
                .sort((a, b) => Number(b[1]) - Number(a[1]))
                .map(([career, count], idx) => {
                  const total = stats.totalUsers || 1;
                  const pct = Math.round((Number(count) / total) * 100);
                  return (
                    <div key={idx} className="space-y-1.5">
                      <div className="flex justify-between text-xs font-semibold text-gray-700">
                        <span>{career}</span>
                        <span>
                          {count} candidate{Number(count) === 1 ? "" : "s"} ({pct}%)
                        </span>
                      </div>
                      <div className="w-full h-2.5 bg-gray-100 rounded-full overflow-hidden">
                        <div
                          className="h-full bg-blue-600 rounded-full transition-all duration-500"
                          style={{ width: `${Math.min(pct, 100)}%` }}
                        />
                      </div>
                    </div>
                  );
                })
            ) : (
              <p className="text-sm text-gray-400 text-center py-6">No career goal distribution data available.</p>
            )}
          </div>
        </div>

        {/* User Experience Level Distribution */}
        <div className="bg-white rounded-2xl p-6 border border-gray-200 shadow-xs">
          <div className="flex items-center justify-between mb-6 pb-4 border-b border-gray-100">
            <div>
              <h2 className="text-lg font-bold text-gray-900">Experience Tier Breakdown</h2>
              <p className="text-xs text-gray-500">Self-declared and assessed user levels</p>
            </div>
            <span className="text-xs font-bold text-purple-600 bg-purple-50 px-2.5 py-1 rounded-lg">
              {stats?.totalUsers ?? 0} Total
            </span>
          </div>

          <div className="space-y-4">
            {stats && stats.userLevelDistribution && Object.keys(stats.userLevelDistribution).length > 0 ? (
              Object.entries(stats.userLevelDistribution).map(([level, count], idx) => {
                const total = stats.totalUsers || 1;
                const pct = Math.round((Number(count) / total) * 100);
                const color =
                  level.toLowerCase() === "advanced"
                    ? "bg-purple-600"
                    : level.toLowerCase() === "intermediate"
                    ? "bg-blue-600"
                    : "bg-emerald-600";
                return (
                  <div key={idx} className="space-y-1.5">
                    <div className="flex justify-between text-xs font-semibold text-gray-700">
                      <span>{level} Tier</span>
                      <span>
                        {count} user{Number(count) === 1 ? "" : "s"} ({pct}%)
                      </span>
                    </div>
                    <div className="w-full h-2.5 bg-gray-100 rounded-full overflow-hidden">
                      <div
                        className={`h-full ${color} rounded-full transition-all duration-500`}
                        style={{ width: `${Math.min(pct, 100)}%` }}
                      />
                    </div>
                  </div>
                );
              })
            ) : (
              <p className="text-sm text-gray-400 text-center py-6">No experience level data available.</p>
            )}
          </div>
        </div>
      </div>

      {/* Governance & Content Management Modules Grid */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200 shadow-xs space-y-6">
        <div>
          <h2 className="text-lg font-bold text-gray-900">Platform Management Modules</h2>
          <p className="text-xs text-gray-500">
            Administrative tooling for user management and future content management
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {/* Active Module: User Management */}
          <Link
            href="/admin/users"
            className="p-5 rounded-2xl border-2 border-purple-500 bg-purple-50/40 hover:bg-purple-50 transition block group relative"
          >
            <div className="flex items-center justify-between mb-3">
              <span className="w-10 h-10 rounded-xl bg-purple-600 text-white flex items-center justify-center text-lg shadow-sm">
                👥
              </span>
              <span className="px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-purple-600 text-white">
                ACTIVE
              </span>
            </div>
            <h3 className="font-bold text-gray-900 group-hover:text-purple-700 transition text-sm">
              User Governance
            </h3>
            <p className="text-xs text-gray-600 mt-1">
              Inspect candidate profiles, roles, skill portfolios, resume uploads, and interview history.
            </p>
          </Link>

          {/* Phase 13 Placeholder: Careers Management */}
          <div className="p-5 rounded-2xl border border-gray-200 bg-gray-50/70 opacity-80">
            <div className="flex items-center justify-between mb-3">
              <span className="w-10 h-10 rounded-xl bg-gray-200 text-gray-700 flex items-center justify-center text-lg">
                🚀
              </span>
              <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-gray-200 text-gray-600">
                Coming in Phase 13
              </span>
            </div>
            <h3 className="font-bold text-gray-700 text-sm">Career Tracks Management</h3>
            <p className="text-xs text-gray-500 mt-1">
              Create, update, and manage technical career tracks and matching weight algorithms.
            </p>
          </div>

          {/* Phase 13 Placeholder: Roadmaps Management */}
          <div className="p-5 rounded-2xl border border-gray-200 bg-gray-50/70 opacity-80">
            <div className="flex items-center justify-between mb-3">
              <span className="w-10 h-10 rounded-xl bg-gray-200 text-gray-700 flex items-center justify-center text-lg">
                🗺️
              </span>
              <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-gray-200 text-gray-600">
                Coming in Phase 13
              </span>
            </div>
            <h3 className="font-bold text-gray-700 text-sm">Roadmaps & Milestones</h3>
            <p className="text-xs text-gray-500 mt-1">
              Define step-by-step curriculum milestones and interactive checklist structures.
            </p>
          </div>

          {/* Phase 13 Placeholder: Skills Dictionary */}
          <div className="p-5 rounded-2xl border border-gray-200 bg-gray-50/70 opacity-80">
            <div className="flex items-center justify-between mb-3">
              <span className="w-10 h-10 rounded-xl bg-gray-200 text-gray-700 flex items-center justify-center text-lg">
                ⚡
              </span>
              <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-gray-200 text-gray-600">
                Coming in Phase 13
              </span>
            </div>
            <h3 className="font-bold text-gray-700 text-sm">Skills Vocabulary & Aliases</h3>
            <p className="text-xs text-gray-500 mt-1">
              Manage canonical technical skill taxonomy and regex alias normalization mappings.
            </p>
          </div>

          {/* Phase 13 Placeholder: Problems Management */}
          <div className="p-5 rounded-2xl border border-gray-200 bg-gray-50/70 opacity-80">
            <div className="flex items-center justify-between mb-3">
              <span className="w-10 h-10 rounded-xl bg-gray-200 text-gray-700 flex items-center justify-center text-lg">
                💡
              </span>
              <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-gray-200 text-gray-600">
                Coming in Phase 13
              </span>
            </div>
            <h3 className="font-bold text-gray-700 text-sm">Coding Problems & Test Suites</h3>
            <p className="text-xs text-gray-500 mt-1">
              Author Algorithmic DSA problems, sample code templates, constraints, and test suites.
            </p>
          </div>

          {/* Phase 13 Placeholder: Mock Interview Questions */}
          <div className="p-5 rounded-2xl border border-gray-200 bg-gray-50/70 opacity-80">
            <div className="flex items-center justify-between mb-3">
              <span className="w-10 h-10 rounded-xl bg-gray-200 text-gray-700 flex items-center justify-center text-lg">
                🎯
              </span>
              <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-gray-200 text-gray-600">
                Coming in Phase 13
              </span>
            </div>
            <h3 className="font-bold text-gray-700 text-sm">Interview Question Bank</h3>
            <p className="text-xs text-gray-500 mt-1">
              Manage conceptual multi-choice question items, explanations, and difficulty tags.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
