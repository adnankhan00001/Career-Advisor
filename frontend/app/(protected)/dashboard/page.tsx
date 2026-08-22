"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { getCareerSuggestions } from "@/lib/careerLogic";
import { careersData, calculateSkillMatch } from "@/lib/careersData";
import { roadmapData } from "@/lib/roadmapData";
import { progressService, ProgressSummary } from "@/lib/progressService";
import { problemService, ProblemProgressSummary } from "@/lib/problemService";
import { recommendationService, PersonalizedIntelligence } from "@/lib/recommendationService";
import { interviewService, InterviewSummary } from "@/lib/interviewService";
import { getLatestResumeAnalysis, ResumeAnalysis } from "@/lib/resumeService";
import PersonalizedPlan from "@/components/PersonalizedPlan";

export default function Dashboard() {
  const { user } = useAuth();

  const [summary, setSummary] = useState<ProgressSummary | null>(null);
  const [practiceSummary, setPracticeSummary] = useState<ProblemProgressSummary | null>(null);
  const [interviewSummary, setInterviewSummary] = useState<InterviewSummary | null>(null);
  const [intelligence, setIntelligence] = useState<PersonalizedIntelligence | null>(null);
  const [resumeAnalysis, setResumeAnalysis] = useState<ResumeAnalysis | null>(null);
  const [skills, setSkills] = useState<string[]>([]);
  const [completedSteps, setCompletedSteps] = useState<string[]>([]);
  const [careerGoal, setCareerGoal] = useState<string>("Java Backend Developer");
  const [userLevel, setUserLevel] = useState<string | null>(null);
  const [quizScore, setQuizScore] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    async function loadDashboardData() {
      setLoading(true);
      try {
        const [liveSummary, livePractice, liveIntelligence, liveInterview, liveResume] = await Promise.all([
          progressService.getProgressSummary(),
          problemService.getProgressSummary(),
          recommendationService.getRecommendations(),
          interviewService.getInterviewSummary().catch(() => null),
          getLatestResumeAnalysis().catch(() => null),
        ]);

        if (liveIntelligence) {
          setIntelligence(liveIntelligence);
        }

        if (livePractice) {
          setPracticeSummary(livePractice);
        }

        if (liveInterview) {
          setInterviewSummary(liveInterview);
        }

        if (liveResume) {
          setResumeAnalysis(liveResume);
        }

        if (liveSummary) {
          setSummary(liveSummary);
          setCareerGoal(liveSummary.careerGoal);
          setSkills(liveSummary.skills);
          setCompletedSteps(liveSummary.completedSteps);
          setUserLevel(liveSummary.userLevel);
          setQuizScore(liveSummary.latestQuizScore);
        } else {
          // Fallback to local storage
          const savedSkills = JSON.parse(localStorage.getItem("skills") || "[]");
          const savedCompleted = JSON.parse(
            localStorage.getItem("completedSteps") || "[]"
          );
          const savedGoal =
            localStorage.getItem("careerGoal") ||
            localStorage.getItem("selectedCareer") ||
            "Java Backend Developer";
          const savedLevel = localStorage.getItem("userLevel");
          const savedQuizScore = localStorage.getItem("quizScore");

          setSkills(savedSkills);
          setCompletedSteps(savedCompleted);
          setCareerGoal(savedGoal);
          setUserLevel(savedLevel);
          setQuizScore(savedQuizScore);
        }
      } catch {
        // Safe fallback
      } finally {
        setLoading(false);
      }
    }

    loadDashboardData();
  }, []);

  // Career Suggestions
  const suggestions = getCareerSuggestions(skills);

  // Skill Match for Target Career
  const targetCareer = careersData.find((c) => c.title === careerGoal);
  const matchPercentage = targetCareer
    ? calculateSkillMatch(skills, targetCareer.requiredSkills).percentage
    : 0;

  // Active Roadmap Calculation
  const activeRoadmap = roadmapData[careerGoal] || Object.values(roadmapData)[0];
  const totalRoadmapSteps = activeRoadmap
    ? activeRoadmap.reduce((acc, sec) => acc + sec.steps.length, 0)
    : 0;

  const completedInGoal = summary?.completedStepsCount ?? completedSteps.length;
  const roadmapPercent =
    summary?.roadmapPercent ??
    (totalRoadmapSteps > 0
      ? Math.round((completedInGoal / totalRoadmapSteps) * 100)
      : 0);

  // DSA Practice Solved Calculation
  const dsaSolved = practiceSummary?.solvedProblems ?? 0;
  const dsaTotal = practiceSummary?.totalProblems ?? 22;
  const dsaPercent = practiceSummary?.completionPercentage ?? 0;

  const displayName = user?.name || "Explorer";

  return (
    <div className="space-y-8 max-w-7xl mx-auto pb-12">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-gray-950 via-gray-900 to-black text-white p-6 sm:p-8 rounded-2xl border border-gray-800 shadow-md flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              Live Career Track
            </span>
            {userLevel && (
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-500/10 text-blue-400 border border-blue-500/20">
                {userLevel} Tier
              </span>
            )}
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
            Hello, {displayName} 👋
          </h1>
          <p className="text-gray-300 text-sm mt-1 max-w-xl">
            You are actively progressing on the{" "}
            <span className="font-semibold text-white">{careerGoal}</span> track.
            Keep building your skills, roadmaps, and practice questions!
          </p>
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto flex-wrap">
          <Link
            href="/mock-interview"
            className="flex-1 md:flex-none py-2.5 px-4 bg-purple-600 hover:bg-purple-700 text-white text-sm font-semibold rounded-xl transition shadow-sm text-center"
          >
            Mock Interview 🎯
          </Link>
          <Link
            href="/practice"
            className="flex-1 md:flex-none py-2.5 px-4 bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-semibold rounded-xl transition shadow-sm text-center"
          >
            Practice Hub 💡
          </Link>
          <Link
            href={`/roadmap?career=${encodeURIComponent(careerGoal)}`}
            className="flex-1 md:flex-none py-2.5 px-5 bg-white text-black text-sm font-semibold rounded-xl hover:bg-gray-100 transition shadow-sm text-center"
          >
            Resume Roadmap →
          </Link>
        </div>
      </div>

      {/* Personalized Intelligence Plan Section */}
      <PersonalizedPlan intelligence={intelligence} loading={loading} />

      {/* Metrics Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Track Progress */}
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm">
          <span className="text-xs text-gray-500 font-medium block">
            Roadmap Progress
          </span>
          <div className="flex items-baseline gap-2 mt-1">
            <span className="text-2xl font-bold text-gray-900">
              {roadmapPercent}%
            </span>
            <span className="text-xs text-gray-500">
              ({completedInGoal}/{totalRoadmapSteps})
            </span>
          </div>
          <div className="w-full bg-gray-100 h-1.5 rounded-full overflow-hidden mt-3">
            <div
              className="bg-blue-600 h-1.5 rounded-full transition-all duration-500"
              style={{ width: `${roadmapPercent}%` }}
            />
          </div>
        </div>

        {/* DSA & Practice Solved */}
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm">
          <span className="text-xs text-gray-500 font-medium block">
            DSA Practice
          </span>
          <div className="flex items-baseline gap-2 mt-1">
            <span className="text-2xl font-bold text-emerald-600">
              {dsaPercent}%
            </span>
            <span className="text-xs text-gray-500">
              ({dsaSolved}/{dsaTotal} solved)
            </span>
          </div>
          <div className="w-full bg-gray-100 h-1.5 rounded-full overflow-hidden mt-3">
            <div
              className="bg-emerald-500 h-1.5 rounded-full transition-all duration-500"
              style={{ width: `${dsaPercent}%` }}
            />
          </div>
        </div>

        {/* Interview Readiness */}
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm flex flex-col justify-between">
          <div>
            <span className="text-xs text-gray-500 font-medium block">
              Interview Readiness
            </span>
            <div className="flex items-baseline gap-2 mt-1">
              <span className="text-2xl font-bold text-purple-600">
                {interviewSummary && interviewSummary.completedInterviews > 0
                  ? `${interviewSummary.averageScore}%`
                  : "Not taken"}
              </span>
              {interviewSummary && interviewSummary.completedInterviews > 0 && (
                <span className="text-xs text-gray-500">
                  ({interviewSummary.completedInterviews} completed)
                </span>
              )}
            </div>
          </div>
          <Link
            href="/mock-interview"
            className="text-xs font-semibold text-purple-600 hover:underline mt-3 inline-block"
          >
            {interviewSummary && interviewSummary.completedInterviews > 0
              ? `Weakest: ${interviewSummary.weakestCategory} →`
              : "Start Mock Interview →"}
          </Link>
        </div>

        {/* Target Goal Match */}
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm">
          <span className="text-xs text-gray-500 font-medium block">
            Target Goal Match
          </span>
          <div className="flex items-baseline gap-2 mt-1">
            <span className="text-2xl font-bold text-indigo-600">
              {matchPercentage}%
            </span>
            <span className="text-xs text-gray-500">skill alignment</span>
          </div>
          <Link
            href="/careers"
            className="text-xs font-semibold text-blue-600 hover:underline mt-3 inline-block"
          >
            Explore all careers
          </Link>
        </div>
      </div>

      {/* Resume Intelligence Banner */}
      <div className="bg-gradient-to-r from-blue-900/5 via-indigo-900/5 to-purple-900/5 border border-blue-200/80 rounded-2xl p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="w-11 h-11 rounded-xl bg-blue-600 text-white flex items-center justify-center text-xl font-bold shadow-xs">
            📄
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-bold text-gray-900">
                {resumeAnalysis ? "Resume Intelligence & Skill Alignment" : "Automate Skill Gap Detection with Your Resume"}
              </h3>
              {resumeAnalysis && (
                <span className="text-[10px] font-bold px-2 py-0.5 bg-emerald-100 text-emerald-800 rounded-full">
                  Analyzed
                </span>
              )}
            </div>
            <p className="text-xs text-gray-600 mt-0.5">
              {resumeAnalysis
                ? `Resume "${resumeAnalysis.fileName}" analyzed • ${resumeAnalysis.extractedSkills.length} skills detected • Top match: ${resumeAnalysis.matchedCareers[0]?.title || careerGoal} (${resumeAnalysis.matchedCareers[0]?.matchScore || 80}%)`
                : "Upload your resume in PDF or DOCX to extract technical skills and calculate match scores against 7 career roadmaps."}
            </p>
          </div>
        </div>

        <Link
          href="/resume"
          className="whitespace-nowrap px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-xl transition shadow-xs"
        >
          {resumeAnalysis ? "View Resume Analysis →" : "Upload Resume 📄"}
        </Link>
      </div>

      {/* Main Grid: Suggestions & Skills */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Career Suggestions with Intelligence Scores */}
        <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-bold text-gray-900">
              Matched Career Suggestions
            </h2>
            <Link
              href="/careers"
              className="text-xs font-semibold text-blue-600 hover:underline"
            >
              View All →
            </Link>
          </div>

          {skills.length === 0 ? (
            <div className="p-6 text-center bg-gray-50 rounded-xl border border-dashed border-gray-200">
              <p className="text-xs text-gray-500 mb-2">
                Add your technical skills to unlock accurate match ratings and gap analysis.
              </p>
              <Link
                href="/skills"
                className="px-3 py-1.5 bg-black text-white text-xs rounded-lg font-medium inline-block"
              >
                + Add Skills
              </Link>
            </div>
          ) : (
            <div className="space-y-3">
              {suggestions.slice(0, 3).map((career, idx) => (
                <div
                  key={career.careerId || career.title || idx}
                  className="p-4 rounded-xl border border-gray-100 bg-gray-50/50 hover:bg-gray-50 transition flex items-center justify-between"
                >
                  <div>
                    <h3 className="text-sm font-semibold text-gray-900">
                      {career.title}
                    </h3>
                    <p className="text-xs text-gray-500 mt-0.5">
                      {career.matchedCount && career.totalRequired
                        ? `${career.matchedCount} of ${career.totalRequired} skills matched`
                        : "Recommended career track"}
                    </p>
                  </div>
                  <div className="text-right">
                    <span className="text-sm font-bold text-emerald-600">
                      {career.match}
                    </span>
                    <span className="text-[11px] text-gray-400 block">
                      match
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Current Skills Portfolio */}
        <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-bold text-gray-900">
              Your Skills ({skills.length})
            </h2>
            <Link
              href="/skills"
              className="text-xs font-semibold text-blue-600 hover:underline"
            >
              Manage Skills →
            </Link>
          </div>

          {skills.length === 0 ? (
            <div className="p-6 text-center bg-gray-50 rounded-xl border border-dashed border-gray-200">
              <p className="text-xs text-gray-500 mb-2">
                You haven't logged any skills yet.
              </p>
              <Link
                href="/skills"
                className="px-3 py-1.5 bg-black text-white text-xs rounded-lg font-medium inline-block"
              >
                + Add Your First Skill
              </Link>
            </div>
          ) : (
            <div className="flex flex-wrap gap-2">
              {skills.map((skill, index) => (
                <span
                  key={index}
                  className="px-3 py-1.5 bg-gray-50 border border-gray-200 text-gray-800 text-xs font-medium rounded-lg"
                >
                  {skill}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}