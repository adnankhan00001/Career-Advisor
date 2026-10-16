"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  plannerService,
  WeeklyPlan,
  LearningTask,
  PlannerProgress,
  PlannerPreference,
  DailySchedule,
} from "@/lib/plannerService";

export default function PlannerPage() {
  const [plan, setPlan] = useState<WeeklyPlan | null>(null);
  const [progress, setProgress] = useState<PlannerProgress | null>(null);
  const [preferences, setPreferences] = useState<PlannerPreference | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [actionLoading, setActionLoading] = useState<number | null>(null);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [showPrefModal, setShowPrefModal] = useState<boolean>(false);
  const [selectedDate, setSelectedDate] = useState<string>(new Date().toISOString().split("T")[0]);

  // Preference Form State
  const [prefDailyMins, setPrefDailyMins] = useState<number>(60);
  const [prefWindow, setPrefWindow] = useState<string>("FLEXIBLE");
  const [prefDays, setPrefDays] = useState<string[]>([
    "MONDAY",
    "TUESDAY",
    "WEDNESDAY",
    "THURSDAY",
    "FRIDAY",
    "SATURDAY",
    "SUNDAY",
  ]);
  const [prefGoal, setPrefGoal] = useState<string>("");

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    try {
      const [currentPlan, curProgress, curPref] = await Promise.all([
        plannerService.getCurrentPlan(),
        plannerService.getProgress(),
        plannerService.getPreferences(),
      ]);

      if (currentPlan) {
        setPlan(currentPlan);
      }
      if (curProgress) {
        setProgress(curProgress);
      }
      if (curPref) {
        setPreferences(curPref);
        setPrefDailyMins(curPref.dailyStudyMinutes);
        setPrefWindow(curPref.preferredStudyWindow);
        setPrefDays(curPref.preferredDays);
        setPrefGoal(curPref.weeklyGoal);
      }
    } catch {
      // safe fallback
    } finally {
      setLoading(false);
    }
  }

  async function handleRefresh() {
    setRefreshing(true);
    try {
      const refreshed = await plannerService.refreshPlan();
      if (refreshed) {
        setPlan(refreshed);
        const curProgress = await plannerService.getProgress();
        if (curProgress) setProgress(curProgress);
      }
    } catch {
      // fallback
    } finally {
      setRefreshing(false);
    }
  }

  async function handleCompleteTask(taskId: number) {
    setActionLoading(taskId);
    try {
      const updated = await plannerService.completeTask(taskId);
      if (updated) {
        updateLocalTaskStatus(taskId, "COMPLETED");
      }
    } finally {
      setActionLoading(null);
    }
  }

  async function handleSkipTask(taskId: number) {
    setActionLoading(taskId);
    try {
      const updated = await plannerService.skipTask(taskId);
      if (updated) {
        updateLocalTaskStatus(taskId, "SKIPPED");
      }
    } finally {
      setActionLoading(null);
    }
  }

  async function handleStartTask(taskId: number) {
    setActionLoading(taskId);
    try {
      const updated = await plannerService.startTask(taskId);
      if (updated) {
        updateLocalTaskStatus(taskId, "IN_PROGRESS");
      }
    } finally {
      setActionLoading(null);
    }
  }

  function updateLocalTaskStatus(taskId: number, newStatus: any) {
    if (!plan) return;
    const updatedDays = plan.days.map((day) => ({
      ...day,
      tasks: day.tasks.map((t) => (t.id === taskId ? { ...t, status: newStatus } : t)),
    }));
    setPlan({ ...plan, days: updatedDays });
    plannerService.getProgress().then((p) => p && setProgress(p));
  }

  async function handleSavePreferences(e: React.FormEvent) {
    e.preventDefault();
    const updatedPref: PlannerPreference = {
      dailyStudyMinutes: prefDailyMins,
      preferredStudyWindow: prefWindow as any,
      preferredDays: prefDays,
      weeklyGoal: prefGoal,
    };
    const saved = await plannerService.updatePreferences(updatedPref);
    if (saved) {
      setPreferences(saved);
      setShowPrefModal(false);
      handleRefresh();
    }
  }

  const categoryIcons: Record<string, string> = {
    ROADMAP: "🗺️",
    DSA: "💡",
    SKILL: "⚡",
    QUIZ: "📊",
    MOCK_INTERVIEW: "🎯",
    RESUME: "📄",
    PROJECT: "🛠️",
    AI_COACHING: "✨",
    REVIEW: "📝",
  };

  const todayStr = new Date().toISOString().split("T")[0];
  const activeDay = plan?.days.find((d) => d.date === selectedDate) || plan?.days[0];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-indigo-900 via-indigo-800 to-purple-900 rounded-3xl p-6 sm:p-8 text-white shadow-xl flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <span className="text-xs uppercase tracking-wider font-bold bg-white/20 px-3 py-0.5 rounded-full">
              Phase 15C • Adaptive Weekly Planner
            </span>
            <span className="text-xs font-semibold bg-emerald-500/30 text-emerald-200 border border-emerald-400/40 px-2.5 py-0.5 rounded-full">
              {plan?.source === "AI" ? "✨ AI Generated" : "Deterministic Engine"}
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight">
            Weekly Career Planner
          </h1>
          <p className="text-sm text-indigo-200 mt-1 max-w-2xl leading-relaxed">
            {plan?.weeklyGoal || "Execute tailored daily milestones to close skill gaps and achieve job-readiness."}
          </p>
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto">
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="flex-1 md:flex-none px-4 py-2.5 bg-white/10 hover:bg-white/20 text-white text-xs font-bold rounded-xl border border-white/20 transition flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-50"
          >
            {refreshing ? "⚡ Re-adapting..." : "🔄 Refresh Plan"}
          </button>
          <button
            onClick={() => setShowPrefModal(true)}
            className="flex-1 md:flex-none px-4 py-2.5 bg-white text-gray-900 hover:bg-indigo-50 text-xs font-bold rounded-xl shadow transition flex items-center justify-center gap-1.5 cursor-pointer"
          >
            ⚙️ Preferences
          </button>
        </div>
      </div>

      {/* Progress Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-white rounded-2xl border border-gray-200 p-5 shadow-2xs">
          <span className="text-xs font-bold text-gray-500 uppercase tracking-wide block">
            Weekly Completion
          </span>
          <div className="flex items-baseline gap-2 mt-1">
            <span className="text-2xl sm:text-3xl font-black text-indigo-600">
              {progress?.completionPercentage || 0}%
            </span>
            <span className="text-xs text-gray-400">
              ({progress?.completedTasks || 0}/{progress?.totalTasks || 0} tasks)
            </span>
          </div>
          <div className="w-full bg-gray-100 h-2 rounded-full overflow-hidden mt-3">
            <div
              className="bg-indigo-600 h-2 rounded-full transition-all duration-500"
              style={{ width: `${progress?.completionPercentage || 0}%` }}
            />
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-gray-200 p-5 shadow-2xs">
          <span className="text-xs font-bold text-gray-500 uppercase tracking-wide block">
            Study Time
          </span>
          <div className="flex items-baseline gap-1 mt-1">
            <span className="text-2xl sm:text-3xl font-black text-emerald-600">
              {Math.round((progress?.weeklyMinutesCompleted || 0) / 60 * 10) / 10}h
            </span>
            <span className="text-xs text-gray-400">
              / {Math.round((progress?.weeklyMinutesPlanned || 0) / 60 * 10) / 10}h
            </span>
          </div>
          <span className="text-[11px] text-gray-500 mt-3 block truncate">
            Budget: {preferences?.dailyStudyMinutes || 60}m/day
          </span>
        </div>

        <div className="bg-white rounded-2xl border border-gray-200 p-5 shadow-2xs">
          <span className="text-xs font-bold text-gray-500 uppercase tracking-wide block">
            Current Streak
          </span>
          <div className="flex items-baseline gap-1.5 mt-1">
            <span className="text-2xl sm:text-3xl font-black text-amber-500">
              🔥 {progress?.currentStreak || 0}
            </span>
            <span className="text-xs text-gray-400">days</span>
          </div>
          <span className="text-[11px] text-gray-500 mt-3 block">
            Consistent learning streak
          </span>
        </div>

        <div className="bg-white rounded-2xl border border-gray-200 p-5 shadow-2xs">
          <span className="text-xs font-bold text-gray-500 uppercase tracking-wide block">
            Target Career
          </span>
          <div className="mt-1">
            <span className="text-sm sm:text-base font-bold text-gray-900 block truncate">
              {plan?.targetCareer || "Java Backend Developer"}
            </span>
            <span className="text-xs text-indigo-600 font-semibold">
              Readiness: {plan?.readinessScoreAtGeneration || 0}%
            </span>
          </div>
        </div>
      </div>

      {/* 7-Day Interactive Calendar Tabs */}
      <div className="bg-white rounded-2xl border border-gray-200 p-4 sm:p-6 shadow-sm space-y-4">
        <h2 className="text-sm font-bold uppercase tracking-wider text-gray-500">
          7-Day Weekly Schedule
        </h2>
        <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-2.5">
          {plan?.days.map((d: DailySchedule) => {
            const isToday = d.date === todayStr;
            const isSelected = d.date === selectedDate;
            return (
              <button
                key={d.date}
                onClick={() => setSelectedDate(d.date)}
                className={`p-3 rounded-xl border text-left transition flex flex-col justify-between cursor-pointer ${
                  isSelected
                    ? "bg-indigo-50/90 border-indigo-500 ring-2 ring-indigo-500/20"
                    : isToday
                    ? "bg-white border-indigo-200"
                    : "bg-gray-50/70 border-gray-200 hover:bg-gray-100/80"
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs font-extrabold text-gray-900">
                    {d.dayOfWeek.slice(0, 3)}
                  </span>
                  {isToday && (
                    <span className="text-[9px] font-bold bg-indigo-600 text-white px-1.5 py-0.2 rounded">
                      TODAY
                    </span>
                  )}
                </div>
                <span className="text-[11px] text-gray-500 mt-1">
                  {d.date.slice(5)}
                </span>
                <div className="mt-3">
                  <div className="flex items-center justify-between text-[10px] text-gray-600 mb-1">
                    <span>{d.tasks.length} tasks</span>
                    <span className="font-bold">{d.completionPercentage}%</span>
                  </div>
                  <div className="w-full bg-gray-200 h-1 rounded-full overflow-hidden">
                    <div
                      className="bg-indigo-600 h-1 rounded-full transition-all"
                      style={{ width: `${d.completionPercentage}%` }}
                    />
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Selected Day Task List */}
      <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-sm space-y-6">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 pb-4 border-b border-gray-100">
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-lg font-extrabold text-gray-900">
                {activeDay?.dayOfWeek} Tasks ({activeDay?.date})
              </h3>
              {activeDay?.date === todayStr && (
                <span className="text-[10px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200 px-2 py-0.5 rounded-full">
                  Today's Active Focus
                </span>
              )}
            </div>
            <p className="text-xs text-gray-500 mt-0.5">
              {activeDay?.totalMinutesPlanned || 0} minutes planned • {activeDay?.tasks.filter((t) => t.status === "COMPLETED").length || 0} / {activeDay?.tasks.length || 0} completed
            </p>
          </div>
        </div>

        {activeDay?.tasks.length === 0 ? (
          <div className="py-12 text-center text-gray-400">
            <span className="text-3xl block mb-2">🏖️</span>
            <p className="text-sm font-semibold text-gray-700">Rest & Recovery Day</p>
            <p className="text-xs text-gray-500 mt-1">No intensive milestones scheduled for this date.</p>
          </div>
        ) : (
          <div className="space-y-3.5">
            {activeDay?.tasks.map((task: LearningTask) => (
              <div
                key={task.id}
                className={`p-4 sm:p-5 rounded-2xl border transition flex flex-col md:flex-row justify-between items-start md:items-center gap-4 ${
                  task.status === "COMPLETED"
                    ? "bg-emerald-50/40 border-emerald-200"
                    : task.status === "SKIPPED"
                    ? "bg-gray-50 border-gray-200 opacity-60"
                    : task.status === "IN_PROGRESS"
                    ? "bg-indigo-50/40 border-indigo-300 ring-1 ring-indigo-200"
                    : "bg-white border-gray-200 hover:border-gray-300 shadow-2xs"
                }`}
              >
                <div className="space-y-1.5 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-sm">{categoryIcons[task.category] || "📌"}</span>
                    <span className="text-xs font-bold text-gray-900">{task.title}</span>
                    <span
                      className={`text-[9px] font-bold px-2 py-0.5 rounded border ${
                        task.priority === "CRITICAL"
                          ? "bg-rose-50 text-rose-700 border-rose-200"
                          : task.priority === "HIGH"
                          ? "bg-amber-50 text-amber-700 border-amber-200"
                          : "bg-blue-50 text-blue-700 border-blue-200"
                      }`}
                    >
                      {task.priority}
                    </span>
                    <span className="text-[10px] text-gray-500 font-medium">
                      ⏱️ {task.estimatedMinutes}m
                    </span>
                    {task.status === "COMPLETED" && (
                      <span className="text-[10px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded">
                        ✓ Completed
                      </span>
                    )}
                  </div>

                  <p className="text-xs text-gray-600 leading-relaxed">
                    {task.description}
                  </p>

                  {task.whyItMatters && (
                    <p className="text-[11px] text-indigo-700 font-medium italic">
                      Why: {task.whyItMatters}
                    </p>
                  )}
                </div>

                <div className="flex flex-wrap items-center gap-2 self-end md:self-auto">
                  <Link
                    href={task.actionUrl}
                    className="px-3.5 py-1.5 bg-gray-900 hover:bg-black text-white text-xs font-semibold rounded-xl transition shadow-2xs"
                  >
                    Open Resource →
                  </Link>

                  {task.status !== "COMPLETED" && (
                    <button
                      onClick={() => handleCompleteTask(task.id)}
                      disabled={actionLoading === task.id}
                      className="px-3 py-1.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 text-xs font-bold rounded-xl transition cursor-pointer"
                    >
                      ✓ Complete
                    </button>
                  )}

                  {task.status === "PENDING" && (
                    <button
                      onClick={() => handleSkipTask(task.id)}
                      disabled={actionLoading === task.id}
                      className="px-3 py-1.5 bg-gray-100 hover:bg-gray-200 text-gray-600 text-xs font-medium rounded-xl transition cursor-pointer"
                    >
                      Skip
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Preferences Modal */}
      {showPrefModal && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 study-schedule-modal [color-scheme:light]" style={{ colorScheme: "light" }}>
          <style>{`
            .study-schedule-modal select,
            .study-schedule-modal select option,
            .study-schedule-modal input {
              color: #111827 !important;
              background-color: #ffffff !important;
              color-scheme: light !important;
            }
          `}</style>
          <div className="bg-white rounded-3xl p-6 sm:p-8 max-w-lg w-full shadow-2xl space-y-6 text-gray-900" style={{ colorScheme: "light", color: "#111827", backgroundColor: "#ffffff" }}>
            <div className="flex justify-between items-center pb-4 border-b border-gray-100">
              <h3 className="text-lg font-extrabold text-gray-900" style={{ color: "#111827" }}>
                Study Schedule Preferences
              </h3>
              <button
                onClick={() => setShowPrefModal(false)}
                className="text-gray-400 hover:text-gray-600 text-lg cursor-pointer"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSavePreferences} className="space-y-4 text-xs" style={{ colorScheme: "light" }}>
              <div>
                <label className="block font-bold text-gray-700 mb-1" style={{ color: "#374151" }}>
                  Daily Study Budget (Minutes)
                </label>
                <select
                  value={prefDailyMins}
                  onChange={(e) => setPrefDailyMins(Number(e.target.value))}
                  className="w-full p-2.5 bg-white border border-gray-300 rounded-xl font-medium text-gray-900 focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                  style={{ color: "#111827", backgroundColor: "#ffffff", colorScheme: "light" }}
                >
                  <option value={30} className="text-gray-900 bg-white" style={{ color: "#111827", backgroundColor: "#ffffff", colorScheme: "light" }}>30 Minutes / day</option>
                  <option value={45} className="text-gray-900 bg-white" style={{ color: "#111827", backgroundColor: "#ffffff", colorScheme: "light" }}>45 Minutes / day</option>
                  <option value={60} className="text-gray-900 bg-white" style={{ color: "#111827", backgroundColor: "#ffffff", colorScheme: "light" }}>1 Hour / day (Recommended)</option>
                  <option value={90} className="text-gray-900 bg-white" style={{ color: "#111827", backgroundColor: "#ffffff", colorScheme: "light" }}>1.5 Hours / day</option>
                  <option value={120} className="text-gray-900 bg-white" style={{ color: "#111827", backgroundColor: "#ffffff", colorScheme: "light" }}>2 Hours / day</option>
                  <option value={180} className="text-gray-900 bg-white" style={{ color: "#111827", backgroundColor: "#ffffff", colorScheme: "light" }}>3+ Hours / day</option>
                </select>
              </div>

              <div>
                <label className="block font-bold text-gray-700 mb-1" style={{ color: "#374151" }}>
                  Preferred Study Window
                </label>
                <select
                  value={prefWindow}
                  onChange={(e) => setPrefWindow(e.target.value)}
                  className="w-full p-2.5 bg-white border border-gray-300 rounded-xl font-medium text-gray-900 focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                  style={{ color: "#111827", backgroundColor: "#ffffff", colorScheme: "light" }}
                >
                  <option value="MORNING" className="text-gray-900 bg-white" style={{ color: "#111827", backgroundColor: "#ffffff", colorScheme: "light" }}>Morning (06:00 - 12:00)</option>
                  <option value="AFTERNOON" className="text-gray-900 bg-white" style={{ color: "#111827", backgroundColor: "#ffffff", colorScheme: "light" }}>Afternoon (12:00 - 17:00)</option>
                  <option value="EVENING" className="text-gray-900 bg-white" style={{ color: "#111827", backgroundColor: "#ffffff", colorScheme: "light" }}>Evening (17:00 - 23:00)</option>
                  <option value="FLEXIBLE" className="text-gray-900 bg-white" style={{ color: "#111827", backgroundColor: "#ffffff", colorScheme: "light" }}>Flexible / Anytime</option>
                </select>
              </div>

              <div>
                <label className="block font-bold text-gray-700 mb-1" style={{ color: "#374151" }}>
                  Weekly Focus Goal
                </label>
                <input
                  type="text"
                  value={prefGoal}
                  onChange={(e) => setPrefGoal(e.target.value)}
                  placeholder="e.g. Master SQL JOINs & Complete Mock Interview"
                  className="w-full p-2.5 bg-white border border-gray-300 rounded-xl font-medium text-gray-900 placeholder:text-gray-400 focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                  style={{ color: "#111827", backgroundColor: "#ffffff", colorScheme: "light" }}
                />
              </div>

              <div className="flex justify-end gap-2 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowPrefModal(false)}
                  className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-xl font-semibold cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-bold shadow-md cursor-pointer"
                >
                  Save & Regenerate Plan
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
