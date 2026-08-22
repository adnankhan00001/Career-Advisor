"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  interviewService,
  InterviewCategory,
  Difficulty,
  InterviewSummary,
  CATEGORY_LABELS,
} from "@/lib/interviewService";

export default function MockInterviewSetupPage() {
  const router = useRouter();
  const [category, setCategory] = useState<InterviewCategory>("JAVA");
  const [difficulty, setDifficulty] = useState<Difficulty>("MEDIUM");
  const [durationMinutes, setDurationMinutes] = useState<number>(15);
  const [questionCount, setQuestionCount] = useState<number>(5);

  const [summary, setSummary] = useState<InterviewSummary | null>(null);
  const [starting, setStarting] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadSummary() {
      try {
        const data = await interviewService.getInterviewSummary();
        setSummary(data);
      } catch {
        // Fallback for new user
      } finally {
        setLoading(false);
      }
    }
    loadSummary();
  }, []);

  const handleStartInterview = async () => {
    setStarting(true);
    setError(null);
    try {
      const session = await interviewService.startInterview({
        category,
        difficulty,
        durationMinutes,
        questionCount,
      });
      if (session && session.id) {
        router.push(`/mock-interview/${session.id}`);
      }
    } catch {
      setError("Failed to initialize mock interview session. Please try again.");
      setStarting(false);
    }
  };

  const categoryOptions: {
    key: InterviewCategory;
    title: string;
    icon: string;
    desc: string;
  }[] = [
    {
      key: "JAVA",
      title: "Core Java",
      icon: "☕",
      desc: "String pool, HashMap internals, exceptions, concurrency & JVM memory.",
    },
    {
      key: "OOP",
      title: "OOP Design",
      icon: "🧩",
      desc: "SOLID principles, encapsulation, polymorphism, and composition.",
    },
    {
      key: "DBMS",
      title: "DBMS & SQL",
      icon: "🗄️",
      desc: "ACID, B+ Trees, 3NF Normalization, SQL Joins, and Optimistic locking.",
    },
    {
      key: "SPRING_BOOT",
      title: "Spring Boot",
      icon: "🌱",
      desc: "IoC/DI, Bean scopes, Security filter chain, @Transactional & JPA.",
    },
    {
      key: "DSA",
      title: "Data Structures",
      icon: "⚡",
      desc: "Binary search, Trees, Graphs, DP substructure, Heaps & complexity.",
    },
    {
      key: "OS",
      title: "Operating Systems",
      icon: "💻",
      desc: "Coffman deadlock, process vs thread, virtual memory & paging.",
    },
    {
      key: "CN",
      title: "Computer Networks",
      icon: "🌐",
      desc: "TCP 3-way handshake, HTTP/3 QUIC, and DNS resolution.",
    },
  ];

  return (
    <div className="max-w-5xl mx-auto space-y-8 pb-16">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-50 text-purple-700 border border-purple-200">
              Live Mock Interview
            </span>
            <span className="text-xs text-gray-500 font-medium">
              Timed Technical Assessment
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            Technical Mock Interview 🎯
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Simulate real tech interview rounds, test conceptual depth under timed conditions, and identify skill gaps.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Link
            href="/mock-interview/history"
            className="text-xs font-semibold px-4 py-2.5 bg-white hover:bg-gray-50 text-gray-700 border border-gray-200 rounded-xl transition shadow-xs flex items-center gap-1.5"
          >
            📋 Interview History
          </Link>
        </div>
      </div>

      {/* Summary Stats Banner (if user has completed attempts) */}
      {!loading && summary && summary.totalInterviews > 0 && (
        <div className="bg-gradient-to-r from-gray-950 via-gray-900 to-gray-800 text-white rounded-2xl p-6 shadow-md">
          <div className="flex items-center justify-between flex-wrap gap-4 border-b border-gray-800 pb-4 mb-4">
            <div>
              <span className="text-xs text-purple-300 font-bold uppercase tracking-wider block">
                Interview Readiness Benchmark
              </span>
              <h3 className="text-lg font-extrabold text-white mt-0.5">
                Average Score: {summary.averageScore}% · Best: {summary.bestScore}%
              </h3>
            </div>
            <div className="flex gap-2">
              <span className="text-xs bg-gray-800 text-emerald-300 px-3 py-1 rounded-lg border border-gray-700 font-medium">
                Strong: {summary.strongestCategory}
              </span>
              <span className="text-xs bg-gray-800 text-amber-300 px-3 py-1 rounded-lg border border-gray-700 font-medium">
                Focus: {summary.weakestCategory}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-center">
            <div className="bg-gray-900/60 p-3 rounded-xl border border-gray-800">
              <span className="text-xs text-gray-400 block">Total Attempts</span>
              <span className="text-lg font-bold text-white">{summary.totalInterviews}</span>
            </div>
            <div className="bg-gray-900/60 p-3 rounded-xl border border-gray-800">
              <span className="text-xs text-gray-400 block">Completed</span>
              <span className="text-lg font-bold text-emerald-400">{summary.completedInterviews}</span>
            </div>
            <div className="bg-gray-900/60 p-3 rounded-xl border border-gray-800">
              <span className="text-xs text-gray-400 block">Latest Score</span>
              <span className="text-lg font-bold text-blue-400">{summary.latestScore}%</span>
            </div>
            <div className="bg-gray-900/60 p-3 rounded-xl border border-gray-800">
              <span className="text-xs text-gray-400 block">Target Average</span>
              <span className="text-lg font-bold text-purple-400">80%+</span>
            </div>
          </div>
        </div>
      )}

      {/* Main Configuration Card */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm p-6 sm:p-8 space-y-8">
        {error && (
          <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-xs font-semibold text-rose-700">
            {error}
          </div>
        )}

        {/* 1. Category Selection */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <label className="text-xs font-bold uppercase tracking-wider text-gray-500">
              1. Select Technical Subject / Category
            </label>
            <span className="text-xs font-semibold text-purple-700 bg-purple-50 px-2 py-0.5 rounded-md border border-purple-200">
              {CATEGORY_LABELS[category]}
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {categoryOptions.map((cat) => {
              const isSelected = category === cat.key;
              return (
                <div
                  key={cat.key}
                  onClick={() => setCategory(cat.key)}
                  className={`p-4 rounded-xl border transition-all cursor-pointer flex flex-col justify-between gap-2 text-left ${
                    isSelected
                      ? "bg-purple-50/70 border-purple-500 ring-2 ring-purple-500/20 shadow-xs"
                      : "bg-gray-50 hover:bg-gray-100/80 border-gray-200"
                  }`}
                >
                  <div className="flex items-center gap-2.5">
                    <span className="text-xl">{cat.icon}</span>
                    <span className="text-sm font-bold text-gray-900">{cat.title}</span>
                  </div>
                  <p className="text-[11px] text-gray-500 leading-relaxed line-clamp-2">
                    {cat.desc}
                  </p>
                </div>
              );
            })}
          </div>
        </div>

        {/* 2. Difficulty Selection */}
        <div className="space-y-3">
          <label className="text-xs font-bold uppercase tracking-wider text-gray-500">
            2. Choose Difficulty Level
          </label>
          <div className="grid grid-cols-3 gap-3">
            {(["EASY", "MEDIUM", "HARD"] as Difficulty[]).map((diff) => {
              const isSelected = difficulty === diff;
              const colorMap = {
                EASY: isSelected ? "bg-emerald-50 border-emerald-500 text-emerald-800 ring-2 ring-emerald-500/20" : "bg-gray-50 hover:bg-gray-100 text-gray-700 border-gray-200",
                MEDIUM: isSelected ? "bg-amber-50 border-amber-500 text-amber-800 ring-2 ring-amber-500/20" : "bg-gray-50 hover:bg-gray-100 text-gray-700 border-gray-200",
                HARD: isSelected ? "bg-rose-50 border-rose-500 text-rose-800 ring-2 ring-rose-500/20" : "bg-gray-50 hover:bg-gray-100 text-gray-700 border-gray-200",
              };
              return (
                <button
                  key={diff}
                  type="button"
                  onClick={() => setDifficulty(diff)}
                  className={`py-3 px-4 rounded-xl font-bold text-xs border text-center transition cursor-pointer ${colorMap[diff]}`}
                >
                  {diff === "EASY" ? "🟢 Easy" : diff === "MEDIUM" ? "🟡 Medium" : "🔴 Hard"}
                </button>
              );
            })}
          </div>
        </div>

        {/* 3. Duration & Question Count */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 pt-2 border-t border-gray-100">
          <div className="space-y-2">
            <label className="text-xs font-bold uppercase tracking-wider text-gray-500">
              Session Duration (Authoritative Timer)
            </label>
            <div className="grid grid-cols-3 gap-2">
              {[15, 30, 45].map((mins) => (
                <button
                  key={mins}
                  type="button"
                  onClick={() => setDurationMinutes(mins)}
                  className={`py-2 px-3 rounded-xl text-xs font-bold border transition cursor-pointer ${
                    durationMinutes === mins
                      ? "bg-black text-white border-black shadow-xs"
                      : "bg-gray-50 hover:bg-gray-100 text-gray-700 border-gray-200"
                  }`}
                >
                  ⏱️ {mins} mins
                </button>
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-xs font-bold uppercase tracking-wider text-gray-500">
              Question Count
            </label>
            <div className="grid grid-cols-2 gap-2">
              {[5, 10].map((count) => (
                <button
                  key={count}
                  type="button"
                  onClick={() => setQuestionCount(count)}
                  className={`py-2 px-3 rounded-xl text-xs font-bold border transition cursor-pointer ${
                    questionCount === count
                      ? "bg-black text-white border-black shadow-xs"
                      : "bg-gray-50 hover:bg-gray-100 text-gray-700 border-gray-200"
                  }`}
                >
                  📝 {count} Questions
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Start Button CTA */}
        <div className="pt-4 border-t border-gray-100 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="text-xs text-gray-500 space-y-0.5 text-center sm:text-left">
            <span className="block font-medium">
              Format: Multiple Choice Conceptual & Architecture Scenarios.
            </span>
            <span className="block text-gray-400">
              Anti-cheating active: Explanations and answer keys unlock immediately upon submission.
            </span>
          </div>

          <button
            onClick={handleStartInterview}
            disabled={starting}
            className="w-full sm:w-auto px-8 py-3.5 bg-purple-600 hover:bg-purple-700 text-white font-bold text-sm rounded-xl transition shadow-md hover:shadow-lg flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
          >
            {starting ? "Launching Session..." : "Start Mock Interview 🚀"}
          </button>
        </div>
      </div>
    </div>
  );
}
