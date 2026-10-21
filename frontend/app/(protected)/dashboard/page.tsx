"use client";

import { useEffect, useState, useMemo } from "react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { careersData, calculateSkillMatch } from "@/lib/careersData";
import { roadmapData } from "@/lib/roadmapData";
import { progressService, ProgressSummary, RecentActivity } from "@/lib/progressService";
import { careerIntelligenceService, CareerIntelligence } from "@/lib/careerIntelligenceService";
import { getAiActionPlan, AiActionPlan } from "@/lib/aiService";
import { plannerService, PlannerProgress, LearningTask } from "@/lib/plannerService";
import { mentorService, MentorshipRequestDto } from "@/lib/mentorService";
import StatisticModal, { BreakdownItem } from "@/components/StatisticModal";

export default function Dashboard() {
  const { user } = useAuth();

  const [loading, setLoading] = useState<boolean>(true);
  const [summary, setSummary] = useState<ProgressSummary | null>(null);
  const [careerIntelligence, setCareerIntelligence] = useState<CareerIntelligence | null>(null);
  const [aiActionPlan, setAiActionPlan] = useState<AiActionPlan | null>(null);
  const [plannerProgress, setPlannerProgress] = useState<PlannerProgress | null>(null);
  const [todayTasks, setTodayTasks] = useState<LearningTask[]>([]);
  const [sentRequests, setSentRequests] = useState<MentorshipRequestDto[]>([]);
  const [receivedRequests, setReceivedRequests] = useState<MentorshipRequestDto[]>([]);
  const [recentActivities, setRecentActivities] = useState<RecentActivity[]>([]);

  // Statistics Modal state
  const [activeModal, setActiveModal] = useState<"readiness" | "skills" | "roadmap" | "pending" | null>(null);

  // Load student dashboard data
  useEffect(() => {
    let isMounted = true;

    async function loadDashboardTelemetry() {
      setLoading(true);
      try {
        const [
          liveSummary,
          liveCareerIntel,
          liveActionPlan,
          livePlannerProg,
          liveTodayTasks,
          liveSentRequests,
          liveReceivedRequests,
          liveActivities,
        ] = await Promise.all([
          progressService.getProgressSummary().catch(() => null),
          careerIntelligenceService.getCareerIntelligence().catch(() => null),
          getAiActionPlan().catch(() => null),
          plannerService.getProgress().catch(() => null),
          plannerService.getTodayTasks().catch(() => []),
          mentorService.getSentRequests().catch(() => []),
          mentorService.getReceivedRequests().catch(() => []),
          progressService.getRecentActivities().catch(() => []),
        ]);

        if (!isMounted) return;

        if (liveSummary) setSummary(liveSummary);
        if (liveCareerIntel) setCareerIntelligence(liveCareerIntel);
        if (liveActionPlan) setAiActionPlan(liveActionPlan);
        if (livePlannerProg) setPlannerProgress(livePlannerProg);
        if (Array.isArray(liveTodayTasks)) setTodayTasks(liveTodayTasks);
        if (Array.isArray(liveSentRequests)) setSentRequests(liveSentRequests);
        if (Array.isArray(liveReceivedRequests)) setReceivedRequests(liveReceivedRequests);
        if (Array.isArray(liveActivities)) setRecentActivities(liveActivities);
      } catch {
        // Graceful fallback: defaults handle offline rendering
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    loadDashboardTelemetry();
    return () => {
      isMounted = false;
    };
  }, []);

  // Student Identity & Target Goal
  const displayName = user?.name || "Explorer";
  const careerGoal = summary?.careerGoal || "Java Backend Developer";
  const userLevel = summary?.userLevel || "Intermediate";
  const skills = useMemo(() => summary?.skills || [], [summary]);

  // Target Career Specs & Skill Alignment
  const targetCareer = useMemo(() => careersData.find((c) => c.title === careerGoal), [careerGoal]);
  const skillMatch = useMemo(() => {
    if (!targetCareer) return { percentage: 0, matchedSkills: [], missingSkills: [] };
    return calculateSkillMatch(skills, targetCareer.requiredSkills);
  }, [skills, targetCareer]);

  // Roadmap Progress Computations
  const activeRoadmap = useMemo(() => roadmapData[careerGoal] || Object.values(roadmapData)[0], [careerGoal]);
  const totalRoadmapSteps = useMemo(() => {
    if (summary?.totalStepsCount && summary.totalStepsCount > 0) return summary.totalStepsCount;
    return activeRoadmap ? activeRoadmap.reduce((acc, sec) => acc + sec.steps.length, 0) : 0;
  }, [summary, activeRoadmap]);

  const completedInGoal = summary?.completedStepsCount ?? (summary?.completedSteps?.length || 0);
  const roadmapPercent = totalRoadmapSteps > 0
    ? Math.min(100, Math.round((completedInGoal / totalRoadmapSteps) * 100))
    : (summary?.roadmapPercent || 0);

  // Pending Actions Computations (distinct, non-overlapping)
  const pendingStudyTasks = useMemo(
    () => todayTasks.filter((t) => t.status === "PENDING" || t.status === "IN_PROGRESS").length,
    [todayTasks]
  );
  const pendingSentMentorship = useMemo(
    () => sentRequests.filter((r) => r.status === "PENDING").length,
    [sentRequests]
  );
  const pendingReceivedMentorship = useMemo(
    () => receivedRequests.filter((r) => r.status === "PENDING").length,
    [receivedRequests]
  );
  const pendingDiagnosticQuiz = !summary?.latestQuizScore ? 1 : 0;
  const totalPendingActions =
    pendingStudyTasks + pendingSentMentorship + pendingReceivedMentorship + pendingDiagnosticQuiz;

  // Career Readiness Score
  const readinessScore = careerIntelligence?.overallReadinessScore ?? null;
  const readinessTier = careerIntelligence?.readinessStateDisplayName ?? (readinessScore !== null ? `${readinessScore}% Score` : "Pending Assessment");

  // Determine Next Best Step (Hybrid: Rule-based priority with AI enhancement fallback)
  const nextBestStep = useMemo(() => {
    // 1. If AI plan produced a valid prioritized action
    if (aiActionPlan?.actions && aiActionPlan.actions.length > 0) {
      const firstAi = aiActionPlan.actions[0];
      if (firstAi.title && firstAi.actionUrl) {
        return {
          title: firstAi.title,
          explanation: firstAi.description || "Prioritized by your personalized AI career action plan.",
          reasoning: firstAi.whyItMatters || "Directly accelerates readiness on your active career track.",
          actionUrl: firstAi.actionUrl,
          badge: "AI-Personalized",
        };
      }
    }

    // 2. If Career Intelligence produced deterministic prioritized actions
    if (careerIntelligence?.prioritizedActions && careerIntelligence.prioritizedActions.length > 0) {
      const firstIntel = careerIntelligence.prioritizedActions[0];
      if (firstIntel.title && firstIntel.actionUrl) {
        return {
          title: firstIntel.title,
          explanation: firstIntel.description,
          reasoning: firstIntel.reasoning || "High-impact milestone to bridge career readiness gaps.",
          actionUrl: firstIntel.actionUrl,
          badge: "Priority Milestone",
        };
      }
    }

    // 3. Rule-based evaluation order
    if (!summary?.careerGoal || summary.careerGoal === "Unselected") {
      return {
        title: "Select Your Target Career Goal",
        explanation: "Choose a primary technical track to generate your personalized learning roadmap and diagnostic milestones.",
        reasoning: "A selected career goal is the foundation for all curriculum recommendations and skill assessments.",
        actionUrl: "/careers",
        badge: "Essential First Step",
      };
    }

    if (!summary?.latestQuizScore) {
      return {
        title: "Complete Baseline Diagnostic Assessment",
        explanation: "Take the 10-question adaptive assessment to calibrate your verified technical proficiency tier.",
        reasoning: "Essential diagnostic milestone: Unlocks tailored gap recommendations and readiness scoring.",
        actionUrl: "/quiz",
        badge: "Assessment Milestone",
      };
    }

    if (skills.length === 0) {
      return {
        title: "Log Your Technical Competencies",
        explanation: "Add your verified languages, frameworks, and databases to your portfolio for accurate ATS job matching.",
        reasoning: "Portfolio skills directly power your career readiness scoring and interview recommendations.",
        actionUrl: "/skills",
        badge: "Skill Acquisition",
      };
    }

    if (pendingStudyTasks > 0) {
      return {
        title: "Execute Today's Planned Learning Tasks",
        explanation: `You have ${pendingStudyTasks} study milestone${pendingStudyTasks > 1 ? "s" : ""} scheduled for today in your weekly planner.`,
        reasoning: "Daily consistency builds technical mastery and sustains your active learning streak.",
        actionUrl: "/planner",
        badge: "Daily Study Plan",
      };
    }

    if (roadmapPercent < 100) {
      const nextTopic = summary?.nextTopicToLearn || "Next Topic Milestone";
      return {
        title: `Advance Roadmap: ${nextTopic}`,
        explanation: `Continue progress on the ${careerGoal} curriculum by mastering your next scheduled roadmap topic.`,
        reasoning: `Completing core roadmap topics systematically bridges gaps toward your target ${careerGoal} role.`,
        actionUrl: `/roadmap?career=${encodeURIComponent(careerGoal)}`,
        badge: "Curriculum Progress",
      };
    }

    if (skills.length < 5) {
      return {
        title: "Expand Your Technical Competencies",
        explanation: "Add additional verified languages, frameworks, and databases to broaden your portfolio for ATS job matching.",
        reasoning: "Portfolio skills directly power your career readiness scoring and interview recommendations.",
        actionUrl: "/skills",
        badge: "Skill Acquisition",
      };
    }

    return {
      title: "Practice a Technical Mock Interview",
      explanation: "Test your algorithmic and architectural problem solving with a timed, adaptive mock interview session.",
      reasoning: "Simulating live technical interview pressure improves confidence and identifies knowledge blind spots.",
      actionUrl: "/mock-interview",
      badge: "Interview Readiness",
    };
  }, [aiActionPlan, careerIntelligence, summary, pendingStudyTasks, roadmapPercent, careerGoal, skills.length]);

  // Modal Breakdown Items
  const readinessBreakdown: BreakdownItem[] = useMemo(() => {
    if (!careerIntelligence?.dimensionScores) {
      return [
        {
          label: "Assessment Status",
          value: summary?.latestQuizScore ? "Completed" : "Not Attempted",
          detail: summary?.latestQuizScore ? `Diagnostic score: ${summary.latestQuizScore}%` : "Take diagnostic quiz to calculate baseline score",
          progress: summary?.latestQuizScore ? parseInt(summary.latestQuizScore, 10) : 0,
        },
        {
          label: "Roadmap Milestones",
          value: `${roadmapPercent}%`,
          detail: `${completedInGoal} of ${totalRoadmapSteps} milestones completed`,
          progress: roadmapPercent,
        },
        {
          label: "Verified Skills Alignment",
          value: `${skillMatch.percentage}%`,
          detail: `${skillMatch.matchedSkills.length} of ${targetCareer?.requiredSkills.length || 0} core skills logged`,
          progress: skillMatch.percentage,
        },
      ];
    }

    return Object.values(careerIntelligence.dimensionScores).map((dim) => ({
      label: dim.displayName,
      value: `${dim.score}%`,
      detail: `${dim.statusDescription} • Weight: ${dim.weightPercentage}%`,
      progress: dim.score,
      highlight: dim.score >= 70,
    }));
  }, [careerIntelligence, summary, roadmapPercent, completedInGoal, totalRoadmapSteps, skillMatch, targetCareer]);

  const skillsBreakdown: BreakdownItem[] = useMemo(() => {
    const items: BreakdownItem[] = [];

    items.push({
      label: "Target Role Alignment",
      value: `${skillMatch.percentage}%`,
      detail: `${skillMatch.matchedSkills.length} matched, ${skillMatch.missingSkills.length} missing for ${careerGoal}`,
      progress: skillMatch.percentage,
      highlight: true,
    });

    skills.forEach((skill) => {
      const isRequired = targetCareer?.requiredSkills.some((s) => s.toLowerCase() === skill.toLowerCase());
      items.push({
        label: skill,
        value: isRequired ? "Target Requirement" : "Complementary Skill",
        detail: isRequired ? "Matches core job description" : "Broadens full-stack versatility",
      });
    });

    return items;
  }, [skills, skillMatch, careerGoal, targetCareer]);

  const roadmapBreakdown: BreakdownItem[] = useMemo(() => [
    {
      label: "Active Career Track",
      value: careerGoal,
      detail: "Curriculum customized to this industry role",
    },
    {
      label: "Milestones Completed",
      value: `${completedInGoal} / ${totalRoadmapSteps}`,
      detail: `${Math.max(0, totalRoadmapSteps - completedInGoal)} remaining to master`,
      progress: roadmapPercent,
      highlight: true,
    },
    {
      label: "Next Immediate Milestone",
      value: summary?.nextTopicToLearn || "All core milestones complete!",
      detail: "Advance your learning by tackling this concept next",
    },
  ], [careerGoal, completedInGoal, totalRoadmapSteps, roadmapPercent, summary]);

  const pendingBreakdown: BreakdownItem[] = useMemo(() => {
    const items: BreakdownItem[] = [];

    if (pendingStudyTasks > 0) {
      items.push({
        label: "Today's Study Tasks",
        value: `${pendingStudyTasks} pending`,
        detail: "Scheduled in your weekly learning planner",
        highlight: true,
      });
    }

    if (pendingSentMentorship > 0) {
      items.push({
        label: "Outgoing Mentorship Requests",
        value: `${pendingSentMentorship} awaiting mentor`,
        detail: "Pending review by requested industry mentors",
      });
    }

    if (pendingReceivedMentorship > 0) {
      items.push({
        label: "Incoming Mentorship Requests",
        value: `${pendingReceivedMentorship} to review`,
        detail: "Mentees requesting your mentorship guidance",
        highlight: true,
      });
    }

    if (pendingDiagnosticQuiz > 0) {
      items.push({
        label: "Baseline Diagnostic Assessment",
        value: "Unattempted",
        detail: "Complete the quiz to establish your verified level",
        highlight: true,
      });
    }

    if (items.length === 0) {
      items.push({
        label: "All Actions Cleared",
        value: "0 Pending",
        detail: "You have no outstanding tasks or pending requests. Great work!",
      });
    }

    return items;
  }, [pendingStudyTasks, pendingSentMentorship, pendingReceivedMentorship, pendingDiagnosticQuiz]);

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      {/* SECTION 1: WELCOME HEADER */}
      <div className="bg-white rounded-2xl border border-gray-200/90 p-5 sm:p-6 shadow-2xs flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200">
              {careerGoal} Track
            </span>
            {userLevel && (
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-gray-100 text-gray-700 border border-gray-200">
                {userLevel} Tier
              </span>
            )}
            {user?.role === "ADMIN" && (
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-purple-50 text-purple-700 border border-purple-200">
                Admin
              </span>
            )}
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse motion-reduce:animate-none" />
              Live Telemetry
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-gray-900">
            Welcome back, {displayName} 👋
          </h1>
          <p className="text-gray-600 text-sm max-w-xl">
            Here is your live career trajectory overview. Review your key statistics and complete your prioritized next step.
          </p>
        </div>

        {/* Quick Header Actions */}
        <div className="flex items-center gap-2.5 self-start md:self-center">
          <Link
            href="/careers"
            className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-gray-200 bg-gray-50/80 hover:bg-gray-100 text-xs font-semibold text-gray-700 transition focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-indigo-500"
            aria-label="Change target career goal"
          >
            <span>🎯 Target Track</span>
            <span className="text-gray-400 font-normal">Edit</span>
          </Link>
          <Link
            href="/profile"
            className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-gray-200 bg-gray-50/80 hover:bg-gray-100 text-xs font-semibold text-gray-700 transition focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-indigo-500"
            aria-label="View user profile"
          >
            <span>👤 Profile</span>
          </Link>
        </div>
      </div>

      {/* SECTION 2: FOUR ESSENTIAL STATISTICS */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Career Readiness */}
        <button
          type="button"
          onClick={() => setActiveModal("readiness")}
          className="bg-white p-5 rounded-2xl border border-gray-200/90 shadow-2xs hover:shadow-xs hover:border-indigo-300 transition-all text-left group cursor-pointer focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 flex flex-col justify-between h-full min-h-[160px]"
          aria-label="View Career Readiness breakdown"
        >
          <div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider flex items-center gap-1.5">
                <span>🎯</span>
                <span>Career Readiness</span>
              </span>
              <span className="text-xs text-gray-400 group-hover:text-indigo-600 transition flex items-center gap-0.5">
                <span>Details</span>
                <span className="transition-transform group-hover:translate-x-0.5">→</span>
              </span>
            </div>
            <div className="flex items-baseline gap-2 mt-2.5">
              {loading ? (
                <div className="h-9 w-24 bg-gray-200/80 animate-pulse rounded-lg" />
              ) : (
                <span className="text-3xl font-extrabold text-gray-900 tracking-tight">
                  {readinessScore !== null ? `${readinessScore}%` : "Unrated"}
                </span>
              )}
            </div>
            <p className="text-xs text-gray-500 mt-1 truncate">
              {loading ? "Calibrating score..." : readinessTier}
            </p>
          </div>
          <div className="w-full bg-gray-100 h-1.5 rounded-full overflow-hidden mt-3">
            <div
              className="bg-indigo-600 h-1.5 rounded-full transition-all duration-500 motion-reduce:transition-none"
              style={{ width: `${readinessScore !== null ? Math.min(100, readinessScore) : 0}%` }}
            />
          </div>
        </button>

        {/* Card 2: Skills Acquired */}
        <button
          type="button"
          onClick={() => setActiveModal("skills")}
          className="bg-white p-5 rounded-2xl border border-gray-200/90 shadow-2xs hover:shadow-xs hover:border-indigo-300 transition-all text-left group cursor-pointer focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 flex flex-col justify-between h-full min-h-[160px]"
          aria-label="View Skills Acquired breakdown"
        >
          <div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider flex items-center gap-1.5">
                <span>🛠️</span>
                <span>Skills Acquired</span>
              </span>
              <span className="text-xs text-gray-400 group-hover:text-indigo-600 transition flex items-center gap-0.5">
                <span>Details</span>
                <span className="transition-transform group-hover:translate-x-0.5">→</span>
              </span>
            </div>
            <div className="flex items-baseline gap-2 mt-2.5">
              {loading ? (
                <div className="h-9 w-24 bg-gray-200/80 animate-pulse rounded-lg" />
              ) : (
                <>
                  <span className="text-3xl font-extrabold text-emerald-600 tracking-tight">
                    {skills.length}
                  </span>
                  <span className="text-xs text-gray-500 font-medium">skills logged</span>
                </>
              )}
            </div>
            <p className="text-xs text-gray-500 mt-1 truncate">
              {loading ? "Matching target..." : `${skillMatch.percentage}% match for ${careerGoal}`}
            </p>
          </div>
          <div className="w-full bg-gray-100 h-1.5 rounded-full overflow-hidden mt-3">
            <div
              className="bg-emerald-500 h-1.5 rounded-full transition-all duration-500 motion-reduce:transition-none"
              style={{ width: `${Math.min(100, skillMatch.percentage)}%` }}
            />
          </div>
        </button>

        {/* Card 3: Roadmap Progress */}
        <button
          type="button"
          onClick={() => setActiveModal("roadmap")}
          className="bg-white p-5 rounded-2xl border border-gray-200/90 shadow-2xs hover:shadow-xs hover:border-indigo-300 transition-all text-left group cursor-pointer focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 flex flex-col justify-between h-full min-h-[160px]"
          aria-label="View Roadmap Progress breakdown"
        >
          <div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider flex items-center gap-1.5">
                <span>🗺️</span>
                <span>Roadmap Progress</span>
              </span>
              <span className="text-xs text-gray-400 group-hover:text-indigo-600 transition flex items-center gap-0.5">
                <span>Details</span>
                <span className="transition-transform group-hover:translate-x-0.5">→</span>
              </span>
            </div>
            <div className="flex items-baseline gap-2 mt-2.5">
              {loading ? (
                <div className="h-9 w-24 bg-gray-200/80 animate-pulse rounded-lg" />
              ) : (
                <>
                  <span className="text-3xl font-extrabold text-blue-600 tracking-tight">
                    {roadmapPercent}%
                  </span>
                  <span className="text-xs text-gray-500">
                    ({completedInGoal}/{totalRoadmapSteps})
                  </span>
                </>
              )}
            </div>
            <p className="text-xs text-gray-500 mt-1 truncate">
              {loading ? "Loading curriculum..." : (completedInGoal === totalRoadmapSteps && totalRoadmapSteps > 0
                ? "All milestones mastered"
                : `Next: ${summary?.nextTopicToLearn || "Getting Started"}`)}
            </p>
          </div>
          <div className="w-full bg-gray-100 h-1.5 rounded-full overflow-hidden mt-3">
            <div
              className="bg-blue-600 h-1.5 rounded-full transition-all duration-500 motion-reduce:transition-none"
              style={{ width: `${Math.min(100, roadmapPercent)}%` }}
            />
          </div>
        </button>

        {/* Card 4: Pending Actions */}
        <button
          type="button"
          onClick={() => setActiveModal("pending")}
          className="bg-white p-5 rounded-2xl border border-gray-200/90 shadow-2xs hover:shadow-xs hover:border-indigo-300 transition-all text-left group cursor-pointer focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 flex flex-col justify-between h-full min-h-[160px]"
          aria-label="View Pending Actions breakdown"
        >
          <div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider flex items-center gap-1.5">
                <span>⚡</span>
                <span>Pending Actions</span>
              </span>
              <span className="text-xs text-gray-400 group-hover:text-indigo-600 transition flex items-center gap-0.5">
                <span>Details</span>
                <span className="transition-transform group-hover:translate-x-0.5">→</span>
              </span>
            </div>
            <div className="flex items-baseline gap-2 mt-2.5">
              {loading ? (
                <div className="h-9 w-24 bg-gray-200/80 animate-pulse rounded-lg" />
              ) : (
                <>
                  <span
                    className={`text-3xl font-extrabold tracking-tight ${
                      totalPendingActions > 0 ? "text-amber-600" : "text-emerald-600"
                    }`}
                  >
                    {totalPendingActions}
                  </span>
                  <span className="text-xs text-gray-500 font-medium">active items</span>
                </>
              )}
            </div>
            <p className="text-xs text-gray-500 mt-1 truncate">
              {loading ? "Checking pending queue..." : (totalPendingActions === 0
                ? "All action items cleared"
                : `${pendingStudyTasks} tasks • ${pendingSentMentorship + pendingReceivedMentorship} mentor reqs`)}
            </p>
          </div>
          <div className="w-full bg-gray-100 h-1.5 rounded-full overflow-hidden mt-3">
            <div
              className={`h-1.5 rounded-full transition-all duration-500 motion-reduce:transition-none ${
                totalPendingActions > 0 ? "bg-amber-500" : "bg-emerald-500"
              }`}
              style={{ width: `${totalPendingActions > 0 ? Math.min(100, totalPendingActions * 25) : 100}%` }}
            />
          </div>
        </button>
      </div>

      {/* SECTION 3: YOUR NEXT BEST STEP (Primary Focal Point) */}
      <div className="bg-gradient-to-br from-gray-950 via-gray-900 to-indigo-950 text-white rounded-2xl border border-indigo-950/60 p-6 sm:p-7 shadow-sm relative overflow-hidden">
        {loading ? (
          <div className="space-y-4 animate-pulse">
            <div className="h-5 w-40 bg-gray-800 rounded" />
            <div className="h-8 w-2/3 bg-gray-800 rounded" />
            <div className="h-4 w-5/6 bg-gray-800/60 rounded" />
            <div className="h-10 w-36 bg-gray-800 rounded mt-2" />
          </div>
        ) : (
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6 relative z-10">
            <div className="space-y-3 max-w-2xl">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-[11px] font-bold tracking-wider uppercase text-indigo-400 flex items-center gap-1.5 font-mono">
                  <span>🎯</span> Your Next Best Step
                </span>
                <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                  {nextBestStep.badge}
                </span>
              </div>

              <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-white leading-snug break-words">
                {nextBestStep.title}
              </h2>

              <p className="text-gray-300 text-sm leading-relaxed">
                {nextBestStep.explanation}
              </p>

              {nextBestStep.reasoning && (
                <div className="bg-white/5 border border-white/10 rounded-xl p-3 flex items-start gap-2.5 text-xs text-gray-300 mt-1">
                  <span className="text-sm shrink-0">💡</span>
                  <div>
                    <span className="font-semibold text-gray-200">Why this matters: </span>
                    <span className="text-gray-300 leading-normal">{nextBestStep.reasoning}</span>
                  </div>
                </div>
              )}
            </div>

            <div className="w-full md:w-auto shrink-0">
              <Link
                href={nextBestStep.actionUrl}
                className="group inline-flex items-center justify-center w-full md:w-auto px-6 py-3.5 bg-white text-gray-950 hover:bg-gray-100 text-sm font-bold rounded-xl transition-all shadow-xs hover:shadow-md focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-white active:scale-[0.99]"
              >
                <span>Take Action Now</span>
                <span className="ml-1.5 transition-transform group-hover:translate-x-0.5">→</span>
              </Link>
            </div>
          </div>
        )}
      </div>

      {/* SECTION 4: MAIN GRID (Career Progress Overview & Recent Activity) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* SECTION 4A: CAREER PROGRESS OVERVIEW (2 cols on lg) */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-gray-200/90 p-6 shadow-2xs space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-gray-100">
            <div>
              <span className="text-xs uppercase tracking-wider text-gray-500 font-semibold block">
                Career Track Overview
              </span>
              <h2 className="text-lg font-bold text-gray-900 mt-0.5">
                {careerGoal}
              </h2>
            </div>
            <div className="flex items-center gap-2">
              <Link
                href={`/roadmap?career=${encodeURIComponent(careerGoal)}`}
                className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-indigo-500 rounded"
              >
                View Full Roadmap →
              </Link>
              <span className="text-gray-300">•</span>
              <Link
                href="/careers"
                className="text-xs font-semibold text-gray-500 hover:text-gray-700 transition focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-indigo-500 rounded"
              >
                Change Goal
              </Link>
            </div>
          </div>

          {/* Visual Progress Breakdown */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Roadmap Milestones Progress */}
            <div className="bg-gray-50/70 border border-gray-100 rounded-xl p-4 space-y-2">
              <div className="flex justify-between text-xs font-medium">
                <span className="text-gray-600">Curriculum Milestones</span>
                <span className="font-bold text-gray-900">{roadmapPercent}% Complete</span>
              </div>
              <div className="w-full bg-gray-200 h-2 rounded-full overflow-hidden">
                <div
                  className="bg-blue-600 h-2 rounded-full transition-all duration-500 motion-reduce:transition-none"
                  style={{ width: `${Math.min(100, roadmapPercent)}%` }}
                />
              </div>
              <p className="text-[11px] text-gray-500">
                {completedInGoal} of {totalRoadmapSteps} topics mastered on this track
              </p>
            </div>

            {/* Target Skill Match */}
            <div className="bg-gray-50/70 border border-gray-100 rounded-xl p-4 space-y-2">
              <div className="flex justify-between text-xs font-medium">
                <span className="text-gray-600">Core Skill Alignment</span>
                <span className="font-bold text-emerald-600">{skillMatch.percentage}% Matched</span>
              </div>
              <div className="w-full bg-gray-200 h-2 rounded-full overflow-hidden">
                <div
                  className="bg-emerald-500 h-2 rounded-full transition-all duration-500 motion-reduce:transition-none"
                  style={{ width: `${Math.min(100, skillMatch.percentage)}%` }}
                />
              </div>
              <p className="text-[11px] text-gray-500">
                {skillMatch.matchedSkills.length} of {targetCareer?.requiredSkills.length || 0} core requirements in portfolio
              </p>
            </div>
          </div>

          {/* Next Immediate Learning Topic Banner */}
          <div className="bg-indigo-50/50 border border-indigo-100 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-700 block">
                Next Immediate Topic
              </span>
              <p className="text-sm font-bold text-gray-900 mt-0.5">
                {summary?.nextTopicToLearn || "All active curriculum milestones mastered!"}
              </p>
            </div>
            <Link
              href={`/roadmap?career=${encodeURIComponent(careerGoal)}`}
              className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-lg transition whitespace-nowrap shadow-xs text-center focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-indigo-500"
            >
              Continue Topic →
            </Link>
          </div>
        </div>

        {/* SECTION 4B: RECENT VERIFIED ACTIVITY (1 col on lg) */}
        <div className="bg-white rounded-2xl border border-gray-200/90 p-6 shadow-2xs space-y-4 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <h2 className="text-base font-bold text-gray-900">
                Recent Activity
              </h2>
              <span className="text-[11px] font-semibold text-gray-400">
                Live Telemetry
              </span>
            </div>

            {loading ? (
              <div className="space-y-3 mt-3">
                <div className="p-3 rounded-xl border border-gray-100 bg-gray-50/50 animate-pulse space-y-2">
                  <div className="h-4 w-3/4 bg-gray-200 rounded" />
                  <div className="h-3 w-1/2 bg-gray-200 rounded" />
                </div>
                <div className="p-3 rounded-xl border border-gray-100 bg-gray-50/50 animate-pulse space-y-2">
                  <div className="h-4 w-2/3 bg-gray-200 rounded" />
                  <div className="h-3 w-1/3 bg-gray-200 rounded" />
                </div>
              </div>
            ) : recentActivities.length === 0 ? (
              <div className="py-8 text-center space-y-2">
                <div className="w-10 h-10 rounded-full bg-gray-100 text-gray-400 flex items-center justify-center mx-auto text-lg">
                  📝
                </div>
                <p className="text-xs text-gray-600 font-medium">
                  No verified activities logged yet.
                </p>
                <p className="text-[11px] text-gray-400 max-w-xs mx-auto">
                  Take a diagnostic assessment, solve a practice problem, or complete a roadmap milestone to see your verified history.
                </p>
              </div>
            ) : (
              <div className="space-y-3 mt-3">
                {recentActivities.slice(0, 3).map((act) => {
                  const typeIcons: Record<string, string> = {
                    ASSESSMENT: "📝",
                    ROADMAP: "🗺️",
                    INTERVIEW: "🎯",
                    RESUME: "📄",
                    MENTORSHIP: "👥",
                    PLANNER: "🗓️",
                  };
                  const icon = typeIcons[act.activityType] || "⚡";
                  const dateStr = act.timestamp ? new Date(act.timestamp).toLocaleDateString(undefined, { month: "short", day: "numeric" }) : "";

                  return (
                    <Link
                      key={act.id}
                      href={act.actionUrl || "/dashboard"}
                      className="p-3 rounded-xl border border-gray-100 bg-gray-50/50 hover:bg-gray-50 transition flex items-start gap-3 group focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-indigo-500"
                    >
                      <div className="w-7 h-7 rounded-lg bg-white border border-gray-200 flex items-center justify-center text-xs shrink-0 mt-0.5 shadow-2xs">
                        {icon}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center justify-between gap-2">
                          <h4 className="text-xs font-bold text-gray-900 truncate group-hover:text-indigo-600 transition">
                            {act.title}
                          </h4>
                          <span className="text-[10px] text-gray-400 shrink-0">{dateStr}</span>
                        </div>
                        <p className="text-[11px] text-gray-500 truncate mt-0.5">
                          {act.subtitle}
                        </p>
                      </div>
                    </Link>
                  );
                })}
              </div>
            )}
          </div>

          <div className="pt-3 border-t border-gray-100 text-right">
            <Link
              href="/profile"
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-indigo-500 rounded"
            >
              View Full Profile & History →
            </Link>
          </div>
        </div>
      </div>

      {/* STATISTIC BREAKDOWN MODALS */}
      {/* 1. Career Readiness Modal */}
      <StatisticModal
        isOpen={activeModal === "readiness"}
        onClose={() => setActiveModal(null)}
        title="Career Readiness Score"
        value={readinessScore !== null ? `${readinessScore}%` : "Unrated"}
        badge={{
          text: readinessTier,
          colorClass: readinessScore !== null && readinessScore >= 70 ? "bg-emerald-50 text-emerald-700 border-emerald-200" : "bg-indigo-50 text-indigo-700 border-indigo-200",
        }}
        explanation="Your career readiness score is a multi-dimensional weighted index calculated across verified portfolio skills, curriculum milestones, diagnostic assessments, algorithm coding challenges, technical mock interviews, and resume keyword alignment."
        breakdownTitle="6-Dimensional Readiness Breakdown"
        breakdownItems={readinessBreakdown}
        primaryAction={{
          label: "Explore Career Tracks →",
          href: "/careers",
        }}
      />

      {/* 2. Skills Acquired Modal */}
      <StatisticModal
        isOpen={activeModal === "skills"}
        onClose={() => setActiveModal(null)}
        title="Skills Acquired Portfolio"
        value={skills.length}
        badge={{
          text: `${skillMatch.percentage}% Match for ${careerGoal}`,
          colorClass: "bg-emerald-50 text-emerald-700 border-emerald-200",
        }}
        explanation="This metric represents the count of distinct, verified technical competencies logged in your portfolio. These competencies are evaluated against the prerequisite standards required for your chosen career track."
        breakdownTitle="Verified Competencies & Alignment"
        breakdownItems={skillsBreakdown}
        emptyBreakdownMessage="No skills logged yet. Add your technical competencies to unlock match ratings."
        primaryAction={{
          label: "Manage Skills Portfolio →",
          href: "/skills",
        }}
      />

      {/* 3. Roadmap Progress Modal */}
      <StatisticModal
        isOpen={activeModal === "roadmap"}
        onClose={() => setActiveModal(null)}
        title="Curriculum Roadmap Progress"
        value={`${roadmapPercent}%`}
        badge={{
          text: `${completedInGoal} of ${totalRoadmapSteps} Milestones`,
          colorClass: "bg-blue-50 text-blue-700 border-blue-200",
        }}
        explanation="Tracks your progress across structured learning milestones in the active curriculum for your selected career track. Each milestone completed advances your technical foundation toward job readiness."
        breakdownTitle="Milestone Completion Details"
        breakdownItems={roadmapBreakdown}
        primaryAction={{
          label: "Open Full Roadmap →",
          href: `/roadmap?career=${encodeURIComponent(careerGoal)}`,
        }}
      />

      {/* 4. Pending Actions Modal */}
      <StatisticModal
        isOpen={activeModal === "pending"}
        onClose={() => setActiveModal(null)}
        title="Active Pending Actions"
        value={totalPendingActions}
        badge={{
          text: totalPendingActions === 0 ? "All Clear" : "Attention Needed",
          colorClass: totalPendingActions === 0 ? "bg-emerald-50 text-emerald-700 border-emerald-200" : "bg-amber-50 text-amber-700 border-amber-200",
        }}
        explanation="Consolidates actionable items awaiting your focus, including daily study milestones scheduled for today, mentorship requests awaiting mentor response, and unattempted baseline diagnostic assessments."
        breakdownTitle="Itemized Action Queue"
        breakdownItems={pendingBreakdown}
        primaryAction={{
          label: pendingStudyTasks > 0 ? "Open Daily Planner →" : (pendingSentMentorship + pendingReceivedMentorship > 0 ? "Open Mentorship Hub →" : "Take Diagnostic Quiz →"),
          href: pendingStudyTasks > 0 ? "/planner" : (pendingSentMentorship + pendingReceivedMentorship > 0 ? "/mentorship" : "/quiz"),
        }}
      />
    </div>
  );
}