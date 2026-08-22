"use client";

import { useEffect, useState, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { roadmapData, RoadmapSection } from "@/lib/roadmapData";
import { careersData } from "@/lib/careersData";
import { getCareerSuggestions } from "@/lib/careerLogic";
import { roadmapService } from "@/lib/roadmapService";
import { progressService } from "@/lib/progressService";
import { skillService } from "@/lib/skillService";

function RoadmapContent() {
  const searchParams = useSearchParams();

  const [allRoadmaps, setAllRoadmaps] = useState<Record<string, RoadmapSection[]>>(roadmapData);
  const [skills, setSkills] = useState<string[]>([]);
  const [selectedCareer, setSelectedCareer] = useState<string>("Java Backend Developer");
  const [careerGoal, setCareerGoal] = useState<string>("Java Backend Developer");
  const [completed, setCompleted] = useState<string[]>([]);
  const [filterQuery, setFilterQuery] = useState("");
  const [notice, setNotice] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadRoadmapData() {
      setLoading(true);
      try {
        const [loadedRoadmaps, loadedCompleted, loadedSkills, progressSummary] =
          await Promise.all([
            roadmapService.getAllRoadmaps(),
            progressService.getCompletedSteps(),
            skillService.getUserSkills(),
            progressService.getProgressSummary(),
          ]);

        if (loadedRoadmaps && Object.keys(loadedRoadmaps).length > 0) {
          setAllRoadmaps(loadedRoadmaps);
        }

        setCompleted(loadedCompleted);
        setSkills(loadedSkills);

        const goal = progressSummary?.careerGoal || "Java Backend Developer";
        setCareerGoal(goal);

        // Read URL query parameter
        const careerParam = searchParams.get("career");
        if (careerParam && (loadedRoadmaps[careerParam] || roadmapData[careerParam])) {
          setSelectedCareer(careerParam);
        } else if (goal && (loadedRoadmaps[goal] || roadmapData[goal])) {
          setSelectedCareer(goal);
        } else {
          const suggestions = getCareerSuggestions(loadedSkills);
          if (suggestions.length > 0 && (loadedRoadmaps[suggestions[0].title] || roadmapData[suggestions[0].title])) {
            setSelectedCareer(suggestions[0].title);
          } else {
            setSelectedCareer(Object.keys(loadedRoadmaps)[0] || "Java Backend Developer");
          }
        }
      } catch {
        // Fallback
      } finally {
        setLoading(false);
      }
    }

    loadRoadmapData();
  }, [searchParams]);

  const availableCareers = Object.keys(allRoadmaps).length > 0 ? Object.keys(allRoadmaps) : Object.keys(roadmapData);

  const toggleStep = async (step: string) => {
    // Optimistic UI update
    let updated: string[];
    if (completed.includes(step)) {
      updated = completed.filter((s) => s !== step);
    } else {
      updated = [...completed, step];
    }
    setCompleted(updated);

    // Call backend
    const serverResult = await progressService.toggleStep(step, selectedCareer);
    if (serverResult && Array.isArray(serverResult)) {
      setCompleted(serverResult);
    }
  };

  const handleSetMainGoal = async () => {
    setCareerGoal(selectedCareer);
    await progressService.updateCareerGoal(selectedCareer);
    showNotice(`"${selectedCareer}" is now your active career goal!`);
  };

  const showNotice = (msg: string) => {
    setNotice(msg);
    setTimeout(() => setNotice(null), 3000);
  };

  const currentSections: RoadmapSection[] =
    allRoadmaps[selectedCareer] || roadmapData[selectedCareer] || [];

  // Compute total steps and completed count
  const totalStepsInCurrent = currentSections.reduce(
    (acc, section) => acc + section.steps.length,
    0
  );
  const completedInCurrent = currentSections.reduce(
    (acc, section) =>
      acc + section.steps.filter((s) => completed.includes(s)).length,
    0
  );
  const progressPercent =
    totalStepsInCurrent > 0
      ? Math.round((completedInCurrent / totalStepsInCurrent) * 100)
      : 0;

  // Find next uncompleted topic
  let nextStepToLearn: { title: string; sectionTitle: string } | null = null;
  for (const sec of currentSections) {
    const uncompleted = sec.steps.find((s) => !completed.includes(s));
    if (uncompleted) {
      nextStepToLearn = { title: uncompleted, sectionTitle: sec.title };
      break;
    }
  }

  const currentCareerMeta = careersData.find((c) => c.title === selectedCareer);

  return (
    <div className="max-w-5xl mx-auto space-y-8 pb-16">
      {/* Toast Notice */}
      {notice && (
        <div className="fixed top-5 right-5 z-50 bg-black text-white px-5 py-3 rounded-xl shadow-xl text-sm font-medium animate-fade-in flex items-center gap-2">
          <span>✓</span>
          <span>{notice}</span>
        </div>
      )}

      {/* Header Banner */}
      <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <span className="text-xs uppercase tracking-wider text-blue-600 font-bold bg-blue-50 px-2.5 py-0.5 rounded-md">
              Career Roadmap Track
            </span>
            {selectedCareer === careerGoal && (
              <span className="text-xs bg-emerald-50 text-emerald-700 font-bold px-2.5 py-0.5 rounded-md border border-emerald-200">
                ★ Active Goal
              </span>
            )}
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900">
            {selectedCareer}
          </h1>
          <p className="text-gray-600 text-sm mt-1 max-w-xl">
            {currentCareerMeta?.overview ||
              "Milestone-based curriculum designed to guide your daily learning."}
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          {selectedCareer !== careerGoal && (
            <button
              onClick={handleSetMainGoal}
              className="py-2.5 px-4 bg-gray-100 text-gray-800 text-xs sm:text-sm font-semibold rounded-xl hover:bg-gray-200 transition border border-gray-200 cursor-pointer"
            >
              Set as Active Goal ★
            </button>
          )}
          <Link
            href="/careers"
            className="py-2.5 px-4 bg-black text-white text-xs sm:text-sm font-semibold rounded-xl hover:bg-gray-800 transition text-center shadow-sm"
          >
            Explore Other Careers
          </Link>
        </div>
      </div>

      {/* Track Selector & Progress Bar */}
      <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <span className="text-xs font-bold text-gray-500 uppercase tracking-wider">
            Switch Learning Track
          </span>
          <div className="text-xs text-gray-500 font-medium">
            Overall Track Progress:{" "}
            <span className="font-bold text-gray-900">
              {completedInCurrent}/{totalStepsInCurrent} topics ({progressPercent}%)
            </span>
          </div>
        </div>

        {/* Career Selection Pills */}
        <div className="flex flex-wrap gap-2">
          {availableCareers.map((title) => (
            <button
              key={title}
              onClick={() => setSelectedCareer(title)}
              className={`px-3.5 py-2 rounded-xl text-xs sm:text-sm font-semibold transition border cursor-pointer ${
                selectedCareer === title
                  ? "bg-black text-white border-black shadow-sm"
                  : "bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100"
              }`}
            >
              {title}
            </button>
          ))}
        </div>

        {/* Global Track Progress Bar */}
        <div className="w-full bg-gray-100 h-2.5 rounded-full overflow-hidden">
          <div
            className="bg-green-500 h-2.5 rounded-full transition-all duration-500"
            style={{ width: `${progressPercent}%` }}
          />
        </div>
      </div>

      {/* Next Milestone Callout Card */}
      {nextStepToLearn ? (
        <div className="bg-gradient-to-r from-blue-900 to-indigo-950 text-white p-6 rounded-2xl shadow-md flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <span className="text-xs uppercase tracking-widest text-blue-300 font-bold block mb-1">
              Next Up in {nextStepToLearn.sectionTitle}
            </span>
            <h3 className="text-lg sm:text-xl font-bold">
              "{nextStepToLearn.title}"
            </h3>
            <p className="text-gray-300 text-xs sm:text-sm mt-1">
              Mark this milestone when you have studied and practiced this topic.
            </p>
          </div>

          <button
            onClick={() => toggleStep(nextStepToLearn!.title)}
            className="py-2.5 px-5 bg-white text-black text-xs sm:text-sm font-bold rounded-xl hover:bg-gray-100 transition whitespace-nowrap shadow-sm cursor-pointer"
          >
            Mark as Completed ✓
          </button>
        </div>
      ) : (
        <div className="bg-emerald-900 text-white p-6 rounded-2xl shadow-md flex items-center justify-between">
          <div>
            <h3 className="text-lg font-bold">
              🎉 Congratulations! You have completed all milestones!
            </h3>
            <p className="text-emerald-200 text-xs sm:text-sm mt-1">
              You have completed all {totalStepsInCurrent} topics for {selectedCareer}.
            </p>
          </div>
          <Link
            href="/careers"
            className="py-2.5 px-4 bg-white text-emerald-950 text-xs sm:text-sm font-bold rounded-xl"
          >
            Explore Next Track →
          </Link>
        </div>
      )}

      {/* Search within Roadmap */}
      <div className="flex items-center justify-between gap-4">
        <input
          type="text"
          placeholder="Filter topics in this roadmap (e.g. REST, SQL, Hooks)..."
          value={filterQuery}
          onChange={(e) => setFilterQuery(e.target.value)}
          className="w-full max-w-md px-4 py-2 bg-white border border-gray-300 rounded-xl text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-black"
        />
        {filterQuery && (
          <button
            onClick={() => setFilterQuery("")}
            className="text-xs text-gray-500 hover:text-black cursor-pointer"
          >
            Clear
          </button>
        )}
      </div>

      {/* Sections and Milestone Steps */}
      <div className="space-y-6">
        {currentSections.map((section, sectionIdx) => {
          const sectionSteps = section.steps.filter((s) =>
            filterQuery.trim()
              ? s.toLowerCase().includes(filterQuery.toLowerCase())
              : true
          );

          if (sectionSteps.length === 0) return null;

          const sectionCompletedCount = section.steps.filter((s) =>
            completed.includes(s)
          ).length;
          const sectionTotal = section.steps.length;
          const sectionDone = sectionCompletedCount === sectionTotal;

          return (
            <div
              key={sectionIdx}
              className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4"
            >
              {/* Section Title & Header */}
              <div className="flex items-center justify-between pb-3 border-b border-gray-100 flex-wrap gap-2">
                <div className="flex items-center gap-3">
                  <span
                    className={`w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold ${
                      sectionDone
                        ? "bg-green-100 text-green-800"
                        : "bg-blue-50 text-blue-700"
                    }`}
                  >
                    {sectionIdx + 1}
                  </span>
                  <h2 className="text-base sm:text-lg font-bold text-gray-900">
                    {section.title}
                  </h2>
                </div>

                <span className="text-xs text-gray-500 font-medium">
                  {sectionCompletedCount}/{sectionTotal} completed
                </span>
              </div>

              {/* Steps List */}
              <div className="space-y-2.5 pt-1">
                {sectionSteps.map((step, stepIdx) => {
                  const isDone = completed.includes(step);

                  return (
                    <div
                      key={stepIdx}
                      onClick={() => toggleStep(step)}
                      className={`p-3.5 rounded-xl border text-xs sm:text-sm font-medium transition cursor-pointer flex items-center justify-between ${
                        isDone
                          ? "bg-emerald-50/70 border-emerald-300 text-emerald-950"
                          : "bg-gray-50/70 border-gray-200 text-gray-800 hover:bg-gray-100/70"
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div
                          className={`w-5 h-5 rounded-md flex items-center justify-center text-xs font-bold transition ${
                            isDone
                              ? "bg-emerald-600 text-white"
                              : "border border-gray-300 bg-white"
                          }`}
                        >
                          {isDone ? "✓" : ""}
                        </div>
                        <span
                          className={isDone ? "line-through text-gray-500" : ""}
                        >
                          {step}
                        </span>
                      </div>

                      <span
                        className={`text-[11px] font-semibold ${
                          isDone ? "text-emerald-700" : "text-gray-400"
                        }`}
                      >
                        {isDone ? "Completed" : "Click to mark"}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default function Roadmap() {
  return (
    <Suspense
      fallback={
        <div className="p-12 text-center text-gray-500">
          Loading learning roadmap...
        </div>
      }
    >
      <RoadmapContent />
    </Suspense>
  );
}