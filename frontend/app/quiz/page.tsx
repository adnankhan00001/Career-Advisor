"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

/* ================= TYPES ================= */

type Question = {
  id: number;
  question: string;
  options: string[];
  answer: string;
};

type Answer = {
  questionId: number;
  selected: string;
};

type QuizResponse = {
  level: string;
};

/* ================= QUESTIONS ================= */

const questions: Question[] = [
  {
    id: 1,
    question: "What is the time complexity of binary search?",
    options: ["O(n)", "O(log n)", "O(n log n)", "O(1)"],
    answer: "O(log n)",
  },
  {
    id: 2,
    question: "Which data structure follows FIFO?",
    options: ["Stack", "Queue", "Tree", "Graph"],
    answer: "Queue",
  },
  {
    id: 3,
    question: "Which is NOT a linear data structure?",
    options: ["Array", "Linked List", "Tree", "Stack"],
    answer: "Tree",
  },
];

/* ================= COMPONENT ================= */

export default function QuizPage() {
  const router = useRouter();

  const [currentQ, setCurrentQ] = useState<number>(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [answers, setAnswers] = useState<Answer[]>([]);
  const [loading, setLoading] = useState<boolean>(false);

  const totalQuestions = questions.length;
  const progress = ((currentQ + 1) / totalQuestions) * 100;

  /* ================= HANDLERS ================= */

  const handleNext = () => {
    if (!selected) return;

    const updatedAnswers: Answer[] = [
      ...answers,
      {
        questionId: questions[currentQ].id,
        selected,
      },
    ];

    setAnswers(updatedAnswers);
    setSelected(null);

    if (currentQ + 1 < totalQuestions) {
      setCurrentQ((prev) => prev + 1);
    } else {
      submitQuiz(updatedAnswers);
    }
  };

  const submitQuiz = async (answers: Answer[]) => {
    setLoading(true);

    try {
      const res = await fetch("http://localhost:8080/quiz/submit", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ answers }),
      });

      const data: QuizResponse = await res.json();

      // You can also store level in localStorage or context
      if (data.level) {
        localStorage.setItem("userLevel", data.level);
        router.push("/dashboard");
      }
    } catch (error) {
      console.error("Quiz submission failed:", error);
    } finally {
      setLoading(false);
    }
  };

  /* ================= UI ================= */

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50 p-6">

      {/* Progress */}
      <div className="w-full max-w-xl mb-6">
        <div className="h-2 bg-gray-200 rounded">
          <div
            className="h-2 bg-blue-500 rounded transition-all duration-300"
            style={{ width: `${progress}%` }}
          />
        </div>
        <p className="text-sm mt-2 text-gray-600">
          Question {currentQ + 1} of {totalQuestions}
        </p>
      </div>

      {/* Card */}
      <div className="w-full max-w-xl bg-white shadow-lg rounded-2xl p-6">

        {/* Question */}
        <h2 className="text-lg font-semibold mb-5">
          {questions[currentQ].question}
        </h2>

        {/* Options */}
        <div className="space-y-3">
          {questions[currentQ].options.map((opt, index) => (
            <button
              key={index}
              onClick={() => setSelected(opt)}
              className={`w-full text-left p-3 rounded-lg border transition ${
                selected === opt
                  ? "bg-blue-100 border-blue-500"
                  : "hover:bg-gray-100"
              }`}
            >
              {opt}
            </button>
          ))}
        </div>

        {/* Next / Submit */}
        <button
          onClick={handleNext}
          disabled={!selected || loading}
          className={`mt-6 w-full py-2 rounded-lg text-white ${
            selected
              ? "bg-blue-600 hover:bg-blue-700"
              : "bg-gray-400 cursor-not-allowed"
          }`}
        >
          {loading
            ? "Submitting..."
            : currentQ + 1 === totalQuestions
            ? "Submit Quiz"
            : "Next"}
        </button>
      </div>
    </div>
  );
}