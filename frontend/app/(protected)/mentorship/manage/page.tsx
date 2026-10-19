"use client";

import { useEffect, useState, useCallback, useMemo } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import {
  mentorService,
  MentorProfileDto,
  MentorshipRequestDto,
  MentorshipRequestStatus,
} from "@/lib/mentorService";
import { ApiError } from "@/lib/apiClient";

type AuthStateType = "APPROVED" | "APPLIED" | "REJECTED" | "SUSPENDED" | "NOT_FOUND" | "ERROR";

export default function MentorManageDashboardPage() {
  const router = useRouter();
  const { user } = useAuth();

  // Mentor profile & authorization state
  const [profile, setProfile] = useState<MentorProfileDto | null>(null);
  const [authState, setAuthState] = useState<AuthStateType | null>(null);
  const [authErrorMessage, setAuthErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [successBanner, setSuccessBanner] = useState<string | null>(null);

  // Incoming requests state
  const [requests, setRequests] = useState<MentorshipRequestDto[]>([]);
  const [statusFilter, setStatusFilter] = useState<"ALL" | MentorshipRequestStatus>("ALL");

  // Accept Modal state
  const [acceptModalRequest, setAcceptModalRequest] = useState<MentorshipRequestDto | null>(null);
  const [acceptNote, setAcceptNote] = useState<string>("");
  const [isSubmittingAccept, setIsSubmittingAccept] = useState<boolean>(false);
  const [acceptError, setAcceptError] = useState<string | null>(null);

  // Decline Modal state
  const [declineModalRequest, setDeclineModalRequest] = useState<MentorshipRequestDto | null>(null);
  const [declineNote, setDeclineNote] = useState<string>("");
  const [isSubmittingDecline, setIsSubmittingDecline] = useState<boolean>(false);
  const [declineError, setDeclineError] = useState<string | null>(null);

  // Complete Mentorship Modal state
  const [completeModalRequest, setCompleteModalRequest] = useState<MentorshipRequestDto | null>(null);
  const [completeNote, setCompleteNote] = useState<string>("");
  const [isSubmittingComplete, setIsSubmittingComplete] = useState<boolean>(false);
  const [completeError, setCompleteError] = useState<string | null>(null);

  // Load mentor profile and incoming requests
  const loadDashboardData = useCallback(async () => {
    setIsLoading(true);
    setPageError(null);
    setAuthErrorMessage(null);

    try {
      // 1. Retrieve current user's mentor profile
      const myProfile = await mentorService.getMyProfile();
      setProfile(myProfile);

      // 2. Validate verification status
      if (myProfile.verificationStatus !== "APPROVED") {
        setAuthState(myProfile.verificationStatus);
        setIsLoading(false);
        return;
      }

      setAuthState("APPROVED");

      // 3. Retrieve received requests for approved mentor
      const received = await mentorService.getReceivedRequests();
      setRequests(received);
    } catch (err: any) {
      if (err instanceof ApiError) {
        if (err.status === 404) {
          setAuthState("NOT_FOUND");
        } else {
          setAuthState("ERROR");
          setPageError(err.message || `Failed to load dashboard (HTTP ${err.status})`);
        }
      } else {
        setAuthState("ERROR");
        setPageError("Unable to connect to the mentorship service. Please check your connection.");
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboardData();
  }, [loadDashboardData]);

  // Request status counts
  const counts = useMemo(() => {
    return {
      all: requests.length,
      pending: requests.filter((r) => r.status === "PENDING").length,
      accepted: requests.filter((r) => r.status === "ACCEPTED").length,
      declined: requests.filter((r) => r.status === "DECLINED").length,
      completed: requests.filter((r) => r.status === "COMPLETED").length,
    };
  }, [requests]);

  // Filtered and sorted requests: PENDING first, then by date descending
  const filteredRequests = useMemo(() => {
    let result = requests;
    if (statusFilter !== "ALL") {
      result = result.filter((r) => r.status === statusFilter);
    }

    return [...result].sort((a, b) => {
      const getPriority = (status: MentorshipRequestStatus) => {
        switch (status) {
          case "PENDING":
            return 1;
          case "ACCEPTED":
            return 2;
          case "COMPLETED":
            return 3;
          case "DECLINED":
            return 4;
          case "CANCELLED":
            return 5;
          default:
            return 6;
        }
      };

      const pA = getPriority(a.status);
      const pB = getPriority(b.status);
      if (pA !== pB) return pA - pB;

      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });
  }, [requests, statusFilter]);

  // Capacity calculations
  const activeCount = profile?.activeMenteesCount ?? 0;
  const maxCapacity = profile?.maxMentees ?? 1;
  const availableSlots = profile?.availableSlots ?? 0;
  const capacityPercent =
    maxCapacity > 0 ? Math.min(100, Math.round((activeCount / maxCapacity) * 100)) : 0;

  // Handle Accept Submit
  const handleConfirmAccept = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!acceptModalRequest) return;

    if (acceptNote.length > 1000) {
      setAcceptError("Response note cannot exceed 1000 characters.");
      return;
    }

    setIsSubmittingAccept(true);
    setAcceptError(null);

    try {
      const updated = await mentorService.acceptRequest(
        acceptModalRequest.id,
        acceptNote.trim() || undefined
      );

      // Update local requests list
      setRequests((prev) =>
        prev.map((r) => (r.id === acceptModalRequest.id ? updated : r))
      );
      setSuccessBanner(
        `Mentorship request from ${acceptModalRequest.studentName} accepted! A 1-on-1 direct conversation has been established in Messages.`
      );
      setAcceptModalRequest(null);
      setAcceptNote("");

      // Refresh mentor profile to update capacity metrics
      const refreshedProfile = await mentorService.getMyProfile();
      setProfile(refreshedProfile);
    } catch (err: any) {
      if (err instanceof ApiError) {
        if (err.status === 409 || err.status === 400) {
          setAcceptError(err.message || "Failed to accept request. Capacity may be full.");
        } else {
          setAcceptError(err.message || `Acceptance failed with HTTP ${err.status}`);
        }
      } else {
        setAcceptError("An unexpected network error occurred while accepting.");
      }
    } finally {
      setIsSubmittingAccept(false);
    }
  };

  // Handle Decline Submit
  const handleConfirmDecline = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!declineModalRequest) return;

    if (declineNote.length > 1000) {
      setDeclineError("Response note cannot exceed 1000 characters.");
      return;
    }

    setIsSubmittingDecline(true);
    setDeclineError(null);

    try {
      const updated = await mentorService.declineRequest(
        declineModalRequest.id,
        declineNote.trim() || undefined
      );

      setRequests((prev) =>
        prev.map((r) => (r.id === declineModalRequest.id ? updated : r))
      );
      setSuccessBanner(`Mentorship request from ${declineModalRequest.studentName} has been declined.`);
      setDeclineModalRequest(null);
      setDeclineNote("");
    } catch (err: any) {
      if (err instanceof ApiError) {
        setDeclineError(err.message || "Failed to decline request.");
      } else {
        setDeclineError("An unexpected network error occurred while declining.");
      }
    } finally {
      setIsSubmittingDecline(false);
    }
  };

  // Handle Complete Mentorship Submit
  const handleConfirmComplete = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!completeModalRequest) return;

    if (completeNote.length > 1000) {
      setCompleteError("Completion note cannot exceed 1000 characters.");
      return;
    }

    setIsSubmittingComplete(true);
    setCompleteError(null);

    try {
      const updated = await mentorService.completeMentorship(
        completeModalRequest.id,
        completeNote.trim() || undefined
      );

      setRequests((prev) =>
        prev.map((r) => (r.id === completeModalRequest.id ? updated : r))
      );
      setSuccessBanner(
        `Mentorship with ${completeModalRequest.studentName} has been marked as Completed. 1 mentee slot is now available.`
      );
      setCompleteModalRequest(null);
      setCompleteNote("");

      // Refresh mentor profile to update capacity metrics
      const refreshedProfile = await mentorService.getMyProfile();
      setProfile(refreshedProfile);
    } catch (err: any) {
      if (err instanceof ApiError) {
        setCompleteError(err.message || "Failed to mark mentorship as completed.");
      } else {
        setCompleteError("An unexpected network error occurred.");
      }
    } finally {
      setIsSubmittingComplete(false);
    }
  };

  // ------------------------------------------------------------------
  // RENDER: LOADING SKELETON
  // ------------------------------------------------------------------
  if (isLoading) {
    return (
      <div className="max-w-6xl mx-auto space-y-6 pb-12 animate-pulse">
        <div className="h-8 bg-gray-200 rounded w-1/4"></div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="h-32 bg-gray-200 rounded-2xl"></div>
          <div className="h-32 bg-gray-200 rounded-2xl"></div>
          <div className="h-32 bg-gray-200 rounded-2xl"></div>
        </div>
        <div className="h-64 bg-gray-100 rounded-2xl"></div>
      </div>
    );
  }

  // ------------------------------------------------------------------
  // RENDER: NON-APPROVED / UNAUTHORIZED STATES
  // ------------------------------------------------------------------
  if (authState !== "APPROVED") {
    return (
      <div className="max-w-2xl mx-auto space-y-6 py-12 text-center">
        <div className="bg-white rounded-2xl border border-gray-200 shadow-xs p-8 sm:p-12 space-y-5">
          {authState === "NOT_FOUND" && (
            <>
              <span className="text-4xl">✨</span>
              <h1 className="text-xl sm:text-2xl font-extrabold text-gray-900">
                Mentor Application Required
              </h1>
              <p className="text-xs sm:text-sm text-gray-600 max-w-md mx-auto leading-relaxed">
                The Mentor Management Dashboard is available exclusively to approved mentors.
                Share your engineering experience and help guide emerging tech talent!
              </p>
              <div className="pt-4 flex justify-center gap-3">
                <Link
                  href="/mentorship/apply"
                  className="px-5 py-2.5 text-xs font-semibold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 transition shadow-xs"
                >
                  Apply to Become a Mentor
                </Link>
                <Link
                  href="/mentorship"
                  className="px-4 py-2.5 text-xs font-semibold rounded-xl bg-gray-100 text-gray-700 hover:bg-gray-200 transition"
                >
                  Back to Hub
                </Link>
              </div>
            </>
          )}

          {authState === "APPLIED" && (
            <>
              <span className="text-4xl">⏳</span>
              <h1 className="text-xl sm:text-2xl font-extrabold text-gray-900">
                Application Under Review
              </h1>
              <p className="text-xs sm:text-sm text-gray-600 max-w-md mx-auto leading-relaxed">
                Your mentor application has been received and is currently undergoing administrative review.
                Once approved, this dashboard will activate automatically.
              </p>
              <div className="pt-4 flex justify-center gap-3">
                <Link
                  href="/mentorship/apply"
                  className="px-5 py-2.5 text-xs font-semibold rounded-xl bg-amber-600 text-white hover:bg-amber-700 transition shadow-xs"
                >
                  View Application Details
                </Link>
                <Link
                  href="/mentorship"
                  className="px-4 py-2.5 text-xs font-semibold rounded-xl bg-gray-100 text-gray-700 hover:bg-gray-200 transition"
                >
                  Back to Mentorship Hub
                </Link>
              </div>
            </>
          )}

          {authState === "REJECTED" && (
            <>
              <span className="text-4xl">⚠️</span>
              <h1 className="text-xl sm:text-2xl font-extrabold text-gray-900">
                Application Not Approved
              </h1>
              <p className="text-xs sm:text-sm text-gray-600 max-w-md mx-auto leading-relaxed">
                Your mentor profile was reviewed and not approved at this time. Management actions are disabled.
              </p>
              {profile?.rejectionReason && (
                <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-900 text-left max-w-md mx-auto">
                  <span className="font-bold block mb-1">Feedback from Review Team:</span>
                  <p>{profile.rejectionReason}</p>
                </div>
              )}
              <div className="pt-4 flex justify-center gap-3">
                <Link
                  href="/mentorship"
                  className="px-5 py-2.5 text-xs font-semibold rounded-xl bg-gray-900 text-white hover:bg-gray-800 transition"
                >
                  Return to Mentorship Hub
                </Link>
              </div>
            </>
          )}

          {authState === "SUSPENDED" && (
            <>
              <span className="text-4xl">🚫</span>
              <h1 className="text-xl sm:text-2xl font-extrabold text-gray-900">
                Mentorship Privileges Suspended
              </h1>
              <p className="text-xs sm:text-sm text-gray-600 max-w-md mx-auto leading-relaxed">
                Your mentor privileges are temporarily suspended. You cannot accept new requests while suspended.
                Please contact support for resolution.
              </p>
              <div className="pt-4 flex justify-center gap-3">
                <Link
                  href="/messages"
                  className="px-5 py-2.5 text-xs font-semibold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 transition shadow-xs"
                >
                  Contact Support
                </Link>
                <Link
                  href="/mentorship"
                  className="px-4 py-2.5 text-xs font-semibold rounded-xl bg-gray-100 text-gray-700 hover:bg-gray-200 transition"
                >
                  Return to Hub
                </Link>
              </div>
            </>
          )}

          {authState === "ERROR" && (
            <>
              <span className="text-4xl">⚠️</span>
              <h1 className="text-xl sm:text-2xl font-extrabold text-gray-900">
                Dashboard Error
              </h1>
              <p className="text-xs sm:text-sm text-rose-600 max-w-md mx-auto leading-relaxed">
                {pageError || "An error occurred while loading your mentor dashboard."}
              </p>
              <div className="pt-4 flex justify-center gap-3">
                <button
                  onClick={loadDashboardData}
                  className="px-5 py-2.5 text-xs font-semibold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 transition shadow-xs cursor-pointer"
                >
                  Retry Loading
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    );
  }

  // ------------------------------------------------------------------
  // RENDER: APPROVED MENTOR DASHBOARD
  // ------------------------------------------------------------------
  return (
    <div className="max-w-6xl mx-auto space-y-8 pb-12">
      {/* Navigation Breadcrumb */}
      <div className="flex items-center justify-between text-xs font-semibold text-gray-500">
        <div className="flex items-center gap-2">
          <Link href="/mentorship" className="hover:text-indigo-600 transition">
            Mentorship Hub
          </Link>
          <span>/</span>
          <span className="text-gray-900 font-bold">Mentor Management Dashboard</span>
        </div>

        {profile && (
          <Link
            href={`/mentorship/${profile.id}`}
            className="text-indigo-600 hover:underline flex items-center gap-1"
          >
            <span>View Your Public Profile</span>
            <span>↗</span>
          </Link>
        )}
      </div>

      {/* Header Banner */}
      <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-2.5">
            <span className="text-2xl">🛡️</span>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
              Mentor Management Dashboard
            </h1>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
              ● Active Mentor
            </span>
          </div>
          <p className="text-xs sm:text-sm text-gray-600 max-w-2xl leading-relaxed">
            Review incoming mentee requests, manage your active mentee capacity, and guide students
            toward their engineering career goals.
          </p>
        </div>

        <div className="flex items-center gap-3 shrink-0">
          <button
            onClick={loadDashboardData}
            className="px-3.5 py-2 text-xs font-semibold rounded-xl bg-gray-100 text-gray-700 hover:bg-gray-200 transition flex items-center gap-1.5 cursor-pointer"
          >
            <span>↻</span>
            <span>Refresh</span>
          </button>
        </div>
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
      {/* CAPACITY OVERVIEW CARDS                              */}
      {/* ---------------------------------------------------- */}
      <div className="space-y-3">
        <h2 className="text-sm font-bold text-gray-900 uppercase tracking-wider text-[11px]">
          Guidance Capacity Overview
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
          {/* Active Mentees */}
          <div className="bg-white rounded-2xl border border-gray-200 p-5 shadow-xs space-y-2">
            <span className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider block">
              Active Mentees
            </span>
            <div className="flex items-baseline justify-between">
              <span className="text-3xl font-extrabold text-gray-900">{activeCount}</span>
              <span className="text-xs text-gray-500 font-medium">students</span>
            </div>
            <p className="text-[11px] text-gray-500">Currently receiving your 1-on-1 guidance</p>
          </div>

          {/* Maximum Capacity */}
          <div className="bg-white rounded-2xl border border-gray-200 p-5 shadow-xs space-y-2">
            <span className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider block">
              Max Mentee Capacity
            </span>
            <div className="flex items-baseline justify-between">
              <span className="text-3xl font-extrabold text-gray-900">{maxCapacity}</span>
              <span className="text-xs text-gray-500 font-medium">max limit</span>
            </div>
            <p className="text-[11px] text-gray-500">Maximum concurrent active mentorships</p>
          </div>

          {/* Available Slots */}
          <div className="bg-white rounded-2xl border border-gray-200 p-5 shadow-xs space-y-2">
            <span className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider block">
              Available Slots
            </span>
            <div className="flex items-baseline justify-between">
              <span
                className={`text-3xl font-extrabold ${
                  availableSlots > 0 ? "text-emerald-600" : "text-amber-600"
                }`}
              >
                {availableSlots}
              </span>
              <span className="text-xs text-gray-500 font-medium">
                {availableSlots === 1 ? "slot" : "slots"}
              </span>
            </div>
            <p className="text-[11px] text-gray-500">
              {availableSlots > 0
                ? "Open for new incoming mentorship requests"
                : "Capacity reached; finish active mentorships to free slots"}
            </p>
          </div>
        </div>

        {/* Capacity Progress Bar */}
        <div className="bg-white rounded-2xl border border-gray-200 p-4 shadow-xs space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="font-semibold text-gray-700">Capacity Utilization</span>
            <span className="font-bold text-gray-900">
              {activeCount} of {maxCapacity} slots occupied ({capacityPercent}%)
            </span>
          </div>
          <div className="w-full h-2.5 bg-gray-100 rounded-full overflow-hidden">
            <div
              className={`h-full transition-all duration-500 ${
                capacityPercent >= 100
                  ? "bg-rose-500"
                  : capacityPercent >= 75
                  ? "bg-amber-500"
                  : "bg-emerald-500"
              }`}
              style={{ width: `${capacityPercent}%` }}
            ></div>
          </div>
          <p className="text-[11px] text-gray-400">
            * Note: Pending requests do not consume capacity until accepted.
          </p>
        </div>
      </div>

      {/* ---------------------------------------------------- */}
      {/* INCOMING REQUESTS SECTION                            */}
      {/* ---------------------------------------------------- */}
      <div className="space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h2 className="text-lg sm:text-xl font-bold text-gray-900">
              Incoming Mentorship Requests
            </h2>
            <p className="text-xs text-gray-500">
              Review goals submitted by students and accept or decline based on your availability
            </p>
          </div>

          {/* Status Filter Tabs */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-thin">
            <button
              onClick={() => setStatusFilter("ALL")}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition shrink-0 cursor-pointer ${
                statusFilter === "ALL"
                  ? "bg-black text-white"
                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}
            >
              All ({counts.all})
            </button>
            <button
              onClick={() => setStatusFilter("PENDING")}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition shrink-0 cursor-pointer ${
                statusFilter === "PENDING"
                  ? "bg-amber-600 text-white"
                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}
            >
              Pending ({counts.pending})
            </button>
            <button
              onClick={() => setStatusFilter("ACCEPTED")}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition shrink-0 cursor-pointer ${
                statusFilter === "ACCEPTED"
                  ? "bg-emerald-600 text-white"
                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}
            >
              Active ({counts.accepted})
            </button>
            <button
              onClick={() => setStatusFilter("DECLINED")}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition shrink-0 cursor-pointer ${
                statusFilter === "DECLINED"
                  ? "bg-rose-600 text-white"
                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}
            >
              Declined ({counts.declined})
            </button>
            <button
              onClick={() => setStatusFilter("COMPLETED")}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition shrink-0 cursor-pointer ${
                statusFilter === "COMPLETED"
                  ? "bg-blue-600 text-white"
                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}
            >
              Completed ({counts.completed})
            </button>
          </div>
        </div>

        {/* Requests List */}
        {filteredRequests.length > 0 ? (
          <div className="space-y-4">
            {filteredRequests.map((req) => {
              const isPending = req.status === "PENDING";
              const isAccepted = req.status === "ACCEPTED";
              const isDeclined = req.status === "DECLINED";
              const isCompleted = req.status === "COMPLETED";
              const isCancelled = req.status === "CANCELLED";

              return (
                <div
                  key={req.id}
                  className={`bg-white rounded-2xl border p-6 shadow-xs space-y-4 transition ${
                    isPending
                      ? "border-amber-200 bg-amber-50/15"
                      : "border-gray-200 hover:border-gray-300"
                  }`}
                >
                  {/* Card Header */}
                  <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2.5 flex-wrap">
                        <h3 className="text-base font-bold text-gray-900">
                          {req.studentName}
                        </h3>

                        {isPending && (
                          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-amber-50 text-amber-800 border border-amber-200">
                            ⏳ Pending Your Review
                          </span>
                        )}
                        {isAccepted && (
                          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">
                            ✅ Active Mentorship
                          </span>
                        )}
                        {isDeclined && (
                          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-rose-50 text-rose-800 border border-rose-200">
                            ❌ Declined
                          </span>
                        )}
                        {isCompleted && (
                          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-blue-50 text-blue-800 border border-blue-200">
                            🎓 Completed
                          </span>
                        )}
                        {isCancelled && (
                          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-gray-100 text-gray-700 border border-gray-200">
                            🚫 Cancelled by Student
                          </span>
                        )}
                      </div>

                      <p className="text-xs text-gray-500 font-medium">
                        Student Email: <span className="text-gray-700">{req.studentEmail}</span> • Requested on{" "}
                        {new Date(req.createdAt).toLocaleDateString(undefined, {
                          year: "numeric",
                          month: "short",
                          day: "numeric",
                        })}
                      </p>
                    </div>

                    {/* Action Buttons */}
                    <div className="flex items-center gap-2 shrink-0">
                      {isPending && (
                        <>
                          <button
                            onClick={() => {
                              setDeclineModalRequest(req);
                              setDeclineNote("");
                              setDeclineError(null);
                            }}
                            className="px-3.5 py-1.5 rounded-xl text-xs font-semibold text-rose-700 bg-rose-50 border border-rose-200 hover:bg-rose-100 transition cursor-pointer"
                          >
                            Decline
                          </button>
                          <button
                            onClick={() => {
                              setAcceptModalRequest(req);
                              setAcceptNote("");
                              setAcceptError(null);
                            }}
                            disabled={availableSlots <= 0}
                            title={
                              availableSlots <= 0
                                ? "Cannot accept: maximum mentee capacity reached."
                                : "Accept mentorship request"
                            }
                            className="px-4 py-1.5 rounded-xl text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed shadow-xs cursor-pointer"
                          >
                            Accept Request
                          </button>
                        </>
                      )}

                      {isAccepted && (
                        <>
                          <Link
                            href="/messages"
                            className="px-4 py-1.5 rounded-xl text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 transition flex items-center gap-1.5 shadow-xs"
                          >
                            <span>💬</span>
                            <span>Open Messages</span>
                          </Link>
                          <button
                            onClick={() => {
                              setCompleteModalRequest(req);
                              setCompleteNote("");
                              setCompleteError(null);
                            }}
                            className="px-3 py-1.5 rounded-xl text-xs font-semibold text-gray-700 bg-gray-100 hover:bg-gray-200 transition cursor-pointer"
                          >
                            Complete Mentorship
                          </button>
                        </>
                      )}
                    </div>
                  </div>

                  {/* Student Goal Description */}
                  <div className="p-3.5 rounded-xl bg-gray-50 border border-gray-100 text-xs text-gray-700 space-y-1">
                    <span className="font-semibold text-gray-900 block">Student&apos;s Mentorship Goal:</span>
                    <p className="whitespace-pre-line leading-relaxed">{req.goalDescription}</p>
                  </div>

                  {/* Response Note (if already responded) */}
                  {req.responseNote && (
                    <div className="p-3 rounded-xl bg-indigo-50/70 border border-indigo-100 text-xs text-indigo-950 space-y-0.5">
                      <span className="font-bold block text-[11px] text-indigo-900">Your Response Note:</span>
                      <p className="whitespace-pre-line">{req.responseNote}</p>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        ) : (
          <div className="p-12 rounded-2xl bg-white border border-gray-200 text-center space-y-3">
            <span className="text-3xl">📬</span>
            <h3 className="text-base font-bold text-gray-900">
              No {statusFilter !== "ALL" ? statusFilter.toLowerCase() : ""} requests found
            </h3>
            <p className="text-xs text-gray-500 max-w-md mx-auto">
              {statusFilter === "PENDING"
                ? "You have answered all incoming mentorship requests. Great job!"
                : statusFilter === "ALL"
                ? "You have not received any mentorship requests from students yet. Ensure your profile expertise areas are complete."
                : "There are currently no requests under this status filter."}
            </p>
            {statusFilter !== "ALL" && (
              <button
                onClick={() => setStatusFilter("ALL")}
                className="mt-2 px-4 py-2 text-xs font-semibold rounded-lg bg-gray-100 text-gray-800 hover:bg-gray-200 transition cursor-pointer"
              >
                View All Requests
              </button>
            )}
          </div>
        )}
      </div>

      {/* ---------------------------------------------------- */}
      {/* ACCEPT CONFIRMATION MODAL                            */}
      {/* ---------------------------------------------------- */}
      {acceptModalRequest && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in">
          <div
            className="bg-white rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto border border-gray-200 shadow-2xl p-6 sm:p-8 space-y-5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-4 pb-4 border-b border-gray-100">
              <div className="space-y-1">
                <span className="text-xs font-bold text-indigo-600 uppercase tracking-wider">
                  Accept Mentee
                </span>
                <h3 className="text-lg sm:text-xl font-extrabold text-gray-900">
                  Accept {acceptModalRequest.studentName}
                </h3>
                <p className="text-xs text-gray-500">{acceptModalRequest.studentEmail}</p>
              </div>

              <button
                onClick={() => setAcceptModalRequest(null)}
                disabled={isSubmittingAccept}
                className="p-1.5 rounded-lg text-gray-400 hover:text-gray-900 hover:bg-gray-100 transition cursor-pointer"
              >
                ✕
              </button>
            </div>

            {/* Student Goal Preview */}
            <div className="p-3.5 rounded-xl bg-gray-50 border border-gray-100 text-xs text-gray-700 space-y-1">
              <span className="font-semibold text-gray-900 block">Student&apos;s Goal:</span>
              <p className="whitespace-pre-line line-clamp-4">{acceptModalRequest.goalDescription}</p>
            </div>

            {/* Error Banner */}
            {acceptError && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-900 text-xs space-y-1">
                <p className="font-bold">Unable to accept request</p>
                <p>{acceptError}</p>
              </div>
            )}

            <form onSubmit={handleConfirmAccept} className="space-y-4">
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label htmlFor="acceptNote" className="text-xs font-bold text-gray-800">
                    Welcome Note / Guidance Note <span className="text-gray-400 font-normal">(Optional)</span>
                  </label>
                  <span className="text-[11px] text-gray-400">{acceptNote.length}/1000</span>
                </div>
                <textarea
                  id="acceptNote"
                  rows={3}
                  maxLength={1000}
                  value={acceptNote}
                  onChange={(e) => setAcceptNote(e.target.value)}
                  placeholder="Welcome! Looking forward to working together. Let's start by discussing your milestones in Messages..."
                  className="w-full p-3 rounded-xl text-xs sm:text-sm border border-gray-300 bg-white focus:outline-hidden focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition text-gray-900 placeholder:text-gray-400 resize-y"
                />
                <p className="text-[11px] text-gray-500">
                  Accepting automatically creates a 1-on-1 direct conversation with the student in Messages.
                </p>
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setAcceptModalRequest(null)}
                  disabled={isSubmittingAccept}
                  className="px-4 py-2 text-xs font-semibold rounded-xl text-gray-700 bg-gray-100 hover:bg-gray-200 transition cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingAccept}
                  className="px-5 py-2 text-xs font-semibold rounded-xl text-white bg-indigo-600 hover:bg-indigo-700 transition disabled:opacity-50 flex items-center gap-2 cursor-pointer shadow-xs"
                >
                  {isSubmittingAccept ? (
                    <>
                      <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      <span>Accepting...</span>
                    </>
                  ) : (
                    <span>Confirm Acceptance</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ---------------------------------------------------- */}
      {/* DECLINE CONFIRMATION MODAL                           */}
      {/* ---------------------------------------------------- */}
      {declineModalRequest && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in">
          <div
            className="bg-white rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto border border-gray-200 shadow-2xl p-6 sm:p-8 space-y-5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-4 pb-4 border-b border-gray-100">
              <div className="space-y-1">
                <span className="text-xs font-bold text-rose-600 uppercase tracking-wider">
                  Decline Request
                </span>
                <h3 className="text-lg sm:text-xl font-extrabold text-gray-900">
                  Decline {declineModalRequest.studentName}
                </h3>
                <p className="text-xs text-gray-500">{declineModalRequest.studentEmail}</p>
              </div>

              <button
                onClick={() => setDeclineModalRequest(null)}
                disabled={isSubmittingDecline}
                className="p-1.5 rounded-lg text-gray-400 hover:text-gray-900 hover:bg-gray-100 transition cursor-pointer"
              >
                ✕
              </button>
            </div>

            <div className="p-3.5 rounded-xl bg-gray-50 border border-gray-100 text-xs text-gray-700 space-y-1">
              <span className="font-semibold text-gray-900 block">Student&apos;s Goal:</span>
              <p className="whitespace-pre-line line-clamp-3">{declineModalRequest.goalDescription}</p>
            </div>

            {declineError && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-900 text-xs space-y-1">
                <p className="font-bold">Unable to decline request</p>
                <p>{declineError}</p>
              </div>
            )}

            <form onSubmit={handleConfirmDecline} className="space-y-4">
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label htmlFor="declineNote" className="text-xs font-bold text-gray-800">
                    Reason / Guidance for Student <span className="text-gray-400 font-normal">(Optional)</span>
                  </label>
                  <span className="text-[11px] text-gray-400">{declineNote.length}/1000</span>
                </div>
                <textarea
                  id="declineNote"
                  rows={3}
                  maxLength={1000}
                  value={declineNote}
                  onChange={(e) => setDeclineNote(e.target.value)}
                  placeholder="Thank you for reaching out. Unfortunately, my bandwidth is currently full / this topic falls outside my current mentorship scope..."
                  className="w-full p-3 rounded-xl text-xs sm:text-sm border border-gray-300 bg-white focus:outline-hidden focus:border-rose-600 focus:ring-2 focus:ring-rose-100 transition text-gray-900 placeholder:text-gray-400 resize-y"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setDeclineModalRequest(null)}
                  disabled={isSubmittingDecline}
                  className="px-4 py-2 text-xs font-semibold rounded-xl text-gray-700 bg-gray-100 hover:bg-gray-200 transition cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingDecline}
                  className="px-5 py-2 text-xs font-semibold rounded-xl text-white bg-rose-600 hover:bg-rose-700 transition disabled:opacity-50 flex items-center gap-2 cursor-pointer shadow-xs"
                >
                  {isSubmittingDecline ? (
                    <>
                      <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      <span>Declining...</span>
                    </>
                  ) : (
                    <span>Confirm Decline</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ---------------------------------------------------- */}
      {/* COMPLETE MENTORSHIP MODAL                            */}
      {/* ---------------------------------------------------- */}
      {completeModalRequest && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in">
          <div
            className="bg-white rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto border border-gray-200 shadow-2xl p-6 sm:p-8 space-y-5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-4 pb-4 border-b border-gray-100">
              <div className="space-y-1">
                <span className="text-xs font-bold text-blue-600 uppercase tracking-wider">
                  Complete Mentorship
                </span>
                <h3 className="text-lg sm:text-xl font-extrabold text-gray-900">
                  Conclude Mentorship with {completeModalRequest.studentName}
                </h3>
              </div>

              <button
                onClick={() => setCompleteModalRequest(null)}
                disabled={isSubmittingComplete}
                className="p-1.5 rounded-lg text-gray-400 hover:text-gray-900 hover:bg-gray-100 transition cursor-pointer"
              >
                ✕
              </button>
            </div>

            <div className="p-4 rounded-xl bg-blue-50 border border-blue-200 text-xs text-blue-950 space-y-1">
              <p className="font-bold">What does completing do?</p>
              <p className="leading-relaxed">
                Marking this mentorship as completed indicates that the active learning engagement has concluded.
                This will free up 1 slot in your mentee capacity. The conversation and message history will remain
                intact in Messages.
              </p>
            </div>

            {completeError && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-900 text-xs space-y-1">
                <p className="font-bold">Unable to complete mentorship</p>
                <p>{completeError}</p>
              </div>
            )}

            <form onSubmit={handleConfirmComplete} className="space-y-4">
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label htmlFor="completeNote" className="text-xs font-bold text-gray-800">
                    Final Remarks / Completion Note <span className="text-gray-400 font-normal">(Optional)</span>
                  </label>
                  <span className="text-[11px] text-gray-400">{completeNote.length}/1000</span>
                </div>
                <textarea
                  id="completeNote"
                  rows={3}
                  maxLength={1000}
                  value={completeNote}
                  onChange={(e) => setCompleteNote(e.target.value)}
                  placeholder="Congratulations on completing our mentorship goals! Continue practicing system design and building projects..."
                  className="w-full p-3 rounded-xl text-xs sm:text-sm border border-gray-300 bg-white focus:outline-hidden focus:border-blue-600 focus:ring-2 focus:ring-blue-100 transition text-gray-900 placeholder:text-gray-400 resize-y"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setCompleteModalRequest(null)}
                  disabled={isSubmittingComplete}
                  className="px-4 py-2 text-xs font-semibold rounded-xl text-gray-700 bg-gray-100 hover:bg-gray-200 transition cursor-pointer"
                >
                  Keep Active
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingComplete}
                  className="px-5 py-2 text-xs font-semibold rounded-xl text-white bg-blue-600 hover:bg-blue-700 transition disabled:opacity-50 flex items-center gap-2 cursor-pointer shadow-xs"
                >
                  {isSubmittingComplete ? (
                    <>
                      <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      <span>Completing...</span>
                    </>
                  ) : (
                    <span>Confirm Completion</span>
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
