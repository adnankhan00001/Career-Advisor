"use client";

import { useState, useEffect, use } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import {
  interviewService,
  MockInterviewResult,
  CATEGORY_LABELS,
} from "@/lib/interviewService";

interface ResultProps {
  params: Promise<{ id: string }>;
}

export default function MockInterviewResultPage({ params }: ResultProps) {
  const resolvedParams = use(params);
  const interviewId = resolvedParams.id;
  const router = useRouter();
  const searchParams = useSearchParams();
  const wasExpired = searchParams.get("expired") === "true";

  const [result, setResult] = useState<MockInterviewResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [filterMode, setFilterMode] = useState<"ALL" | "INCORRECT" | "CORRECT">("ALL");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadResult() {
      setLoading(true);
      try {
        const data = await interviewService.getInterviewResult(interviewId);
        if (data) {
          setResult(data);
        }
      } catch {
        setError("Failed to load interview assessment result.");
      } finally {
        setLoading(false);
      }
    }

    loadResult();
  }, [interviewId]);

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto p-8 animate-pulse space-y-6">
        <div className="h-10 bg-gray-200 rounded-md w-1/3"></div>
        <div className="h-64 bg-gray-100 rounded-2xl"></div>
        <div className="h-96 bg-gray-100 rounded-2xl"></div>
      </div>
    );
  }

  if (error || !result) {
    return (
      <div className="max-w-2xl mx-auto p-12 text-center bg-white rounded-2xl border border-gray-200 my-12 space-y-4">
        <h2 className="text-xl font-bold text-gray-900">Result Not Found</h2>
        <p className="text-sm text-gray-500">{error || "Could not retrieve the specified interview result."}</p>
        <Link
          href="/mock-interview"
          className="inline-block px-5 py-2.5 bg-black text-white text-xs font-bold rounded-xl"
        >
          ← Back to Mock Interview
        </Link>
      </div>
    );
  }

  const formatDuration = (secs: number) => {
    const mins = Math.floor(secs / 60);
    const s = secs % 60;
    return `${mins}m ${s}s`;
  };

  const scoreColor =
    result.score >= 80
      ? "text-emerald-600 border-emerald-300 bg-emerald-50"
      : result.score >= 50
      ? "text-amber-600 border-amber-300 bg-amber-50"
      : "text-rose-600 border-rose-300 bg-rose-50";

  const filteredReviews = result.questionReviews.filter((q) => {
    if (filterMode === "CORRECT") return q.isCorrect === true;
    if (filterMode === "INCORRECT") return q.isCorrect !== true;
    return true;
  });

  return (
    <div className="max-w-4xl mx-auto space-y-8 pb-20">
      {/* Header Banner */}
      <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-gray-100 pb-6">
          <div>
            <div className="flex items-center gap-2 mb-1.5">
              <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-purple-50 text-purple-700 border border-purple-200">
                {CATEGORY_LABELS[result.category] || result.category}
              </span>
              <span className="text-xs font-semibold px-2 py-0.5 rounded-md bg-gray-100 text-gray-700">
                {result.difficulty}
              </span>
              {wasExpired && (
                <span className="text-xs font-bold px-2 py-0.5 rounded-md bg-rose-50 text-rose-700 border border-rose-200">
                  Auto-Submitted on Timeout
                </span>
              )}
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900">
              Technical Assessment Report 📊
            </h1>
          </div>

          <div className="flex items-center gap-3">
            <Link
              href="/mock-interview"
              className="px-4 py-2 text-xs font-bold bg-purple-600 hover:bg-purple-700 text-white rounded-xl transition shadow-xs"
            >
              Try Another Session 🚀
            </Link>
          </div>
        </div>

        {/* Score & Metrics Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-center pt-2">
          <div className={`p-4 rounded-2xl border ${scoreColor}`}>
            <span className="text-xs font-bold block uppercase tracking-wider opacity-80">
              Overall Score
            </span>
            <span className="text-3xl sm:text-4xl font-extrabold block mt-1">
              {result.score}%
            </span>
          </div>

          <div className="p-4 rounded-2xl border border-gray-200 bg-gray-50/70">
            <span className="text-xs font-bold text-gray-500 block uppercase tracking-wider">
              Accuracy
            </span>
            <span className="text-3xl font-extrabold text-gray-900 block mt-1">
              {result.correctCount} / {result.totalQuestions}
            </span>
          </div>

          <div className="p-4 rounded-2xl border border-gray-200 bg-gray-50/70">
            <span className="text-xs font-bold text-gray-500 block uppercase tracking-wider">
              Time Taken
            </span>
            <span className="text-2xl sm:text-3xl font-extrabold text-gray-900 block mt-1">
              {formatDuration(result.timeTakenSeconds)}
            </span>
          </div>

          <div className="p-4 rounded-2xl border border-gray-200 bg-gray-50/70">
            <span className="text-xs font-bold text-gray-500 block uppercase tracking-wider">
              Status
            </span>
            <span className="text-lg font-extrabold text-emerald-700 block mt-2">
              ✓ Completed
            </span>
          </div>
        </div>

        {/* Strong & Weak Areas */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
          <div className="p-4 rounded-xl border border-emerald-200 bg-emerald-50/50 space-y-2">
            <span className="text-xs font-bold text-emerald-900 uppercase tracking-wider flex items-center gap-1">
              <span>🌟</span> Strong Conceptual Areas
            </span>
            <div className="flex flex-wrap gap-1.5">
              {result.strongAreas && result.strongAreas.length > 0 ? (
                result.strongAreas.map((area, idx) => (
                  <span
                    key={idx}
                    className="text-xs font-semibold bg-emerald-100 text-emerald-800 px-2.5 py-1 rounded-md"
                  >
                    {area}
                  </span>
                ))
              ) : (
                <span className="text-xs text-gray-500">Need more practice to establish strengths.</span>
              )}
            </div>
          </div>

          <div className="p-4 rounded-xl border border-amber-200 bg-amber-50/50 space-y-2">
            <span className="text-xs font-bold text-amber-900 uppercase tracking-wider flex items-center gap-1">
              <span>🎯</span> Focus & Revision Required
            </span>
            <div className="flex flex-wrap gap-1.5">
              {result.weakAreas && result.weakAreas.length > 0 ? (
                result.weakAreas.map((area, idx) => (
                  <span
                    key={idx}
                    className="text-xs font-semibold bg-amber-100 text-amber-900 px-2.5 py-1 rounded-md"
                  >
                    {area}
                  </span>
                ))
              ) : (
                <span className="text-xs text-emerald-700 font-semibold">No major weak areas detected!</span>
              )}
            </div>
          </div>
        </div>

        {/* Actionable Recommendation */}
        {result.recommendation && (
          <div className="p-4 rounded-xl bg-purple-50/70 border border-purple-200 flex items-start gap-3">
            <span className="text-xl mt-0.5">💡</span>
            <div>
              <span className="text-xs font-bold text-purple-900 uppercase tracking-wider block">
                Personalized Recommendation
              </span>
              <p className="text-xs text-purple-950 font-medium leading-relaxed mt-0.5">
                {result.recommendation}
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Question Review Section */}
      <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-xs space-y-6">
        <div className="flex items-center justify-between flex-wrap gap-3 border-b border-gray-100 pb-4">
          <div>
            <h2 className="text-lg font-extrabold text-gray-900">
              In-Depth Question & Solution Review 📝
            </h2>
            <p className="text-xs text-gray-500">
              Inspect correct answers, in-depth architectural explanations, and concept tags.
            </p>
          </div>

          {/* Filter Pills */}
          <div className="flex items-center gap-1.5 bg-gray-100 p-1 rounded-xl text-xs font-bold">
            <button
              onClick={() => setFilterMode("ALL")}
              className={`px-3 py-1 rounded-lg transition cursor-pointer ${
                filterMode === "ALL" ? "bg-white text-black shadow-xs" : "text-gray-600 hover:text-black"
              }`}
            >
              All ({result.totalQuestions})
            </button>
            <button
              onClick={() => setFilterMode("INCORRECT")}
              className={`px-3 py-1 rounded-lg transition cursor-pointer ${
                filterMode === "INCORRECT"
                  ? "bg-white text-rose-700 shadow-xs"
                  : "text-gray-600 hover:text-black"
              }`}
            >
              Incorrect ({result.totalQuestions - result.correctCount})
            </button>
            <button
              onClick={() => setFilterMode("CORRECT")}
              className={`px-3 py-1 rounded-lg transition cursor-pointer ${
                filterMode === "CORRECT"
                  ? "bg-white text-emerald-700 shadow-xs"
                  : "text-gray-600 hover:text-black"
              }`}
            >
              Correct ({result.correctCount})
            </button>
          </div>
        </div>

        {/* Questions List */}
        <div className="space-y-6">
          {filteredReviews.map((review, idx) => {
            const isCorrect = review.isCorrect === true;

            return (
              <div
                key={review.id}
                className={`p-5 rounded-2xl border transition-all space-y-4 ${
                  isCorrect
                    ? "bg-emerald-50/30 border-emerald-200"
                    : "bg-rose-50/30 border-rose-200"
                }`}
              >
                {/* Header */}
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-2">
                    <span
                      className={`text-xs font-bold px-2.5 py-0.5 rounded-full border ${
                        isCorrect
                          ? "bg-emerald-100 text-emerald-800 border-emerald-300"
                          : "bg-rose-100 text-rose-800 border-rose-300"
                      }`}
                    >
                      {isCorrect ? "✓ Correct" : "✕ Incorrect / Skipped"}
                    </span>
                    <span className="text-xs font-semibold text-gray-600 bg-gray-100 px-2 py-0.5 rounded-md">
                      {review.topic}
                    </span>
                  </div>

                  <span className="text-xs text-gray-400 font-medium">
                    Question {idx + 1}
                  </span>
                </div>

                {/* Question */}
                <h3 className="text-sm font-bold text-gray-900 leading-relaxed">
                  {review.question}
                </h3>

                {/* Answer Comparisons */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                  <div className="p-3 bg-white rounded-xl border border-gray-200 space-y-1">
                    <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400 block">
                      Your Answer:
                    </span>
                    <span
                      className={`font-semibold block ${
                        isCorrect ? "text-emerald-700" : "text-rose-700"
                      }`}
                    >
                      {review.userAnswer || "No answer submitted"}
                    </span>
                  </div>

                  <div className="p-3 bg-white rounded-xl border border-gray-200 space-y-1">
                    <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400 block">
                      Correct Answer:
                    </span>
                    <span className="font-semibold text-emerald-800 block">
                      {review.correctAnswer}
                    </span>
                  </div>
                </div>

                {/* Explanation */}
                {review.explanation && (
                  <div className="p-4 bg-white/80 rounded-xl border border-gray-200 text-xs space-y-1.5">
                    <span className="font-bold text-purple-900 uppercase tracking-wider text-[11px] block">
                      Technical Explanation & Concept:
                    </span>
                    <p className="text-gray-800 leading-relaxed font-sans">
                      {review.explanation}
                    </p>
                  </div>
                )}

                {/* Expected Concepts */}
                {review.expectedConcepts && review.expectedConcepts.length > 0 && (
                  <div className="flex items-center gap-1.5 flex-wrap pt-1 text-[11px]">
                    <span className="text-gray-400 font-medium">Key Concepts:</span>
                    {review.expectedConcepts.map((c, cIdx) => (
                      <span
                        key={cIdx}
                        className="bg-gray-100 text-gray-700 px-2 py-0.5 rounded-md font-medium"
                      >
                        {c}
                      </span>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Footer Navigation */}
        <div className="pt-6 border-t border-gray-100 flex flex-col sm:flex-row items-center justify-between gap-3">
          <Link
            href="/mock-interview/history"
            className="text-xs font-bold text-gray-600 hover:text-black transition"
          >
            ← View All Interview History
          </Link>

          <div className="flex items-center gap-3">
            <Link
              href="/dashboard"
              className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-800 text-xs font-bold rounded-xl transition"
            >
              Dashboard 🏠
            </Link>
            <Link
              href="/mock-interview"
              className="px-5 py-2 bg-black hover:bg-gray-800 text-white text-xs font-bold rounded-xl transition shadow-xs"
            >
              New Mock Interview 🎯
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
