"use client";

import { useState } from "react";
import Link from "next/link";
import { quizQuestions, evaluateQuizResult } from "@/lib/quizData";
import { quizService, QuizSubmissionResponse } from "@/lib/quizService";
import { progressService } from "@/lib/progressService";

export default function QuizPage() {
  const [currentIdx, setCurrentIdx] = useState<number>(0);
  const [selectedAnswers, setSelectedAnswers] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState<boolean>(false);
  const [isCompleted, setIsCompleted] = useState<boolean>(false);
  const [result, setResult] = useState<ReturnType<typeof evaluateQuizResult> | null>(null);

  const currentQ = quizQuestions[currentIdx];
  const totalQuestions = quizQuestions.length;
  const currentSelected = selectedAnswers[currentQ.id] || null;
  const answeredCount = Object.keys(selectedAnswers).length;
  const progressPercent = ((currentIdx + 1) / totalQuestions) * 100;

  const handleSelectOption = (opt: string) => {
    setSelectedAnswers((prev) => ({
      ...prev,
      [currentQ.id]: opt,
    }));
  };

  const handlePrevious = () => {
    if (currentIdx > 0) {
      setCurrentIdx((prev) => prev - 1);
    }
  };

  const handleNext = () => {
    if (currentIdx + 1 < totalQuestions) {
      setCurrentIdx((prev) => prev + 1);
    } else {
      handleSubmitQuiz();
    }
  };

  const handleSubmitQuiz = async () => {
    setLoading(true);

    const evalResult = evaluateQuizResult(selectedAnswers);
    setResult(evalResult);

    // Call backend API with token if logged in
    try {
      const serverResponse: QuizSubmissionResponse = await quizService.submitQuiz(selectedAnswers);
      if (serverResponse) {
        evalResult.score = serverResponse.score;
        evalResult.percentage = serverResponse.percentage;
        evalResult.level = serverResponse.level as any;
        evalResult.recommendedCareer = serverResponse.recommendedCareer;
      }
      // Update active career goal with backend
      await progressService.updateCareerGoal(evalResult.recommendedCareer);
    } catch {
      // Local fallback
    } finally {
      setLoading(false);
      setIsCompleted(true);
    }
  };

  // Result View
  if (isCompleted && result) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4 sm:p-6">
        <div className="bg-white rounded-2xl max-w-2xl w-full border border-gray-200 shadow-xl p-6 sm:p-8 space-y-6">
          {/* Header */}
          <div className="text-center space-y-2">
            <div className="w-16 h-16 bg-blue-50 text-blue-600 text-3xl rounded-2xl flex items-center justify-center mx-auto shadow-sm">
              🎯
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900">
              Assessment Completed!
            </h1>
            <p className="text-gray-500 text-sm">
              Here is your readiness evaluation and recommended track
            </p>
          </div>

          {/* Scores Banner */}
          <div className="grid grid-cols-3 gap-3 p-4 bg-gray-50 rounded-xl border border-gray-100 text-center">
            <div>
              <span className="text-xs text-gray-500 font-medium">Overall Score</span>
              <p className="text-xl font-bold text-gray-900 mt-0.5">
                {result.score}/{result.totalQuestions}
              </p>
            </div>
            <div>
              <span className="text-xs text-gray-500 font-medium">Accuracy</span>
              <p className="text-xl font-bold text-blue-600 mt-0.5">
                {result.percentage}%
              </p>
            </div>
            <div>
              <span className="text-xs text-gray-500 font-medium">Assessed Tier</span>
              <p className="text-xl font-bold text-emerald-600 mt-0.5">
                {result.level}
              </p>
            </div>
          </div>

          {/* Category Breakdown */}
          <div className="space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-gray-500">
              Category Breakdown
            </h3>
            <div className="space-y-2">
              {Object.entries(result.categoryScores).map(
                ([cat, { total, correct }]) => {
                  const catPercent = Math.round((correct / total) * 100);
                  return (
                    <div
                      key={cat}
                      className="p-3 bg-gray-50 rounded-xl border border-gray-100 flex items-center justify-between text-xs sm:text-sm"
                    >
                      <span className="font-semibold text-gray-800">{cat}</span>
                      <div className="flex items-center gap-3">
                        <span className="text-gray-500">
                          {correct}/{total} correct
                        </span>
                        <span
                          className={`font-bold ${
                            catPercent >= 70
                              ? "text-green-600"
                              : catPercent >= 40
                              ? "text-blue-600"
                              : "text-gray-600"
                          }`}
                        >
                          {catPercent}%
                        </span>
                      </div>
                    </div>
                  );
                }
              )}
            </div>
          </div>

          {/* Recommended Career Track */}
          <div className="bg-gradient-to-r from-gray-900 to-black text-white p-6 rounded-2xl shadow-md">
            <span className="text-xs uppercase tracking-wider text-blue-400 font-semibold block mb-1">
              Top Recommended Career Path
            </span>
            <h3 className="text-xl sm:text-2xl font-bold">
              {result.recommendedCareer}
            </h3>
            <p className="text-gray-300 text-xs sm:text-sm mt-2 leading-relaxed">
              Based on your strong aptitude in problem solving and subject
              evaluations, this track is ideal for your career acceleration.
            </p>
          </div>

          {/* Action CTAs */}
          <div className="flex flex-col sm:flex-row gap-3 pt-2">
            <Link
              href="/dashboard"
              className="flex-1 py-3 px-4 border border-gray-300 text-gray-700 text-sm font-semibold rounded-xl text-center hover:bg-gray-50 transition"
            >
              Go to Dashboard
            </Link>
            <Link
              href={`/roadmap?career=${encodeURIComponent(
                result.recommendedCareer
              )}`}
              className="flex-1 py-3 px-4 bg-black text-white text-sm font-semibold rounded-xl text-center hover:bg-gray-800 transition shadow-sm"
            >
              Start {result.recommendedCareer} Roadmap →
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // Quiz Taking View
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-4 sm:p-6">
      <div className="max-w-2xl w-full space-y-6">
        {/* Top Header & Progress */}
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-blue-600 bg-blue-50 px-2.5 py-1 rounded-md">
              Category: {currentQ.category}
            </span>
            <span className="text-xs text-gray-500 font-medium">
              Answered {answeredCount} of {totalQuestions}
            </span>
          </div>

          {/* Progress bar */}
          <div className="w-full bg-gray-200 h-2 rounded-full overflow-hidden">
            <div
              className="bg-blue-600 h-2 rounded-full transition-all duration-300"
              style={{ width: `${progressPercent}%` }}
            />
          </div>

          {/* Question navigator dots */}
          <div className="flex items-center justify-between gap-1 pt-1">
            {quizQuestions.map((q, idx) => {
              const isAnswered = Boolean(selectedAnswers[q.id]);
              const isCurrent = idx === currentIdx;
              return (
                <button
                  key={q.id}
                  onClick={() => setCurrentIdx(idx)}
                  className={`flex-1 h-1.5 rounded-full transition cursor-pointer ${
                    isCurrent
                      ? "bg-black"
                      : isAnswered
                      ? "bg-blue-400"
                      : "bg-gray-200"
                  }`}
                  aria-label={`Jump to question ${idx + 1}`}
                />
              );
            })}
          </div>
        </div>

        {/* Question Card */}
        <div className="bg-white rounded-2xl border border-gray-200 shadow-sm p-6 sm:p-8 space-y-6">
          <div>
            <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
              Question {currentIdx + 1} of {totalQuestions}
            </span>
            <h2 className="text-lg sm:text-xl font-bold text-gray-900 mt-1 leading-snug">
              {currentQ.question}
            </h2>
          </div>

          {/* Options */}
          <div className="space-y-3">
            {currentQ.options.map((opt, index) => {
              const isSelected = currentSelected === opt;
              return (
                <button
                  key={index}
                  onClick={() => handleSelectOption(opt)}
                  className={`w-full text-left p-4 rounded-xl border text-sm font-medium transition flex items-center justify-between cursor-pointer ${
                    isSelected
                      ? "bg-blue-50/80 border-blue-500 text-blue-950 shadow-sm ring-1 ring-blue-500"
                      : "bg-white border-gray-200 text-gray-800 hover:bg-gray-50 hover:border-gray-300"
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <span
                      className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                        isSelected
                          ? "bg-blue-600 text-white"
                          : "bg-gray-100 text-gray-600"
                      }`}
                    >
                      {String.fromCharCode(65 + index)}
                    </span>
                    <span>{opt}</span>
                  </div>
                  {isSelected && (
                    <span className="text-blue-600 font-bold text-sm">✓</span>
                  )}
                </button>
              );
            })}
          </div>

          {/* Navigation Controls */}
          <div className="flex items-center justify-between gap-3 pt-4 border-t border-gray-100">
            <button
              onClick={handlePrevious}
              disabled={currentIdx === 0}
              className={`px-4 py-2.5 rounded-xl border text-sm font-medium transition cursor-pointer ${
                currentIdx === 0
                  ? "border-gray-200 text-gray-300 cursor-not-allowed"
                  : "border-gray-300 text-gray-700 hover:bg-gray-50"
              }`}
            >
              ← Previous
            </button>

            <button
              onClick={handleNext}
              disabled={!currentSelected || loading}
              className={`px-6 py-2.5 rounded-xl text-sm font-semibold transition flex items-center gap-2 cursor-pointer ${
                !currentSelected || loading
                  ? "bg-gray-300 text-gray-500 cursor-not-allowed"
                  : "bg-black text-white hover:bg-gray-800 shadow-sm"
              }`}
            >
              {loading ? (
                "Evaluating..."
              ) : currentIdx + 1 === totalQuestions ? (
                "Submit Assessment →"
              ) : (
                "Next Question →"
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}