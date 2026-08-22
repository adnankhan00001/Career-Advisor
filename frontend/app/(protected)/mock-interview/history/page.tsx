"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import {
  interviewService,
  MockInterviewHistoryItem,
  InterviewCategory,
  CATEGORY_LABELS,
} from "@/lib/interviewService";

export default function MockInterviewHistoryPage() {
  const [history, setHistory] = useState<MockInterviewHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState<string>("ALL");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadHistory() {
      setLoading(true);
      try {
        const data = await interviewService.getInterviewHistory();
        setHistory(data || []);
      } catch {
        setError("Failed to load interview history.");
      } finally {
        setLoading(false);
      }
    }
    loadHistory();
  }, []);

  const filteredHistory = history.filter((item) => {
    if (selectedCategory !== "ALL" && item.category !== selectedCategory) {
      return false;
    }
    return true;
  });

  const categories: InterviewCategory[] = [
    "JAVA",
    "OOP",
    "DBMS",
    "SPRING_BOOT",
    "DSA",
    "OS",
    "CN",
  ];

  return (
    <div className="max-w-5xl mx-auto space-y-8 pb-20">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Link
              href="/mock-interview"
              className="text-xs font-semibold text-gray-500 hover:text-black transition"
            >
              ← Back to Mock Interview
            </Link>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            Interview Performance History 📋
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Track your progression, accuracy trends, and historical technical assessments.
          </p>
        </div>

        <Link
          href="/mock-interview"
          className="px-5 py-2.5 bg-purple-600 hover:bg-purple-700 text-white font-bold text-xs rounded-xl transition shadow-xs"
        >
          + New Mock Interview 🎯
        </Link>
      </div>

      {/* Category Filter Chips */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
        <button
          onClick={() => setSelectedCategory("ALL")}
          className={`px-3.5 py-1.5 rounded-xl text-xs font-bold whitespace-nowrap transition cursor-pointer ${
            selectedCategory === "ALL"
              ? "bg-black text-white shadow-xs"
              : "bg-white text-gray-600 border border-gray-200 hover:bg-gray-50"
          }`}
        >
          All Subjects ({history.length})
        </button>
        {categories.map((cat) => {
          const count = history.filter((h) => h.category === cat).length;
          return (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold whitespace-nowrap transition cursor-pointer ${
                selectedCategory === cat
                  ? "bg-black text-white shadow-xs"
                  : "bg-white text-gray-600 border border-gray-200 hover:bg-gray-50"
              }`}
            >
              {cat} ({count})
            </button>
          );
        })}
      </div>

      {/* History List */}
      {loading ? (
        <div className="space-y-4 animate-pulse">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-24 bg-gray-100 rounded-2xl"></div>
          ))}
        </div>
      ) : filteredHistory.length === 0 ? (
        <div className="bg-white rounded-2xl border border-gray-200 p-12 text-center space-y-4">
          <div className="text-3xl">🎯</div>
          <h3 className="text-base font-bold text-gray-900">No Interview Records Found</h3>
          <p className="text-xs text-gray-500 max-w-sm mx-auto">
            {selectedCategory === "ALL"
              ? "You haven't completed any mock technical interview sessions yet."
              : `No sessions found under ${selectedCategory}.`}
          </p>
          <Link
            href="/mock-interview"
            className="inline-block px-5 py-2.5 bg-purple-600 hover:bg-purple-700 text-white font-bold text-xs rounded-xl shadow-xs"
          >
            Start Your First Interview 🚀
          </Link>
        </div>
      ) : (
        <div className="space-y-3.5">
          {filteredHistory.map((item) => {
            const scoreColor =
              item.score >= 80
                ? "bg-emerald-50 text-emerald-700 border-emerald-300"
                : item.score >= 50
                ? "bg-amber-50 text-amber-700 border-amber-300"
                : "bg-rose-50 text-rose-700 border-rose-300";

            const dateStr = item.startedAt
              ? new Date(item.startedAt).toLocaleDateString("en-US", {
                  month: "short",
                  day: "numeric",
                  year: "numeric",
                  hour: "2-digit",
                  minute: "2-digit",
                })
              : "Recent";

            return (
              <div
                key={item.id}
                className="bg-white rounded-2xl border border-gray-200 hover:border-gray-300 transition-all p-5 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4"
              >
                <div className="space-y-1.5">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-purple-50 text-purple-700 border border-purple-200">
                      {CATEGORY_LABELS[item.category] || item.category}
                    </span>
                    <span className="text-xs font-semibold px-2 py-0.5 rounded-md bg-gray-100 text-gray-700">
                      {item.difficulty}
                    </span>
                    <span
                      className={`text-[11px] font-bold px-2 py-0.5 rounded-md ${
                        item.status === "COMPLETED"
                          ? "bg-emerald-50 text-emerald-700"
                          : item.status === "EXPIRED"
                          ? "bg-rose-50 text-rose-700"
                          : "bg-blue-50 text-blue-700"
                      }`}
                    >
                      {item.status}
                    </span>
                  </div>

                  <div className="text-xs text-gray-400">
                    Attempted on: <span className="font-medium text-gray-700">{dateStr}</span>
                  </div>
                </div>

                {/* Score & View Result CTA */}
                <div className="flex items-center justify-between sm:justify-end gap-5">
                  <div className="flex items-center gap-4">
                    <div className="text-right">
                      <span className="text-[11px] text-gray-400 block font-medium">Passed</span>
                      <span className="text-xs font-bold text-gray-800">
                        {item.correctCount} / {item.totalQuestions}
                      </span>
                    </div>

                    <div
                      className={`px-3 py-1.5 rounded-xl border text-sm font-extrabold text-center min-w-[65px] ${scoreColor}`}
                    >
                      {item.score}%
                    </div>
                  </div>

                  <Link
                    href={`/mock-interview/${item.id}/result`}
                    className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-800 text-xs font-bold rounded-xl transition shadow-xs text-center"
                  >
                    View Report →
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
