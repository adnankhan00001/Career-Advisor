"use client";

import { useEffect, useState } from "react";

export default function Dashboard() {
  const [level, setLevel] = useState<string | null>(null);

  useEffect(() => {
    const storedLevel = localStorage.getItem("userLevel");
    setLevel(storedLevel);
  }, []);

    return (
      <div className="p-6 space-y-6 bg-gray-50 min-h-screen">

        {/* HERO */}
        <div className="bg-gradient-to-r from-blue-500 to-indigo-600 text-white p-6 rounded-2xl shadow">
          <h1 className="text-2xl font-bold">Welcome back 👋</h1>
          <p className="mt-2">
            {level ? `You are ${level} – Beginner` : "Loading..."}
          </p>

          <button className="mt-4 bg-white text-blue-600 px-4 py-2 rounded-lg font-medium hover:bg-gray-100">
            Start Today’s Task
          </button>
        </div>

        {/* PROGRESS */}
        <div className="bg-white p-5 rounded-xl shadow">
          <h2 className="font-semibold mb-3">📊 Your Progress</h2>

          <div className="w-full bg-gray-200 h-2 rounded">
            <div className="bg-blue-500 h-2 rounded w-[20%]" />
          </div>

          <p className="text-sm mt-2 text-gray-600">
            6 / 30 problems completed
          </p>
        </div>

        {/* ROADMAP */}
        <div className="bg-white p-5 rounded-xl shadow">
          <h2 className="font-semibold mb-3">📍 Your Roadmap</h2>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="p-4 border rounded-lg hover:shadow">
              <h3 className="font-medium">Arrays</h3>
              <p className="text-sm text-gray-500">Start with basics</p>
            </div>

            <div className="p-4 border rounded-lg hover:shadow">
              <h3 className="font-medium">Strings</h3>
              <p className="text-sm text-gray-500">Pattern practice</p>
            </div>

            <div className="p-4 border rounded-lg hover:shadow">
              <h3 className="font-medium">Recursion</h3>
              <p className="text-sm text-gray-500">Build logic</p>
            </div>
          </div>
        </div>

        {/* WEEKLY PLAN */}
        <div className="bg-white p-5 rounded-xl shadow">
          <h2 className="font-semibold mb-3">📅 Weekly Plan</h2>

          <div className="space-y-2 text-gray-700">
            <p>Week 1 → Arrays (10 questions)</p>
            <p>Week 2 → Strings (10 questions)</p>
            <p>Week 3 → Recursion basics</p>
          </div>
        </div>

        {/* TODAY TASK */}
        <div className="bg-white p-5 rounded-xl shadow flex justify-between items-center">
          <div>
            <h2 className="font-semibold">🎯 Today’s Goal</h2>
            <p className="text-gray-600">Solve 2 problems</p>
          </div>

          <button className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700">
            Start
          </button>
        </div>

      </div>
    );
}