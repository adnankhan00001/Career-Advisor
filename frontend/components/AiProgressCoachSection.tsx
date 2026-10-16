"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  progressCoachService,
  WeeklyProgressReport,
  TrendDirection,
} from "@/lib/progressCoachService";

export default function AiProgressCoachSection() {
  const [report, setReport] = useState<WeeklyProgressReport | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [refreshError, setRefreshError] = useState<string | null>(null);

  useEffect(() => {
    loadReport();
  }, []);

  async function loadReport() {
    setLoading(true);
    try {
      const data = await progressCoachService.getWeeklyReport();
      if (data) setReport(data);
    } catch {
      // safe fallback
    } finally {
      setLoading(false);
    }
  }

  async function handleRefresh() {
    setRefreshing(true);
    setRefreshError(null);
    try {
      const data = await progressCoachService.refreshWeeklyReport();
      if (data) {
        setReport(data);
      } else {
        setRefreshError("Could not refresh at this moment. Please try again shortly.");
      }
    } catch {
      setRefreshError("Refresh failed. Please check connection.");
    } finally {
      setRefreshing(false);
    }
  }

  function getTrendBadge(trend: TrendDirection) {
    switch (trend) {
      case "IMPROVING":
        return <span className="text-[10px] font-bold text-emerald-700 bg-emerald-100 border border-emerald-200 px-2 py-0.5 rounded-full flex items-center gap-1">↗ Improving</span>;
      case "STABLE":
        return <span className="text-[10px] font-bold text-blue-700 bg-blue-100 border border-blue-200 px-2 py-0.5 rounded-full flex items-center gap-1">→ Stable</span>;
      case "DECLINING":
        return <span className="text-[10px] font-bold text-rose-700 bg-rose-100 border border-rose-200 px-2 py-0.5 rounded-full flex items-center gap-1">↘ Needs Work</span>;
      default:
        return <span className="text-[10px] font-medium text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">New Area</span>;
    }
  }

  if (loading) {
    return (
      <div className="bg-white rounded-3xl border border-gray-200 p-6 sm:p-8 shadow-xs animate-pulse space-y-4">
        <div className="h-6 bg-gray-200 rounded w-1/3"></div>
        <div className="h-4 bg-gray-100 rounded w-2/3"></div>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 pt-4">
          <div className="h-20 bg-gray-100 rounded-2xl"></div>
          <div className="h-20 bg-gray-100 rounded-2xl"></div>
          <div className="h-20 bg-gray-100 rounded-2xl"></div>
        </div>
      </div>
    );
  }

  if (!report) return null;

  return (
    <div className="bg-white rounded-3xl border border-gray-200 p-6 sm:p-8 shadow-xs space-y-6">
      {/* Section Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 pb-4 border-b border-gray-100">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs uppercase tracking-wider font-extrabold text-indigo-600 bg-indigo-50 border border-indigo-100 px-2.5 py-0.5 rounded-full">
              Phase 15D • AI Progress Coach
            </span>
            <span className="text-xs font-semibold bg-gray-100 text-gray-700 px-2 py-0.5 rounded-full">
              {report.source === "AI" ? "✨ AI Grounded" : "Deterministic Engine"}
            </span>
            {getTrendBadge(report.overallTrend)}
          </div>
          <h2 className="text-xl sm:text-2xl font-black text-gray-900 tracking-tight">
            Weekly Progress & Career Intelligence
          </h2>
          <p className="text-xs text-gray-500 mt-0.5">
            Week of {report.periodStart} to {report.periodEnd} • Real-time platform execution telemetry
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl shadow-xs transition flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
          >
            {refreshing ? "🔄 Analyzing..." : "🔄 Refresh Coach Analysis"}
          </button>
        </div>
      </div>

      {refreshError && (
        <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-xl font-medium">
          {refreshError}
        </div>
      )}

      {/* Metrics Summary Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3.5">
        <div className="p-4 rounded-2xl bg-gradient-to-br from-indigo-50 to-indigo-100/40 border border-indigo-200/80">
          <span className="text-[11px] font-bold text-indigo-700 uppercase tracking-wide block">
            Overall Progress
          </span>
          <div className="flex items-baseline gap-1 mt-1">
            <span className="text-2xl font-black text-indigo-900">
              {report.overallProgressScore}%
            </span>
          </div>
          <div className="w-full bg-indigo-200/60 h-1.5 rounded-full overflow-hidden mt-2">
            <div className="bg-indigo-600 h-1.5 rounded-full" style={{ width: `${report.overallProgressScore}%` }}></div>
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-gray-50 border border-gray-200">
          <span className="text-[11px] font-bold text-gray-500 uppercase tracking-wide block">
            Career Readiness
          </span>
          <div className="flex items-baseline gap-1 mt-1">
            <span className="text-2xl font-black text-gray-900">
              {report.careerReadinessScore}%
            </span>
          </div>
          <span className="text-[10px] text-gray-500 mt-2 block truncate">Target Alignment</span>
        </div>

        <div className="p-4 rounded-2xl bg-gray-50 border border-gray-200">
          <div className="flex justify-between items-start">
            <span className="text-[11px] font-bold text-gray-500 uppercase tracking-wide block">
              Planner Habit
            </span>
            {getTrendBadge(report.plannerTrend)}
          </div>
          <div className="flex items-baseline gap-1 mt-1">
            <span className="text-2xl font-black text-emerald-600">
              {report.plannerConsistency}%
            </span>
          </div>
          <span className="text-[10px] text-gray-500 mt-2 block truncate">
            🔥 {report.streakDays}d streak • {report.completedTasks} tasks
          </span>
        </div>

        <div className="p-4 rounded-2xl bg-gray-50 border border-gray-200">
          <div className="flex justify-between items-start">
            <span className="text-[11px] font-bold text-gray-500 uppercase tracking-wide block">
              Roadmap
            </span>
            {getTrendBadge(report.roadmapTrend)}
          </div>
          <div className="flex items-baseline gap-1 mt-1">
            <span className="text-2xl font-black text-blue-600">
              {report.roadmapProgress}%
            </span>
          </div>
          <span className="text-[10px] text-gray-500 mt-2 block truncate">Milestones completed</span>
        </div>

        <div className="p-4 rounded-2xl bg-gray-50 border border-gray-200">
          <div className="flex justify-between items-start">
            <span className="text-[11px] font-bold text-gray-500 uppercase tracking-wide block">
              DSA Practice
            </span>
            {getTrendBadge(report.dsaTrend)}
          </div>
          <div className="flex items-baseline gap-1 mt-1">
            <span className="text-2xl font-black text-purple-600">
              {report.dsaSolvedCount}
            </span>
            <span className="text-xs text-gray-400 font-semibold">/{report.dsaTotalCount}</span>
          </div>
          <span className="text-[10px] text-gray-500 mt-2 block truncate">{report.dsaProgress}% solved</span>
        </div>

        <div className="p-4 rounded-2xl bg-gray-50 border border-gray-200">
          <div className="flex justify-between items-start">
            <span className="text-[11px] font-bold text-gray-500 uppercase tracking-wide block">
              Interviews
            </span>
            {getTrendBadge(report.interviewTrend)}
          </div>
          <div className="flex items-baseline gap-1 mt-1">
            <span className="text-2xl font-black text-amber-600">
              {report.interviewProgress}%
            </span>
          </div>
          <span className="text-[10px] text-gray-500 mt-2 block truncate">
            {report.interviewCompletedCount} completed
          </span>
        </div>
      </div>

      {/* Coach Assessment Narrative Card */}
      <div className="p-5 rounded-2xl bg-gradient-to-r from-indigo-900 to-slate-900 text-white shadow-sm space-y-2">
        <div className="flex items-center gap-2">
          <span className="text-base">🎙️</span>
          <h3 className="text-sm font-extrabold tracking-wide uppercase text-indigo-300">
            Coach's Assessment & Strategic Review
          </h3>
        </div>
        <p className="text-xs sm:text-sm text-indigo-100 leading-relaxed font-normal">
          "{report.coachSummary}"
        </p>
      </div>

      {/* Two Columns: This Week's Wins vs Needs Attention */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Wins */}
        <div className="bg-emerald-50/40 rounded-2xl border border-emerald-200/80 p-5 space-y-3">
          <div className="flex items-center gap-2 pb-2 border-b border-emerald-200/60">
            <span className="text-emerald-600 text-base">🏆</span>
            <h4 className="text-xs font-bold text-emerald-900 uppercase tracking-wider">
              This Week's Wins & Momentum
            </h4>
          </div>
          <div className="space-y-2">
            {report.topAchievements && report.topAchievements.length > 0 ? (
              report.topAchievements.map((item, idx) => (
                <div key={idx} className="flex items-start gap-2 text-xs text-emerald-950 font-medium">
                  <span className="text-emerald-600 font-bold">✓</span>
                  <span>{item}</span>
                </div>
              ))
            ) : (
              <p className="text-xs text-gray-500 italic">Complete daily milestones to record weekly wins.</p>
            )}
          </div>
        </div>

        {/* Needs Attention */}
        <div className="bg-amber-50/40 rounded-2xl border border-amber-200/80 p-5 space-y-3">
          <div className="flex items-center gap-2 pb-2 border-b border-amber-200/60">
            <span className="text-amber-600 text-base">⚠️</span>
            <h4 className="text-xs font-bold text-amber-900 uppercase tracking-wider">
              Needs Attention & Growth Areas
            </h4>
          </div>
          <div className="space-y-2">
            {report.priorityAreas && report.priorityAreas.length > 0 ? (
              report.priorityAreas.map((item, idx) => (
                <div key={idx} className="flex items-start gap-2 text-xs text-amber-950 font-medium">
                  <span className="text-amber-600 font-bold">!</span>
                  <span>{item}</span>
                </div>
              ))
            ) : (
              <p className="text-xs text-gray-500 italic">No critical blockers identified for this cycle.</p>
            )}
          </div>
        </div>
      </div>

      {/* Next Week Focus Actions */}
      {report.nextWeekFocus && report.nextWeekFocus.length > 0 && (
        <div className="space-y-3 pt-2">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-extrabold text-gray-800 uppercase tracking-wider flex items-center gap-2">
              <span>🎯</span> Next Week's Recommended Focus Priorities
            </h4>
            <span className="text-[11px] text-gray-400 font-medium">
              Sorted by highest career readiness impact
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3.5">
            {report.nextWeekFocus.map((action, idx) => (
              <div
                key={idx}
                className="bg-white rounded-2xl border border-gray-200 p-4 hover:border-indigo-300 hover:shadow-sm transition flex flex-col justify-between space-y-3"
              >
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between gap-1">
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-indigo-50 text-indigo-700 border border-indigo-100">
                      {action.category}
                    </span>
                    <span className="text-[10px] text-gray-400 font-medium">
                      ⏱️ {action.estimatedEffort}
                    </span>
                  </div>
                  <h5 className="text-xs font-bold text-gray-900 leading-snug">
                    {action.title}
                  </h5>
                  <p className="text-[11px] text-gray-500 line-clamp-2">
                    {action.whyItMatters}
                  </p>
                </div>

                <Link
                  href={action.actionUrl}
                  className="w-full py-1.5 bg-gray-900 hover:bg-black text-white text-center text-xs font-semibold rounded-xl transition block"
                >
                  Start Action →
                </Link>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
