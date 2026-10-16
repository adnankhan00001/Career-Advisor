"use client";

import { useState } from "react";
import Link from "next/link";
import { CareerIntelligence, CareerGap, DimensionScore } from "@/lib/careerIntelligenceService";
import { AiActionPlan, AiActionPlanItem, refreshAiActionPlan } from "@/lib/aiService";

interface CareerIntelligenceSectionProps {
  intelligence: CareerIntelligence | null;
  initialActionPlan?: AiActionPlan | null;
  loading?: boolean;
}

export default function CareerIntelligenceSection({
  intelligence,
  initialActionPlan = null,
  loading = false,
}: CareerIntelligenceSectionProps) {
  const [actionPlan, setActionPlan] = useState<AiActionPlan | null>(initialActionPlan);
  const [refreshing, setRefreshing] = useState<boolean>(false);

  if (loading) {
    return (
      <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm animate-pulse space-y-4">
        <div className="h-6 bg-gray-200 rounded-md w-1/3"></div>
        <div className="h-4 bg-gray-100 rounded-md w-3/4"></div>
        <div className="grid grid-cols-2 md:grid-cols-6 gap-3 pt-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-20 bg-gray-100 rounded-xl"></div>
          ))}
        </div>
      </div>
    );
  }

  if (!intelligence) return null;

  const currentPlan = actionPlan || initialActionPlan;

  const handleRefreshPlan = async () => {
    setRefreshing(true);
    try {
      const fresh = await refreshAiActionPlan();
      if (fresh) {
        setActionPlan(fresh);
      }
    } catch {
      // safe fallback
    } finally {
      setRefreshing(false);
    }
  };

  const stateBadges: Record<string, { bg: string; text: string; border: string }> = {
    JOB_READY: { bg: "bg-emerald-50", text: "text-emerald-700", border: "border-emerald-200" },
    ADVANCED: { bg: "bg-blue-50", text: "text-blue-700", border: "border-blue-200" },
    DEVELOPING: { bg: "bg-indigo-50", text: "text-indigo-700", border: "border-indigo-200" },
    FOUNDATIONAL: { bg: "bg-amber-50", text: "text-amber-700", border: "border-amber-200" },
    BEGINNER: { bg: "bg-rose-50", text: "text-rose-700", border: "border-rose-200" },
  };

  const categoryIcons: Record<string, string> = {
    ROADMAP: "🗺️",
    DSA: "💡",
    RESUME: "📄",
    INTERVIEW: "🎯",
    ASSESSMENT: "📊",
    SKILL: "⚡",
  };

  const badge = stateBadges[intelligence.readinessState] || stateBadges.BEGINNER;
  const dimensionList: DimensionScore[] = Object.values(intelligence.dimensionScores || {});

  return (
    <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-sm space-y-7">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 pb-5 border-b border-gray-100">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs uppercase tracking-wider text-indigo-600 font-bold">
              Unified Career Intelligence Engine
            </span>
            <span className={`text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${badge.bg} ${badge.text} ${badge.border}`}>
              {intelligence.readinessStateDisplayName} ({intelligence.overallReadinessScore}%)
            </span>
          </div>
          <h2 className="text-lg sm:text-xl font-extrabold text-gray-900 leading-tight">
            Target Track: {intelligence.targetCareer}
          </h2>
          <p className="text-xs sm:text-sm text-gray-600 mt-1 leading-relaxed max-w-3xl">
            {intelligence.summary}
          </p>
        </div>

        <div className="bg-gradient-to-br from-indigo-50 to-purple-50 border border-indigo-100 rounded-2xl p-4 flex items-center gap-4 min-w-[200px] justify-between shadow-2xs">
          <div>
            <span className="text-[11px] text-indigo-700 font-bold uppercase block">
              Readiness Score
            </span>
            <span className="text-3xl font-black text-gray-900">
              {intelligence.overallReadinessScore}%
            </span>
          </div>
          <div className="w-12 h-12 rounded-full border-4 border-indigo-500 flex items-center justify-center font-bold text-xs text-indigo-700 bg-white shadow-2xs">
            {intelligence.overallReadinessScore}%
          </div>
        </div>
      </div>

      {/* 6 Dimensions Breakdown */}
      <div>
        <h3 className="text-xs font-bold uppercase tracking-wider text-gray-500 mb-3">
          6-Dimensional Readiness Breakdown
        </h3>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          {dimensionList.map((dim) => (
            <div
              key={dim.dimension}
              className="p-3.5 bg-gray-50/80 rounded-xl border border-gray-200 flex flex-col justify-between"
            >
              <div>
                <span className="text-[11px] font-semibold text-gray-600 block truncate">
                  {dim.displayName}
                </span>
                <div className="flex items-baseline gap-1 mt-1">
                  <span className="text-lg font-bold text-gray-900">
                    {dim.score}%
                  </span>
                  <span className="text-[10px] text-gray-400">
                    ({dim.weightPercentage}% wt)
                  </span>
                </div>
              </div>
              <div className="w-full bg-gray-200 h-1.5 rounded-full overflow-hidden mt-2">
                <div
                  className="bg-indigo-600 h-1.5 rounded-full transition-all duration-500"
                  style={{ width: `${dim.score}%` }}
                />
              </div>
              <span className="text-[10px] text-gray-500 mt-1.5 truncate">
                {dim.detailMetric}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Strengths & Growth Areas + Critical Skill Gaps */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-1">
        {/* Strengths & Weaknesses */}
        <div className="p-4 bg-gray-50/80 rounded-xl border border-gray-200 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-700 uppercase tracking-wide">
              Top Strength & Growth
            </span>
          </div>
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                Strongest:
              </span>
              <span className="text-xs text-gray-800 font-medium">
                {intelligence.strongestDimensions.length > 0 ? intelligence.strongestDimensions.join(", ") : "Building initial competency"}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-rose-700 bg-rose-50 px-2 py-0.5 rounded-md border border-rose-200">
                Growth Area:
              </span>
              <span className="text-xs text-gray-800 font-medium">
                {intelligence.weakestDimensions.length > 0 ? intelligence.weakestDimensions.join(", ") : "Milestone execution"}
              </span>
            </div>
          </div>
        </div>

        {/* Critical Skill Gaps */}
        <div className="p-4 bg-gray-50/80 rounded-xl border border-gray-200 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-700 uppercase tracking-wide">
              Target Skill Gaps ({intelligence.skillGaps.length})
            </span>
          </div>
          <div className="flex flex-wrap gap-1.5">
            {intelligence.skillGaps.length > 0 ? (
              intelligence.skillGaps.slice(0, 5).map((gap: CareerGap, idx: number) => (
                <span
                  key={idx}
                  className={`text-[11px] font-medium px-2.5 py-1 rounded-lg border ${
                    gap.priority === "CRITICAL"
                      ? "bg-rose-50 text-rose-700 border-rose-200 font-semibold"
                      : gap.priority === "HIGH"
                      ? "bg-amber-50 text-amber-700 border-amber-200"
                      : "bg-gray-100 text-gray-700 border-gray-200"
                  }`}
                  title={gap.evidence.join(" • ")}
                >
                  {gap.skill} <span className="text-[9px] opacity-75">({gap.priority})</span>
                </span>
              ))
            ) : (
              <span className="text-xs text-emerald-600 font-medium">
                ✓ All core target skills verified in portfolio!
              </span>
            )}
          </div>
        </div>
      </div>

      {/* AI Career Action Plan Section */}
      <div className="space-y-4 pt-2 border-t border-gray-100">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-base font-extrabold text-gray-900">
                {currentPlan?.title || "AI Career Action Plan"}
              </span>
              <span
                className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${
                  currentPlan?.source === "AI_GENERATED"
                    ? "bg-purple-50 text-purple-700 border-purple-200"
                    : "bg-gray-100 text-gray-700 border-gray-200"
                }`}
              >
                {currentPlan?.source === "AI_GENERATED" ? "✨ AI Coach Guided" : "Deterministic Engine"}
              </span>
            </div>
            <p className="text-xs text-gray-600 mt-1">
              {currentPlan?.summary || "Targeted actionable steps prioritizing your highest-value career milestones."}
            </p>
          </div>

          <button
            onClick={handleRefreshPlan}
            disabled={refreshing}
            className="self-start sm:self-auto px-3.5 py-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-semibold rounded-xl border border-indigo-200 transition flex items-center gap-1.5 disabled:opacity-50 cursor-pointer shadow-2xs"
          >
            {refreshing ? (
              <>
                <span className="inline-block animate-spin">⚡</span>
                <span>Generating Fresh AI Plan...</span>
              </>
            ) : (
              <>
                <span>✨</span>
                <span>Refresh AI Plan</span>
              </>
            )}
          </button>
        </div>

        {/* Motivational Note / Timeframe Banner */}
        {currentPlan && (
          <div className="p-3 bg-gradient-to-r from-indigo-50/60 to-purple-50/60 rounded-xl border border-indigo-100/80 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 text-xs">
            <div className="flex items-center gap-2 text-indigo-900">
              <span className="font-bold">🎯 Focus Timeframe:</span>
              <span>{currentPlan.estimatedTimeframe || "2-4 Weeks"}</span>
            </div>
            <div className="text-gray-700 italic text-[11px]">
              "{currentPlan.motivationalNote}"
            </div>
          </div>
        )}

        {/* Action Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3.5">
          {(currentPlan?.actions && currentPlan.actions.length > 0
            ? currentPlan.actions
            : intelligence.prioritizedActions
          ).slice(0, 3).map((act: any) => (
            <div
              key={act.id}
              className="p-4 bg-gray-50/90 hover:bg-gray-50 rounded-xl border border-gray-200 flex flex-col justify-between gap-3.5 shadow-2xs hover:shadow-xs transition"
            >
              <div className="space-y-2">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-sm">{categoryIcons[act.category] || "📌"}</span>
                  <div className="flex items-center gap-1.5">
                    <span
                      className={`text-[9px] font-bold px-2 py-0.5 rounded border ${
                        act.priority === "CRITICAL"
                          ? "bg-rose-50 text-rose-700 border-rose-200"
                          : act.priority === "HIGH"
                          ? "bg-amber-50 text-amber-700 border-amber-200"
                          : "bg-blue-50 text-blue-700 border-blue-200"
                      }`}
                    >
                      {act.priority}
                    </span>
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-indigo-50 text-indigo-700 border border-indigo-200">
                      +{act.estimatedImpactPoints || 10} pts
                    </span>
                  </div>
                </div>

                <h4 className="text-xs font-bold text-gray-900 leading-snug">
                  {act.title}
                </h4>
                <p className="text-[11px] text-gray-600 leading-normal">
                  {act.description}
                </p>

                {act.whyItMatters && (
                  <p className="text-[10px] text-indigo-700 font-medium leading-snug">
                    <span className="font-bold">Why:</span> {act.whyItMatters}
                  </p>
                )}

                {act.estimatedEffort && (
                  <span className="text-[10px] text-gray-500 block">
                    ⏱️ Estimated Effort: {act.estimatedEffort}
                  </span>
                )}
              </div>

              <Link
                href={act.actionUrl}
                className="w-full py-1.5 px-3 bg-white hover:bg-gray-900 hover:text-white text-gray-900 text-xs font-semibold rounded-lg border border-gray-300 text-center transition shadow-2xs"
              >
                Execute Action →
              </Link>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
