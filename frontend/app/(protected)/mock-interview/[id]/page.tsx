"use client";

import { useState, useEffect, use } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  interviewService,
  MockInterviewSession,
  CATEGORY_LABELS,
} from "@/lib/interviewService";

interface ActiveInterviewProps {
  params: Promise<{ id: string }>;
}

export default function ActiveMockInterviewPage({ params }: ActiveInterviewProps) {
  const resolvedParams = use(params);
  const interviewId = resolvedParams.id;
  const router = useRouter();

  const [session, setSession] = useState<MockInterviewSession | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<number, string>>({});
  const [remainingSeconds, setRemainingSeconds] = useState<number>(900);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [savingQuestionId, setSavingQuestionId] = useState<number | null>(null);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [hasExpired, setHasExpired] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 1. Initial Session Load
  useEffect(() => {
    async function loadSession() {
      setLoading(true);
      try {
        const data = await interviewService.getActiveSession(interviewId);
        if (data) {
          if (data.status === "COMPLETED") {
            router.replace(`/mock-interview/${interviewId}/result`);
            return;
          }

          setSession(data);

          // Populate existing answers
          const existingAns: Record<number, string> = {};
          data.questions.forEach((q) => {
            if (q.selectedAnswer) {
              existingAns[q.id] = q.selectedAnswer;
            }
          });
          setAnswers(existingAns);

          // Calculate authoritative remaining seconds
          const deadlineTime = new Date(data.deadline).getTime();
          const nowTime = Date.now();
          const diff = Math.max(0, Math.floor((deadlineTime - nowTime) / 1000));
          setRemainingSeconds(diff);

          if (diff <= 0 || data.status === "EXPIRED") {
            setHasExpired(true);
          }
        }
      } catch {
        setError("Failed to load active mock interview. Session may not exist or has expired.");
      } finally {
        setLoading(false);
      }
    }

    loadSession();
  }, [interviewId, router]);

  // 2. Authoritative Countdown Timer Interval
  useEffect(() => {
    if (loading || submitting || hasExpired) return;

    const timer = setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          setHasExpired(true);
          handleAutoSubmit();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [loading, submitting, hasExpired]);

  // 3. Auto-Submit on Timeout
  const handleAutoSubmit = async () => {
    if (submitting) return;
    setSubmitting(true);
    try {
      await interviewService.submitInterview(interviewId);
      router.push(`/mock-interview/${interviewId}/result?expired=true`);
    } catch {
      router.push(`/mock-interview/${interviewId}/result`);
    }
  };

  // 4. Save Answer Selection
  const handleSelectOption = async (questionId: number, optionText: string) => {
    if (hasExpired || submitting) return;

    // Optimistically update UI
    setAnswers((prev) => ({ ...prev, [questionId]: optionText }));
    setSavingQuestionId(questionId);

    try {
      await interviewService.saveAnswer(interviewId, questionId, optionText);
    } catch {
      // Graceful error logging
    } finally {
      setSavingQuestionId(null);
    }
  };

  // 5. Explicit Submit
  const handleConfirmSubmit = async () => {
    setShowConfirmModal(false);
    setSubmitting(true);
    try {
      const res = await interviewService.submitInterview(interviewId);
      if (res && res.id) {
        router.push(`/mock-interview/${interviewId}/result`);
      }
    } catch {
      setError("Failed to submit interview. Please try again.");
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto p-8 animate-pulse space-y-6">
        <div className="h-8 bg-gray-200 rounded-md w-1/3"></div>
        <div className="h-64 bg-gray-100 rounded-2xl"></div>
      </div>
    );
  }

  if (error || !session) {
    return (
      <div className="max-w-2xl mx-auto p-12 text-center bg-white rounded-2xl border border-gray-200 my-12 space-y-4">
        <h2 className="text-xl font-bold text-gray-900">Session Error</h2>
        <p className="text-sm text-gray-500">{error || "Could not retrieve mock interview session."}</p>
        <Link
          href="/mock-interview"
          className="inline-block px-5 py-2.5 bg-black text-white text-xs font-bold rounded-xl"
        >
          ← Return to Interview Setup
        </Link>
      </div>
    );
  }

  const currentQuestion = session.questions[currentIndex];
  const totalQuestions = session.questions.length;
  const answeredCount = Object.keys(answers).length;
  const unansweredCount = totalQuestions - answeredCount;

  // Format MM:SS
  const formatTime = (secs: number) => {
    const mins = Math.floor(secs / 60);
    const s = secs % 60;
    return `${mins.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
  };

  const isLowTime = remainingSeconds < 120; // Under 2 minutes

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-20">
      {/* Top Header with Timer & Category */}
      <div className="bg-white rounded-2xl border border-gray-200 p-4 sm:px-6 flex items-center justify-between gap-4 shadow-xs flex-wrap">
        <div className="flex items-center gap-3 flex-wrap">
          <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-purple-50 text-purple-700 border border-purple-200">
            {CATEGORY_LABELS[session.category] || session.category}
          </span>
          <span className="text-xs font-semibold px-2 py-0.5 rounded-md bg-gray-100 text-gray-700">
            {session.difficulty}
          </span>
          <span className="text-xs text-gray-400">
            Question {currentIndex + 1} of {totalQuestions}
          </span>
        </div>

        {/* Live Authoritative Timer & Submit */}
        <div className="flex items-center gap-3">
          <div
            className={`px-3.5 py-1.5 rounded-xl font-mono text-sm font-bold flex items-center gap-1.5 transition-all ${
              isLowTime
                ? "bg-rose-50 text-rose-700 border border-rose-300 animate-pulse"
                : "bg-gray-900 text-white shadow-xs"
            }`}
          >
            <span>⏱️</span>
            <span>{formatTime(remainingSeconds)}</span>
          </div>

          <button
            onClick={() => setShowConfirmModal(true)}
            disabled={submitting}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl transition shadow-xs cursor-pointer disabled:opacity-50"
          >
            {submitting ? "Submitting..." : "Submit Interview 🚀"}
          </button>
        </div>
      </div>

      {/* Main Layout: Split Question & Navigator */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Active Question Card (8 Cols) */}
        <div className="lg:col-span-8 bg-white rounded-2xl border border-gray-200 shadow-sm p-6 sm:p-8 space-y-6">
          {/* Question Header */}
          <div className="flex items-center justify-between border-b border-gray-100 pb-3">
            <span className="text-xs font-bold uppercase tracking-wider text-purple-700 bg-purple-50 px-2.5 py-1 rounded-md">
              {currentQuestion.topic}
            </span>
            <div className="flex items-center gap-2">
              {savingQuestionId === currentQuestion.id ? (
                <span className="text-[11px] text-gray-400 font-medium animate-pulse">
                  Saving answer...
                </span>
              ) : answers[currentQuestion.id] ? (
                <span className="text-[11px] text-emerald-600 font-semibold flex items-center gap-1">
                  ✓ Answer Saved
                </span>
              ) : (
                <span className="text-[11px] text-gray-400">Unanswered</span>
              )}
            </div>
          </div>

          {/* Question Statement */}
          <div>
            <h2 className="text-base sm:text-lg font-bold text-gray-900 leading-snug">
              {currentIndex + 1}. {currentQuestion.question}
            </h2>
          </div>

          {/* Options Radio List */}
          <div className="space-y-3 pt-2">
            {currentQuestion.options.map((opt, optIdx) => {
              const isSelected = answers[currentQuestion.id] === opt;
              const optionLetter = String.fromCharCode(65 + optIdx); // A, B, C, D

              return (
                <div
                  key={optIdx}
                  onClick={() => handleSelectOption(currentQuestion.id, opt)}
                  className={`p-4 rounded-xl border transition-all cursor-pointer flex items-start gap-3.5 ${
                    isSelected
                      ? "bg-purple-50/80 border-purple-500 ring-2 ring-purple-500/20 shadow-xs"
                      : "bg-gray-50/60 hover:bg-gray-100/70 border-gray-200"
                  }`}
                >
                  <span
                    className={`w-6 h-6 rounded-full text-xs font-bold flex items-center justify-center shrink-0 mt-0.5 ${
                      isSelected
                        ? "bg-purple-600 text-white"
                        : "bg-white text-gray-600 border border-gray-300"
                    }`}
                  >
                    {optionLetter}
                  </span>
                  <span
                    className={`text-xs sm:text-sm leading-relaxed ${
                      isSelected ? "font-bold text-purple-950" : "font-medium text-gray-800"
                    }`}
                  >
                    {opt}
                  </span>
                </div>
              );
            })}
          </div>

          {/* Expected Concepts */}
          {currentQuestion.expectedConcepts && currentQuestion.expectedConcepts.length > 0 && (
            <div className="pt-3 border-t border-gray-100 flex items-center gap-2 flex-wrap">
              <span className="text-[11px] text-gray-400 font-semibold">Tested Concepts:</span>
              {currentQuestion.expectedConcepts.map((concept, idx) => (
                <span
                  key={idx}
                  className="text-[11px] bg-gray-100 text-gray-600 px-2 py-0.5 rounded-md font-medium"
                >
                  {concept}
                </span>
              ))}
            </div>
          )}

          {/* Navigation Controls */}
          <div className="pt-4 border-t border-gray-100 flex items-center justify-between gap-3">
            <button
              onClick={() => setCurrentIndex((prev) => Math.max(0, prev - 1))}
              disabled={currentIndex === 0}
              className="px-4 py-2 text-xs font-bold bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl transition cursor-pointer disabled:opacity-40"
            >
              ← Previous Question
            </button>

            <span className="text-xs text-gray-400 font-medium">
              {currentIndex + 1} of {totalQuestions}
            </span>

            {currentIndex < totalQuestions - 1 ? (
              <button
                onClick={() => setCurrentIndex((prev) => Math.min(totalQuestions - 1, prev + 1))}
                className="px-5 py-2 text-xs font-bold bg-black hover:bg-gray-800 text-white rounded-xl transition cursor-pointer shadow-xs"
              >
                Next Question →
              </button>
            ) : (
              <button
                onClick={() => setShowConfirmModal(true)}
                className="px-5 py-2 text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl transition cursor-pointer shadow-xs"
              >
                Review & Submit 🚀
              </button>
            )}
          </div>
        </div>

        {/* Right Column: Question Navigator (4 Cols) */}
        <div className="lg:col-span-4 bg-white rounded-2xl border border-gray-200 shadow-sm p-6 space-y-6">
          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-gray-500 mb-1">
              Question Navigator
            </h3>
            <p className="text-xs text-gray-400">
              Jump directly to any question. Answers persist automatically.
            </p>
          </div>

          {/* Grid of question bubbles */}
          <div className="grid grid-cols-5 gap-2.5">
            {session.questions.map((q, idx) => {
              const isCurrent = idx === currentIndex;
              const isAnswered = Boolean(answers[q.id]);

              return (
                <button
                  key={q.id}
                  onClick={() => setCurrentIndex(idx)}
                  className={`h-11 rounded-xl font-bold text-xs transition cursor-pointer flex flex-col items-center justify-center gap-0.5 ${
                    isCurrent
                      ? "bg-purple-600 text-white ring-2 ring-purple-400 shadow-sm"
                      : isAnswered
                      ? "bg-emerald-50 text-emerald-700 border border-emerald-300 hover:bg-emerald-100"
                      : "bg-gray-50 hover:bg-gray-100 text-gray-600 border border-gray-200"
                  }`}
                >
                  <span>Q{idx + 1}</span>
                  <span className="text-[10px]">
                    {isAnswered ? "✓" : "○"}
                  </span>
                </button>
              );
            })}
          </div>

          {/* Progress summary pill */}
          <div className="bg-gray-50 p-4 rounded-xl border border-gray-200 space-y-2 text-xs">
            <div className="flex justify-between text-gray-600 font-medium">
              <span>Answered:</span>
              <span className="font-bold text-emerald-700">{answeredCount} / {totalQuestions}</span>
            </div>
            <div className="flex justify-between text-gray-600 font-medium">
              <span>Unanswered:</span>
              <span className="font-bold text-amber-700">{unansweredCount}</span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-1.5 overflow-hidden">
              <div
                className="bg-emerald-500 h-1.5 transition-all duration-300"
                style={{ width: `${Math.round((answeredCount / totalQuestions) * 100)}%` }}
              />
            </div>
          </div>

          <div className="pt-2 text-center">
            <button
              onClick={() => setShowConfirmModal(true)}
              className="w-full py-3 bg-gray-900 hover:bg-black text-white text-xs font-bold rounded-xl transition shadow-xs cursor-pointer"
            >
              Finish & Submit 🚀
            </button>
          </div>
        </div>
      </div>

      {/* Confirmation Modal */}
      {showConfirmModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 border border-gray-200 shadow-xl">
            <h3 className="text-lg font-extrabold text-gray-900">
              Submit Mock Interview?
            </h3>
            <p className="text-xs text-gray-600 leading-relaxed">
              {unansweredCount > 0
                ? `You still have ${unansweredCount} unanswered question(s). Are you sure you want to finalize and calculate your score?`
                : "You have answered all questions. Submit now to evaluate your performance and view detailed explanations?"}
            </p>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={() => setShowConfirmModal(false)}
                className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-semibold rounded-xl transition cursor-pointer"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmSubmit}
                disabled={submitting}
                className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl transition shadow-xs cursor-pointer disabled:opacity-50"
              >
                {submitting ? "Submitting..." : "Yes, Submit 🚀"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
