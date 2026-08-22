"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { careersData, calculateSkillMatch } from "@/lib/careersData";
import { roadmapData } from "@/lib/roadmapData";
import { getCareerSuggestions } from "@/lib/careerLogic";
import { progressService, ProgressSummary } from "@/lib/progressService";
import { problemService, ProblemProgressSummary } from "@/lib/problemService";
import { recommendationService, PersonalizedIntelligence } from "@/lib/recommendationService";
import { careerService } from "@/lib/careerService";
import { skillService } from "@/lib/skillService";
import { interviewService, InterviewSummary } from "@/lib/interviewService";
import { getLatestResumeAnalysis, ResumeAnalysis } from "@/lib/resumeService";

export default function Profile() {
  const router = useRouter();
  const { user, logout } = useAuth();

  const [summary, setSummary] = useState<ProgressSummary | null>(null);
  const [practiceSummary, setPracticeSummary] = useState<ProblemProgressSummary | null>(null);
  const [interviewSummary, setInterviewSummary] = useState<InterviewSummary | null>(null);
  const [intelligence, setIntelligence] = useState<PersonalizedIntelligence | null>(null);
  const [resumeAnalysis, setResumeAnalysis] = useState<ResumeAnalysis | null>(null);
  const [skills, setSkills] = useState<string[]>([]);
  const [completedSteps, setCompletedSteps] = useState<string[]>([]);
  const [careerGoal, setCareerGoal] = useState<string>("Java Backend Developer");
  const [userLevel, setUserLevel] = useState<string>("Intermediate");
  const [quizScore, setQuizScore] = useState<string | null>(null);
  const [isEditingGoal, setIsEditingGoal] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [allCareers, setAllCareers] = useState(careersData);

  useEffect(() => {
    async function loadProfileData() {
      try {
        const [liveSummary, liveCareers, liveSkills, livePractice, liveIntelligence, liveInterview, liveResume] =
          await Promise.all([
            progressService.getProgressSummary(),
            careerService.getAllCareers(),
            skillService.getUserSkills(),
            problemService.getProgressSummary(),
            recommendationService.getRecommendations(),
            interviewService.getInterviewSummary().catch(() => null),
            getLatestResumeAnalysis().catch(() => null),
          ]);

        if (liveIntelligence) {
          setIntelligence(liveIntelligence);
        }

        if (liveResume) {
          setResumeAnalysis(liveResume);
        }

        if (livePractice) {
          setPracticeSummary(livePractice);
        }

        if (liveInterview) {
          setInterviewSummary(liveInterview);
        }

        if (liveCareers && liveCareers.length > 0) {
          setAllCareers(liveCareers);
        }

        if (liveSummary) {
          setSummary(liveSummary);
          setCareerGoal(liveSummary.careerGoal);
          setSkills(liveSummary.skills);
          setCompletedSteps(liveSummary.completedSteps);
          setUserLevel(liveSummary.userLevel || "Intermediate");
          setQuizScore(liveSummary.latestQuizScore);
        } else {
          setSkills(liveSkills);
          const savedGoal =
            localStorage.getItem("careerGoal") ||
            localStorage.getItem("selectedCareer") ||
            "Java Backend Developer";
          const savedLevel = localStorage.getItem("userLevel") || "Intermediate";
          const savedQuizScore = localStorage.getItem("quizScore");
          const savedCompleted = JSON.parse(
            localStorage.getItem("completedSteps") || "[]"
          );

          setCareerGoal(savedGoal);
          setUserLevel(savedLevel);
          setQuizScore(savedQuizScore);
          setCompletedSteps(savedCompleted);
        }
      } catch {
        // Fallback
      }
    }

    loadProfileData();
  }, []);

  const handleGoalSave = async (newGoal: string) => {
    setCareerGoal(newGoal);
    setIsEditingGoal(false);
    showNotice("Updating target career goal...");
    await progressService.updateCareerGoal(newGoal);
    showNotice("Target career goal updated!");
  };

  const handleResetProgress = async () => {
    if (
      window.confirm(
        "Are you sure you want to reset your roadmap milestone progress?"
      )
    ) {
      showNotice("Resetting progress on server...");
      await progressService.resetProgress();
      setCompletedSteps([]);
      showNotice("Progress reset successfully.");
    }
  };

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  const showNotice = (msg: string) => {
    setStatusMessage(msg);
    setTimeout(() => setStatusMessage(null), 3000);
  };

  // Target Career Stats
  const targetCareerObj =
    allCareers.find((c) => c.title === careerGoal) || allCareers[0];
  const matchPercentage = targetCareerObj
    ? calculateSkillMatch(skills, targetCareerObj.requiredSkills).percentage
    : 0;

  // Active Roadmap stats
  const activeRoadmap =
    roadmapData[careerGoal] || Object.values(roadmapData)[0];
  const totalStepsInGoal = activeRoadmap
    ? activeRoadmap.reduce((acc, sec) => acc + sec.steps.length, 0)
    : 0;
  const completedStepsInGoal =
    summary?.completedStepsCount ?? completedSteps.length;
  const roadmapPercent =
    summary?.roadmapPercent ??
    (totalStepsInGoal > 0
      ? Math.round((completedStepsInGoal / totalStepsInGoal) * 100)
      : 0);

  // Suggestions
  const suggestions = getCareerSuggestions(skills);

  const displayName = user?.name || "Candidate";
  const displayEmail = user?.email || "candidate@example.com";

  return (
    <div className="max-w-4xl mx-auto space-y-8 pb-12">
      {/* Page Title */}
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
          Your Profile & Career Settings
        </h1>
        <p className="text-sm text-gray-500 mt-1">
          Manage your career target, track multi-dimensional skills, review learning milestones, and inspect mock interview records.
        </p>
      </div>

      {statusMessage && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold rounded-xl animate-fade-in flex items-center justify-between">
          <span>{statusMessage}</span>
          <button onClick={() => setStatusMessage(null)}>✕</button>
        </div>
      )}

      {/* Main Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {/* Left Column: User Card & Quick Actions */}
        <div className="space-y-6">
          {/* Identity Card */}
          <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm text-center">
            <div className="w-20 h-20 bg-gradient-to-tr from-black to-gray-700 text-white rounded-full flex items-center justify-center text-2xl font-bold mx-auto mb-4 shadow-md">
              {displayName.charAt(0).toUpperCase()}
            </div>
            <h2 className="text-lg font-bold text-gray-900">{displayName}</h2>
            <p className="text-xs text-gray-500">{displayEmail}</p>

            <div className="mt-4 pt-4 border-t border-gray-100 flex justify-center gap-2">
              <span className="px-2.5 py-1 bg-gray-100 text-gray-700 rounded-md text-xs font-medium">
                {userLevel}
              </span>
              <span className="px-2.5 py-1 bg-blue-50 text-blue-700 rounded-md text-xs font-medium">
                Active Candidate
              </span>
            </div>

            <div className="mt-6 pt-4 border-t border-gray-100 space-y-2 text-left text-xs text-gray-600">
              <div className="flex justify-between">
                <span className="text-gray-400">Target Track:</span>
                <span className="font-semibold text-gray-800">{careerGoal}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400">Total Skills:</span>
                <span className="font-semibold text-gray-800">
                  {skills.length} logged
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400">Problems Solved:</span>
                <span className="font-semibold text-emerald-600">
                  {practiceSummary?.solvedProblems ?? 0}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400">Interviews Completed:</span>
                <span className="font-semibold text-purple-600">
                  {interviewSummary?.completedInterviews ?? 0}
                </span>
              </div>
            </div>

            <button
              onClick={handleLogout}
              className="mt-6 w-full py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl text-xs font-semibold transition cursor-pointer"
            >
              Sign Out
            </button>
          </div>

          {/* Quick Actions Card */}
          <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm space-y-3">
            <h3 className="text-xs uppercase tracking-wider text-gray-400 font-bold">
              Quick Shortcuts
            </h3>
            <div className="flex flex-col gap-2">
              <Link
                href="/resume"
                className="p-2.5 bg-blue-50 hover:bg-blue-100 text-blue-900 rounded-xl text-xs font-semibold transition flex items-center justify-between"
              >
                <span>Resume Analyzer</span>
                <span>📄</span>
              </Link>
              <Link
                href="/mock-interview"
                className="p-2.5 bg-purple-50 hover:bg-purple-100 text-purple-900 rounded-xl text-xs font-semibold transition flex items-center justify-between"
              >
                <span>Mock Interviews</span>
                <span>🎯</span>
              </Link>
              <Link
                href="/mock-interview/history"
                className="p-2.5 bg-gray-50 hover:bg-gray-100 text-gray-700 rounded-xl text-xs font-semibold transition flex items-center justify-between"
              >
                <span>Interview History</span>
                <span>📋</span>
              </Link>
              <Link
                href="/practice"
                className="p-2.5 bg-gray-50 hover:bg-gray-100 text-gray-700 rounded-xl text-xs font-semibold transition flex items-center justify-between"
              >
                <span>Practice Hub</span>
                <span>💡</span>
              </Link>
              <Link
                href="/skills"
                className="p-2.5 bg-gray-50 hover:bg-gray-100 text-gray-700 rounded-xl text-xs font-semibold transition flex items-center justify-between"
              >
                <span>Manage Skills Portfolio</span>
                <span>⚡</span>
              </Link>
              <Link
                href="/roadmap"
                className="p-2.5 bg-gray-50 hover:bg-gray-100 text-gray-700 rounded-xl text-xs font-semibold transition flex items-center justify-between"
              >
                <span>Explore Roadmaps</span>
                <span>🗺️</span>
              </Link>
            </div>
          </div>
        </div>

        {/* Right Column: Goal Settings & Detailed Stats */}
        <div className="md:col-span-2 space-y-6">
          {/* Target Career Goal Card */}
          <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <span className="text-xs uppercase tracking-wider text-gray-400 font-semibold">
                  Career Objective
                </span>
                <h2 className="text-lg font-bold text-gray-900">
                  Target Tech Career
                </h2>
              </div>
              <button
                onClick={() => setIsEditingGoal(!isEditingGoal)}
                className="text-xs font-semibold text-blue-600 hover:text-blue-800 transition cursor-pointer"
              >
                {isEditingGoal ? "Cancel" : "Change Goal ✎"}
              </button>
            </div>

            {isEditingGoal ? (
              <div className="space-y-3 pt-2">
                <label className="text-xs font-semibold text-gray-700 block">
                  Select your new primary target career:
                </label>
                <select
                  value={careerGoal}
                  onChange={(e) => handleGoalSave(e.target.value)}
                  className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-sm font-semibold text-gray-800 outline-none focus:ring-2 focus:ring-black"
                >
                  {allCareers.map((c) => (
                    <option key={c.id} value={c.title}>
                      {c.title} ({c.category})
                    </option>
                  ))}
                </select>
                <p className="text-xs text-gray-500">
                  Changing your career goal adjusts your roadmap benchmarks and
                  skill match metrics.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 bg-gray-50 rounded-xl border border-gray-100">
                  <div>
                    <h3 className="font-bold text-gray-900 text-lg">
                      {careerGoal}
                    </h3>
                    <p className="text-xs text-gray-500 mt-0.5">
                      {targetCareerObj?.description ||
                        "Structured industry curriculum"}
                    </p>
                  </div>
                  <div className="text-right sm:border-l sm:pl-4 border-gray-200">
                    <span className="text-xs text-gray-500 block">
                      Skill Alignment
                    </span>
                    <span className="text-lg font-bold text-blue-600">
                      {matchPercentage}%
                    </span>
                  </div>
                </div>

                {intelligence?.skillGaps?.highPriorityMissing && intelligence.skillGaps.highPriorityMissing.length > 0 && (
                  <div className="p-3 bg-amber-50 rounded-xl border border-amber-200 text-xs flex items-center justify-between">
                    <span className="text-amber-900 font-medium">
                      Top missing skills to target:{" "}
                      <span className="font-bold">
                        {intelligence.skillGaps.highPriorityMissing.join(", ")}
                      </span>
                    </span>
                    <Link
                      href="/skills"
                      className="text-amber-800 font-bold hover:underline ml-2"
                    >
                      + Add
                    </Link>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Resume & Skill Alignment Card */}
          <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <span className="text-xs uppercase tracking-wider text-gray-400 font-semibold">
                  Resume Intelligence
                </span>
                <h2 className="text-lg font-bold text-gray-900">
                  Resume & Skill Alignment
                </h2>
              </div>
              <Link
                href="/resume"
                className="text-xs font-semibold text-blue-600 hover:text-blue-800"
              >
                {resumeAnalysis ? "Analyze Again 📄" : "Upload Resume 📄"}
              </Link>
            </div>

            {resumeAnalysis ? (
              <div className="space-y-3">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 bg-gray-50 rounded-xl border border-gray-100">
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-bold text-gray-900 text-sm">
                        {resumeAnalysis.fileName}
                      </h3>
                      <span className="text-[10px] font-bold px-2 py-0.5 bg-emerald-100 text-emerald-800 rounded-full">
                        ✓ Analyzed
                      </span>
                    </div>
                    <p className="text-xs text-gray-500 mt-1">
                      {resumeAnalysis.extractedSkills.length} technical skills detected • Top match:{" "}
                      <span className="font-semibold text-gray-800">
                        {resumeAnalysis.matchedCareers[0]?.title || careerGoal}
                      </span>
                    </p>
                  </div>
                  <div className="text-right sm:border-l sm:pl-4 border-gray-200">
                    <span className="text-xs text-gray-500 block">
                      Resume Match
                    </span>
                    <span className="text-lg font-bold text-emerald-600">
                      {resumeAnalysis.matchedCareers[0]?.matchScore || 80}%
                    </span>
                  </div>
                </div>

                <div className="flex items-center justify-between pt-1">
                  <div className="flex flex-wrap gap-1.5">
                    {resumeAnalysis.extractedSkills.slice(0, 6).map((s) => (
                      <span
                        key={s.skillName}
                        className="text-[11px] font-medium px-2.5 py-1 bg-gray-100 text-gray-700 rounded-lg"
                      >
                        {s.skillName}
                      </span>
                    ))}
                    {resumeAnalysis.extractedSkills.length > 6 && (
                      <span className="text-[11px] font-medium px-2 py-1 bg-gray-100 text-gray-500 rounded-lg">
                        +{resumeAnalysis.extractedSkills.length - 6} more
                      </span>
                    )}
                  </div>

                  <Link
                    href="/resume"
                    className="text-xs font-bold text-blue-600 hover:underline shrink-0 ml-3"
                  >
                    View Details →
                  </Link>
                </div>
              </div>
            ) : (
              <div className="p-5 text-center bg-gray-50/70 rounded-xl border border-dashed border-gray-200 space-y-2">
                <p className="text-xs text-gray-500">
                  No resume analyzed yet. Upload your resume to extract skills, compare career tracks, and receive personalized recommendations.
                </p>
                <Link
                  href="/resume"
                  className="px-4 py-2 bg-black text-white text-xs font-bold rounded-xl hover:bg-gray-800 transition inline-block"
                >
                  Upload Resume 📄
                </Link>
              </div>
            )}
          </div>

          {/* Assessment & Skill Readiness */}
          <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div>
                <span className="text-xs uppercase tracking-wider text-gray-400 font-semibold">
                  Evaluation & Readiness
                </span>
                <h2 className="text-lg font-bold text-gray-900">
                  Assessment & Intelligence
                </h2>
              </div>
              <Link
                href="/quiz"
                className="text-xs font-semibold text-blue-600 hover:text-blue-800"
              >
                Retake Assessment 📝
              </Link>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-4">
              <div className="bg-gray-50 p-4 rounded-xl border border-gray-100">
                <span className="text-xs text-gray-500 block">Assessed Level</span>
                <p className="text-lg font-bold text-gray-900 mt-1">
                  {userLevel}
                </p>
              </div>
              <div className="bg-gray-50 p-4 rounded-xl border border-gray-100">
                <span className="text-xs text-gray-500 block">Latest Score</span>
                <p className="text-lg font-bold text-gray-900 mt-1">
                  {quizScore ? `${quizScore}` : "Completed"}
                </p>
              </div>
              <div className="bg-gray-50 p-4 rounded-xl border border-gray-100">
                <span className="text-xs text-gray-500 block">Readiness Score</span>
                <p className="text-lg font-bold text-emerald-600 mt-1">
                  {intelligence?.overallReadinessScore ?? 0}%
                </p>
              </div>
            </div>

            {intelligence?.interviewFocusAction && (
              <div className="p-3.5 bg-blue-50/80 rounded-xl border border-blue-200 text-xs flex items-center justify-between">
                <span className="text-blue-950 font-medium">
                  Recommended Interview Focus:{" "}
                  <span className="font-bold">
                    {intelligence.interviewFocusAction.subject}
                  </span>
                </span>
                <Link
                  href="/mock-interview"
                  className="text-blue-800 font-bold hover:underline"
                >
                  Interview →
                </Link>
              </div>
            )}
          </div>

          {/* Mock Interview Stats Card */}
          <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <span className="text-xs uppercase tracking-wider text-gray-400 font-semibold">
                  Technical Assessments
                </span>
                <h2 className="text-lg font-bold text-gray-900">
                  Mock Interview Performance
                </h2>
              </div>
              <Link
                href="/mock-interview/history"
                className="text-xs font-semibold text-purple-600 hover:text-purple-800"
              >
                View History 📋 →
              </Link>
            </div>

            {interviewSummary && interviewSummary.completedInterviews > 0 ? (
              <div className="space-y-4">
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
                  <div className="p-3 bg-purple-50 rounded-xl border border-purple-100">
                    <span className="text-xs text-purple-700 block">Average</span>
                    <span className="text-xl font-extrabold text-purple-950 mt-0.5 block">
                      {interviewSummary.averageScore}%
                    </span>
                  </div>
                  <div className="p-3 bg-emerald-50 rounded-xl border border-emerald-100">
                    <span className="text-xs text-emerald-700 block">Best Score</span>
                    <span className="text-xl font-extrabold text-emerald-950 mt-0.5 block">
                      {interviewSummary.bestScore}%
                    </span>
                  </div>
                  <div className="p-3 bg-blue-50 rounded-xl border border-blue-100">
                    <span className="text-xs text-blue-700 block">Latest</span>
                    <span className="text-xl font-extrabold text-blue-950 mt-0.5 block">
                      {interviewSummary.latestScore}%
                    </span>
                  </div>
                  <div className="p-3 bg-gray-50 rounded-xl border border-gray-200">
                    <span className="text-xs text-gray-500 block">Completed</span>
                    <span className="text-xl font-extrabold text-gray-900 mt-0.5 block">
                      {interviewSummary.completedInterviews}
                    </span>
                  </div>
                </div>

                <div className="flex items-center justify-between text-xs p-3 bg-gray-50 rounded-xl border border-gray-100 flex-wrap gap-2">
                  <span>
                    Strongest: <span className="font-bold text-emerald-700">{interviewSummary.strongestCategory}</span>
                  </span>
                  <span>
                    Focus Area: <span className="font-bold text-amber-700">{interviewSummary.weakestCategory}</span>
                  </span>
                </div>
              </div>
            ) : (
              <div className="p-6 text-center bg-gray-50 rounded-xl border border-dashed border-gray-200 space-y-2">
                <p className="text-xs text-gray-500">
                  No mock technical interviews completed yet. Test your knowledge under timed conditions.
                </p>
                <Link
                  href="/mock-interview"
                  className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white font-bold text-xs rounded-xl inline-block"
                >
                  Start First Mock Interview 🎯
                </Link>
              </div>
            )}
          </div>

          {/* Learning Progress Breakdown */}
          <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
            <span className="text-xs uppercase tracking-wider text-gray-400 font-semibold">
              Milestones
            </span>
            <h2 className="text-lg font-bold text-gray-900 mb-4">
              Learning Progress & Roadmap
            </h2>

            <div className="space-y-4">
              {/* Roadmap Progress */}
              <div className="p-4 bg-gray-50 rounded-xl border border-gray-100">
                <div className="flex justify-between items-center text-sm font-semibold mb-2">
                  <span className="text-gray-800">{careerGoal} Roadmap</span>
                  <span className="text-blue-600">{roadmapPercent}% Complete</span>
                </div>
                <div className="w-full bg-gray-200 h-2.5 rounded-full overflow-hidden mb-2">
                  <div
                    className="bg-blue-600 h-2.5 rounded-full transition-all duration-500"
                    style={{ width: `${roadmapPercent}%` }}
                  />
                </div>
                <div className="flex justify-between text-xs text-gray-500">
                  <span>
                    {completedStepsInGoal} of {totalStepsInGoal} milestone topics
                    checked
                  </span>
                  <span>{totalStepsInGoal - completedStepsInGoal} topics remaining</span>
                </div>
              </div>

              {/* Reset Danger Zone */}
              <div className="pt-2 flex justify-end">
                <button
                  onClick={handleResetProgress}
                  className="text-xs text-rose-600 hover:text-rose-800 font-semibold transition cursor-pointer"
                >
                  Reset Milestone Progress
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}