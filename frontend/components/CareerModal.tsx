"use client";

import { useEffect } from "react";
import { Career, calculateSkillMatch } from "@/lib/careersData";
import Link from "next/link";

interface CareerModalProps {
  career: Career | null;
  userSkills: string[];
  isOpen: boolean;
  onClose: () => void;
  onSelectCareer: (career: Career) => void;
}

export default function CareerModal({
  career,
  userSkills,
  isOpen,
  onClose,
  onSelectCareer,
}: CareerModalProps) {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
      }
    };
    if (isOpen) {
      document.body.style.overflow = "hidden";
      window.addEventListener("keydown", handleKeyDown);
    }
    return () => {
      document.body.style.overflow = "unset";
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen || !career) return null;

  const matchInfo = calculateSkillMatch(userSkills, career.requiredSkills);

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto shadow-2xl p-6 sm:p-8 relative"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 text-gray-400 hover:text-black w-8 h-8 rounded-full flex items-center justify-center hover:bg-gray-100 transition text-lg"
          aria-label="Close modal"
        >
          ✕
        </button>

        {/* Header */}
        <div className="mb-6 pr-8">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-blue-600 bg-blue-50 px-2.5 py-0.5 rounded-md">
              {career.category}
            </span>
            <span className="text-xs px-2.5 py-0.5 rounded-md bg-gray-100 text-gray-700 font-medium">
              {career.level} Level
            </span>
            <span className="text-xs px-2.5 py-0.5 rounded-md bg-gray-100 text-gray-700 font-medium">
              ⏳ {career.duration}
            </span>
          </div>
          <h2 className="text-2xl sm:text-3xl font-bold text-gray-900">
            {career.title}
          </h2>
          <p className="text-gray-600 mt-2 text-sm sm:text-base leading-relaxed">
            {career.overview}
          </p>
        </div>

        {/* Salary & Metrics Banner */}
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mb-6 p-4 bg-gray-50 rounded-xl border border-gray-100">
          <div>
            <p className="text-xs text-gray-500 font-medium">Avg Compensation</p>
            <p className="text-sm sm:text-base font-semibold text-gray-900">
              {career.salaryRange}
            </p>
          </div>
          <div>
            <p className="text-xs text-gray-500 font-medium">Est. Roadmap Time</p>
            <p className="text-sm sm:text-base font-semibold text-gray-900">
              {career.duration}
            </p>
          </div>
          <div className="col-span-2 sm:col-span-1">
            <p className="text-xs text-gray-500 font-medium">Your Match Score</p>
            <p
              className={`text-sm sm:text-base font-semibold ${
                matchInfo.percentage > 0 ? "text-blue-600" : "text-gray-600"
              }`}
            >
              {matchInfo.percentage}% Match
            </p>
          </div>
        </div>

        {/* Required Skills Analysis */}
        <div className="mb-6">
          <h4 className="text-sm font-bold uppercase tracking-wider text-gray-800 mb-3">
            Required Skills Breakdown
          </h4>
          <div className="flex flex-wrap gap-2">
            {career.requiredSkills.map((skill, idx) => {
              const hasSkill = matchInfo.matchedSkills.includes(skill);
              return (
                <span
                  key={idx}
                  className={`text-xs px-3 py-1.5 rounded-lg font-medium flex items-center gap-1.5 border ${
                    hasSkill
                      ? "bg-emerald-50 text-emerald-800 border-emerald-200"
                      : "bg-gray-50 text-gray-700 border-gray-200"
                  }`}
                >
                  {hasSkill ? "✓ " : "+ "}
                  {skill}
                  {hasSkill && (
                    <span className="text-[10px] bg-emerald-200 text-emerald-900 px-1.5 py-0.2 rounded font-bold">
                      You have
                    </span>
                  )}
                </span>
              );
            })}
          </div>
          {matchInfo.missingSkills.length > 0 && (
            <p className="text-xs text-gray-500 mt-2">
              💡 You will learn {matchInfo.missingSkills.join(", ")} in this
              career roadmap.
            </p>
          )}
        </div>

        {/* Key Technologies */}
        <div className="mb-6">
          <h4 className="text-sm font-bold uppercase tracking-wider text-gray-800 mb-3">
            Core Technologies & Tools
          </h4>
          <div className="flex flex-wrap gap-2">
            {career.technologies.map((tech, idx) => (
              <span
                key={idx}
                className="bg-gray-100 text-gray-800 text-xs px-3 py-1.5 rounded-lg font-medium"
              >
                {tech}
              </span>
            ))}
          </div>
        </div>

        {/* Responsibilities */}
        <div className="mb-8">
          <h4 className="text-sm font-bold uppercase tracking-wider text-gray-800 mb-3">
            Key Responsibilities
          </h4>
          <ul className="space-y-2">
            {career.responsibilities.map((resp, idx) => (
              <li
                key={idx}
                className="text-xs sm:text-sm text-gray-600 flex items-start gap-2"
              >
                <span className="text-blue-500 font-bold">•</span>
                <span>{resp}</span>
              </li>
            ))}
          </ul>
        </div>

        {/* Modal Actions */}
        <div className="flex flex-col sm:flex-row gap-3 pt-4 border-t border-gray-100">
          <button
            onClick={onClose}
            className="py-3 px-5 border border-gray-300 rounded-xl text-gray-700 text-sm font-medium hover:bg-gray-50 transition order-2 sm:order-1"
          >
            Close
          </button>
          <Link
            href={`/roadmap?career=${encodeURIComponent(career.title)}`}
            onClick={() => {
              onSelectCareer(career);
              onClose();
            }}
            className="py-3 px-6 bg-black text-white text-sm font-semibold rounded-xl text-center hover:bg-gray-800 transition shadow-sm order-1 sm:order-2 flex-1"
          >
            Start Roadmap for {career.title} →
          </Link>
        </div>
      </div>
    </div>
  );
}
