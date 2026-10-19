"use client";

import { useEffect, useState, use, useCallback } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import {
  mentorService,
  MentorProfileDto,
  MentorshipRequestDto,
} from "@/lib/mentorService";
import { parseExpertiseAreas } from "@/lib/mentorRecommendation";
import { ApiError } from "@/lib/apiClient";

interface MentorProfilePageProps {
  params: Promise<{ id: string }>;
}

export default function IndividualMentorProfilePage({ params }: MentorProfilePageProps) {
  const resolvedParams = use(params);
  const mentorId = resolvedParams.id;
  const router = useRouter();
  const { user } = useAuth();

  // Profile data states
  const [mentor, setMentor] = useState<MentorProfileDto | null>(null);
  const [existingRequest, setExistingRequest] = useState<MentorshipRequestDto | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [isNotFound, setIsNotFound] = useState<boolean>(false);

  // Request modal states
  const [isRequestModalOpen, setIsRequestModalOpen] = useState<boolean>(false);
  const [goalDescription, setGoalDescription] = useState<string>("");
  const [isSubmittingRequest, setIsSubmittingRequest] = useState<boolean>(false);
  const [requestError, setRequestError] = useState<string | null>(null);
  const [successBanner, setSuccessBanner] = useState<string | null>(null);

  // Fetch mentor profile and current user's request status
  const loadMentorData = useCallback(async () => {
    setIsLoading(true);
    setPageError(null);
    setIsNotFound(false);

    try {
      // 1. Fetch mentor profile by ID
      const profile = await mentorService.getMentorById(mentorId);
      setMentor(profile);

      // 2. Fetch user's sent requests to determine existing relationship with this mentor
      try {
        const sent = await mentorService.getSentRequests();
        const matchingRequest = sent.find((r) => r.mentorProfileId === profile.id);
        if (matchingRequest) {
          setExistingRequest(matchingRequest);
        }
      } catch {
        // Non-blocking: student may not have sent any requests yet
      }
    } catch (err: any) {
      if (err instanceof ApiError) {
        if (err.status === 404) {
          setIsNotFound(true);
          setPageError("This mentor profile does not exist, has been deactivated, or is no longer available.");
        } else {
          setPageError(err.message || `Failed to load profile (HTTP ${err.status})`);
        }
      } else {
        setPageError("Unable to connect to the mentorship service. Please verify your connection.");
      }
    } finally {
      setIsLoading(false);
    }
  }, [mentorId]);

  useEffect(() => {
    loadMentorData();
  }, [loadMentorData]);

  // Handle request submission
  const handleSubmitRequest = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!mentor) return;

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
      const created = await mentorService.createRequest(mentor.id, {
        goalDescription: trimmedGoal,
      });

      setExistingRequest(created);
      setIsRequestModalOpen(false);
      setGoalDescription("");
      setSuccessBanner(
        `Your mentorship request to ${mentor.name} has been submitted successfully!`
      );

      // Refresh mentor profile to update available slot counts
      const updatedMentor = await mentorService.getMentorById(mentor.id);
      setMentor(updatedMentor);
    } catch (err: any) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setRequestError(
            err.message || "You already have a pending or active request with this mentor."
          );
        } else if (err.status === 400) {
          setRequestError(err.message || "Invalid request. Please verify your goal description.");
        } else {
          setRequestError(err.message || "Failed to submit mentorship request.");
        }
      } else {
        setRequestError("An unexpected network error occurred. Please try again.");
      }
    } finally {
      setIsSubmittingRequest(false);
    }
  };

  // ------------------------------------------------------------------
  // RENDER: LOADING SKELETON
  // ------------------------------------------------------------------
  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto space-y-6 pb-12 animate-pulse">
        <div className="h-4 bg-gray-200 rounded w-1/4"></div>
        <div className="bg-white rounded-2xl border border-gray-200 p-8 space-y-6">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 bg-gray-200 rounded-2xl"></div>
            <div className="space-y-2 flex-1">
              <div className="h-6 bg-gray-200 rounded w-1/3"></div>
              <div className="h-4 bg-gray-100 rounded w-1/2"></div>
            </div>
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div className="h-16 bg-gray-100 rounded-xl"></div>
            <div className="h-16 bg-gray-100 rounded-xl"></div>
            <div className="h-16 bg-gray-100 rounded-xl"></div>
          </div>
          <div className="h-24 bg-gray-100 rounded-xl"></div>
        </div>
      </div>
    );
  }

  // ------------------------------------------------------------------
  // RENDER: NOT FOUND / ERROR STATE
  // ------------------------------------------------------------------
  if (isNotFound || !mentor) {
    return (
      <div className="max-w-2xl mx-auto space-y-6 py-12 text-center">
        <div className="bg-white rounded-2xl border border-gray-200 shadow-xs p-8 sm:p-12 space-y-4">
          <span className="text-4xl">🔍</span>
          <h1 className="text-xl sm:text-2xl font-extrabold text-gray-900">
            {isNotFound ? "Mentor Profile Not Found" : "Error Loading Mentor"}
          </h1>
          <p className="text-xs sm:text-sm text-gray-600 max-w-md mx-auto leading-relaxed">
            {pageError || "The requested mentor profile could not be retrieved."}
          </p>
          <div className="pt-4 flex justify-center gap-3">
            <Link
              href="/mentorship"
              className="px-4 py-2 text-xs font-semibold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 transition shadow-xs"
            >
              ← Back to Mentorship Directory
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // ------------------------------------------------------------------
  // RENDER: PROFILE DATA CALCULATIONS
  // ------------------------------------------------------------------
  const expertiseTags = parseExpertiseAreas(mentor.expertiseAreas);
  const avatarInitial = (mentor.name || "M").charAt(0).toUpperCase();

  const isSelf = Boolean(user?.id && mentor.userId === user.id);
  const isFull = mentor.availableSlots <= 0;
  const isPending = existingRequest?.status === "PENDING";
  const isAccepted = existingRequest?.status === "ACCEPTED";

  const hasExternalLinks = Boolean(
    mentor.linkedinUrl || mentor.githubUrl || mentor.codingProfileUrl || mentor.portfolioUrl
  );

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-12">
      {/* Breadcrumb Navigation */}
      <div className="flex items-center gap-2 text-xs font-semibold text-gray-500">
        <Link href="/mentorship" className="hover:text-indigo-600 transition">
          Mentorship Hub
        </Link>
        <span>/</span>
        <span className="text-gray-900 font-bold truncate">{mentor.name}</span>
      </div>

      {/* Success Notification Banner */}
      {successBanner && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 text-sm flex items-start justify-between gap-3 animate-fade-in">
          <div className="flex items-center gap-2 font-medium">
            <span>✓</span>
            <span>{successBanner}</span>
          </div>
          <button
            onClick={() => setSuccessBanner(null)}
            className="text-emerald-700 hover:text-emerald-900 font-bold px-1.5 cursor-pointer"
          >
            ✕
          </button>
        </div>
      )}

      {/* ---------------------------------------------------- */}
      {/* MAIN PROFILE CARD                                    */}
      {/* ---------------------------------------------------- */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-xs p-6 sm:p-8 space-y-8">
        {/* Mentor Header Overview */}
        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-6 pb-6 border-b border-gray-100">
          <div className="flex items-start gap-4">
            <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-indigo-600 text-white flex items-center justify-center text-2xl sm:text-3xl font-extrabold shadow-sm shrink-0">
              {avatarInitial}
            </div>

            <div className="space-y-1">
              <div className="flex items-center gap-2 flex-wrap">
                <h1 className="text-xl sm:text-2xl font-extrabold text-gray-900">
                  {mentor.name}
                </h1>
                <span className="inline-flex items-center gap-1 text-[11px] font-bold px-2.5 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200">
                  <span>✓</span>
                  <span>Verified Mentor</span>
                </span>
              </div>

              <p className="text-sm font-semibold text-gray-700">
                {mentor.jobTitle}
              </p>
              <p className="text-xs text-gray-500 font-medium">
                {mentor.company}
              </p>
            </div>
          </div>

          {/* Quick Request CTA in Header */}
          <div className="shrink-0">
            {isSelf ? (
              <span className="px-3.5 py-2 rounded-xl text-xs font-semibold bg-gray-100 text-gray-500 border border-gray-200 inline-block">
                This is your mentor profile
              </span>
            ) : isPending ? (
              <span className="px-3.5 py-2 rounded-xl text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-200 inline-block">
                ⏳ Request Pending Review
              </span>
            ) : isAccepted ? (
              <Link
                href={existingRequest?.conversationId ? `/messages?convId=${existingRequest.conversationId}` : "/messages"}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white transition shadow-xs"
              >
                <span>💬</span>
                <span>Connected • Open Chat</span>
              </Link>
            ) : isFull ? (
              <span className="px-3.5 py-2 rounded-xl text-xs font-semibold bg-gray-100 text-gray-500 border border-gray-200 inline-block">
                ○ Fully Booked (0 slots)
              </span>
            ) : (
              <button
                onClick={() => {
                  setIsRequestModalOpen(true);
                  setRequestError(null);
                }}
                className="px-5 py-2.5 rounded-xl text-xs sm:text-sm font-semibold bg-indigo-600 hover:bg-indigo-700 text-white transition shadow-xs flex items-center gap-2 cursor-pointer"
              >
                <span>✨</span>
                <span>Request Mentorship</span>
              </button>
            )}
          </div>
        </div>

        {/* Key Metrics / Availability Stats */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="p-4 rounded-xl bg-gray-50 border border-gray-100 space-y-1">
            <span className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider block">
              Experience
            </span>
            <p className="text-lg font-extrabold text-gray-900">
              {mentor.yearsOfExperience} {mentor.yearsOfExperience === 1 ? "year" : "years"}
            </p>
            <span className="text-[11px] text-gray-500">Industry track record</span>
          </div>

          <div className="p-4 rounded-xl bg-gray-50 border border-gray-100 space-y-1">
            <span className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider block">
              Active Mentees
            </span>
            <p className="text-lg font-extrabold text-gray-900">
              {mentor.activeMenteesCount} / {mentor.maxMentees}
            </p>
            <span className="text-[11px] text-gray-500">Current guidance capacity</span>
          </div>

          <div className="p-4 rounded-xl bg-gray-50 border border-gray-100 space-y-1">
            <span className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider block">
              Slot Availability
            </span>
            {mentor.availableSlots > 0 ? (
              <p className="text-lg font-extrabold text-emerald-700 flex items-center gap-1.5">
                <span>●</span>
                <span>{mentor.availableSlots} {mentor.availableSlots === 1 ? "slot" : "slots"} open</span>
              </p>
            ) : (
              <p className="text-lg font-extrabold text-gray-500 flex items-center gap-1.5">
                <span>○</span>
                <span>Capacity Full</span>
              </p>
            )}
            <span className="text-[11px] text-gray-500">Accepting new students</span>
          </div>
        </div>

        {/* Existing Request Status Callout (if student has already sent a request) */}
        {existingRequest && (
          <div className="p-4 rounded-xl bg-indigo-50/70 border border-indigo-100 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-indigo-950 flex items-center gap-1.5">
                <span>📬</span>
                <span>Your Mentorship Request Status: {existingRequest.status}</span>
              </span>
              <span className="text-[11px] text-indigo-700">
                Submitted {new Date(existingRequest.createdAt).toLocaleDateString()}
              </span>
            </div>
            <div className="text-xs text-indigo-900 bg-white/80 p-3 rounded-lg border border-indigo-100 space-y-1">
              <span className="font-semibold block text-[11px] text-gray-500">Submitted Goal:</span>
              <p className="whitespace-pre-line">{existingRequest.goalDescription}</p>
            </div>
            {existingRequest.responseNote && (
              <div className="text-xs text-indigo-950 bg-indigo-100/60 p-3 rounded-lg border border-indigo-200">
                <span className="font-bold block mb-0.5">Note from {mentor.name}:</span>
                <p>{existingRequest.responseNote}</p>
              </div>
            )}
          </div>
        )}

        {/* Expertise Areas */}
        {expertiseTags.length > 0 && (
          <div className="space-y-3">
            <h2 className="text-sm font-bold text-gray-900 uppercase tracking-wider text-[11px]">
              Technical & Career Expertise
            </h2>
            <div className="flex items-center gap-2 flex-wrap">
              {expertiseTags.map((tag) => (
                <span
                  key={tag}
                  className="text-xs font-semibold px-3 py-1.5 rounded-lg bg-gray-100 text-gray-800 border border-gray-200"
                >
                  {tag}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Biography */}
        {mentor.bio && (
          <div className="space-y-3">
            <h2 className="text-sm font-bold text-gray-900 uppercase tracking-wider text-[11px]">
              About the Mentor
            </h2>
            <div className="p-4 rounded-xl bg-gray-50 border border-gray-100 text-xs sm:text-sm text-gray-700 leading-relaxed whitespace-pre-line">
              {mentor.bio}
            </div>
          </div>
        )}

        {/* Verified Profile Links */}
        {hasExternalLinks && (
          <div className="space-y-3">
            <h2 className="text-sm font-bold text-gray-900 uppercase tracking-wider text-[11px]">
              Verified Profiles & Portfolio
            </h2>
            <div className="flex flex-col sm:flex-row gap-3 text-xs flex-wrap">
              {mentor.linkedinUrl && (
                <a
                  href={mentor.linkedinUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 p-3 rounded-xl border border-gray-200 bg-white hover:bg-gray-50 text-indigo-600 font-semibold transition"
                >
                  <span>🔗</span>
                  <span>LinkedIn Profile ↗</span>
                </a>
              )}

              {mentor.githubUrl && (
                <a
                  href={mentor.githubUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 p-3 rounded-xl border border-gray-200 bg-white hover:bg-gray-50 text-gray-800 font-semibold transition"
                >
                  <span>💻</span>
                  <span>GitHub Profile ↗</span>
                </a>
              )}

              {mentor.codingProfileUrl && (
                <a
                  href={mentor.codingProfileUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 p-3 rounded-xl border border-amber-200 bg-amber-50/40 hover:bg-amber-50 text-amber-800 font-semibold transition"
                >
                  <span>⚡</span>
                  <span>Coding Practice Profile ↗</span>
                </a>
              )}

              {mentor.portfolioUrl && (
                <a
                  href={mentor.portfolioUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 p-3 rounded-xl border border-gray-200 bg-white hover:bg-gray-50 text-indigo-700 font-semibold transition"
                >
                  <span>🌐</span>
                  <span>Portfolio & Website ↗</span>
                </a>
              )}
            </div>
          </div>
        )}

        {/* Bottom Navigation & Actions */}
        <div className="pt-6 border-t border-gray-100 flex items-center justify-between">
          <Link
            href="/mentorship"
            className="px-4 py-2 text-xs font-semibold rounded-xl bg-gray-100 text-gray-700 hover:bg-gray-200 transition"
          >
            ← Return to Directory
          </Link>

          {!isSelf && !isPending && !isAccepted && !isFull && (
            <button
              onClick={() => {
                setIsRequestModalOpen(true);
                setRequestError(null);
              }}
              className="px-5 py-2 text-xs font-semibold rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white transition shadow-xs cursor-pointer"
            >
              Request Mentorship with {mentor.name}
            </button>
          )}
        </div>
      </div>

      {/* ---------------------------------------------------- */}
      {/* REQUEST MENTORSHIP MODAL                             */}
      {/* ---------------------------------------------------- */}
      {isRequestModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in">
          <div
            className="bg-white rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto border border-gray-200 shadow-2xl p-6 sm:p-8 space-y-5"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div className="flex items-start justify-between gap-4 pb-4 border-b border-gray-100">
              <div className="space-y-1">
                <span className="text-xs font-bold text-indigo-600 uppercase tracking-wider">
                  Request 1-on-1 Mentorship
                </span>
                <h2 className="text-lg sm:text-xl font-extrabold text-gray-900">
                  Connect with {mentor.name}
                </h2>
                <p className="text-xs text-gray-500">
                  {mentor.jobTitle} @ {mentor.company}
                </p>
              </div>

              <button
                onClick={() => setIsRequestModalOpen(false)}
                disabled={isSubmittingRequest}
                className="p-1.5 rounded-lg text-gray-400 hover:text-gray-900 hover:bg-gray-100 transition cursor-pointer"
                aria-label="Close modal"
              >
                ✕
              </button>
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
                  placeholder="Explain what specific guidance, code architecture reviews, interview preparation, or skill milestones you wish to achieve..."
                  className="w-full p-3 rounded-xl text-xs sm:text-sm border border-gray-300 bg-white focus:outline-hidden focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition text-gray-900 placeholder:text-gray-400 resize-y"
                />
                <p className="text-[11px] text-gray-500">
                  Mentors accept requests based on clarity of purpose and realistic goals.
                </p>
              </div>

              {/* Form Actions */}
              <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setIsRequestModalOpen(false)}
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
    </div>
  );
}
