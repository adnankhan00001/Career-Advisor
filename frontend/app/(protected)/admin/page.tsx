"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { getAdminStatsOverview, AdminStatsOverview } from "@/lib/adminService";
import {
  getUserAnalytics,
  getCareerPrepAnalytics,
  getMentorshipAnalytics,
  UserAnalytics,
  CareerPrepAnalytics,
  MentorshipAnalytics,
  TimeSeriesPoint,
} from "@/lib/adminAnalyticsService";

type AnalyticsTab = "overview" | "users" | "career-prep" | "mentorship";

export default function AdminDashboardPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();

  // Overview stats (legacy + global)
  const [stats, setStats] = useState<AdminStatsOverview | null>(null);
  const [isLoadingStats, setIsLoadingStats] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Time range & active tab
  const [selectedRange, setSelectedRange] = useState<number>(30);
  const [activeTab, setActiveTab] = useState<AnalyticsTab>("overview");

  // Advanced analytics state
  const [userAnalytics, setUserAnalytics] = useState<UserAnalytics | null>(null);
  const [careerPrepAnalytics, setCareerPrepAnalytics] = useState<CareerPrepAnalytics | null>(null);
  const [mentorshipAnalytics, setMentorshipAnalytics] = useState<MentorshipAnalytics | null>(null);
  const [isLoadingAnalytics, setIsLoadingAnalytics] = useState<boolean>(true);
  const [analyticsError, setAnalyticsError] = useState<string | null>(null);

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
      loadAnalytics(selectedRange);
    }
  }, [user, selectedRange]);

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

  const loadAnalytics = async (days: number) => {
    setIsLoadingAnalytics(true);
    setAnalyticsError(null);
    try {
      const [uData, cData, mData] = await Promise.all([
        getUserAnalytics(days),
        getCareerPrepAnalytics(days),
        getMentorshipAnalytics(days),
      ]);
      setUserAnalytics(uData);
      setCareerPrepAnalytics(cData);
      setMentorshipAnalytics(mData);
    } catch (err: any) {
      console.error("Failed to load advanced analytics:", err);
      setAnalyticsError(err.message || "Failed to load platform analytics telemetry.");
    } finally {
      setIsLoadingAnalytics(false);
    }
  };

  const handleRefresh = () => {
    loadStats();
    loadAnalytics(selectedRange);
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
      value: stats?.totalUsers ?? userAnalytics?.totalUsers ?? 0,
      icon: "👥",
      color: "bg-blue-500/10 text-blue-600 border-blue-200",
      desc: "Total platform candidates",
    },
    {
      title: "Resumes Analyzed",
      value: stats?.totalResumes ?? careerPrepAnalytics?.totalResumes ?? 0,
      icon: "📄",
      color: "bg-emerald-500/10 text-emerald-600 border-emerald-200",
      desc: "Parsed PDF & DOCX resumes",
    },
    {
      title: "Quiz Assessments",
      value: stats?.totalQuizAttempts ?? careerPrepAnalytics?.totalQuizAttempts ?? 0,
      icon: "📝",
      color: "bg-amber-500/10 text-amber-600 border-amber-200",
      desc: "Skill calibration tests",
    },
    {
      title: "DSA Solved",
      value: stats?.totalSolvedProblems ?? careerPrepAnalytics?.totalSolvedProblems ?? 0,
      icon: "💡",
      color: "bg-purple-500/10 text-purple-600 border-purple-200",
      desc: "Accepted code submissions",
    },
    {
      title: "Mock Interviews",
      value: stats?.totalMockInterviews ?? careerPrepAnalytics?.totalMockInterviews ?? 0,
      icon: "🎯",
      color: "bg-rose-500/10 text-rose-600 border-rose-200",
      desc: "Initialized interview rounds",
    },
    {
      title: "Mentorship Requests",
      value: mentorshipAnalytics?.totalRequests ?? 0,
      icon: "🤝",
      desc: "Total candidate mentorship requests",
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
              <span>Analytics & Oversight</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
              Administrative Control Center
            </h1>
            <p className="text-gray-300 text-sm max-w-2xl">
              System governance, live platform telemetry, and mentorship oversight for the OneStop Career Advisor ecosystem.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Link
              href="/admin/users"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white text-gray-900 font-bold text-sm hover:bg-gray-100 shadow-md transition cursor-pointer"
            >
              <span>Manage Users</span>
              <span>👥</span>
            </Link>
            <Link
              href="/admin/mentors"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-purple-600 text-white font-bold text-sm hover:bg-purple-700 shadow-md transition cursor-pointer"
            >
              <span>Mentors</span>
              <span>🎓</span>
            </Link>
            <Link
              href="/admin/mentorships"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-indigo-600 text-white font-bold text-sm hover:bg-indigo-700 shadow-md transition cursor-pointer"
            >
              <span>Mentorship Oversight</span>
              <span>🤝</span>
            </Link>
            <button
              onClick={handleRefresh}
              disabled={isLoadingStats || isLoadingAnalytics}
              className="inline-flex items-center gap-2 px-3.5 py-2.5 rounded-xl bg-white/10 text-white font-semibold text-sm hover:bg-white/20 border border-white/20 transition cursor-pointer"
              title="Refresh Platform Analytics"
            >
              <span className={isLoadingStats || isLoadingAnalytics ? "animate-spin" : ""}>🔄</span>
            </button>
          </div>
        </div>
      </div>

      {/* Error Alerts */}
      {(error || analyticsError) && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-sm flex items-center justify-between">
          <p className="font-medium">⚠️ {error || analyticsError}</p>
          <button
            onClick={handleRefresh}
            className="text-xs font-bold underline hover:text-red-900 cursor-pointer"
          >
            Retry
          </button>
        </div>
      )}

      {/* High-Level Overview KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        {kpis.map((kpi, idx) => (
          <div
            key={idx}
            className="bg-white rounded-2xl p-5 border border-gray-200 shadow-xs flex flex-col justify-between hover:border-gray-300 transition"
          >
            <div className="flex items-center justify-between mb-3">
              <span className="text-[11px] font-bold uppercase tracking-wider text-gray-500">
                {kpi.title}
              </span>
              <span
                className={`w-8 h-8 rounded-lg flex items-center justify-center text-sm border ${kpi.color}`}
              >
                {kpi.icon}
              </span>
            </div>
            <div>
              <p className="text-2xl font-extrabold text-gray-900 tracking-tight">
                {isLoadingStats && isLoadingAnalytics ? "..." : kpi.value.toLocaleString()}
              </p>
              <p className="text-[11px] text-gray-500 mt-1 line-clamp-1">{kpi.desc}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Navigation Tabs and Date-Range Selector Bar */}
      <div className="bg-white rounded-2xl p-4 border border-gray-200 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        {/* Navigation Tabs */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => setActiveTab("overview")}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition cursor-pointer ${
              activeTab === "overview"
                ? "bg-gray-900 text-white shadow-xs"
                : "bg-gray-100 text-gray-600 hover:bg-gray-200 hover:text-gray-900"
            }`}
          >
            Overview & Modules
          </button>
          <button
            onClick={() => setActiveTab("users")}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition cursor-pointer ${
              activeTab === "users"
                ? "bg-blue-600 text-white shadow-xs"
                : "bg-gray-100 text-gray-600 hover:bg-gray-200 hover:text-gray-900"
            }`}
          >
            User Growth & Roles
          </button>
          <button
            onClick={() => setActiveTab("career-prep")}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition cursor-pointer ${
              activeTab === "career-prep"
                ? "bg-emerald-600 text-white shadow-xs"
                : "bg-gray-100 text-gray-600 hover:bg-gray-200 hover:text-gray-900"
            }`}
          >
            Career Preparation
          </button>
          <button
            onClick={() => setActiveTab("mentorship")}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition cursor-pointer ${
              activeTab === "mentorship"
                ? "bg-indigo-600 text-white shadow-xs"
                : "bg-gray-100 text-gray-600 hover:bg-gray-200 hover:text-gray-900"
            }`}
          >
            Mentorship Activity
          </button>
        </div>

        {/* Date Range Selector */}
        <div className="flex items-center gap-2 self-end md:self-auto">
          <span className="text-xs font-semibold text-gray-500">Period:</span>
          <div className="inline-flex rounded-xl bg-gray-100 p-1 border border-gray-200">
            {[
              { label: "7 Days", value: 7 },
              { label: "30 Days", value: 30 },
              { label: "90 Days", value: 90 },
            ].map((r) => (
              <button
                key={r.value}
                onClick={() => setSelectedRange(r.value)}
                className={`px-3 py-1 rounded-lg text-xs font-bold transition cursor-pointer ${
                  selectedRange === r.value
                    ? "bg-white text-gray-900 shadow-xs"
                    : "text-gray-600 hover:text-gray-900"
                }`}
              >
                {r.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* TAB CONTENT: Overview & Modules */}
      {activeTab === "overview" && (
        <div className="space-y-8">
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
                Administrative tooling for candidate oversight, mentor governance, and mentorship workflows
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

              {/* Active Module: Mentor Governance */}
              <Link
                href="/admin/mentors"
                className="p-5 rounded-2xl border-2 border-emerald-500 bg-emerald-50/40 hover:bg-emerald-50 transition block group relative"
              >
                <div className="flex items-center justify-between mb-3">
                  <span className="w-10 h-10 rounded-xl bg-emerald-600 text-white flex items-center justify-center text-lg shadow-sm">
                    🎓
                  </span>
                  <span className="px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-emerald-600 text-white">
                    ACTIVE
                  </span>
                </div>
                <h3 className="font-bold text-gray-900 group-hover:text-emerald-700 transition text-sm">
                  Mentor Governance
                </h3>
                <p className="text-xs text-gray-600 mt-1">
                  Review pending mentor applications, inspect active mentors, suspend profiles, and configure platform capacity limits.
                </p>
              </Link>

              {/* Active Module: Mentorship Oversight */}
              <Link
                href="/admin/mentorships"
                className="p-5 rounded-2xl border-2 border-indigo-500 bg-indigo-50/40 hover:bg-indigo-50 transition block group relative"
              >
                <div className="flex items-center justify-between mb-3">
                  <span className="w-10 h-10 rounded-xl bg-indigo-600 text-white flex items-center justify-center text-lg shadow-sm">
                    🤝
                  </span>
                  <span className="px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-indigo-600 text-white">
                    ACTIVE
                  </span>
                </div>
                <h3 className="font-bold text-gray-900 group-hover:text-indigo-700 transition text-sm">
                  Mentorship Oversight
                </h3>
                <p className="text-xs text-gray-600 mt-1">
                  Inspect candidate mentorship requests, monitor ongoing mentor-mentee partnerships, track response latencies, and execute administrative cancellations.
                </p>
              </Link>

              {/* Phase 13 Placeholder: Careers Management */}
              <div className="p-5 rounded-2xl border border-gray-200 bg-gray-50/70 opacity-80">
                <div className="flex items-center justify-between mb-3">
                  <span className="w-10 h-10 rounded-xl bg-gray-200 text-gray-700 flex items-center justify-center text-lg">
                    🚀
                  </span>
                  <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-gray-200 text-gray-600">
                    Phase 13 Roadmap
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
                    Phase 13 Roadmap
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
                    Phase 13 Roadmap
                  </span>
                </div>
                <h3 className="font-bold text-gray-700 text-sm">Skills Vocabulary & Aliases</h3>
                <p className="text-xs text-gray-500 mt-1">
                  Manage canonical technical skill taxonomy and regex alias normalization mappings.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB CONTENT: User Growth & Demographics */}
      {activeTab === "users" && (
        <div className="space-y-8">
          {/* Summary Metric Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-xs">
              <span className="text-xs font-bold uppercase tracking-wider text-gray-500">Total Users</span>
              <p className="text-3xl font-extrabold text-gray-900 mt-2">
                {isLoadingAnalytics ? "..." : userAnalytics?.totalUsers.toLocaleString()}
              </p>
              <p className="text-xs text-gray-500 mt-1">All time registered accounts</p>
            </div>
            <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-xs">
              <span className="text-xs font-bold uppercase tracking-wider text-blue-600">
                New in {selectedRange} Days
              </span>
              <p className="text-3xl font-extrabold text-blue-600 mt-2">
                {isLoadingAnalytics ? "..." : userAnalytics?.newUsersInPeriod.toLocaleString()}
              </p>
              <p className="text-xs text-gray-500 mt-1">Recent signups in window</p>
            </div>
            <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-xs">
              <span className="text-xs font-bold uppercase tracking-wider text-emerald-600">Active Status</span>
              <p className="text-3xl font-extrabold text-emerald-600 mt-2">
                {isLoadingAnalytics ? "..." : userAnalytics?.activeUsers.toLocaleString()}
              </p>
              <p className="text-xs text-gray-500 mt-1">Good standing accounts</p>
            </div>
            <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-xs">
              <span className="text-xs font-bold uppercase tracking-wider text-red-600">Suspended / Deactivated</span>
              <p className="text-3xl font-extrabold text-red-600 mt-2">
                {isLoadingAnalytics
                  ? "..."
                  : ((userAnalytics?.suspendedUsers ?? 0) + (userAnalytics?.deactivatedUsers ?? 0)).toLocaleString()}
              </p>
              <p className="text-xs text-gray-500 mt-1">Restricted accounts</p>
            </div>
          </div>

          {/* Registration Trend Chart */}
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200 shadow-xs">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-gray-100 gap-2 mb-6">
              <div>
                <h2 className="text-lg font-bold text-gray-900">User Registration Trajectory</h2>
                <p className="text-xs text-gray-500">
                  Daily new account creations over the last {selectedRange} days
                </p>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-blue-600 inline-block"></span>
                <span className="text-xs font-bold text-gray-700">Daily Registrations</span>
              </div>
            </div>

            {isLoadingAnalytics ? (
              <div className="h-64 flex items-center justify-center">
                <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
              </div>
            ) : (
              <TimeSeriesBarChart
                data={userAnalytics?.registrationTrend || []}
                barColor="bg-blue-600 hover:bg-blue-700"
                emptyLabel="No new registrations in this window"
              />
            )}
          </div>

          {/* Role & Status Distributions */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Role Breakdown */}
            <div className="bg-white rounded-2xl p-6 border border-gray-200 shadow-xs">
              <div className="flex items-center justify-between mb-6 pb-4 border-b border-gray-100">
                <h3 className="text-base font-bold text-gray-900">Platform Role Distribution</h3>
                <span className="text-xs font-bold text-purple-600 bg-purple-50 px-2.5 py-1 rounded-lg">
                  RBAC Breakdown
                </span>
              </div>
              <div className="space-y-4">
                {userAnalytics && Object.keys(userAnalytics.roleDistribution).length > 0 ? (
                  Object.entries(userAnalytics.roleDistribution).map(([role, count]) => {
                    const total = userAnalytics.totalUsers || 1;
                    const pct = Math.round((count / total) * 100);
                    const color = role === "ADMIN" ? "bg-purple-600" : "bg-blue-600";
                    return (
                      <div key={role} className="space-y-1.5">
                        <div className="flex justify-between text-xs font-semibold text-gray-700">
                          <span>Role: {role}</span>
                          <span>
                            {count} account{count === 1 ? "" : "s"} ({pct}%)
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
                  <p className="text-sm text-gray-400 text-center py-6">No role distribution data.</p>
                )}
              </div>
            </div>

            {/* Account Status Breakdown */}
            <div className="bg-white rounded-2xl p-6 border border-gray-200 shadow-xs">
              <div className="flex items-center justify-between mb-6 pb-4 border-b border-gray-100">
                <h3 className="text-base font-bold text-gray-900">Account Standing Breakdown</h3>
                <span className="text-xs font-bold text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-lg">
                  User Health
                </span>
              </div>
              <div className="space-y-4">
                {userAnalytics && Object.keys(userAnalytics.statusDistribution).length > 0 ? (
                  Object.entries(userAnalytics.statusDistribution).map(([st, count]) => {
                    const total = userAnalytics.totalUsers || 1;
                    const pct = Math.round((count / total) * 100);
                    const color =
                      st === "ACTIVE"
                        ? "bg-emerald-600"
                        : st === "SUSPENDED"
                        ? "bg-red-600"
                        : "bg-gray-500";
                    return (
                      <div key={st} className="space-y-1.5">
                        <div className="flex justify-between text-xs font-semibold text-gray-700">
                          <span>Status: {st}</span>
                          <span>
                            {count} user{count === 1 ? "" : "s"} ({pct}%)
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
                  <p className="text-sm text-gray-400 text-center py-6">No account status data.</p>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB CONTENT: Career Preparation */}
      {activeTab === "career-prep" && (
        <div className="space-y-8">
          {/* Summary Metric Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-xs">
              <span className="text-xs font-bold uppercase tracking-wider text-gray-500">Total Resumes</span>
              <p className="text-3xl font-extrabold text-emerald-600 mt-2">
                {isLoadingAnalytics ? "..." : careerPrepAnalytics?.totalResumes.toLocaleString()}
              </p>
              <p className="text-xs text-gray-500 mt-1">
                {careerPrepAnalytics?.resumesUploadedInPeriod ?? 0} uploaded in last {selectedRange}d
              </p>
            </div>
            <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-xs">
              <span className="text-xs font-bold uppercase tracking-wider text-amber-600">Quiz Attempts</span>
              <p className="text-3xl font-extrabold text-amber-600 mt-2">
                {isLoadingAnalytics ? "..." : careerPrepAnalytics?.totalQuizAttempts.toLocaleString()}
              </p>
              <p className="text-xs text-gray-500 mt-1">Skill assessments completed</p>
            </div>
            <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-xs">
              <span className="text-xs font-bold uppercase tracking-wider text-purple-600">DSA Solved</span>
              <p className="text-3xl font-extrabold text-purple-600 mt-2">
                {isLoadingAnalytics ? "..." : careerPrepAnalytics?.totalSolvedProblems.toLocaleString()}
              </p>
              <p className="text-xs text-gray-500 mt-1">Algorithms and code challenges</p>
            </div>
            <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-xs">
              <span className="text-xs font-bold uppercase tracking-wider text-rose-600">Mock Interviews</span>
              <p className="text-3xl font-extrabold text-rose-600 mt-2">
                {isLoadingAnalytics ? "..." : careerPrepAnalytics?.totalMockInterviews.toLocaleString()}
              </p>
              <p className="text-xs text-gray-500 mt-1">
                {careerPrepAnalytics?.completedInterviewsCount ?? 0} evaluated rounds
              </p>
            </div>
          </div>

          {/* Trend Charts Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Resume Upload Trend */}
            <div className="bg-white rounded-3xl p-6 border border-gray-200 shadow-xs">
              <div className="flex items-center justify-between pb-4 border-b border-gray-100 gap-2 mb-6">
                <div>
                  <h3 className="text-base font-bold text-gray-900">Resume Upload Activity</h3>
                  <p className="text-xs text-gray-500">Daily uploads ({selectedRange}d window)</p>
                </div>
                <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-lg">
                  {careerPrepAnalytics?.resumesUploadedInPeriod ?? 0} in period
                </span>
              </div>
              {isLoadingAnalytics ? (
                <div className="h-48 flex items-center justify-center">
                  <div className="w-6 h-6 border-4 border-emerald-600 border-t-transparent rounded-full animate-spin"></div>
                </div>
              ) : (
                <TimeSeriesBarChart
                  data={careerPrepAnalytics?.resumeUploadTrend || []}
                  barColor="bg-emerald-600 hover:bg-emerald-700"
                  emptyLabel="No resume uploads in this period"
                />
              )}
            </div>

            {/* Mock Interview Trend */}
            <div className="bg-white rounded-3xl p-6 border border-gray-200 shadow-xs">
              <div className="flex items-center justify-between pb-4 border-b border-gray-100 gap-2 mb-6">
                <div>
                  <h3 className="text-base font-bold text-gray-900">Mock Interview Activity</h3>
                  <p className="text-xs text-gray-500">Daily interview sessions ({selectedRange}d window)</p>
                </div>
                <span className="text-xs font-bold text-rose-700 bg-rose-50 px-2.5 py-1 rounded-lg">
                  {careerPrepAnalytics?.mockInterviewsInPeriod ?? 0} in period
                </span>
              </div>
              {isLoadingAnalytics ? (
                <div className="h-48 flex items-center justify-center">
                  <div className="w-6 h-6 border-4 border-rose-600 border-t-transparent rounded-full animate-spin"></div>
                </div>
              ) : (
                <TimeSeriesBarChart
                  data={careerPrepAnalytics?.mockInterviewTrend || []}
                  barColor="bg-rose-600 hover:bg-rose-700"
                  emptyLabel="No mock interviews in this period"
                />
              )}
            </div>
          </div>
        </div>
      )}

      {/* TAB CONTENT: Mentorship Activity */}
      {activeTab === "mentorship" && (
        <div className="space-y-8">
          {/* Summary Metric Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-xs">
              <span className="text-xs font-bold uppercase tracking-wider text-gray-500">Total Requests</span>
              <p className="text-3xl font-extrabold text-indigo-600 mt-2">
                {isLoadingAnalytics ? "..." : mentorshipAnalytics?.totalRequests.toLocaleString()}
              </p>
              <p className="text-xs text-gray-500 mt-1">
                {mentorshipAnalytics?.requestsInPeriod ?? 0} in last {selectedRange} days
              </p>
            </div>
            <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-xs">
              <span className="text-xs font-bold uppercase tracking-wider text-emerald-600">Acceptance Rate</span>
              <p className="text-3xl font-extrabold text-emerald-600 mt-2">
                {isLoadingAnalytics ? "..." : `${mentorshipAnalytics?.acceptanceRate ?? 0}%`}
              </p>
              <p className="text-xs text-gray-500 mt-1">Of processed mentor requests</p>
            </div>
            <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-xs">
              <span className="text-xs font-bold uppercase tracking-wider text-amber-600">Average Latency</span>
              <p className="text-3xl font-extrabold text-amber-600 mt-2">
                {isLoadingAnalytics
                  ? "..."
                  : mentorshipAnalytics?.averageResponseTimeHours !== null && mentorshipAnalytics?.averageResponseTimeHours !== undefined
                  ? `${mentorshipAnalytics.averageResponseTimeHours} hrs`
                  : "N/A"}
              </p>
              <p className="text-xs text-gray-500 mt-1">Initial mentor response time</p>
            </div>
            <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-xs">
              <span className="text-xs font-bold uppercase tracking-wider text-purple-600">Active Mentors</span>
              <p className="text-3xl font-extrabold text-purple-600 mt-2">
                {isLoadingAnalytics ? "..." : mentorshipAnalytics?.activeMentorsCount.toLocaleString()}
              </p>
              <p className="text-xs text-gray-500 mt-1">
                {mentorshipAnalytics?.totalApprovedMentors ?? 0} approved total
              </p>
            </div>
          </div>

          {/* Request Creation Trend Chart */}
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200 shadow-xs">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-gray-100 gap-2 mb-6">
              <div>
                <h2 className="text-lg font-bold text-gray-900">Mentorship Request Demand</h2>
                <p className="text-xs text-gray-500">
                  Daily incoming candidate mentorship requests over the last {selectedRange} days
                </p>
              </div>
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-2">
                  <span className="w-3 h-3 rounded-full bg-indigo-600 inline-block"></span>
                  <span className="text-xs font-bold text-gray-700">Daily Requests</span>
                </div>
                <Link
                  href="/admin/mentorships"
                  className="px-3 py-1.5 rounded-lg bg-indigo-50 text-indigo-700 text-xs font-bold hover:bg-indigo-100 transition"
                >
                  View All Requests &rarr;
                </Link>
              </div>
            </div>

            {isLoadingAnalytics ? (
              <div className="h-64 flex items-center justify-center">
                <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
              </div>
            ) : (
              <TimeSeriesBarChart
                data={mentorshipAnalytics?.requestCreationTrend || []}
                barColor="bg-indigo-600 hover:bg-indigo-700"
                emptyLabel="No mentorship requests submitted in this window"
              />
            )}
          </div>

          {/* Status Breakdown & Capacity Utilization */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Status Distribution */}
            <div className="bg-white rounded-2xl p-6 border border-gray-200 shadow-xs">
              <div className="flex items-center justify-between mb-6 pb-4 border-b border-gray-100">
                <h3 className="text-base font-bold text-gray-900">Request Lifecycle Status</h3>
                <span className="text-xs font-bold text-indigo-600 bg-indigo-50 px-2.5 py-1 rounded-lg">
                  {mentorshipAnalytics?.totalRequests ?? 0} Total Requests
                </span>
              </div>
              <div className="space-y-4">
                {mentorshipAnalytics && Object.keys(mentorshipAnalytics.statusDistribution).length > 0 ? (
                  Object.entries(mentorshipAnalytics.statusDistribution).map(([status, count]) => {
                    const total = mentorshipAnalytics.totalRequests || 1;
                    const pct = Math.round((count / total) * 100);
                    const color =
                      status === "ACCEPTED"
                        ? "bg-emerald-600"
                        : status === "PENDING"
                        ? "bg-amber-500"
                        : status === "COMPLETED"
                        ? "bg-blue-600"
                        : status === "TERMINATED"
                        ? "bg-red-700"
                        : status === "DECLINED"
                        ? "bg-red-500"
                        : "bg-gray-500";
                    return (
                      <div key={status} className="space-y-1.5">
                        <div className="flex justify-between text-xs font-semibold text-gray-700">
                          <span>{status}</span>
                          <span>
                            {count} request{count === 1 ? "" : "s"} ({pct}%)
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
                  <p className="text-sm text-gray-400 text-center py-6">No request status data.</p>
                )}
              </div>
            </div>

            {/* Platform Mentorship Capacity & Rates */}
            <div className="bg-white rounded-2xl p-6 border border-gray-200 shadow-xs space-y-6">
              <div className="flex items-center justify-between pb-4 border-b border-gray-100">
                <h3 className="text-base font-bold text-gray-900">Capacity & Outcome Conversion</h3>
                <span className="text-xs font-bold text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-lg">
                  Ecosystem Health
                </span>
              </div>

              {/* Capacity Gauge */}
              <div className="p-4 rounded-xl bg-gray-50 border border-gray-200 space-y-3">
                <div className="flex justify-between items-center text-xs font-bold text-gray-700">
                  <span>Mentor Capacity Utilization</span>
                  <span className="text-indigo-600">
                    {mentorshipAnalytics?.utilizedCapacity ?? 0} / {mentorshipAnalytics?.totalCapacity ?? 0} slots
                  </span>
                </div>
                <div className="w-full h-3 bg-gray-200 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-indigo-600 rounded-full transition-all duration-500"
                    style={{
                      width: `${Math.min(
                        Math.round(
                          ((mentorshipAnalytics?.utilizedCapacity ?? 0) /
                            Math.max(mentorshipAnalytics?.totalCapacity ?? 1, 1)) *
                            100
                        ),
                        100
                      )}%`,
                    }}
                  />
                </div>
                <p className="text-[11px] text-gray-500">
                  {mentorshipAnalytics?.totalCapacity && mentorshipAnalytics.totalCapacity > 0
                    ? `${Math.round(
                        ((mentorshipAnalytics.utilizedCapacity ?? 0) / mentorshipAnalytics.totalCapacity) * 100
                      )}% of active mentor capacity is currently actively mentoring students.`
                    : "No approved active mentor capacity registered."}
                </p>
              </div>

              {/* Conversion Rates */}
              <div className="grid grid-cols-3 gap-3 text-center">
                <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-100">
                  <p className="text-xs font-bold text-emerald-600 uppercase">Acceptance</p>
                  <p className="text-xl font-extrabold text-emerald-700 mt-1">
                    {mentorshipAnalytics?.acceptanceRate ?? 0}%
                  </p>
                </div>
                <div className="p-3 rounded-xl bg-red-50 border border-red-100">
                  <p className="text-xs font-bold text-red-600 uppercase">Rejection</p>
                  <p className="text-xl font-extrabold text-red-700 mt-1">
                    {mentorshipAnalytics?.rejectionRate ?? 0}%
                  </p>
                </div>
                <div className="p-3 rounded-xl bg-blue-50 border border-blue-100">
                  <p className="text-xs font-bold text-blue-600 uppercase">Completion</p>
                  <p className="text-xl font-extrabold text-blue-700 mt-1">
                    {mentorshipAnalytics?.completionRate ?? 0}%
                  </p>
                </div>
              </div>

              {/* Oversight Action CTA */}
              <div className="pt-2">
                <Link
                  href="/admin/mentorships"
                  className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-indigo-600 text-white font-bold text-sm hover:bg-indigo-700 shadow-sm transition"
                >
                  <span>Open Full Mentorship Oversight Console</span>
                  <span>&rarr;</span>
                </Link>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/**
 * Lightweight, zero-dependency SVG & CSS time-series bar chart component
 */
function TimeSeriesBarChart({
  data,
  barColor,
  emptyLabel,
}: {
  data: TimeSeriesPoint[];
  barColor: string;
  emptyLabel: string;
}) {
  if (!data || data.length === 0) {
    return <p className="text-sm text-gray-400 text-center py-12">{emptyLabel}</p>;
  }

  const maxVal = Math.max(...data.map((d) => d.count), 1);
  const totalInSeries = data.reduce((sum, d) => sum + d.count, 0);

  return (
    <div className="space-y-4">
      {/* Chart Canvas */}
      <div className="h-56 flex items-end gap-1 sm:gap-2 px-2 pt-6 pb-2 border-b border-gray-200">
        {data.map((point, idx) => {
          const heightPct = Math.round((point.count / maxVal) * 100);
          const dateObj = new Date(point.date);
          const displayLabel = !isNaN(dateObj.getTime())
            ? dateObj.toLocaleDateString("en-US", { month: "short", day: "numeric" })
            : point.date;

          return (
            <div
              key={idx}
              className="flex-1 flex flex-col items-center h-full justify-end group relative"
            >
              {/* Hover Tooltip */}
              <div className="absolute -top-10 hidden group-hover:flex flex-col items-center z-20 pointer-events-none">
                <div className="bg-gray-900 text-white text-[11px] font-bold py-1 px-2.5 rounded-lg shadow-lg whitespace-nowrap">
                  {displayLabel}: <span className="text-emerald-400">{point.count}</span>
                </div>
                <div className="w-2 h-1 bg-gray-900 clip-triangle"></div>
              </div>

              {/* Bar */}
              <div
                className={`w-full max-w-[28px] rounded-t-md transition-all duration-300 ${barColor}`}
                style={{
                  height: point.count > 0 ? `${Math.max(heightPct, 6)}%` : "3px",
                  opacity: point.count > 0 ? 1 : 0.25,
                }}
              />
            </div>
          );
        })}
      </div>

      {/* X-axis labels summary */}
      <div className="flex justify-between items-center text-[11px] text-gray-500 font-semibold px-2">
        <span>{data[0]?.date ? new Date(data[0].date).toLocaleDateString() : ""}</span>
        <span className="text-gray-700 font-bold">Total in window: {totalInSeries.toLocaleString()}</span>
        <span>
          {data[data.length - 1]?.date
            ? new Date(data[data.length - 1].date).toLocaleDateString()
            : ""}
        </span>
      </div>
    </div>
  );
}
