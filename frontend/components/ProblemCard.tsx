"use client";

import Link from "next/link";
import { CodingProblem } from "@/lib/problemService";

interface ProblemCardProps {
  problem: CodingProblem;
  onOpen: (problem: CodingProblem) => void;
  onToggleSolved: (problemId: number) => Promise<void>;
  isToggling?: boolean;
}

export default function ProblemCard({
  problem,
  onOpen,
  onToggleSolved,
  isToggling = false,
}: ProblemCardProps) {
  const difficultyColors = {
    EASY: "bg-emerald-50 text-emerald-700 border-emerald-200",
    MEDIUM: "bg-amber-50 text-amber-700 border-amber-200",
    HARD: "bg-rose-50 text-rose-700 border-rose-200",
  };

  return (
    <div
      onClick={() => onOpen(problem)}
      className={`p-4 sm:p-5 rounded-2xl border transition-all duration-200 cursor-pointer flex flex-col justify-between gap-3 shadow-xs hover:shadow-md ${
        problem.solved
          ? "bg-emerald-50/40 border-emerald-200 hover:border-emerald-300"
          : "bg-white border-gray-200 hover:border-gray-300"
      }`}
    >
      <div className="space-y-2">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <span
              className={`text-[11px] font-bold px-2 py-0.5 rounded-full border ${
                difficultyColors[problem.difficulty]
              }`}
            >
              {problem.difficulty}
            </span>
            <span className="text-[11px] font-semibold bg-gray-100 text-gray-700 px-2 py-0.5 rounded-md">
              {problem.topic}
            </span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={(e) => {
                e.stopPropagation();
                onToggleSolved(problem.id);
              }}
              disabled={isToggling}
              className={`w-6 h-6 rounded-md flex items-center justify-center text-xs font-bold transition cursor-pointer ${
                problem.solved
                  ? "bg-emerald-600 text-white shadow-xs"
                  : "border border-gray-300 bg-white hover:border-gray-400"
              }`}
              title={problem.solved ? "Mark as unsolved" : "Mark as solved"}
            >
              {problem.solved ? "✓" : ""}
            </button>
          </div>
        </div>

        <h3
          className={`text-sm sm:text-base font-bold transition leading-snug ${
            problem.solved
              ? "text-emerald-950 line-through opacity-80"
              : "text-gray-900"
          }`}
        >
          {problem.title}
        </h3>

        <p className="text-xs text-gray-500 line-clamp-2 leading-relaxed">
          {problem.description}
        </p>
      </div>

      <div className="flex items-center justify-between pt-2 border-t border-gray-100/80 text-[11px]">
        <div className="flex gap-1 overflow-hidden text-gray-400">
          {problem.tags.slice(0, 2).map((tag, idx) => (
            <span key={idx} className="truncate">
              #{tag}
            </span>
          ))}
        </div>

        <div className="flex items-center gap-2">
          <Link
            href={`/practice/${problem.slug || problem.id}`}
            onClick={(e) => e.stopPropagation()}
            className="text-xs font-bold text-emerald-700 hover:text-emerald-900 bg-emerald-50 px-2.5 py-1 rounded-lg border border-emerald-200"
          >
            Code 💻
          </Link>
        </div>
      </div>
    </div>
  );
}
