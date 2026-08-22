"use client";

import Link from "next/link";
import { PersonalizedIntelligence, RecommendationItem } from "@/lib/recommendationService";

interface PersonalizedPlanProps {
  intelligence: PersonalizedIntelligence | null;
  loading?: boolean;
}

export default function PersonalizedPlan({ intelligence, loading = false }: PersonalizedPlanProps) {
  if (loading) {
    return (
      <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm animate-pulse space-y-4">
        <div className="h-6 bg-gray-200 rounded-md w-1/3"></div>
        <div className="h-4 bg-gray-100 rounded-md w-3/4"></div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          <div className="h-28 bg-gray-100 rounded-xl"></div>
          <div className="h-28 bg-gray-100 rounded-xl"></div>
        </div>
      </div>
    );
  }

  if (!intelligence) return null;

  const typeIcons: Record<string, string> = {
    ROADMAP: "🗺️",
    SKILL_GAP: "⚡",
    PRACTICE: "💡",
    INTERVIEW: "🎯",
    CAREER: "🧭",
    ONBOARDING: "🚀",
  };

  const priorityBadges: Record<string, string> = {
    HIGH: "bg-rose-50 text-rose-700 border-rose-200",
    MEDIUM: "bg-blue-50 text-blue-700 border-blue-200",
    LOW: "bg-gray-100 text-gray-700 border-gray-200",
  };

  return (
    <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-sm space-y-6">
      {/* Header & Overall Readiness Score */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 pb-5 border-b border-gray-100">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs uppercase tracking-wider text-blue-600 font-bold">
              AI & Rule-Driven Intelligence
            </span>
            <span className="bg-emerald-50 text-emerald-700 text-[10px] font-bold px-2 py-0.5 rounded-full border border-emerald-200">
              {intelligence.userState} TIER
            </span>
          </div>
          <h2 className="text-lg sm:text-xl font-extrabold text-gray-900 leading-tight">
            Your Personalized Career & Interview Plan
          </h2>
          <p className="text-xs sm:text-sm text-gray-600 mt-1 leading-relaxed">
            {intelligence.summaryHeadline}
          </p>
        </div>

        <div className="bg-gray-50 border border-gray-200 rounded-2xl p-3.5 sm:px-5 flex items-center gap-4 min-w-[200px] justify-between">
          <div>
            <span className="text-[11px] text-gray-500 font-bold uppercase block">
              Market Readiness
            </span>
            <span className="text-2xl font-black text-gray-900">
              {intelligence.overallReadinessScore}%
            </span>
          </div>
          <div className="w-12 h-12 rounded-full border-4 border-emerald-500 flex items-center justify-center font-bold text-xs text-emerald-700 bg-emerald-50">
            {intelligence.overallReadinessScore}%
          </div>
        </div>
      </div>

      {/* Action Plan Grid */}
      <div className="space-y-3">
        <h3 className="text-xs font-bold uppercase tracking-wider text-gray-500">
          Prioritized Action Steps ({intelligence.actionPlan.length})
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {intelligence.actionPlan.map((item: RecommendationItem) => (
            <div
              key={item.id}
              className="p-5 bg-gray-50/80 hover:bg-gray-50 rounded-2xl border border-gray-200 transition-all flex flex-col justify-between gap-4 shadow-2xs hover:shadow-xs"
            >
              <div className="space-y-2">
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="text-base">{typeIcons[item.type] || "📌"}</span>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-md border ${
                        priorityBadges[item.priority] || priorityBadges.LOW
                      }`}
                    >
                      {item.priority} PRIORITY
                    </span>
                  </div>
                  <span className="text-[11px] font-semibold text-gray-400 uppercase">
                    {item.type.replace("_", " ")}
                  </span>
                </div>

                <h4 className="text-sm sm:text-base font-bold text-gray-900 leading-snug">
                  {item.title}
                </h4>

                <p className="text-xs text-gray-600 leading-relaxed">
                  {item.description}
                </p>

                {item.reason && (
                  <div className="text-[11px] text-gray-500 bg-white p-2.5 rounded-xl border border-gray-200/80">
                    <span className="font-bold text-gray-700">Rationale: </span>
                    <span>{item.reason}</span>
                  </div>
                )}
              </div>

              <div className="pt-2 border-t border-gray-200/60 flex items-center justify-between">
                <Link
                  href={item.actionUrl}
                  className="w-full text-center py-2 px-4 bg-black text-white text-xs font-bold rounded-xl hover:bg-gray-800 transition shadow-xs"
                >
                  {item.actionText} →
                </Link>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
