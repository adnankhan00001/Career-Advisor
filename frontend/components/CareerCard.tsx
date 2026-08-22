"use client";

import { Career, calculateSkillMatch } from "@/lib/careersData";
import Link from "next/link";

interface CareerCardProps {
  career: Career;
  userSkills: string[];
  intelligenceScore?: number;
  missingSkills?: string[];
  isTargetGoal?: boolean;
  onViewDetails: (career: Career) => void;
  onSelectCareer: (career: Career) => void;
}

export default function CareerCard({
  career,
  userSkills,
  intelligenceScore,
  missingSkills,
  isTargetGoal = false,
  onViewDetails,
  onSelectCareer,
}: CareerCardProps) {
  const matchInfo = calculateSkillMatch(userSkills, career.requiredSkills);
  const displayScore = intelligenceScore !== undefined ? intelligenceScore : matchInfo.percentage;

  const getLevelBadgeColor = (level: Career["level"]) => {
    switch (level) {
      case "Beginner":
        return "bg-emerald-50 text-emerald-700 border-emerald-200";
      case "Intermediate":
        return "bg-blue-50 text-blue-700 border-blue-200";
      case "Advanced":
        return "bg-purple-50 text-purple-700 border-purple-200";
      default:
        return "bg-gray-100 text-gray-700 border-gray-200";
    }
  };

  return (
    <div
      className={`bg-white rounded-2xl border transition-all duration-200 p-6 shadow-sm hover:shadow-md flex flex-col justify-between ${
        isTargetGoal ? "border-blue-400 ring-2 ring-blue-400/20" : "border-gray-200"
      }`}
    >
      <div>
        {/* Top Header / Badges */}
        <div className="flex items-center justify-between gap-2 mb-3">
          <div className="flex items-center gap-1.5">
            <span className="text-xs font-semibold uppercase tracking-wider text-gray-500">
              {career.category}
            </span>
            {isTargetGoal && (
              <span className="bg-blue-600 text-white text-[10px] font-bold px-2 py-0.5 rounded-full">
                Active Goal
              </span>
            )}
          </div>
          <span
            className={`text-xs px-2.5 py-0.5 rounded-full font-medium border ${getLevelBadgeColor(
              career.level
            )}`}
          >
            {career.level}
          </span>
        </div>

        {/* Title */}
        <h3 className="text-xl font-bold text-gray-900 mb-2">{career.title}</h3>

        {/* Description */}
        <p className="text-sm text-gray-600 mb-4 line-clamp-2 leading-relaxed">
          {career.description}
        </p>

        {/* Skill Match Indicator */}
        <div className="mb-4 bg-gray-50 p-3.5 rounded-xl border border-gray-100 space-y-2">
          <div className="flex items-center justify-between text-xs font-medium">
            <span className="text-gray-700 font-semibold">Career Match Score</span>
            <span
              className={`font-bold text-sm ${
                displayScore >= 70
                  ? "text-emerald-600"
                  : displayScore >= 40
                  ? "text-blue-600"
                  : "text-gray-500"
              }`}
            >
              {displayScore}%
            </span>
          </div>
          <div className="w-full bg-gray-200 h-1.5 rounded-full overflow-hidden">
            <div
              className={`h-1.5 rounded-full transition-all duration-500 ${
                displayScore >= 70 ? "bg-emerald-500" : "bg-blue-600"
              }`}
              style={{ width: `${Math.max(displayScore, 4)}%` }}
            />
          </div>

          {missingSkills && missingSkills.length > 0 && (
            <div className="text-[11px] text-gray-500 pt-1 flex items-center justify-between">
              <span>{matchInfo.matchedSkills.length}/{career.requiredSkills.length} skills acquired</span>
              <span className="text-amber-700 font-medium">{missingSkills.length} to learn</span>
            </div>
          )}
        </div>

        {/* Technologies preview */}
        <div className="mb-5">
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
            Key Technologies
          </p>
          <div className="flex flex-wrap gap-1.5">
            {career.technologies.slice(0, 4).map((tech, idx) => (
              <span
                key={idx}
                className="bg-gray-100 text-gray-700 text-xs px-2.5 py-1 rounded-md font-medium"
              >
                {tech}
              </span>
            ))}
            {career.technologies.length > 4 && (
              <span className="text-gray-400 text-xs px-1.5 py-1 font-medium">
                +{career.technologies.length - 4} more
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="pt-4 border-t border-gray-100 flex items-center gap-3">
        <button
          onClick={() => onViewDetails(career)}
          className="flex-1 py-2.5 px-3 border border-gray-300 text-gray-700 text-sm font-medium rounded-lg hover:bg-gray-50 hover:text-black transition cursor-pointer"
        >
          View Details
        </button>

        <Link
          href={`/roadmap?career=${encodeURIComponent(career.title)}`}
          onClick={() => onSelectCareer(career)}
          className="flex-1 py-2.5 px-3 bg-black text-white text-sm font-medium rounded-lg text-center hover:bg-gray-800 transition shadow-sm"
        >
          Start Roadmap →
        </Link>
      </div>
    </div>
  );
}
