"use client";

import { useEffect, useState, useCallback, useMemo } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import {
  mentorService,
  MentorProfileDto,
  MentorshipCapacityConfigDto,
} from "@/lib/mentorService";
import { ApiError } from "@/lib/apiClient";

type AdminMentorTab = "pending" | "approved" | "suspended" | "config";

export default function AdminMentorsPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();

  // Tab State
  const [activeTab, setActiveTab] = useState<AdminMentorTab>("pending");

  // Data State
  const [pendingApplications, setPendingApplications] = useState<MentorProfileDto[]>([]);
  const [approvedMentors, setApprovedMentors] = useState<MentorProfileDto[]>([]);
  const [suspendedMentors, setSuspendedMentors] = useState<MentorProfileDto[]>([]);
  const [capacityConfig, setCapacityConfig] = useState<MentorshipCapacityConfigDto>({
    minMentees: 1,
    maxMentees: 10,
    defaultMentees: 3,
  });

  // Loading & Error States
  const [isLoadingData, setIsLoadingData] = useState<boolean>(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error" | "warning";
    message: string;
  } | null>(null);

  // Search filter for approved & suspended mentors
  const [searchQuery, setSearchQuery] = useState<string>("");

  // Inspect / Action Modals
  const [inspectModalProfile, setInspectModalProfile] = useState<MentorProfileDto | null>(null);
  const [rejectModalProfile, setRejectModalProfile] = useState<MentorProfileDto | null>(null);
  const [rejectReason, setRejectReason] = useState<string>("");
  const [isSubmittingReject, setIsSubmittingReject] = useState<boolean>(false);

  const [suspendModalProfile, setSuspendModalProfile] = useState<MentorProfileDto | null>(null);
  const [suspendReason, setSuspendReason] = useState<string>("");
  const [isSubmittingSuspend, setIsSubmittingSuspend] = useState<boolean>(false);

  // Capacity Form State
  const [capacityForm, setCapacityForm] = useState<MentorshipCapacityConfigDto>({
    minMentees: 1,
    maxMentees: 10,
    defaultMentees: 3,
  });
  const [isSavingConfig, setIsSavingConfig] = useState<boolean>(false);
  const [configSuccessMsg, setConfigSuccessMsg] = useState<string | null>(null);

  // Client-side Route Guard for ROLE_ADMIN
  useEffect(() => {
    if (!isAuthLoading) {
      if (!isAuthenticated) {
        router.replace("/login");
      } else if (user?.role !== "ADMIN") {
        router.replace("/dashboard");
      }
    }
  }, [isAuthLoading, isAuthenticated, user, router]);

  // Keyboard shortcut: close modals on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        if (rejectModalProfile) setRejectModalProfile(null);
        else if (suspendModalProfile) setSuspendModalProfile(null);
        else if (inspectModalProfile) setInspectModalProfile(null);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [rejectModalProfile, suspendModalProfile, inspectModalProfile]);

  // Load All Admin Mentor Data
  const loadAdminData = useCallback(async () => {
    setIsLoadingData(true);
    setLoadError(null);
    setFeedback(null);
    try {
      const [pendingRes, approvedRes, suspendedRes, configRes] = await Promise.allSettled([
        mentorService.getPendingApplications(),
        mentorService.getAllMentors("APPROVED"),
        mentorService.getAllMentors("SUSPENDED"),
        mentorService.getCapacityConfig(),
      ]);

      let hasAuthError = false;
      let generalError: string | null = null;

      if (pendingRes.status === "fulfilled") {
        setPendingApplications(pendingRes.value);
      } else {
        if (pendingRes.reason instanceof ApiError && pendingRes.reason.status === 403) {
          hasAuthError = true;
        } else {
          generalError = pendingRes.reason?.message || "Failed to load pending applications";
        }
      }

      if (approvedRes.status === "fulfilled") {
        setApprovedMentors(approvedRes.value);
      } else {
        if (approvedRes.reason instanceof ApiError && approvedRes.reason.status === 403) {
          hasAuthError = true;
        } else {
          generalError = approvedRes.reason?.message || "Failed to load approved mentors";
        }
      }

      if (suspendedRes.status === "fulfilled") {
        setSuspendedMentors(suspendedRes.value);
      } else {
        if (suspendedRes.reason instanceof ApiError && suspendedRes.reason.status === 403) {
          hasAuthError = true;
        } else {
          generalError = suspendedRes.reason?.message || "Failed to load suspended mentors";
        }
      }

      if (configRes.status === "fulfilled") {
        setCapacityConfig(configRes.value);
        setCapacityForm(configRes.value);
      } else {
        if (configRes.reason instanceof ApiError && configRes.reason.status === 403) {
          hasAuthError = true;
        } else {
          generalError = configRes.reason?.message || "Failed to load capacity configuration";
        }
      }

      if (hasAuthError) {
        setLoadError("Administrator privilege verification failed (HTTP 403 Forbidden). Please ensure your account has administrative authority.");
      } else if (generalError) {
        setLoadError(generalError);
      }
    } catch (err: any) {
      setLoadError(err.message || "Failed to load mentorship administration data.");
    } finally {
      setIsLoadingData(false);
    }
  }, []);

  useEffect(() => {
    if (user?.role === "ADMIN") {
      loadAdminData();
    }
  }, [user, loadAdminData]);

  // Auto-dismiss success feedback after 6 seconds
  useEffect(() => {
    if (feedback?.type === "success") {
      const timer = setTimeout(() => setFeedback(null), 6000);
      return () => clearTimeout(timer);
    }
  }, [feedback]);

  // Capacity validation helpers
  const capacityValidationErrors = useMemo(() => {
    const errors: string[] = [];
    const min = Number(capacityForm.minMentees);
    const max = Number(capacityForm.maxMentees);
    const def = Number(capacityForm.defaultMentees);

    if (!Number.isInteger(min) || min < 1) {
      errors.push("Minimum mentees must be an integer of at least 1.");
    }
    if (!Number.isInteger(max) || max < 1) {
      errors.push("Maximum mentees must be an integer of at least 1.");
    }
    if (!Number.isInteger(def) || def < 1) {
      errors.push("Default mentees must be an integer of at least 1.");
    }
    if (Number.isInteger(min) && Number.isInteger(max) && max < min) {
      errors.push("Maximum capacity cannot be less than minimum capacity.");
    }
    if (
      Number.isInteger(min) &&
      Number.isInteger(max) &&
      Number.isInteger(def) &&
      (def < min || def > max)
    ) {
      errors.push(`Default capacity (${def}) must be between minimum (${min}) and maximum (${max}).`);
    }
    return errors;
  }, [capacityForm]);

  // Approve Application Handler
  const handleApproveApplication = async (mentorProfileId: number) => {
    setActionLoadingId(mentorProfileId);
    setFeedback(null);
    try {
      await mentorService.approveApplication(mentorProfileId);
      setFeedback({
        type: "success",
        message: `Mentor application #${mentorProfileId} approved successfully!`,
      });
      if (inspectModalProfile?.id === mentorProfileId) {
        setInspectModalProfile(null);
      }
      await loadAdminData();
    } catch (err: any) {
      const isSelfApprovalError =
        err?.message && err.message.toLowerCase().includes("cannot approve their own");
      if (isSelfApprovalError) {
        setFeedback({
          type: "warning",
          message:
            "Self-Approval Guard: Administrators cannot approve their own mentor application. Please have another administrator approve this profile.",
        });
      } else {
        setFeedback({
          type: "error",
          message: err?.message || "Failed to approve mentor application.",
        });
      }
    } finally {
      setActionLoadingId(null);
    }
  };

  // Reject Application Handler
  const handleConfirmReject = async () => {
    if (!rejectModalProfile) return;
    setIsSubmittingReject(true);
    setFeedback(null);
    try {
      await mentorService.rejectApplication(
        rejectModalProfile.id,
        rejectReason.trim() || undefined
      );
      setFeedback({
        type: "success",
        message: `Mentor application for ${rejectModalProfile.name} has been rejected.`,
      });
      setRejectModalProfile(null);
      setRejectReason("");
      if (inspectModalProfile?.id === rejectModalProfile.id) {
        setInspectModalProfile(null);
      }
      await loadAdminData();
    } catch (err: any) {
      setFeedback({
        type: "error",
        message: err?.message || "Failed to reject mentor application.",
      });
    } finally {
      setIsSubmittingReject(false);
    }
  };

  // Suspend Mentor Handler
  const handleConfirmSuspend = async () => {
    if (!suspendModalProfile) return;
    setIsSubmittingSuspend(true);
    setFeedback(null);
    try {
      await mentorService.suspendMentor(
        suspendModalProfile.id,
        suspendReason.trim() || undefined
      );
      setFeedback({
        type: "success",
        message: `Mentor ${suspendModalProfile.name} has been suspended.`,
      });
      setSuspendModalProfile(null);
      setSuspendReason("");
      if (inspectModalProfile?.id === suspendModalProfile.id) {
        setInspectModalProfile(null);
      }
      await loadAdminData();
    } catch (err: any) {
      setFeedback({
        type: "error",
        message: err?.message || "Failed to suspend mentor.",
      });
    } finally {
      setIsSubmittingSuspend(false);
    }
  };

  // Update Capacity Configuration Handler
  const handleSaveCapacityConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    if (capacityValidationErrors.length > 0) return;

    setIsSavingConfig(true);
    setConfigSuccessMsg(null);
    setFeedback(null);
    try {
      const updated = await mentorService.updateCapacityConfig({
        minMentees: Number(capacityForm.minMentees),
        maxMentees: Number(capacityForm.maxMentees),
        defaultMentees: Number(capacityForm.defaultMentees),
      });
      setCapacityConfig(updated);
      setCapacityForm(updated);
      setConfigSuccessMsg("Mentorship capacity parameters updated successfully.");
      setFeedback({
        type: "success",
        message: "Global mentorship capacity configuration saved successfully.",
      });
    } catch (err: any) {
      setFeedback({
        type: "error",
        message: err?.message || "Failed to update mentorship capacity configuration.",
      });
    } finally {
      setIsSavingConfig(false);
    }
  };

  // Filtered Approved Mentors
  const filteredApprovedMentors = useMemo(() => {
    if (!searchQuery.trim()) return approvedMentors;
    const q = searchQuery.toLowerCase();
    return approvedMentors.filter(
      (m) =>
        m.name?.toLowerCase().includes(q) ||
        m.email?.toLowerCase().includes(q) ||
        m.company?.toLowerCase().includes(q) ||
        m.jobTitle?.toLowerCase().includes(q) ||
        m.expertiseAreas?.toLowerCase().includes(q)
    );
  }, [approvedMentors, searchQuery]);

  // Filtered Suspended Mentors
  const filteredSuspendedMentors = useMemo(() => {
    if (!searchQuery.trim()) return suspendedMentors;
    const q = searchQuery.toLowerCase();
    return suspendedMentors.filter(
      (m) =>
        m.name?.toLowerCase().includes(q) ||
        m.email?.toLowerCase().includes(q) ||
        m.company?.toLowerCase().includes(q) ||
        m.jobTitle?.toLowerCase().includes(q) ||
        m.expertiseAreas?.toLowerCase().includes(q)
    );
  }, [suspendedMentors, searchQuery]);

  // Render Access Denied / Auth Checking Spinner
  if (isAuthLoading || (user && user.role !== "ADMIN")) {
    return (
      <div className="min-h-[50vh] flex flex-col items-center justify-center text-gray-500 gap-3">
        <div className="w-8 h-8 border-4 border-black border-t-transparent rounded-full animate-spin"></div>
        <p className="text-sm font-medium">Verifying administrator authorization...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8 pb-12">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-gray-900 via-purple-950 to-gray-900 text-white rounded-3xl p-6 sm:p-8 shadow-xl relative overflow-hidden">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-xs font-semibold text-purple-300 mb-1">
              <Link href="/admin" className="hover:underline transition">
                Admin Dashboard
              </Link>
              <span>›</span>
              <span>Mentor Management</span>
            </div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-500/20 border border-purple-400/30 text-purple-300 text-xs font-bold uppercase tracking-wider">
              <span>🛡️ Platform Governance</span>
              <span>•</span>
              <span>Phase 3B.5 Mentorship Review</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
              Mentor Management
            </h1>
            <p className="text-gray-300 text-sm max-w-2xl">
              Review mentor applications, manage verified mentors, and configure mentorship capacity.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Link
              href="/mentorship"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white/10 text-white font-semibold text-sm hover:bg-white/20 border border-white/20 transition cursor-pointer"
            >
              <span>View Hub</span>
              <span>👥</span>
            </Link>
            <button
              onClick={loadAdminData}
              disabled={isLoadingData}
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white text-gray-900 font-bold text-sm hover:bg-gray-100 shadow-md transition cursor-pointer disabled:opacity-50"
            >
              <span>Refresh</span>
              <span className={isLoadingData ? "animate-spin" : ""}>🔄</span>
            </button>
          </div>
        </div>
      </div>

      {/* Recoverable Loading Error Alert */}
      {loadError && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-800 text-sm flex items-center justify-between gap-3 shadow-xs">
          <div className="flex items-center gap-2">
            <span>⚠️</span>
            <p className="font-medium">{loadError}</p>
          </div>
          <button
            onClick={loadAdminData}
            className="px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-lg transition cursor-pointer"
          >
            Retry
          </button>
        </div>
      )}

      {/* Action Feedback Banner */}
      {feedback && (
        <div
          className={`p-4 rounded-2xl border text-sm flex items-start justify-between gap-3 shadow-xs ${
            feedback.type === "success"
              ? "bg-emerald-50 border-emerald-200 text-emerald-800"
              : feedback.type === "warning"
              ? "bg-amber-50 border-amber-200 text-amber-800"
              : "bg-red-50 border-red-200 text-red-800"
          }`}
        >
          <div className="flex items-start gap-2.5">
            <span className="text-base mt-0.5">
              {feedback.type === "success"
                ? "✅"
                : feedback.type === "warning"
                ? "⚠️"
                : "❌"}
            </span>
            <p className="font-medium">{feedback.message}</p>
          </div>
          <button
            onClick={() => setFeedback(null)}
            className="text-gray-400 hover:text-gray-700 font-bold text-base leading-none cursor-pointer"
            aria-label="Dismiss feedback"
          >
            ✕
          </button>
        </div>
      )}

      {/* KPI Stats Strip */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Pending Approvals */}
        <div
          onClick={() => setActiveTab("pending")}
          className={`p-5 rounded-2xl border shadow-xs transition cursor-pointer ${
            activeTab === "pending"
              ? "bg-amber-500/10 border-amber-300 ring-2 ring-amber-400/40"
              : "bg-white border-gray-200 hover:border-gray-300"
          }`}
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold uppercase tracking-wider text-gray-500">
              Pending Approvals
            </span>
            <span className="w-8 h-8 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center font-bold text-sm">
              ⏳
            </span>
          </div>
          <p className="text-2xl font-extrabold text-gray-900">
            {isLoadingData ? "..." : pendingApplications.length}
          </p>
          <p className="text-xs text-gray-500 mt-1">
            {pendingApplications.length === 1 ? "1 application waiting" : `${pendingApplications.length} applications waiting`}
          </p>
        </div>

        {/* Approved Mentors */}
        <div
          onClick={() => setActiveTab("approved")}
          className={`p-5 rounded-2xl border shadow-xs transition cursor-pointer ${
            activeTab === "approved"
              ? "bg-emerald-500/10 border-emerald-300 ring-2 ring-emerald-400/40"
              : "bg-white border-gray-200 hover:border-gray-300"
          }`}
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold uppercase tracking-wider text-gray-500">
              Active Mentors
            </span>
            <span className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold text-sm">
              🌟
            </span>
          </div>
          <p className="text-2xl font-extrabold text-gray-900">
            {isLoadingData ? "..." : approvedMentors.length}
          </p>
          <p className="text-xs text-gray-500 mt-1">
            {approvedMentors.reduce((acc, m) => acc + (m.activeMenteesCount || 0), 0)} active mentees served
          </p>
        </div>

        {/* Suspended Mentors */}
        <div
          onClick={() => setActiveTab("suspended")}
          className={`p-5 rounded-2xl border shadow-xs transition cursor-pointer ${
            activeTab === "suspended"
              ? "bg-red-500/10 border-red-300 ring-2 ring-red-400/40"
              : "bg-white border-gray-200 hover:border-gray-300"
          }`}
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold uppercase tracking-wider text-gray-500">
              Suspended Mentors
            </span>
            <span className="w-8 h-8 rounded-lg bg-red-50 text-red-600 flex items-center justify-center font-bold text-sm">
              🚫
            </span>
          </div>
          <p className="text-2xl font-extrabold text-gray-900">
            {isLoadingData ? "..." : suspendedMentors.length}
          </p>
          <p className="text-xs text-gray-500 mt-1">Access revoked / on hold</p>
        </div>

        {/* Capacity Settings */}
        <div
          onClick={() => setActiveTab("config")}
          className={`p-5 rounded-2xl border shadow-xs transition cursor-pointer ${
            activeTab === "config"
              ? "bg-purple-500/10 border-purple-300 ring-2 ring-purple-400/40"
              : "bg-white border-gray-200 hover:border-gray-300"
          }`}
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold uppercase tracking-wider text-gray-500">
              Capacity Limits
            </span>
            <span className="w-8 h-8 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center font-bold text-sm">
              ⚙️
            </span>
          </div>
          <p className="text-2xl font-extrabold text-gray-900">
            {capacityConfig.defaultMentees} <span className="text-sm font-normal text-gray-500">def</span>
          </p>
          <p className="text-xs text-gray-500 mt-1">
            Range: {capacityConfig.minMentees} - {capacityConfig.maxMentees} mentees
          </p>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="flex border-b border-gray-200 gap-2 sm:gap-4 overflow-x-auto" role="tablist">
        <button
          role="tab"
          aria-selected={activeTab === "pending"}
          onClick={() => setActiveTab("pending")}
          className={`pb-3 px-3 text-sm font-bold flex items-center gap-2 border-b-2 transition whitespace-nowrap cursor-pointer ${
            activeTab === "pending"
              ? "border-purple-600 text-purple-700"
              : "border-transparent text-gray-500 hover:text-gray-900"
          }`}
        >
          <span>Pending Applications</span>
          <span
            className={`px-2 py-0.5 rounded-full text-xs font-extrabold ${
              pendingApplications.length > 0
                ? "bg-amber-100 text-amber-800"
                : "bg-gray-100 text-gray-600"
            }`}
          >
            {pendingApplications.length}
          </span>
        </button>

        <button
          role="tab"
          aria-selected={activeTab === "approved"}
          onClick={() => setActiveTab("approved")}
          className={`pb-3 px-3 text-sm font-bold flex items-center gap-2 border-b-2 transition whitespace-nowrap cursor-pointer ${
            activeTab === "approved"
              ? "border-purple-600 text-purple-700"
              : "border-transparent text-gray-500 hover:text-gray-900"
          }`}
        >
          <span>Approved Mentors</span>
          <span className="px-2 py-0.5 rounded-full text-xs font-extrabold bg-emerald-100 text-emerald-800">
            {approvedMentors.length}
          </span>
        </button>

        <button
          role="tab"
          aria-selected={activeTab === "suspended"}
          onClick={() => setActiveTab("suspended")}
          className={`pb-3 px-3 text-sm font-bold flex items-center gap-2 border-b-2 transition whitespace-nowrap cursor-pointer ${
            activeTab === "suspended"
              ? "border-purple-600 text-purple-700"
              : "border-transparent text-gray-500 hover:text-gray-900"
          }`}
        >
          <span>Suspended Mentors</span>
          <span className="px-2 py-0.5 rounded-full text-xs font-extrabold bg-red-100 text-red-800">
            {suspendedMentors.length}
          </span>
        </button>

        <button
          role="tab"
          aria-selected={activeTab === "config"}
          onClick={() => setActiveTab("config")}
          className={`pb-3 px-3 text-sm font-bold flex items-center gap-2 border-b-2 transition whitespace-nowrap cursor-pointer ${
            activeTab === "config"
              ? "border-purple-600 text-purple-700"
              : "border-transparent text-gray-500 hover:text-gray-900"
          }`}
        >
          <span>⚙️ Capacity Configuration</span>
        </button>
      </div>

      {/* ======================================================== */}
      {/* SECTION A: PENDING APPLICATIONS                         */}
      {/* ======================================================== */}
      {activeTab === "pending" && (
        <section aria-labelledby="pending-heading" className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h2 id="pending-heading" className="text-xl font-bold text-gray-900">
                Pending Mentor Applications ({pendingApplications.length})
              </h2>
              <p className="text-xs text-gray-500">
                Evaluate candidates applying to become verified mentors on OneStop Career Advisor.
              </p>
            </div>
          </div>

          {isLoadingData ? (
            <div className="p-12 text-center bg-white rounded-2xl border border-gray-200">
              <div className="w-8 h-8 border-4 border-purple-600 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
              <p className="text-sm text-gray-500">Loading pending applications...</p>
            </div>
          ) : pendingApplications.length === 0 ? (
            <div className="p-12 text-center bg-white rounded-2xl border border-gray-200 space-y-3">
              <span className="text-4xl">🎉</span>
              <h3 className="text-base font-bold text-gray-900">No Pending Applications</h3>
              <p className="text-xs text-gray-500 max-w-md mx-auto">
                All submitted mentor applications have been reviewed. When new industry professionals apply, their profiles will appear here for governance.
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4">
              {pendingApplications.map((app) => (
                <div
                  key={app.id}
                  className="bg-white rounded-2xl border border-gray-200 p-6 shadow-xs hover:border-gray-300 transition flex flex-col lg:flex-row lg:items-center justify-between gap-6"
                >
                  {/* Left Column: Applicant Bio & Credentials */}
                  <div className="space-y-3 flex-1 min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="text-lg font-bold text-gray-900">{app.name}</h3>
                      <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-100 text-amber-800 border border-amber-200">
                        {app.verificationStatus}
                      </span>
                      <span className="text-xs text-gray-400">
                        Applied {new Date(app.createdAt).toLocaleDateString()}
                      </span>
                    </div>

                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-600">
                      <span className="font-semibold text-gray-900">
                        {app.jobTitle} at {app.company}
                      </span>
                      <span>•</span>
                      <span>{app.yearsOfExperience} yrs experience</span>
                      <span>•</span>
                      <span>Requested Capacity: {app.maxMentees} mentees</span>
                      <span>•</span>
                      <span className="text-gray-500">{app.email}</span>
                    </div>

                    {app.bio && (
                      <p className="text-xs text-gray-600 line-clamp-2 italic">
                        &quot;{app.bio}&quot;
                      </p>
                    )}

                    {/* Expertise Tags */}
                    {app.expertiseAreas && (
                      <div className="flex flex-wrap gap-1.5 pt-1">
                        {app.expertiseAreas
                          .split(",")
                          .map((tag) => tag.trim())
                          .filter(Boolean)
                          .map((tag, idx) => (
                            <span
                              key={idx}
                              className="px-2 py-0.5 rounded-md bg-purple-50 text-purple-700 text-[11px] font-medium border border-purple-100"
                            >
                              {tag}
                            </span>
                          ))}
                      </div>
                    )}

                    {/* External links preview safely rendered */}
                    <div className="flex flex-wrap items-center gap-3 pt-1 text-xs font-semibold">
                      {app.linkedinUrl && (
                        <a
                          href={app.linkedinUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-blue-600 hover:underline flex items-center gap-1"
                        >
                          <span>LinkedIn</span>
                          <span>↗</span>
                        </a>
                      )}
                      {app.githubUrl && (
                        <a
                          href={app.githubUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-gray-800 hover:underline flex items-center gap-1"
                        >
                          <span>GitHub</span>
                          <span>↗</span>
                        </a>
                      )}
                      {app.portfolioUrl && (
                        <a
                          href={app.portfolioUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-emerald-700 hover:underline flex items-center gap-1"
                        >
                          <span>Portfolio</span>
                          <span>↗</span>
                        </a>
                      )}
                    </div>
                  </div>

                  {/* Right Column: Actions */}
                  <div className="flex sm:flex-row lg:flex-col items-stretch gap-2 min-w-[170px]">
                    <button
                      onClick={() => setInspectModalProfile(app)}
                      className="px-4 py-2 rounded-xl border border-gray-300 text-gray-700 hover:bg-gray-50 font-bold text-xs transition cursor-pointer text-center"
                    >
                      Review Application
                    </button>
                    <button
                      onClick={() => handleApproveApplication(app.id)}
                      disabled={actionLoadingId === app.id}
                      className="px-4 py-2 rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 font-bold text-xs shadow-xs transition cursor-pointer disabled:opacity-50 text-center flex items-center justify-center gap-1.5"
                    >
                      {actionLoadingId === app.id ? (
                        <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      ) : (
                        <span>✓</span>
                      )}
                      <span>Approve</span>
                    </button>
                    <button
                      onClick={() => {
                        setRejectModalProfile(app);
                        setRejectReason("");
                      }}
                      disabled={actionLoadingId === app.id}
                      className="px-4 py-2 rounded-xl bg-red-50 text-red-700 hover:bg-red-100 border border-red-200 font-bold text-xs transition cursor-pointer text-center"
                    >
                      Reject with Note
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {/* ======================================================== */}
      {/* SECTION B: APPROVED MENTORS                             */}
      {/* ======================================================== */}
      {activeTab === "approved" && (
        <section aria-labelledby="approved-heading" className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h2 id="approved-heading" className="text-xl font-bold text-gray-900">
                Verified Mentors ({approvedMentors.length})
              </h2>
              <p className="text-xs text-gray-500">
                Active platform mentors currently matching and conducting sessions with candidates.
              </p>
            </div>

            {/* Search Filter */}
            <div className="w-full sm:w-72">
              <input
                type="text"
                placeholder="Search mentors by name, company, skill..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full px-3.5 py-2 text-xs rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
            </div>
          </div>

          {isLoadingData ? (
            <div className="p-12 text-center bg-white rounded-2xl border border-gray-200">
              <div className="w-8 h-8 border-4 border-purple-600 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
              <p className="text-sm text-gray-500">Loading approved mentors...</p>
            </div>
          ) : filteredApprovedMentors.length === 0 ? (
            <div className="p-12 text-center bg-white rounded-2xl border border-gray-200 space-y-2">
              <p className="text-base font-bold text-gray-700">No approved mentors found</p>
              <p className="text-xs text-gray-500">
                {searchQuery
                  ? "No verified mentors match your search query."
                  : "No mentors are currently approved. Review pending applications to approve candidates."}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {filteredApprovedMentors.map((mentor) => (
                <div
                  key={mentor.id}
                  className="bg-white rounded-2xl border border-gray-200 p-5 shadow-xs hover:border-gray-300 transition flex flex-col justify-between gap-4"
                >
                  <div className="space-y-3">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="font-bold text-gray-900 text-base">{mentor.name}</h3>
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-100 text-emerald-800">
                            {mentor.verificationStatus}
                          </span>
                        </div>
                        <p className="text-xs text-gray-600 font-medium">
                          {mentor.jobTitle} at {mentor.company}
                        </p>
                        <p className="text-[11px] text-gray-400">{mentor.email} • {mentor.yearsOfExperience} yrs exp</p>
                      </div>

                      <div className="text-right">
                        <span
                          className={`inline-block px-2.5 py-1 rounded-lg text-xs font-extrabold ${
                            mentor.availableSlots > 0
                              ? "bg-blue-50 text-blue-700 border border-blue-200"
                              : "bg-gray-100 text-gray-700 border border-gray-200"
                          }`}
                        >
                          {mentor.activeMenteesCount} / {mentor.maxMentees} Mentees
                        </span>
                        <p className="text-[10px] text-gray-400 mt-1">
                          {mentor.availableSlots} slots free
                        </p>
                      </div>
                    </div>

                    {mentor.bio && (
                      <p className="text-xs text-gray-600 line-clamp-2">
                        {mentor.bio}
                      </p>
                    )}

                    {mentor.expertiseAreas && (
                      <div className="flex flex-wrap gap-1">
                        {mentor.expertiseAreas
                          .split(",")
                          .map((s) => s.trim())
                          .filter(Boolean)
                          .map((tag, idx) => (
                            <span
                              key={idx}
                              className="px-2 py-0.5 bg-gray-100 text-gray-700 text-[10px] rounded-md font-medium"
                            >
                              {tag}
                            </span>
                          ))}
                      </div>
                    )}
                  </div>

                  {/* Actions Strip */}
                  <div className="pt-3 border-t border-gray-100 flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setInspectModalProfile(mentor)}
                        className="px-3 py-1.5 rounded-lg text-xs font-bold text-gray-700 hover:bg-gray-100 border border-gray-200 transition cursor-pointer"
                      >
                        Inspect Dossier
                      </button>
                      <Link
                        href={`/mentorship/${mentor.id}`}
                        className="text-xs font-bold text-purple-700 hover:text-purple-900 hover:underline"
                      >
                        Public Profile ↗
                      </Link>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => {
                          setSuspendModalProfile(mentor);
                          setSuspendReason("");
                        }}
                        className="px-3 py-1.5 rounded-lg text-xs font-bold text-red-600 hover:bg-red-50 border border-red-200 transition cursor-pointer"
                      >
                        Suspend Mentor
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {/* ======================================================== */}
      {/* SECTION C: SUSPENDED MENTORS                            */}
      {/* ======================================================== */}
      {activeTab === "suspended" && (
        <section aria-labelledby="suspended-heading" className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h2 id="suspended-heading" className="text-xl font-bold text-gray-900">
                Suspended Mentors ({suspendedMentors.length})
              </h2>
              <p className="text-xs text-gray-500">
                Mentors whose platform credentials and public discovery have been suspended. Note: Reactivation is not supported by backend APIs without re-application or manual database adjustment.
              </p>
            </div>

            {/* Search Filter */}
            <div className="w-full sm:w-72">
              <input
                type="text"
                placeholder="Search suspended mentors..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full px-3.5 py-2 text-xs rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
            </div>
          </div>

          {isLoadingData ? (
            <div className="p-12 text-center bg-white rounded-2xl border border-gray-200">
              <div className="w-8 h-8 border-4 border-purple-600 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
              <p className="text-sm text-gray-500">Loading suspended mentors...</p>
            </div>
          ) : filteredSuspendedMentors.length === 0 ? (
            <div className="p-12 text-center bg-white rounded-2xl border border-gray-200 space-y-2">
              <span className="text-3xl">🛡️</span>
              <p className="text-base font-bold text-gray-700">No Suspended Mentors</p>
              <p className="text-xs text-gray-500">
                There are currently no suspended mentor accounts on the platform.
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4">
              {filteredSuspendedMentors.map((mentor) => (
                <div
                  key={mentor.id}
                  className="bg-white rounded-2xl border border-red-200 p-5 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4"
                >
                  <div className="space-y-2 flex-1">
                    <div className="flex items-center gap-2">
                      <h3 className="font-bold text-gray-900 text-base">{mentor.name}</h3>
                      <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-red-100 text-red-800 border border-red-200">
                        {mentor.verificationStatus}
                      </span>
                      <span className="text-xs text-gray-400">
                        Updated {new Date(mentor.updatedAt).toLocaleDateString()}
                      </span>
                    </div>

                    <div className="text-xs text-gray-600">
                      <span className="font-semibold text-gray-900">
                        {mentor.jobTitle} at {mentor.company}
                      </span>
                      <span> • {mentor.email}</span>
                    </div>

                    {mentor.rejectionReason && (
                      <div className="p-3 rounded-xl bg-red-50/70 border border-red-100 text-xs text-red-800">
                        <span className="font-bold">Suspension Reason: </span>
                        <span>{mentor.rejectionReason}</span>
                      </div>
                    )}
                  </div>

                  <div className="flex flex-col sm:flex-row items-start sm:items-center gap-2">
                    <button
                      onClick={() => setInspectModalProfile(mentor)}
                      className="px-4 py-2 rounded-xl border border-gray-300 text-gray-700 hover:bg-gray-50 font-bold text-xs transition cursor-pointer"
                    >
                      Inspect Dossier
                    </button>
                    <span className="text-[11px] text-gray-500 italic bg-gray-50 border border-gray-200 px-3 py-1.5 rounded-xl">
                      Reactivation unsupported
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {/* ======================================================== */}
      {/* SECTION D: CAPACITY CONFIGURATION                       */}
      {/* ======================================================== */}
      {activeTab === "config" && (
        <section aria-labelledby="capacity-heading" className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200 shadow-xs space-y-6">
          <div className="border-b border-gray-100 pb-4">
            <h2 id="capacity-heading" className="text-xl font-bold text-gray-900">
              Global Mentorship Capacity Configuration
            </h2>
            <p className="text-xs text-gray-500 mt-1">
              Define the boundary constraints for concurrent mentee capacities across the Career Advisor ecosystem.
            </p>
          </div>

          {configSuccessMsg && (
            <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold flex items-center justify-between">
              <span>✅ {configSuccessMsg}</span>
              <button
                onClick={() => setConfigSuccessMsg(null)}
                className="text-emerald-700 hover:text-emerald-900 font-bold cursor-pointer"
              >
                ✕
              </button>
            </div>
          )}

          <form onSubmit={handleSaveCapacityConfig} className="space-y-6 max-w-2xl">
            {/* Visual constraint rule banner */}
            <div className="p-4 rounded-2xl bg-purple-50 border border-purple-200 text-purple-900 text-xs space-y-1">
              <p className="font-bold">System Invariant:</p>
              <p>
                Platform capacity requires: <code className="bg-purple-100 px-1.5 py-0.5 rounded font-mono font-bold">1 &le; Minimum &le; Default &le; Maximum</code>
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
              {/* Minimum Mentees */}
              <div className="space-y-2">
                <label htmlFor="minMenteesInput" className="block text-xs font-bold text-gray-700">
                  Minimum Capacity <span className="text-red-500">*</span>
                </label>
                <input
                  id="minMenteesInput"
                  type="number"
                  min="1"
                  max="100"
                  value={capacityForm.minMentees}
                  onChange={(e) =>
                    setCapacityForm({
                      ...capacityForm,
                      minMentees: parseInt(e.target.value, 10) || 1,
                    })
                  }
                  required
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 font-semibold"
                />
                <p className="text-[11px] text-gray-500">
                  Floor for any mentor profile (minimum &ge; 1).
                </p>
              </div>

              {/* Default Mentees */}
              <div className="space-y-2">
                <label htmlFor="defaultMenteesInput" className="block text-xs font-bold text-gray-700">
                  Default Capacity <span className="text-red-500">*</span>
                </label>
                <input
                  id="defaultMenteesInput"
                  type="number"
                  min="1"
                  max="100"
                  value={capacityForm.defaultMentees}
                  onChange={(e) =>
                    setCapacityForm({
                      ...capacityForm,
                      defaultMentees: parseInt(e.target.value, 10) || 1,
                    })
                  }
                  required
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 font-semibold"
                />
                <p className="text-[11px] text-gray-500">
                  Assigned when applicant omits preference.
                </p>
              </div>

              {/* Maximum Mentees */}
              <div className="space-y-2">
                <label htmlFor="maxMenteesInput" className="block text-xs font-bold text-gray-700">
                  Maximum Ceiling <span className="text-red-500">*</span>
                </label>
                <input
                  id="maxMenteesInput"
                  type="number"
                  min="1"
                  max="100"
                  value={capacityForm.maxMentees}
                  onChange={(e) =>
                    setCapacityForm({
                      ...capacityForm,
                      maxMentees: parseInt(e.target.value, 10) || 1,
                    })
                  }
                  required
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 font-semibold"
                />
                <p className="text-[11px] text-gray-500">
                  Hard upper limit per individual mentor.
                </p>
              </div>
            </div>

            {/* In-form Validation Warnings */}
            {capacityValidationErrors.length > 0 && (
              <div className="p-3 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs space-y-1">
                {capacityValidationErrors.map((err, idx) => (
                  <p key={idx}>⚠️ {err}</p>
                ))}
              </div>
            )}

            <div className="pt-4 border-t border-gray-100 flex items-center justify-between">
              <button
                type="button"
                onClick={() => setCapacityForm(capacityConfig)}
                className="px-4 py-2.5 rounded-xl border border-gray-300 text-gray-700 font-semibold text-xs hover:bg-gray-50 transition cursor-pointer"
              >
                Reset to Current
              </button>

              <button
                type="submit"
                disabled={isSavingConfig || capacityValidationErrors.length > 0}
                className="px-6 py-2.5 rounded-xl bg-purple-700 text-white font-bold text-xs hover:bg-purple-800 shadow-md transition cursor-pointer disabled:opacity-50 flex items-center gap-2"
              >
                {isSavingConfig ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Saving Configuration...</span>
                  </>
                ) : (
                  <span>Save Capacity Limits</span>
                )}
              </button>
            </div>
          </form>
        </section>
      )}

      {/* ======================================================== */}
      {/* MODAL: INSPECT FULL APPLICATION / DOSSIER               */}
      {/* ======================================================== */}
      {inspectModalProfile && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="inspect-dialog-title"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs"
        >
          <div className="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] overflow-y-auto p-6 sm:p-8 space-y-6 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-start justify-between border-b border-gray-100 pb-4">
              <div>
                <span className="text-xs font-bold uppercase tracking-wider text-purple-600">
                  Mentor Profile Dossier
                </span>
                <h3 id="inspect-dialog-title" className="text-xl font-extrabold text-gray-900 mt-1">
                  {inspectModalProfile.name}
                </h3>
                <p className="text-xs text-gray-500">
                  Applied on {new Date(inspectModalProfile.createdAt).toLocaleString()} • Status: <strong className="text-gray-900">{inspectModalProfile.verificationStatus}</strong>
                </p>
              </div>
              <button
                onClick={() => setInspectModalProfile(null)}
                className="text-gray-400 hover:text-gray-600 font-bold text-lg p-1 cursor-pointer"
                aria-label="Close dossier modal"
              >
                ✕
              </button>
            </div>

            <div className="space-y-4 text-xs">
              <div className="grid grid-cols-2 gap-4 p-4 rounded-2xl bg-gray-50 border border-gray-100">
                <div>
                  <span className="text-gray-400 block font-medium">Job Title & Company</span>
                  <span className="font-bold text-gray-900 text-sm">
                    {inspectModalProfile.jobTitle} at {inspectModalProfile.company}
                  </span>
                </div>
                <div>
                  <span className="text-gray-400 block font-medium">Industry Experience</span>
                  <span className="font-bold text-gray-900 text-sm">
                    {inspectModalProfile.yearsOfExperience} Years
                  </span>
                </div>
                <div>
                  <span className="text-gray-400 block font-medium">Email Address</span>
                  <span className="font-bold text-gray-900">{inspectModalProfile.email}</span>
                </div>
                <div>
                  <span className="text-gray-400 block font-medium">Capacity (Active / Max)</span>
                  <span className="font-bold text-gray-900">
                    {inspectModalProfile.activeMenteesCount} / {inspectModalProfile.maxMentees} mentees ({inspectModalProfile.availableSlots} available)
                  </span>
                </div>
              </div>

              {/* Bio */}
              <div>
                <h4 className="font-bold text-gray-900 mb-1">Professional Bio & Motivation</h4>
                <div className="p-4 rounded-xl bg-gray-50 border border-gray-100 text-gray-700 leading-relaxed whitespace-pre-wrap">
                  {inspectModalProfile.bio || "No biographical statement provided."}
                </div>
              </div>

              {/* Technical Expertise */}
              <div>
                <h4 className="font-bold text-gray-900 mb-1">Declared Expertise Areas</h4>
                <div className="flex flex-wrap gap-1.5">
                  {inspectModalProfile.expertiseAreas ? (
                    inspectModalProfile.expertiseAreas
                      .split(",")
                      .map((s) => s.trim())
                      .filter(Boolean)
                      .map((tag, idx) => (
                        <span
                          key={idx}
                          className="px-2.5 py-1 rounded-lg bg-purple-50 text-purple-700 font-semibold border border-purple-100"
                        >
                          {tag}
                        </span>
                      ))
                  ) : (
                    <span className="text-gray-400 italic">No expertise areas listed.</span>
                  )}
                </div>
              </div>

              {/* External Links */}
              <div>
                <h4 className="font-bold text-gray-900 mb-1">External Portfolios & Profiles</h4>
                <div className="space-y-1.5">
                  {inspectModalProfile.linkedinUrl && (
                    <a
                      href={inspectModalProfile.linkedinUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="block text-blue-600 hover:underline font-semibold"
                    >
                      🔗 LinkedIn: {inspectModalProfile.linkedinUrl}
                    </a>
                  )}
                  {inspectModalProfile.githubUrl && (
                    <a
                      href={inspectModalProfile.githubUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="block text-gray-800 hover:underline font-semibold"
                    >
                      💻 GitHub: {inspectModalProfile.githubUrl}
                    </a>
                  )}
                  {inspectModalProfile.portfolioUrl && (
                    <a
                      href={inspectModalProfile.portfolioUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="block text-emerald-700 hover:underline font-semibold"
                    >
                      🌐 Portfolio: {inspectModalProfile.portfolioUrl}
                    </a>
                  )}
                  {!inspectModalProfile.linkedinUrl &&
                    !inspectModalProfile.githubUrl &&
                    !inspectModalProfile.portfolioUrl && (
                      <p className="text-gray-400 italic">No external links provided.</p>
                    )}
                </div>
              </div>

              {/* Suspension details if suspended */}
              {inspectModalProfile.verificationStatus === "SUSPENDED" && inspectModalProfile.rejectionReason && (
                <div className="p-3.5 rounded-xl bg-red-50 border border-red-200 text-red-800 space-y-1">
                  <p className="font-bold">Suspension Audit Note:</p>
                  <p>{inspectModalProfile.rejectionReason}</p>
                </div>
              )}
            </div>

            {/* Modal Actions */}
            <div className="pt-4 border-t border-gray-100 flex items-center justify-between gap-3">
              <button
                onClick={() => setInspectModalProfile(null)}
                className="px-4 py-2.5 rounded-xl border border-gray-300 text-gray-700 font-bold text-xs hover:bg-gray-50 transition cursor-pointer"
              >
                Close
              </button>

              <div className="flex items-center gap-2">
                {inspectModalProfile.verificationStatus === "APPLIED" && (
                  <>
                    <button
                      onClick={() => {
                        setRejectModalProfile(inspectModalProfile);
                        setRejectReason("");
                      }}
                      className="px-4 py-2.5 rounded-xl bg-red-50 text-red-700 hover:bg-red-100 border border-red-200 font-bold text-xs transition cursor-pointer"
                    >
                      Reject...
                    </button>
                    <button
                      onClick={() => handleApproveApplication(inspectModalProfile.id)}
                      disabled={actionLoadingId === inspectModalProfile.id}
                      className="px-5 py-2.5 rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 font-bold text-xs shadow-md transition cursor-pointer disabled:opacity-50 flex items-center gap-1.5"
                    >
                      {actionLoadingId === inspectModalProfile.id ? (
                        <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      ) : (
                        <span>✓</span>
                      )}
                      <span>Approve Application</span>
                    </button>
                  </>
                )}

                {inspectModalProfile.verificationStatus === "APPROVED" && (
                  <button
                    onClick={() => {
                      setSuspendModalProfile(inspectModalProfile);
                      setSuspendReason("");
                    }}
                    className="px-4 py-2.5 rounded-xl bg-red-600 text-white hover:bg-red-700 font-bold text-xs shadow-md transition cursor-pointer"
                  >
                    Suspend Mentor...
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ======================================================== */}
      {/* MODAL: REJECT APPLICATION WITH REASON                   */}
      {/* ======================================================== */}
      {rejectModalProfile && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="reject-dialog-title"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs"
        >
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-5 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-start justify-between border-b border-gray-100 pb-3">
              <div>
                <h3 id="reject-dialog-title" className="text-lg font-extrabold text-gray-900">
                  Reject Mentor Application
                </h3>
                <p className="text-xs text-gray-500 mt-0.5">
                  Candidate: {rejectModalProfile.name} ({rejectModalProfile.jobTitle} at {rejectModalProfile.company})
                </p>
              </div>
              <button
                onClick={() => setRejectModalProfile(null)}
                className="text-gray-400 hover:text-gray-600 font-bold text-base cursor-pointer"
                aria-label="Close reject modal"
              >
                ✕
              </button>
            </div>

            <div className="p-3.5 rounded-2xl bg-red-50 border border-red-200 text-red-900 text-xs space-y-1">
              <p className="font-bold flex items-center gap-1.5">
                <span>ℹ️</span>
                <span>Application Rejection</span>
              </p>
              <p className="text-red-800">
                The application will be set to REJECTED with your constructive feedback note. The record remains preserved for administrative auditing.
              </p>
            </div>

            <div className="space-y-3">
              <label htmlFor="rejectReasonTextarea" className="block text-xs font-bold text-gray-700">
                Reason / Constructive Feedback (Optional)
              </label>
              <textarea
                id="rejectReasonTextarea"
                rows={4}
                maxLength={1000}
                placeholder="e.g. Additional industry experience or portfolio verification required..."
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                className="w-full px-3 py-2.5 text-xs rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-red-500 resize-none"
              />
              <div className="flex justify-between text-[11px] text-gray-400">
                <span>Stored for candidate feedback</span>
                <span>{rejectReason.length}/1000</span>
              </div>
            </div>

            <div className="pt-3 border-t border-gray-100 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setRejectModalProfile(null)}
                className="px-4 py-2 rounded-xl border border-gray-300 text-gray-700 font-bold text-xs hover:bg-gray-50 transition cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmReject}
                disabled={isSubmittingReject}
                className="px-4 py-2 rounded-xl bg-red-600 text-white font-bold text-xs hover:bg-red-700 shadow-md transition cursor-pointer disabled:opacity-50 flex items-center gap-1.5"
              >
                {isSubmittingReject ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Rejecting...</span>
                  </>
                ) : (
                  <span>Confirm Rejection</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ======================================================== */}
      {/* MODAL: SUSPEND MENTOR WITH CONSEQUENCE WARNING          */}
      {/* ======================================================== */}
      {suspendModalProfile && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="suspend-dialog-title"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs"
        >
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-5 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-start justify-between border-b border-gray-100 pb-3">
              <div>
                <h3 id="suspend-dialog-title" className="text-lg font-extrabold text-red-700">
                  Suspend Mentor Profile
                </h3>
                <p className="text-xs text-gray-500 mt-0.5">
                  Mentor: {suspendModalProfile.name}
                </p>
              </div>
              <button
                onClick={() => setSuspendModalProfile(null)}
                className="text-gray-400 hover:text-gray-600 font-bold text-base cursor-pointer"
                aria-label="Close suspend modal"
              >
                ✕
              </button>
            </div>

            <div className="p-3.5 rounded-2xl bg-amber-50 border border-amber-200 text-amber-900 text-xs space-y-1.5">
              <p className="font-bold flex items-center gap-1.5">
                <span>⚠️</span>
                <span>Consequences of Suspension:</span>
              </p>
              <ul className="list-disc pl-4 space-y-1 text-amber-800">
                <li>The mentor is immediately removed from public discovery in the Mentorship Hub.</li>
                <li>Students cannot submit new mentorship requests to this mentor.</li>
                <li>Existing accepted mentorships, chat conversations, and message history remain preserved.</li>
                <li>Reactivation is not supported by backend APIs without administrative database intervention.</li>
              </ul>
            </div>

            <div className="space-y-3">
              <label htmlFor="suspendReasonTextarea" className="block text-xs font-bold text-gray-700">
                Reason for Suspension (Optional)
              </label>
              <textarea
                id="suspendReasonTextarea"
                rows={4}
                maxLength={1000}
                placeholder="e.g. Inactivity, unfulfilled mentorship commitments, or policy violation..."
                value={suspendReason}
                onChange={(e) => setSuspendReason(e.target.value)}
                className="w-full px-3 py-2.5 text-xs rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-red-500 resize-none"
              />
              <div className="flex justify-between text-[11px] text-gray-400">
                <span>Stored in audit record</span>
                <span>{suspendReason.length}/1000</span>
              </div>
            </div>

            <div className="pt-3 border-t border-gray-100 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setSuspendModalProfile(null)}
                className="px-4 py-2 rounded-xl border border-gray-300 text-gray-700 font-bold text-xs hover:bg-gray-50 transition cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmSuspend}
                disabled={isSubmittingSuspend}
                className="px-4 py-2 rounded-xl bg-red-600 text-white font-bold text-xs hover:bg-red-700 shadow-md transition cursor-pointer disabled:opacity-50 flex items-center gap-1.5"
              >
                {isSubmittingSuspend ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Suspending...</span>
                  </>
                ) : (
                  <span>Confirm Suspension</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
