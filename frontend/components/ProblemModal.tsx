"use client";

import Link from "next/link";
import { CodingProblem } from "@/lib/problemService";

interface ProblemModalProps {
  problem: CodingProblem | null;
  isOpen: boolean;
  onClose: () => void;
  onToggleSolved: (problemId: number) => Promise<void>;
  isToggling?: boolean;
}

export default function ProblemModal({
  problem,
  isOpen,
  onClose,
  onToggleSolved,
  isToggling = false,
}: ProblemModalProps) {
  if (!isOpen || !problem) return null;

  const difficultyColors = {
    EASY: "bg-emerald-50 text-emerald-700 border-emerald-200",
    MEDIUM: "bg-amber-50 text-amber-700 border-amber-200",
    HARD: "bg-rose-50 text-rose-700 border-rose-200",
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
      <div
        className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-gray-200 shadow-2xl p-6 sm:p-8 space-y-6"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-start justify-between gap-4 pb-4 border-b border-gray-100">
          <div className="space-y-1.5">
            <div className="flex items-center gap-2 flex-wrap">
              <span
                className={`text-xs font-bold px-2.5 py-0.5 rounded-full border ${
                  difficultyColors[problem.difficulty]
                }`}
              >
                {problem.difficulty}
              </span>
              <span className="text-xs font-semibold bg-gray-100 text-gray-700 px-2.5 py-0.5 rounded-full border border-gray-200">
                {problem.topic}
              </span>
              <span className="text-xs text-gray-400 font-medium">
                {problem.category}
              </span>
            </div>
            <h2 className="text-xl sm:text-2xl font-extrabold text-gray-900">
              {problem.title}
            </h2>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-xl text-gray-400 hover:text-gray-900 hover:bg-gray-100 transition cursor-pointer"
            aria-label="Close dialog"
          >
            ✕
          </button>
        </div>

        {/* Tags & Acceptance */}
        <div className="flex items-center justify-between flex-wrap gap-2 text-xs">
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="font-semibold text-gray-500">Tags:</span>
            {problem.tags.map((tag, idx) => (
              <span
                key={idx}
                className="bg-gray-50 text-gray-700 px-2.5 py-0.5 rounded-md border border-gray-200 font-medium"
              >
                {tag}
              </span>
            ))}
          </div>

          {problem.acceptanceRate && (
            <div className="text-gray-500 font-medium">
              Acceptance: <span className="font-bold text-gray-800">{problem.acceptanceRate}</span>
            </div>
          )}
        </div>

        {/* Problem Statement */}
        <div className="space-y-2">
          <h3 className="text-xs font-bold uppercase tracking-wider text-gray-400">
            Problem Summary & Concept
          </h3>
          <div className="bg-gray-50 p-4 rounded-xl border border-gray-100 text-sm text-gray-800 leading-relaxed font-sans">
            {problem.description}
          </div>
        </div>

        {/* Solved Status Callout */}
        <div
          className={`p-4 rounded-xl border flex items-center justify-between text-xs sm:text-sm font-medium ${
            problem.solved
              ? "bg-emerald-50/80 border-emerald-300 text-emerald-950"
              : "bg-gray-50 border-gray-200 text-gray-600"
          }`}
        >
          <div className="flex items-center gap-2.5">
            <span
              className={`w-5 h-5 rounded-md flex items-center justify-center text-xs font-bold ${
                problem.solved
                  ? "bg-emerald-600 text-white"
                  : "border border-gray-300 bg-white"
              }`}
            >
              {problem.solved ? "✓" : ""}
            </span>
            <span>
              {problem.solved
                ? "You have marked this problem as solved!"
                : "You haven't solved this problem yet."}
            </span>
          </div>

          <button
            onClick={() => onToggleSolved(problem.id)}
            disabled={isToggling}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer ${
              problem.solved
                ? "bg-emerald-600 text-white hover:bg-emerald-700"
                : "bg-black text-white hover:bg-gray-800"
            }`}
          >
            {isToggling ? "Updating..." : problem.solved ? "Mark Unsolved" : "Mark Solved ✓"}
          </button>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 pt-3 border-t border-gray-100">
          <Link
            href={`/practice/${problem.slug || problem.id}`}
            onClick={onClose}
            className="py-2.5 px-5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs sm:text-sm font-bold rounded-xl text-center transition shadow-sm"
          >
            Open Code Workspace 💻 →
          </Link>

          {problem.externalUrl && (
            <a
              href={problem.externalUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="py-2.5 px-4 bg-gray-100 text-gray-900 hover:bg-gray-200 text-xs sm:text-sm font-semibold rounded-xl text-center transition flex items-center justify-center gap-1.5 border border-gray-200"
            >
              <span>Practice on External Site</span>
              <span>↗</span>
            </a>
          )}
        </div>
      </div>
    </div>
  );
}
