"use client";

import { useState, useEffect, useMemo } from "react";
import { Career } from "@/lib/careersData";
import { careerService } from "@/lib/careerService";
import { skillService } from "@/lib/skillService";
import { progressService } from "@/lib/progressService";
import { recommendationService, PersonalizedIntelligence } from "@/lib/recommendationService";
import CareerCard from "@/components/CareerCard";
import CareerModal from "@/components/CareerModal";

export default function CareersPage() {
  const [careers, setCareers] = useState<Career[]>([]);
  const [userSkills, setUserSkills] = useState<string[]>([]);
  const [intelligence, setIntelligence] = useState<PersonalizedIntelligence | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState<string>("All");
  const [selectedLevel, setSelectedLevel] = useState<string>("All");
  const [activeModalCareer, setActiveModalCareer] = useState<Career | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadData() {
      setLoading(true);
      try {
        const [loadedCareers, loadedSkills, loadedIntelligence] = await Promise.all([
          careerService.getAllCareers(),
          skillService.getUserSkills(),
          recommendationService.getRecommendations(),
        ]);
        setCareers(loadedCareers);
        setUserSkills(loadedSkills);
        if (loadedIntelligence) {
          setIntelligence(loadedIntelligence);
        }
      } catch {
        // Safe fallback handled in services
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, []);

  const categories = ["All", "Software Engineering", "Data & AI", "Cloud & DevOps"];
  const levels = ["All", "Beginner", "Intermediate", "Advanced"];

  // Map career matches for fast lookup
  const matchMap = useMemo(() => {
    const map = new Map<string, { score: number; missing: string[]; isTarget: boolean }>();
    if (intelligence?.careerMatches) {
      intelligence.careerMatches.forEach((m) => {
        map.set(m.title.toLowerCase(), {
          score: m.matchScore,
          missing: m.missingSkills,
          isTarget: m.targetGoal,
        });
      });
    }
    return map;
  }, [intelligence]);

  // Filtered & sorted careers
  const filteredCareers = useMemo(() => {
    return careers
      .filter((career) => {
        // Category filter
        if (selectedCategory !== "All" && career.category !== selectedCategory) {
          return false;
        }

        // Level filter
        if (selectedLevel !== "All" && career.level !== selectedLevel) {
          return false;
        }

        // Search query
        if (searchQuery.trim()) {
          const q = searchQuery.toLowerCase();
          const matchTitle = career.title.toLowerCase().includes(q);
          const matchDesc = career.description.toLowerCase().includes(q);
          const matchTech = career.technologies.some((t) =>
            t.toLowerCase().includes(q)
          );
          const matchSkill = career.requiredSkills.some((s) =>
            s.toLowerCase().includes(q)
          );

          return matchTitle || matchDesc || matchTech || matchSkill;
        }

        return true;
      })
      .sort((a, b) => {
        const scoreA = matchMap.get(a.title.toLowerCase())?.score ?? 0;
        const scoreB = matchMap.get(b.title.toLowerCase())?.score ?? 0;
        return scoreB - scoreA;
      });
  }, [careers, searchQuery, selectedCategory, selectedLevel, matchMap]);

  const handleSelectCareer = async (career: Career) => {
    await progressService.updateCareerGoal(career.title);
  };

  const handleResetFilters = () => {
    setSearchQuery("");
    setSelectedCategory("All");
    setSelectedLevel("All");
  };

  return (
    <div className="max-w-7xl mx-auto space-y-8 pb-12">
      {/* Hero / Header Section */}
      <div className="bg-gradient-to-r from-gray-900 via-gray-800 to-black text-white p-8 rounded-2xl shadow-lg relative overflow-hidden">
        <div className="relative z-10 max-w-3xl">
          <span className="text-xs uppercase tracking-widest text-blue-400 font-semibold mb-2 block">
            Career Intelligence & Discovery
          </span>
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
            Explore Tech Career Paths 🎯
          </h1>
          <p className="text-gray-300 text-sm sm:text-base mt-2 leading-relaxed">
            Discover tailored career tracks with calibrated match scores, required
            technologies, and step-by-step milestone roadmaps.
          </p>
        </div>
      </div>

      {/* Filter and Search Controls */}
      <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
        <div className="flex flex-col md:flex-row gap-4 justify-between items-stretch md:items-center">
          {/* Search bar */}
          <div className="relative flex-1">
            <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gray-400">
              🔍
            </span>
            <input
              type="text"
              placeholder="Search careers by title, skill, or tech stack (e.g. React, Java, Docker)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-black focus:bg-white transition"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery("")}
                className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-xs text-gray-400 hover:text-black"
              >
                Clear
              </button>
            )}
          </div>

          {/* Level Filter */}
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
              Level:
            </span>
            <div className="flex gap-1 bg-gray-100 p-1 rounded-xl">
              {levels.map((level) => (
                <button
                  key={level}
                  onClick={() => setSelectedLevel(level)}
                  className={`text-xs px-3 py-1.5 rounded-lg font-medium transition cursor-pointer ${
                    selectedLevel === level
                      ? "bg-white text-black shadow-xs"
                      : "text-gray-600 hover:text-black"
                  }`}
                >
                  {level}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Category Pills */}
        <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-gray-100">
          <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider mr-2">
            Category:
          </span>
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`text-xs px-3 py-1.5 rounded-full font-medium transition cursor-pointer ${
                selectedCategory === cat
                  ? "bg-black text-white"
                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* Results Header */}
      <div className="flex items-center justify-between px-1">
        <p className="text-sm font-semibold text-gray-700">
          Showing {filteredCareers.length} career track
          {filteredCareers.length === 1 ? "" : "s"}
        </p>
        {(searchQuery || selectedCategory !== "All" || selectedLevel !== "All") && (
          <button
            onClick={handleResetFilters}
            className="text-xs text-blue-600 font-semibold hover:underline cursor-pointer"
          >
            Reset All Filters
          </button>
        )}
      </div>

      {/* Careers Grid */}
      {filteredCareers.length === 0 ? (
        <div className="bg-white rounded-2xl border border-gray-200 p-12 text-center shadow-sm">
          <div className="text-4xl mb-3">🔍</div>
          <h3 className="text-lg font-bold text-gray-900">No careers found</h3>
          <p className="text-sm text-gray-500 max-w-md mx-auto mt-1 mb-6">
            We couldn't find any career track matching your search criteria. Try
            adjusting your filters.
          </p>
          <button
            onClick={handleResetFilters}
            className="bg-black text-white px-5 py-2 rounded-xl text-sm font-medium hover:bg-gray-800 transition"
          >
            Clear Filters
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredCareers.map((career) => {
            const matchData = matchMap.get(career.title.toLowerCase());
            return (
              <CareerCard
                key={career.id}
                career={career}
                userSkills={userSkills}
                intelligenceScore={matchData?.score}
                missingSkills={matchData?.missing}
                isTargetGoal={matchData?.isTarget}
                onViewDetails={(c) => setActiveModalCareer(c)}
                onSelectCareer={handleSelectCareer}
              />
            );
          })}
        </div>
      )}

      {/* Career Details Modal */}
      <CareerModal
        career={activeModalCareer}
        isOpen={Boolean(activeModalCareer)}
        userSkills={userSkills}
        onClose={() => setActiveModalCareer(null)}
        onSelectCareer={handleSelectCareer}
      />
    </div>
  );
}