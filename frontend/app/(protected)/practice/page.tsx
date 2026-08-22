"use client";

import { useState, useEffect, useMemo } from "react";
import {
  problemService,
  CodingProblem,
  ProblemProgressSummary,
} from "@/lib/problemService";
import ProblemCard from "@/components/ProblemCard";
import ProblemModal from "@/components/ProblemModal";

export default function PracticeHubPage() {
  const [problems, setProblems] = useState<CodingProblem[]>([]);
  const [summary, setSummary] = useState<ProblemProgressSummary | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState<string>("ALL");
  const [selectedTopic, setSelectedTopic] = useState<string>("ALL");
  const [selectedDifficulty, setSelectedDifficulty] = useState<string>("ALL");
  const [selectedStatus, setSelectedStatus] = useState<string>("ALL");
  const [activeModalProblem, setActiveModalProblem] = useState<CodingProblem | null>(null);
  const [loading, setLoading] = useState(true);
  const [togglingId, setTogglingId] = useState<number | null>(null);

  useEffect(() => {
    async function loadPracticeData() {
      setLoading(true);
      try {
        const [loadedProblems, loadedSummary] = await Promise.all([
          problemService.getProblems(),
          problemService.getProgressSummary(),
        ]);
        setProblems(loadedProblems);
        setSummary(loadedSummary);
      } catch {
        // Safe fallback
      } finally {
        setLoading(false);
      }
    }

    loadPracticeData();
  }, []);

  const categories = [
    { key: "ALL", label: "All Subjects" },
    { key: "DSA", label: "DSA ⚡" },
    { key: "JAVA", label: "Core Java" },
    { key: "DBMS", label: "DBMS & SQL" },
    { key: "OS", label: "Operating Systems" },
    { key: "SPRING_BOOT", label: "Spring Boot" },
    { key: "OOP", label: "OOP & Design" },
    { key: "CN", label: "Networks" },
  ];

  // Extract unique topics from loaded problems
  const topics = useMemo(() => {
    const topicSet = new Set<string>();
    problems.forEach((p) => {
      if (selectedCategory === "ALL" || p.category === selectedCategory) {
        topicSet.add(p.topic);
      }
    });
    return ["ALL", ...Array.from(topicSet)];
  }, [problems, selectedCategory]);

  const difficulties = ["ALL", "EASY", "MEDIUM", "HARD"];
  const statuses = ["ALL", "SOLVED", "UNSOLVED"];

  // Filtered problems
  const filteredProblems = useMemo(() => {
    return problems.filter((p) => {
      // Category filter
      if (selectedCategory !== "ALL" && p.category !== selectedCategory) {
        return false;
      }

      // Topic filter
      if (selectedTopic !== "ALL" && p.topic.toLowerCase() !== selectedTopic.toLowerCase()) {
        return false;
      }

      // Difficulty filter
      if (selectedDifficulty !== "ALL" && p.difficulty !== selectedDifficulty) {
        return false;
      }

      // Solved status filter
      if (selectedStatus === "SOLVED" && !p.solved) return false;
      if (selectedStatus === "UNSOLVED" && p.solved) return false;

      // Search query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchTitle = p.title.toLowerCase().includes(q);
        const matchTopic = p.topic.toLowerCase().includes(q);
        const matchDesc = p.description.toLowerCase().includes(q);
        const matchTag = p.tags.some((t) => t.toLowerCase().includes(q));
        return matchTitle || matchTopic || matchDesc || matchTag;
      }

      return true;
    });
  }, [problems, selectedCategory, selectedTopic, selectedDifficulty, selectedStatus, searchQuery]);

  const handleToggleSolved = async (problemId: number) => {
    setTogglingId(problemId);

    // Optimistic UI update
    setProblems((prev) =>
      prev.map((p) => (p.id === problemId ? { ...p, solved: !p.solved } : p))
    );

    if (activeModalProblem && activeModalProblem.id === problemId) {
      setActiveModalProblem((prev) =>
        prev ? { ...prev, solved: !prev.solved } : null
      );
    }

    try {
      const updated = await problemService.toggleProblemSolved(problemId);
      if (updated) {
        setProblems((prev) =>
          prev.map((p) => (p.id === problemId ? { ...p, solved: updated.solved } : p))
        );
        if (activeModalProblem && activeModalProblem.id === problemId) {
          setActiveModalProblem(updated);
        }
      }

      // Refresh summary stats
      const freshSummary = await problemService.getProgressSummary();
      if (freshSummary) {
        setSummary(freshSummary);
      }
    } catch {
      // Revert on error
      setProblems((prev) =>
        prev.map((p) => (p.id === problemId ? { ...p, solved: !p.solved } : p))
      );
    } finally {
      setTogglingId(null);
    }
  };

  const handleResetFilters = () => {
    setSearchQuery("");
    setSelectedCategory("ALL");
    setSelectedTopic("ALL");
    setSelectedDifficulty("ALL");
    setSelectedStatus("ALL");
  };

  const solvedCount = summary?.solvedProblems ?? problems.filter((p) => p.solved).length;
  const totalCount = summary?.totalProblems ?? problems.length;
  const completionPercent =
    summary?.completionPercentage ??
    (totalCount > 0 ? Math.round((solvedCount / totalCount) * 100) : 0);

  return (
    <div className="max-w-7xl mx-auto space-y-8 pb-16">
      {/* Hero Header */}
      <div className="bg-gradient-to-r from-gray-900 via-gray-800 to-black text-white p-6 sm:p-8 rounded-2xl shadow-lg relative overflow-hidden flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
        <div className="max-w-2xl">
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs uppercase tracking-widest text-emerald-400 font-bold block">
              Interview Preparation
            </span>
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
            Practice Hub & Problem Solving 💡
          </h1>
          <p className="text-gray-300 text-sm mt-1 leading-relaxed">
            Master high-frequency Data Structures, Algorithms, and Core CS interview
            topics with structured difficulty tiers and tracked completion.
          </p>
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto">
          <div className="bg-white/10 backdrop-blur-md px-5 py-3 rounded-xl border border-white/10 text-center w-full md:w-auto">
            <span className="text-[11px] text-gray-300 block uppercase font-bold tracking-wider">
              Solved Progress
            </span>
            <span className="text-xl sm:text-2xl font-extrabold text-white">
              {solvedCount} / {totalCount}
            </span>
            <span className="text-xs text-emerald-400 font-bold ml-1">
              ({completionPercent}%)
            </span>
          </div>
        </div>
      </div>

      {/* Progress Metrics Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Completion */}
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm">
          <span className="text-xs text-gray-500 font-medium block">
            Overall Completion
          </span>
          <div className="flex items-baseline gap-2 mt-1">
            <span className="text-2xl font-bold text-gray-900">
              {completionPercent}%
            </span>
            <span className="text-xs text-gray-500">
              ({solvedCount}/{totalCount})
            </span>
          </div>
          <div className="w-full bg-gray-100 h-1.5 rounded-full overflow-hidden mt-3">
            <div
              className="bg-emerald-500 h-1.5 rounded-full transition-all duration-500"
              style={{ width: `${completionPercent}%` }}
            />
          </div>
        </div>

        {/* Easy Tier */}
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm">
          <span className="text-xs text-gray-500 font-medium block">
            Easy Problems
          </span>
          <div className="flex items-baseline gap-2 mt-1">
            <span className="text-2xl font-bold text-emerald-600">
              {summary?.easySolved ?? 0}
            </span>
            <span className="text-xs text-gray-500">
              / {summary?.easyTotal ?? 0} solved
            </span>
          </div>
          <div className="w-full bg-gray-100 h-1.5 rounded-full overflow-hidden mt-3">
            <div
              className="bg-emerald-500 h-1.5 rounded-full transition-all duration-500"
              style={{
                width: `${
                  (summary?.easyTotal ?? 0) > 0
                    ? Math.round(
                        ((summary?.easySolved ?? 0) / (summary?.easyTotal ?? 1)) *
                          100
                      )
                    : 0
                }%`,
              }}
            />
          </div>
        </div>

        {/* Medium Tier */}
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm">
          <span className="text-xs text-gray-500 font-medium block">
            Medium Problems
          </span>
          <div className="flex items-baseline gap-2 mt-1">
            <span className="text-2xl font-bold text-amber-600">
              {summary?.mediumSolved ?? 0}
            </span>
            <span className="text-xs text-gray-500">
              / {summary?.mediumTotal ?? 0} solved
            </span>
          </div>
          <div className="w-full bg-gray-100 h-1.5 rounded-full overflow-hidden mt-3">
            <div
              className="bg-amber-500 h-1.5 rounded-full transition-all duration-500"
              style={{
                width: `${
                  (summary?.mediumTotal ?? 0) > 0
                    ? Math.round(
                        ((summary?.mediumSolved ?? 0) /
                          (summary?.mediumTotal ?? 1)) *
                          100
                      )
                    : 0
                }%`,
              }}
            />
          </div>
        </div>

        {/* Hard Tier */}
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm">
          <span className="text-xs text-gray-500 font-medium block">
            Hard Problems
          </span>
          <div className="flex items-baseline gap-2 mt-1">
            <span className="text-2xl font-bold text-rose-600">
              {summary?.hardSolved ?? 0}
            </span>
            <span className="text-xs text-gray-500">
              / {summary?.hardTotal ?? 0} solved
            </span>
          </div>
          <div className="w-full bg-gray-100 h-1.5 rounded-full overflow-hidden mt-3">
            <div
              className="bg-rose-500 h-1.5 rounded-full transition-all duration-500"
              style={{
                width: `${
                  (summary?.hardTotal ?? 0) > 0
                    ? Math.round(
                        ((summary?.hardSolved ?? 0) / (summary?.hardTotal ?? 1)) *
                          100
                      )
                    : 0
                }%`,
              }}
            />
          </div>
        </div>
      </div>

      {/* Category Pills Header */}
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-xs font-bold text-gray-500 uppercase tracking-wider mr-1">
          Subject:
        </span>
        {categories.map((cat) => (
          <button
            key={cat.key}
            onClick={() => {
              setSelectedCategory(cat.key);
              setSelectedTopic("ALL");
            }}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition cursor-pointer ${
              selectedCategory === cat.key
                ? "bg-black text-white shadow-sm"
                : "bg-white text-gray-700 border border-gray-200 hover:bg-gray-100"
            }`}
          >
            {cat.label}
          </button>
        ))}
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm space-y-4">
        <div className="flex flex-col md:flex-row gap-4 justify-between items-stretch md:items-center">
          {/* Search Input */}
          <div className="relative flex-1">
            <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gray-400">
              🔍
            </span>
            <input
              type="text"
              placeholder="Search problems by title, topic, or tag (e.g. Two Sum, Sliding Window, Tree)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-300 rounded-xl text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-black focus:bg-white transition"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery("")}
                className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-xs text-gray-400 hover:text-black cursor-pointer"
              >
                Clear
              </button>
            )}
          </div>

          {/* Difficulty Filter */}
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
              Difficulty:
            </span>
            <div className="flex gap-1 bg-gray-100 p-1 rounded-xl">
              {difficulties.map((diff) => (
                <button
                  key={diff}
                  onClick={() => setSelectedDifficulty(diff)}
                  className={`text-xs px-2.5 py-1 rounded-lg font-semibold transition cursor-pointer ${
                    selectedDifficulty === diff
                      ? "bg-white text-black shadow-xs"
                      : "text-gray-600 hover:text-black"
                  }`}
                >
                  {diff}
                </button>
              ))}
            </div>
          </div>

          {/* Status Filter */}
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
              Status:
            </span>
            <div className="flex gap-1 bg-gray-100 p-1 rounded-xl">
              {statuses.map((st) => (
                <button
                  key={st}
                  onClick={() => setSelectedStatus(st)}
                  className={`text-xs px-2.5 py-1 rounded-lg font-semibold transition cursor-pointer ${
                    selectedStatus === st
                      ? "bg-white text-black shadow-xs"
                      : "text-gray-600 hover:text-black"
                  }`}
                >
                  {st}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Topic Pills */}
        <div className="flex flex-wrap items-center gap-1.5 pt-2 border-t border-gray-100">
          <span className="text-xs font-bold text-gray-500 uppercase tracking-wider mr-1">
            Topic:
          </span>
          {topics.map((top) => (
            <button
              key={top}
              onClick={() => setSelectedTopic(top)}
              className={`text-xs px-2.5 py-1 rounded-lg font-medium transition cursor-pointer ${
                selectedTopic === top
                  ? "bg-gray-900 text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {top}
            </button>
          ))}
        </div>
      </div>

      {/* Results Header */}
      <div className="flex items-center justify-between px-1">
        <p className="text-xs sm:text-sm font-semibold text-gray-700">
          Showing {filteredProblems.length} problem
          {filteredProblems.length === 1 ? "" : "s"}
        </p>
        {(searchQuery ||
          selectedCategory !== "ALL" ||
          selectedTopic !== "ALL" ||
          selectedDifficulty !== "ALL" ||
          selectedStatus !== "ALL") && (
          <button
            onClick={handleResetFilters}
            className="text-xs text-blue-600 font-semibold hover:underline cursor-pointer"
          >
            Reset Filters
          </button>
        )}
      </div>

      {/* Problems Grid */}
      {filteredProblems.length === 0 ? (
        <div className="bg-white rounded-2xl border border-gray-200 p-12 text-center shadow-sm">
          <div className="text-4xl mb-3">🔍</div>
          <h3 className="text-lg font-bold text-gray-900">No problems found</h3>
          <p className="text-sm text-gray-500 max-w-md mx-auto mt-1 mb-6">
            We couldn't find any problem matching your search query and filters.
          </p>
          <button
            onClick={handleResetFilters}
            className="bg-black text-white px-5 py-2 rounded-xl text-sm font-medium hover:bg-gray-800 transition cursor-pointer"
          >
            Clear Filters
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-5">
          {filteredProblems.map((problem) => (
            <ProblemCard
              key={problem.id}
              problem={problem}
              onOpen={(p) => setActiveModalProblem(p)}
              onToggleSolved={handleToggleSolved}
              isToggling={togglingId === problem.id}
            />
          ))}
        </div>
      )}

      {/* Problem Detail Modal */}
      <ProblemModal
        problem={activeModalProblem}
        isOpen={Boolean(activeModalProblem)}
        onClose={() => setActiveModalProblem(null)}
        onToggleSolved={handleToggleSolved}
        isToggling={togglingId === activeModalProblem?.id}
      />
    </div>
  );
}
