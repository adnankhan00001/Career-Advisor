"use client";

import { useEffect, useState, useMemo, useCallback } from "react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import {
  mentorService,
  MentorProfileDto,
  MentorshipRequestDto,
} from "@/lib/mentorService";
import {
  rankMentors,
  parseExpertiseAreas,
  RecommendedMentor,
} from "@/lib/mentorRecommendation";
import { progressService } from "@/lib/progressService";
import { skillService } from "@/lib/skillService";
import { ApiError } from "@/lib/apiClient";

export default function MentorshipHubPage() {
  const { user } = useAuth();

  // Page data states
  const [approvedMentors, setApprovedMentors] = useState<MentorProfileDto[]>([]);
  const [sentRequests, setSentRequests] = useState<MentorshipRequestDto[]>([]);
  const [myMentorProfile, setMyMentorProfile] = useState<MentorProfileDto | null>(null);
  const [careerGoal, setCareerGoal] = useState<string | null>(null);
  const [skills, setSkills] = useState<string[]>([]);

  // UI state
  const [activeTab, setActiveTab] = useState<"explore" | "requests">("explore");
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [successBanner, setSuccessBanner] = useState<string | null>(null);

  // Search & filter state
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [selectedExpertise, setSelectedExpertise] = useState<string>("ALL");

  // Mentorship request modal state
  const [requestModalMentor, setRequestModalMentor] = useState<MentorProfileDto | null>(null);
  const [goalDescription, setGoalDescription] = useState<string>("");
  const [isSubmittingRequest, setIsSubmittingRequest] = useState<boolean>(false);
  const [requestError, setRequestError] = useState<string | null>(null);

  // Action feedback states
  const [cancellingRequestId, setCancellingRequestId] = useState<number | null>(null);
  const [cancelError, setCancelError] = useState<string | null>(null);

  // Info modal state (for Apply / Dashboard navigation notices)
  const [infoModal, setInfoModal] = useState<{
    title: string;
    description: string;
    badge?: string;
  } | null>(null);

  // Initial data load
  const loadInitialData = useCallback(async () => {
    setIsLoading(true);
    setPageError(null);

    try {
      // 1. Fetch approved mentors (core requirement)
      const mentors = await mentorService.getApprovedMentors();
      setApprovedMentors(mentors);

      // 2. Fetch student's sent mentorship requests
      try {
        const requests = await mentorService.getSentRequests();
        setSentRequests(requests);
      } catch {
        setSentRequests([]);
      }

      // 3. Check if current user has a mentor profile
      try {
        const myProfile = await mentorService.getMyProfile();
        setMyMentorProfile(myProfile);
      } catch {
        setMyMentorProfile(null);
      }

      // 4. Load student's career goal and skills for recommendations
      try {
        const progress = await progressService.getProgressSummary();
        if (progress) {
          setCareerGoal(progress.careerGoal || null);
          setSkills(progress.skills || []);
        } else {
          const userSkills = await skillService.getUserSkills();
          setSkills(userSkills);
          const savedGoal =
            typeof window !== "undefined"
              ? localStorage.getItem("careerGoal") || localStorage.getItem("selectedCareer")
              : null;
          setCareerGoal(savedGoal);
        }
      } catch {
        // Fallback to local storage if API is unreachable
        if (typeof window !== "undefined") {
          try {
            const savedSkills = JSON.parse(localStorage.getItem("skills") || "[]");
            setSkills(savedSkills);
            const savedGoal =
              localStorage.getItem("careerGoal") || localStorage.getItem("selectedCareer");
            setCareerGoal(savedGoal);
          } catch {
            setSkills([]);
          }
        }
      }
    } catch (err: any) {
      if (err instanceof ApiError) {
        setPageError(`Failed to load mentorship directory: ${err.message} (HTTP ${err.status})`);
      } else {
        setPageError("Failed to connect to the mentorship service. Please verify your connection.");
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadInitialData();
  }, [loadInitialData]);

  // Compute deterministic recommendations
  const recommendedMentors = useMemo(() => {
    return rankMentors(approvedMentors, { careerGoal, skills });
  }, [approvedMentors, careerGoal, skills]);

  // Extract unique expertise tags across all approved mentors
  const allExpertiseTags = useMemo(() => {
    const tagSet = new Set<string>();
    approvedMentors.forEach((m) => {
      parseExpertiseAreas(m.expertiseAreas).forEach((tag) => tagSet.add(tag));
    });
    return Array.from(tagSet).sort((a, b) => a.localeCompare(b));
  }, [approvedMentors]);

  // Filter mentors in general directory
  const filteredMentors = useMemo(() => {
    return approvedMentors.filter((mentor) => {
      // 1. Search query (matches name, company, or jobTitle)
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const matchesName = (mentor.name || "").toLowerCase().includes(q);
        const matchesTitle = (mentor.jobTitle || "").toLowerCase().includes(q);
        const matchesCompany = (mentor.company || "").toLowerCase().includes(q);
        const matchesExpertise = (mentor.expertiseAreas || "").toLowerCase().includes(q);

        if (!matchesName && !matchesTitle && !matchesCompany && !matchesExpertise) {
          return false;
        }
      }

      // 2. Selected expertise tag
      if (selectedExpertise !== "ALL") {
        const mentorTags = parseExpertiseAreas(mentor.expertiseAreas).map((t) =>
          t.toLowerCase()
        );
        if (!mentorTags.includes(selectedExpertise.toLowerCase())) {
          return false;
        }
      }

      return true;
    });
  }, [approvedMentors, searchQuery, selectedExpertise]);

  // Check request status for a mentor
  const getRequestForMentor = useCallback(
    (mentorProfileId: number) => {
      return sentRequests.find((r) => r.mentorProfileId === mentorProfileId);
    },
    [sentRequests]
  );

  // Handle open request modal
  const handleOpenRequestModal = (mentor: MentorProfileDto) => {
    setRequestModalMentor(mentor);
    setGoalDescription("");
    setRequestError(null);
  };

  // Handle close request modal
  const handleCloseRequestModal = () => {
    setRequestModalMentor(null);
    setGoalDescription("");
    setRequestError(null);
  };

  // Handle mentorship request submission
  const handleSubmitRequest = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!requestModalMentor) return;

    const trimmedGoal = goalDescription.trim();
    if (!trimmedGoal) {
      setRequestError("Please describe your mentorship goal before submitting.");
      return;
    }

    if (trimmedGoal.length > 2000) {
      setRequestError("Mentorship goal must not exceed 2000 characters.");
      return;
    }

    setIsSubmittingRequest(true);
    setRequestError(null);

    try {
      const newRequest = await mentorService.createRequest(requestModalMentor.id, {
        goalDescription: trimmedGoal,
      });

      // Update local state without full reload
      setSentRequests((prev) => [newRequest, ...prev]);
      setSuccessBanner(
        `Your mentorship request to ${requestModalMentor.name} has been sent successfully!`
      );
      handleCloseRequestModal();

      // Refresh data in background to synchronize slot counts
      const updatedMentors = await mentorService.getApprovedMentors();
      setApprovedMentors(updatedMentors);
    } catch (err: any) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setRequestError(
            err.message || "You already have a pending or active request with this mentor."
          );
        } else if (err.status === 400) {
          setRequestError(err.message || "Invalid request. Please check your goal description.");
        } else {
          setRequestError(err.message || "Failed to submit request.");
        }
      } else {
        setRequestError("An unexpected error occurred. Please try again.");
      }
    } finally {
      setIsSubmittingRequest(false);
    }
  };

  // Handle cancelling a pending request
  const handleCancelRequest = async (requestId: number) => {
    if (!confirm("Are you sure you want to cancel this mentorship request?")) {
      return;
    }

    setCancellingRequestId(requestId);
    setCancelError(null);

    try {
      const updated = await mentorService.cancelRequest(requestId);
      setSentRequests((prev) =>
        prev.map((r) => (r.id === requestId ? updated : r))
      );
      setSuccessBanner("Mentorship request was successfully cancelled.");

      // Refresh mentors to update available slots if needed
      const refreshedMentors = await mentorService.getApprovedMentors();
      setApprovedMentors(refreshedMentors);
    } catch (err: any) {
      if (err instanceof ApiError) {
        setCancelError(err.message || "Failed to cancel mentorship request.");
      } else {
        setCancelError("An unexpected network error occurred while cancelling.");
      }
    } finally {
      setCancellingRequestId(null);
    }
  };

  // Helper for status badge colors
  const getStatusBadge = (status: MentorshipRequestDto["status"]) => {
    switch (status) {
      case "PENDING":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-200">
            ⏳ Pending Mentor Review
          </span>
        );
      case "ACCEPTED":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
            ✅ Accepted & Active
          </span>
        );
      case "DECLINED":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-50 text-rose-800 border border-rose-200">
            ❌ Declined
          </span>
        );
      case "CANCELLED":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-gray-100 text-gray-700 border border-gray-200">
            🚫 Cancelled
          </span>
        );
      case "COMPLETED":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-800 border border-blue-200">
            🎓 Completed
          </span>
        );
      default:
        return null;
    }
  };

  return (
    <div className="space-y-8 pb-12">
      {/* ---------------------------------------------------- */}
      {/* A. PAGE HEADER & ACTIONS                             */}
      {/* ---------------------------------------------------- */}
      <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <span className="text-2xl">👥</span>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
              Mentorship Hub
            </h1>
          </div>
          <p className="text-sm sm:text-base text-gray-600 max-w-2xl">
            Connect directly with verified tech professionals, gain personalized career guidance,
            sharpen in-demand skills, and accelerate your engineering journey.
          </p>
        </div>

        {/* Dynamic Entry Points based on User Verification Status */}
        <div className="flex items-center gap-3 shrink-0 flex-wrap">
          {myMentorProfile?.verificationStatus === "APPROVED" && (
            <Link
              href="/mentorship/manage"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 shadow-sm transition"
            >
              <span>🛡️</span>
              <span>Mentor Dashboard</span>
            </Link>
          )}

          {myMentorProfile?.verificationStatus === "APPLIED" && (
            <Link
              href="/mentorship/apply"
              className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold text-amber-800 bg-amber-50 border border-amber-200 hover:bg-amber-100 transition"
            >
              <span>⏳</span>
              <span>Mentor Application Under Review</span>
            </Link>
          )}

          {myMentorProfile?.verificationStatus === "REJECTED" && (
            <Link
              href="/mentorship/apply"
              className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold text-rose-800 bg-rose-50 border border-rose-200 hover:bg-rose-100 transition"
            >
              <span>⚠️</span>
              <span>Application Reviewed</span>
            </Link>
          )}

          {!myMentorProfile && (
            <Link
              href="/mentorship/apply"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-semibold text-gray-800 bg-gray-100 hover:bg-gray-200 border border-gray-200 transition"
            >
              <span>✨</span>
              <span>Apply to Become a Mentor</span>
            </Link>
          )}
        </div>
      </div>

      {/* Success Notification Banner */}
      {successBanner && (
        <div className="flex items-center justify-between p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 text-sm animate-fade-in">
          <div className="flex items-center gap-2 font-medium">
            <span>✓</span>
            <span>{successBanner}</span>
          </div>
          <button
            onClick={() => setSuccessBanner(null)}
            className="text-emerald-700 hover:text-emerald-900 font-bold px-2 py-0.5 cursor-pointer"
          >
            ✕
          </button>
        </div>
      )}

      {/* Global Page Error */}
      {pageError && (
        <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-900 text-sm flex items-start gap-3">
          <span className="text-base">⚠️</span>
          <div className="space-y-1">
            <p className="font-bold">Error loading Mentorship Hub</p>
            <p className="text-xs text-red-800">{pageError}</p>
            <button
              onClick={loadInitialData}
              className="mt-2 text-xs font-semibold text-red-700 underline hover:text-red-900 cursor-pointer"
            >
              Retry
            </button>
          </div>
        </div>
      )}

      {/* Cancel Error Banner */}
      {cancelError && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-900 text-sm flex items-center justify-between">
          <p>{cancelError}</p>
          <button
            onClick={() => setCancelError(null)}
            className="text-rose-700 hover:text-rose-900 font-bold px-2"
          >
            ✕
          </button>
        </div>
      )}

      {/* Main Tabs */}
      <div className="flex border-b border-gray-200 gap-8">
        <button
          onClick={() => setActiveTab("explore")}
          className={`pb-3 text-sm font-bold flex items-center gap-2 border-b-2 transition cursor-pointer ${
            activeTab === "explore"
              ? "border-indigo-600 text-indigo-600"
              : "border-transparent text-gray-500 hover:text-gray-800"
          }`}
        >
          <span>🔍</span>
          <span>Explore Mentors</span>
          <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-700">
            {approvedMentors.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab("requests")}
          className={`pb-3 text-sm font-bold flex items-center gap-2 border-b-2 transition cursor-pointer ${
            activeTab === "requests"
              ? "border-indigo-600 text-indigo-600"
              : "border-transparent text-gray-500 hover:text-gray-800"
          }`}
        >
          <span>📬</span>
          <span>My Mentorships</span>
          <span
            className={`text-xs px-2 py-0.5 rounded-full ${
              sentRequests.length > 0
                ? "bg-indigo-100 text-indigo-800 font-bold"
                : "bg-gray-100 text-gray-700"
            }`}
          >
            {sentRequests.length}
          </span>
        </button>
      </div>

      {/* ---------------------------------------------------- */}
      {/* TAB 1: EXPLORE & RECOMMENDATIONS                     */}
      {/* ---------------------------------------------------- */}
      {activeTab === "explore" && (
        <div className="space-y-10">
          {/* B. PERSONALIZED RECOMMENDATIONS SECTION */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <h2 className="text-lg sm:text-xl font-bold text-gray-900">
                  Recommended for You
                </h2>
                <span className="text-[11px] font-semibold px-2 py-0.5 rounded-md bg-indigo-50 text-indigo-700 border border-indigo-200">
                  Rule-based Match
                </span>
              </div>
              {careerGoal && (
                <span className="hidden sm:inline-block text-xs text-gray-500 font-medium">
                  Goal: <strong className="text-gray-700">{careerGoal}</strong>
                </span>
              )}
            </div>

            {/* Recommendations Content */}
            {isLoading ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {[1, 2, 3].map((i) => (
                  <div
                    key={i}
                    className="h-64 rounded-2xl bg-white border border-gray-200 p-6 animate-pulse space-y-4"
                  >
                    <div className="w-1/2 h-5 bg-gray-200 rounded"></div>
                    <div className="w-3/4 h-4 bg-gray-200 rounded"></div>
                    <div className="w-full h-16 bg-gray-100 rounded"></div>
                  </div>
                ))}
              </div>
            ) : recommendedMentors.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {recommendedMentors.slice(0, 3).map((rec) => (
                  <MentorCard
                    key={rec.mentor.id}
                    mentor={rec.mentor}
                    currentUserId={user?.id}
                    existingRequest={getRequestForMentor(rec.mentor.id)}
                    recommendationReason={rec.recommendationReason}
                    onRequestClick={() => handleOpenRequestModal(rec.mentor)}
                  />
                ))}
              </div>
            ) : (
              <div className="p-6 rounded-2xl bg-indigo-50/60 border border-indigo-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="space-y-1">
                  <p className="text-sm font-bold text-indigo-950 flex items-center gap-1.5">
                    <span>💡</span>
                    <span>Unlock Tailored Mentor Recommendations</span>
                  </p>
                  <p className="text-xs text-indigo-800">
                    {skills.length === 0 && !careerGoal
                      ? "Add your career target and technical skills to your profile to view mentors matching your goals."
                      : "No mentors currently match your specific skills or career target. Browse the general directory below!"}
                  </p>
                </div>
                <Link
                  href="/profile"
                  className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-semibold text-indigo-700 bg-white border border-indigo-200 hover:bg-indigo-50 shrink-0 transition"
                >
                  <span>Update Profile</span>
                  <span>→</span>
                </Link>
              </div>
            )}
          </div>

          {/* C. EXPLORE ALL MENTORS SECTION */}
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-lg sm:text-xl font-bold text-gray-900">
                  All Approved Mentors
                </h2>
                <p className="text-xs text-gray-500">
                  Browse all active mentors across tech domains and request 1-on-1 guidance
                </p>
              </div>

              {/* Search Bar */}
              <div className="relative w-full sm:w-72">
                <input
                  type="text"
                  placeholder="Search name, title, or company..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-9 pr-8 py-2 rounded-xl text-xs sm:text-sm border border-gray-300 bg-white focus:outline-hidden focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition text-gray-900 placeholder:text-gray-400"
                />
                <span className="absolute left-3 top-2.5 text-xs text-gray-400">🔍</span>
                {searchQuery && (
                  <button
                    onClick={() => setSearchQuery("")}
                    className="absolute right-2.5 top-2.5 text-xs text-gray-400 hover:text-gray-700 cursor-pointer"
                  >
                    ✕
                  </button>
                )}
              </div>
            </div>

            {/* Expertise Filters */}
            {allExpertiseTags.length > 0 && (
              <div className="flex items-center gap-1.5 overflow-x-auto pb-2 scrollbar-thin">
                <span className="text-xs font-semibold text-gray-400 shrink-0 mr-1">
                  Filter:
                </span>
                <button
                  onClick={() => setSelectedExpertise("ALL")}
                  className={`px-3 py-1 rounded-lg text-xs font-semibold transition shrink-0 cursor-pointer ${
                    selectedExpertise === "ALL"
                      ? "bg-black text-white"
                      : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                  }`}
                >
                  All ({approvedMentors.length})
                </button>
                {allExpertiseTags.map((tag) => (
                  <button
                    key={tag}
                    onClick={() => setSelectedExpertise(tag)}
                    className={`px-3 py-1 rounded-lg text-xs font-semibold transition shrink-0 cursor-pointer ${
                      selectedExpertise.toLowerCase() === tag.toLowerCase()
                        ? "bg-indigo-600 text-white"
                        : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                    }`}
                  >
                    {tag}
                  </button>
                ))}
              </div>
            )}

            {/* Mentors Grid */}
            {isLoading ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {[1, 2, 3, 4, 5, 6].map((i) => (
                  <div
                    key={i}
                    className="h-72 rounded-2xl bg-white border border-gray-200 p-6 animate-pulse space-y-4"
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 bg-gray-200 rounded-xl"></div>
                      <div className="space-y-2 flex-1">
                        <div className="h-4 bg-gray-200 rounded w-1/2"></div>
                        <div className="h-3 bg-gray-200 rounded w-3/4"></div>
                      </div>
                    </div>
                    <div className="h-16 bg-gray-100 rounded"></div>
                    <div className="h-8 bg-gray-200 rounded w-full"></div>
                  </div>
                ))}
              </div>
            ) : filteredMentors.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {filteredMentors.map((mentor) => (
                  <MentorCard
                    key={mentor.id}
                    mentor={mentor}
                    currentUserId={user?.id}
                    existingRequest={getRequestForMentor(mentor.id)}
                    onRequestClick={() => handleOpenRequestModal(mentor)}
                  />
                ))}
              </div>
            ) : (
              <div className="p-12 rounded-2xl bg-white border border-gray-200 text-center space-y-3">
                <span className="text-3xl">🔍</span>
                <h3 className="text-base font-bold text-gray-900">No Mentors Found</h3>
                <p className="text-xs text-gray-500 max-w-md mx-auto">
                  No mentors match your search query or filter. Try clearing the filter to view
                  all available mentors.
                </p>
                {(searchQuery || selectedExpertise !== "ALL") && (
                  <button
                    onClick={() => {
                      setSearchQuery("");
                      setSelectedExpertise("ALL");
                    }}
                    className="mt-2 px-4 py-2 text-xs font-semibold rounded-lg bg-gray-100 hover:bg-gray-200 text-gray-800 transition cursor-pointer"
                  >
                    Clear All Filters
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ---------------------------------------------------- */}
      {/* TAB 2: MY MENTORSHIPS                                */}
      {/* ---------------------------------------------------- */}
      {activeTab === "requests" && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg sm:text-xl font-bold text-gray-900">
                My Mentorship Engagements
              </h2>
              <p className="text-xs text-gray-500">
                Track your sent mentorship requests, mentor responses, and active guidance
              </p>
            </div>
            <button
              onClick={loadInitialData}
              className="text-xs text-gray-500 hover:text-gray-900 flex items-center gap-1 cursor-pointer"
            >
              <span>↻</span>
              <span>Refresh</span>
            </button>
          </div>

          {sentRequests.length > 0 ? (
            <div className="space-y-4">
              {sentRequests.map((req) => (
                <div
                  key={req.id}
                  className="bg-white rounded-2xl border border-gray-200 p-6 shadow-xs space-y-4 transition hover:border-gray-300"
                >
                  <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <h3 className="text-base font-bold text-gray-900">
                          {req.mentorName}
                        </h3>
                        {getStatusBadge(req.status)}
                      </div>
                      <p className="text-xs text-gray-600 font-medium">
                        {req.mentorJobTitle} • {req.mentorCompany}
                      </p>
                      <p className="text-[11px] text-gray-400">
                        Submitted: {new Date(req.createdAt).toLocaleDateString(undefined, {
                          year: "numeric",
                          month: "short",
                          day: "numeric",
                        })}
                      </p>
                    </div>

                    {/* Actions based on Request Status */}
                    <div className="flex items-center gap-2 shrink-0">
                      {req.status === "PENDING" && (
                        <button
                          onClick={() => handleCancelRequest(req.id)}
                          disabled={cancellingRequestId === req.id}
                          className="px-3 py-1.5 text-xs font-semibold rounded-lg bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100 transition disabled:opacity-50 cursor-pointer"
                        >
                          {cancellingRequestId === req.id ? "Cancelling..." : "Cancel Request"}
                        </button>
                      )}

                      {req.status === "ACCEPTED" && (
                        <Link
                          href="/messages"
                          className="px-3.5 py-1.5 text-xs font-semibold rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 transition flex items-center gap-1.5 shadow-xs"
                        >
                          <span>💬</span>
                          <span>Open Messages</span>
                        </Link>
                      )}
                    </div>
                  </div>

                  {/* Goal Description Submitted */}
                  <div className="p-3.5 rounded-xl bg-gray-50 border border-gray-100 text-xs text-gray-700 space-y-1">
                    <span className="font-semibold text-gray-900 block">Your Goal:</span>
                    <p className="whitespace-pre-line leading-relaxed">{req.goalDescription}</p>
                  </div>

                  {/* Mentor Response Note (if present) */}
                  {req.responseNote && (
                    <div className="p-3.5 rounded-xl bg-indigo-50/70 border border-indigo-100 text-xs text-indigo-950 space-y-1">
                      <span className="font-bold flex items-center gap-1">
                        <span>📝</span>
                        <span>Note from {req.mentorName}:</span>
                      </span>
                      <p className="whitespace-pre-line leading-relaxed">{req.responseNote}</p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className="p-12 rounded-2xl bg-white border border-gray-200 text-center space-y-3">
              <span className="text-3xl">📬</span>
              <h3 className="text-base font-bold text-gray-900">No Mentorship Requests Yet</h3>
              <p className="text-xs text-gray-500 max-w-md mx-auto">
                You haven&apos;t sent any mentorship requests yet. Browse our directory of approved
                mentors to find an experienced professional aligned with your career goals.
              </p>
              <button
                onClick={() => setActiveTab("explore")}
                className="mt-2 px-4 py-2 text-xs font-semibold rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 transition cursor-pointer"
              >
                Find a Mentor Now
              </button>
            </div>
          )}
        </div>
      )}

      {/* ---------------------------------------------------- */}
      {/* D. MENTORSHIP REQUEST MODAL                          */}
      {/* ---------------------------------------------------- */}
      {requestModalMentor && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in">
          <div
            className="bg-white rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto border border-gray-200 shadow-2xl p-6 sm:p-8 space-y-5"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div className="flex items-start justify-between gap-4 pb-4 border-b border-gray-100">
              <div className="space-y-1">
                <span className="text-xs font-bold text-indigo-600 uppercase tracking-wider">
                  Request Guidance
                </span>
                <h2 className="text-lg sm:text-xl font-extrabold text-gray-900">
                  Request Mentorship with {requestModalMentor.name}
                </h2>
                <p className="text-xs text-gray-500">
                  {requestModalMentor.jobTitle} @ {requestModalMentor.company}
                </p>
              </div>

              <button
                onClick={handleCloseRequestModal}
                disabled={isSubmittingRequest}
                className="p-1.5 rounded-lg text-gray-400 hover:text-gray-900 hover:bg-gray-100 transition cursor-pointer"
                aria-label="Close modal"
              >
                ✕
              </button>
            </div>

            {/* Mentor Availability Overview */}
            <div className="flex items-center justify-between p-3 rounded-xl bg-gray-50 border border-gray-200 text-xs text-gray-700">
              <span>Experience: <strong>{requestModalMentor.yearsOfExperience} years</strong></span>
              <span className="font-semibold text-emerald-700">
                ● {requestModalMentor.availableSlots} available slots
              </span>
            </div>

            {/* Error Banner */}
            {requestError && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-900 text-xs space-y-1">
                <p className="font-bold">Unable to send request</p>
                <p>{requestError}</p>
              </div>
            )}

            {/* Request Form */}
            <form onSubmit={handleSubmitRequest} className="space-y-4">
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label
                    htmlFor="goalDescription"
                    className="text-xs font-bold text-gray-800"
                  >
                    Your Mentorship Goal & Objectives <span className="text-red-500">*</span>
                  </label>
                  <span
                    className={`text-[11px] ${
                      goalDescription.length > 1900 ? "text-amber-600 font-bold" : "text-gray-400"
                    }`}
                  >
                    {goalDescription.length}/2000
                  </span>
                </div>
                <textarea
                  id="goalDescription"
                  rows={4}
                  required
                  maxLength={2000}
                  value={goalDescription}
                  onChange={(e) => setGoalDescription(e.target.value)}
                  placeholder="Explain what specific guidance, architecture reviews, interview preparation, or skill milestones you wish to achieve with this mentor..."
                  className="w-full p-3 rounded-xl text-xs sm:text-sm border border-gray-300 bg-white focus:outline-hidden focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition text-gray-900 placeholder:text-gray-400 resize-y"
                />
                <p className="text-[11px] text-gray-500">
                  Mentors accept requests based on clarity of purpose and realistic goals.
                </p>
              </div>

              {/* Form Buttons */}
              <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={handleCloseRequestModal}
                  disabled={isSubmittingRequest}
                  className="px-4 py-2 text-xs font-semibold rounded-xl text-gray-700 bg-gray-100 hover:bg-gray-200 transition cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingRequest || !goalDescription.trim()}
                  className="px-4 py-2 text-xs font-semibold rounded-xl text-white bg-indigo-600 hover:bg-indigo-700 transition disabled:opacity-50 flex items-center gap-2 cursor-pointer shadow-xs"
                >
                  {isSubmittingRequest ? (
                    <>
                      <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      <span>Submitting...</span>
                    </>
                  ) : (
                    <span>Submit Request</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ---------------------------------------------------- */}
      {/* INFO NOTIFICATION MODAL                              */}
      {/* ---------------------------------------------------- */}
      {infoModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in">
          <div
            className="bg-white rounded-2xl max-w-md w-full border border-gray-200 shadow-2xl p-6 sm:p-7 space-y-4"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-4">
              <div className="space-y-1">
                {infoModal.badge && (
                  <span className="text-[10px] font-extrabold uppercase tracking-wider px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200">
                    {infoModal.badge}
                  </span>
                )}
                <h3 className="text-base font-bold text-gray-900">{infoModal.title}</h3>
              </div>
              <button
                onClick={() => setInfoModal(null)}
                className="p-1 text-gray-400 hover:text-gray-900 rounded-lg cursor-pointer"
              >
                ✕
              </button>
            </div>
            <p className="text-xs text-gray-600 leading-relaxed">{infoModal.description}</p>
            <div className="flex justify-end pt-2">
              <button
                onClick={() => setInfoModal(null)}
                className="px-4 py-2 rounded-xl text-xs font-semibold bg-black text-white hover:bg-gray-800 transition cursor-pointer"
              >
                Got It
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ====================================================================
// REUSABLE MENTOR CARD COMPONENT
// ====================================================================
interface MentorCardProps {
  mentor: MentorProfileDto;
  currentUserId?: number;
  existingRequest?: MentorshipRequestDto;
  recommendationReason?: string;
  onRequestClick: () => void;
}

function MentorCard({
  mentor,
  currentUserId,
  existingRequest,
  recommendationReason,
  onRequestClick,
}: MentorCardProps) {
  const expertiseTags = parseExpertiseAreas(mentor.expertiseAreas);
  const isSelf = Boolean(currentUserId && mentor.userId === currentUserId);
  const isFull = mentor.availableSlots <= 0;
  const isPending = existingRequest?.status === "PENDING";
  const isAccepted = existingRequest?.status === "ACCEPTED";

  const avatarInitial = (mentor.name || "M").charAt(0).toUpperCase();

  return (
    <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-xs flex flex-col justify-between gap-5 transition hover:shadow-md hover:border-gray-300">
      <div className="space-y-4">
        {/* Recommendation Reason Pill (if present) */}
        {recommendationReason && (
          <div className="p-2 rounded-xl bg-indigo-50/80 border border-indigo-100 text-[11px] font-semibold text-indigo-900 flex items-center gap-1.5">
            <span>🎯</span>
            <span className="truncate">{recommendationReason}</span>
          </div>
        )}

        {/* Mentor Identity */}
        <div className="flex items-start gap-3.5">
          <div className="w-12 h-12 rounded-xl bg-indigo-600 text-white flex items-center justify-center text-base font-extrabold shadow-xs shrink-0">
            {avatarInitial}
          </div>
          <div className="min-w-0 flex-1 space-y-0.5">
            <Link
              href={`/mentorship/${mentor.id}`}
              className="text-sm sm:text-base font-bold text-gray-900 truncate hover:text-indigo-600 transition block"
            >
              {mentor.name}
            </Link>
            <p className="text-xs text-gray-600 font-medium truncate">
              {mentor.jobTitle}
            </p>
            <p className="text-[11px] text-gray-500 font-medium truncate">
              {mentor.company}
            </p>
          </div>
        </div>

        {/* Experience & Slots Badges */}
        <div className="flex items-center gap-2 flex-wrap text-xs">
          <span className="px-2.5 py-1 rounded-lg bg-gray-100 text-gray-700 font-medium">
            💼 {mentor.yearsOfExperience} yrs exp
          </span>

          {mentor.availableSlots > 0 ? (
            <span className="px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200 font-semibold">
              ● {mentor.availableSlots} {mentor.availableSlots === 1 ? "slot" : "slots"} left
            </span>
          ) : (
            <span className="px-2.5 py-1 rounded-lg bg-gray-100 text-gray-500 border border-gray-200 font-medium">
              ○ Fully Booked
            </span>
          )}
        </div>

        {/* Bio (if available) */}
        {mentor.bio && (
          <p className="text-xs text-gray-600 line-clamp-3 leading-relaxed">
            {mentor.bio}
          </p>
        )}

        {/* Expertise Tags */}
        {expertiseTags.length > 0 && (
          <div className="flex items-center gap-1.5 flex-wrap">
            {expertiseTags.slice(0, 4).map((tag) => (
              <span
                key={tag}
                className="text-[10px] font-semibold px-2 py-0.5 rounded-md bg-gray-50 text-gray-700 border border-gray-200"
              >
                {tag}
              </span>
            ))}
            {expertiseTags.length > 4 && (
              <span className="text-[10px] text-gray-400 font-medium">
                +{expertiseTags.length - 4} more
              </span>
            )}
          </div>
        )}
      </div>

      {/* Action Buttons */}
      <div className="pt-4 border-t border-gray-100 flex items-center gap-2">
        <Link
          href={`/mentorship/${mentor.id}`}
          className="py-2 px-3 rounded-xl text-xs font-semibold text-gray-700 bg-gray-100 hover:bg-gray-200 transition text-center shrink-0"
        >
          View Profile
        </Link>
        <div className="flex-1">
          {isSelf ? (
            <button
              disabled
              className="w-full py-2 px-3 rounded-xl text-xs font-semibold bg-gray-100 text-gray-400 cursor-not-allowed"
            >
              Your Profile
            </button>
          ) : isPending ? (
            <div className="w-full py-2 px-3 rounded-xl text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-200 text-center truncate">
              ⏳ Pending
            </div>
          ) : isAccepted ? (
            <Link
              href="/messages"
              className="w-full py-2 px-3 rounded-xl text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white flex items-center justify-center gap-1.5 transition shadow-xs truncate"
            >
              <span>💬</span>
              <span>Open Chat</span>
            </Link>
          ) : isFull ? (
            <button
              disabled
              className="w-full py-2 px-3 rounded-xl text-xs font-semibold bg-gray-100 text-gray-400 border border-gray-200 cursor-not-allowed"
            >
              Fully Booked
            </button>
          ) : (
            <button
              onClick={onRequestClick}
              className="w-full py-2 px-3 rounded-xl text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 text-white flex items-center justify-center gap-1.5 transition cursor-pointer shadow-xs truncate"
            >
              <span>✨</span>
              <span>Request</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
