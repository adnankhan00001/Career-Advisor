"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { getCareerSuggestions } from "@/lib/careerLogic";
import { skillService } from "@/lib/skillService";

const SUGGESTED_POPULAR_SKILLS = [
  "Java",
  "Spring Boot",
  "React",
  "Next.js",
  "TypeScript",
  "JavaScript",
  "Python",
  "SQL",
  "PostgreSQL",
  "Docker",
  "Kubernetes",
  "AWS",
  "Machine Learning",
  "PyTorch",
  "Git",
  "Pandas",
  "Power BI",
  "HTML",
  "CSS",
  "REST APIs",
];

export default function Skills() {
  const [skills, setSkills] = useState<string[]>([]);
  const [input, setInput] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadSkills() {
      setLoading(true);
      try {
        const liveSkills = await skillService.getUserSkills();
        setSkills(liveSkills);
      } catch {
        // Fallback
      } finally {
        setLoading(false);
      }
    }

    loadSkills();
  }, []);

  const handleAdd = async (skillToAdd?: string) => {
    const target = (skillToAdd || input).trim();
    if (!target) return;

    // Check duplicate locally
    if (skills.some((s) => s.toLowerCase() === target.toLowerCase())) {
      showMessage(`"${target}" is already in your skills portfolio.`);
      setInput("");
      return;
    }

    // Optimistic update
    const optimistic = [...skills, target];
    setSkills(optimistic);
    setInput("");
    showMessage(`Added "${target}" to your skills!`);

    // Call backend
    const updated = await skillService.addSkill(target);
    if (updated && Array.isArray(updated)) {
      setSkills(updated);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      handleAdd();
    }
  };

  const handleRemove = async (skillToRemove: string) => {
    // Optimistic update
    const optimistic = skills.filter((s) => s !== skillToRemove);
    setSkills(optimistic);
    showMessage(`Removed "${skillToRemove}".`);

    // Call backend
    const updated = await skillService.removeSkill(skillToRemove);
    if (updated && Array.isArray(updated)) {
      setSkills(updated);
    }
  };

  const showMessage = (msg: string) => {
    setMessage(msg);
    setTimeout(() => setMessage(null), 3000);
  };

  const suggestions = getCareerSuggestions(skills);
  const unselectedPopular = SUGGESTED_POPULAR_SKILLS.filter(
    (pop) => !skills.some((s) => s.toLowerCase() === pop.toLowerCase())
  );

  return (
    <div className="max-w-4xl mx-auto space-y-8 pb-16">
      {/* Toast Notice */}
      {message && (
        <div className="fixed top-5 right-5 z-50 bg-black text-white px-5 py-3 rounded-xl shadow-xl text-xs sm:text-sm font-medium animate-fade-in flex items-center gap-2">
          <span>✓</span>
          <span>{message}</span>
        </div>
      )}

      {/* Header */}
      <div>
        <span className="text-xs uppercase tracking-wider text-blue-600 font-bold bg-blue-50 px-2.5 py-0.5 rounded-md">
          Skills Portfolio
        </span>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 mt-1">
          Manage Your Technical Skills ⚡
        </h1>
        <p className="text-gray-600 text-sm mt-1">
          Add your programming languages, frameworks, databases, and developer
          tools to calculate real-time career match scores.
        </p>
      </div>

      {/* Add Skill Input Card */}
      <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
        <label className="block text-xs font-bold uppercase tracking-wider text-gray-600">
          Add Custom Skill
        </label>

        <div className="flex gap-3">
          <input
            type="text"
            placeholder="e.g. Docker, Spring Security, GraphQL..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            className="flex-1 bg-gray-50 border border-gray-300 px-4 py-3 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-black focus:bg-white transition"
          />

          <button
            onClick={() => handleAdd()}
            disabled={!input.trim()}
            className={`px-6 py-3 rounded-xl font-semibold text-sm transition cursor-pointer ${
              input.trim()
                ? "bg-black text-white hover:bg-gray-800 shadow-sm"
                : "bg-gray-200 text-gray-400 cursor-not-allowed"
            }`}
          >
            + Add
          </button>
        </div>

        {/* Quick Add Suggestions */}
        {unselectedPopular.length > 0 && (
          <div className="pt-2">
            <span className="text-xs text-gray-500 block mb-2 font-medium">
              Popular skills you might know (click to add):
            </span>
            <div className="flex flex-wrap gap-1.5">
              {unselectedPopular.slice(0, 10).map((pop) => (
                <button
                  key={pop}
                  onClick={() => handleAdd(pop)}
                  className="bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs px-3 py-1.5 rounded-lg font-medium transition flex items-center gap-1 border border-gray-200 cursor-pointer"
                >
                  <span>+</span>
                  <span>{pop}</span>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Active Skills Card */}
      <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-gray-100">
          <h2 className="text-base font-bold text-gray-900">
            Active Skills ({skills.length})
          </h2>
          {skills.length > 0 && (
            <span className="text-xs text-gray-400">
              Click &times; on any badge to remove
            </span>
          )}
        </div>

        {skills.length === 0 ? (
          <div className="text-center py-10 bg-gray-50 rounded-xl border border-dashed border-gray-200">
            <p className="text-sm font-semibold text-gray-700 mb-1">
              No skills added yet
            </p>
            <p className="text-xs text-gray-500 max-w-sm mx-auto">
              Type a skill above or click one of the suggested popular skills to
              get started.
            </p>
          </div>
        ) : (
          <div className="flex flex-wrap gap-2.5">
            {skills.map((skill, index) => (
              <div
                key={index}
                className="flex items-center gap-2 bg-gray-900 text-white px-3.5 py-1.5 rounded-xl text-xs sm:text-sm font-medium shadow-xs"
              >
                <span>{skill}</span>
                <button
                  onClick={() => handleRemove(skill)}
                  className="w-4 h-4 rounded-full bg-white/20 hover:bg-red-500 hover:text-white flex items-center justify-center text-xs transition cursor-pointer"
                  aria-label={`Remove ${skill}`}
                >
                  ✕
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Career Recommendations Impact */}
      <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-bold text-gray-900">
              Your Skills Unlock Career Opportunities
            </h2>
            <p className="text-xs text-gray-500 mt-0.5">
              Based on your {skills.length} logged skills:
            </p>
          </div>
          <Link
            href="/careers"
            className="text-xs font-semibold text-blue-600 hover:underline"
          >
            Explore Careers →
          </Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-1">
          {suggestions.slice(0, 3).map((item, idx) => (
            <div
              key={idx}
              className="p-4 bg-gray-50 rounded-xl border border-gray-100 flex flex-col justify-between"
            >
              <div>
                <span className="text-xs font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded-md inline-block mb-1">
                  {item.match} match
                </span>
                <h3 className="text-sm font-bold text-gray-900 mt-1">
                  {item.title}
                </h3>
              </div>
              <Link
                href={`/roadmap?career=${encodeURIComponent(item.title)}`}
                className="text-xs text-gray-700 hover:text-black font-semibold mt-3 block"
              >
                Open Roadmap →
              </Link>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}