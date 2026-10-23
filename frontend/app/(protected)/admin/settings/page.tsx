"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import {
  getPlatformSettings,
  updatePlatformSettings,
  PlatformSettings,
  MentorshipCapacityConfig,
} from "@/lib/adminSettingsService";

type SettingsTab = "mentorship" | "platform" | "security" | "system";

export default function AdminSettingsPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();

  // Active Tab
  const [activeTab, setActiveTab] = useState<SettingsTab>("mentorship");

  // Settings State
  const [settings, setSettings] = useState<PlatformSettings | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Form State (Mentorship Capacity)
  const [minMentees, setMinMentees] = useState<number>(1);
  const [maxMentees, setMaxMentees] = useState<number>(10);
  const [defaultMentees, setDefaultMentees] = useState<number>(3);

  // Action States
  const [isSaving, setIsSaving] = useState(false);
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [showConfirmModal, setShowConfirmModal] = useState(false);

  // Route guard
  useEffect(() => {
    if (!isAuthLoading) {
      if (!isAuthenticated) {
        router.replace("/login");
      } else if (user?.role !== "ADMIN") {
        router.replace("/dashboard");
      }
    }
  }, [isAuthLoading, isAuthenticated, user, router]);

  const loadSettings = useCallback(async () => {
    setIsLoading(true);
    setLoadError(null);
    try {
      const data = await getPlatformSettings();
      setSettings(data);
      if (data?.mentorshipCapacity) {
        setMinMentees(data.mentorshipCapacity.minMentees);
        setMaxMentees(data.mentorshipCapacity.maxMentees);
        setDefaultMentees(data.mentorshipCapacity.defaultMentees);
      }
    } catch (err: any) {
      console.error("Failed to load platform settings:", err);
      setLoadError(err.message || "Failed to load platform settings.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated && user?.role === "ADMIN") {
      loadSettings();
    }
  }, [isAuthenticated, user, loadSettings]);

  // ESC key listener for modal
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && showConfirmModal) {
        setShowConfirmModal(false);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [showConfirmModal]);

  // Form Validation
  const validateForm = (): string | null => {
    if (minMentees < 1) {
      return "Minimum mentee capacity must be at least 1.";
    }
    if (maxMentees < minMentees) {
      return `Maximum capacity (${maxMentees}) cannot be less than minimum capacity (${minMentees}).`;
    }
    if (maxMentees > 100) {
      return "Maximum mentee capacity cannot exceed the product safety limit of 100.";
    }
    if (defaultMentees < minMentees || defaultMentees > maxMentees) {
      return `Default mentee capacity (${defaultMentees}) must be between minimum (${minMentees}) and maximum (${maxMentees}).`;
    }
    return null;
  };

  const validationError = validateForm();

  // Dirty check
  const isDirty =
    settings?.mentorshipCapacity &&
    (minMentees !== settings.mentorshipCapacity.minMentees ||
      maxMentees !== settings.mentorshipCapacity.maxMentees ||
      defaultMentees !== settings.mentorshipCapacity.defaultMentees);

  // Handle Save
  const handleSaveClick = (e: React.FormEvent) => {
    e.preventDefault();
    if (validationError) {
      setFeedback({ type: "error", message: validationError });
      return;
    }

    // Check if lowering maxMentees as a consequential change
    if (settings?.mentorshipCapacity && maxMentees < settings.mentorshipCapacity.maxMentees) {
      setShowConfirmModal(true);
      return;
    }

    executeSave();
  };

  const executeSave = async () => {
    setIsSaving(true);
    setFeedback(null);
    setShowConfirmModal(false);

    try {
      const payload: MentorshipCapacityConfig = {
        minMentees: Number(minMentees),
        maxMentees: Number(maxMentees),
        defaultMentees: Number(defaultMentees),
      };

      const updated = await updatePlatformSettings({ mentorshipCapacity: payload });
      setSettings(updated);
      if (updated?.mentorshipCapacity) {
        setMinMentees(updated.mentorshipCapacity.minMentees);
        setMaxMentees(updated.mentorshipCapacity.maxMentees);
        setDefaultMentees(updated.mentorshipCapacity.defaultMentees);
      }

      setFeedback({
        type: "success",
        message: "Platform settings successfully updated and persisted to MySQL database.",
      });
    } catch (err: any) {
      console.error("Failed to update platform settings:", err);
      setFeedback({
        type: "error",
        message: err.message || "Failed to persist platform settings.",
      });
    } finally {
      setIsSaving(false);
    }
  };

  const handleReset = () => {
    if (settings?.mentorshipCapacity) {
      setMinMentees(settings.mentorshipCapacity.minMentees);
      setMaxMentees(settings.mentorshipCapacity.maxMentees);
      setDefaultMentees(settings.mentorshipCapacity.defaultMentees);
      setFeedback(null);
    }
  };

  const formatTimestamp = (dateString?: string | null) => {
    if (!dateString) return "Never modified (Default configuration)";
    try {
      const d = new Date(dateString);
      return d.toLocaleString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      });
    } catch {
      return dateString;
    }
  };

  if (isAuthLoading) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center text-gray-500 gap-3">
        <div className="w-8 h-8 border-4 border-black border-t-transparent rounded-full animate-spin"></div>
        <p className="text-sm font-medium">Verifying administrator authorization...</p>
      </div>
    );
  }

  if (!isAuthenticated || user?.role !== "ADMIN") {
    return null;
  }

  return (
    <div className="space-y-6 pb-12">
      {/* Top Breadcrumb Navigation */}
      <nav aria-label="Breadcrumb" className="text-xs text-gray-500 flex items-center gap-1.5 pb-1">
        <Link href="/admin" className="hover:text-black transition font-medium">Admin Dashboard</Link>
        <span>›</span>
        <span className="text-gray-800 font-semibold">Platform Settings</span>
      </nav>

      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-gray-200/80 shadow-sm">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-gray-100 text-gray-800 border border-gray-200">
              System Configuration
            </span>
            <span className="text-xs text-gray-400">•</span>
            <span className="text-xs text-gray-500 font-medium">MySQL Persistent Engine</span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-gray-900">
            Platform Settings & Governance
          </h1>
          <p className="text-sm text-gray-500 mt-1 max-w-2xl">
            Configure system-wide constraints and runtime parameters. All modifications are persisted to MySQL and recorded in the immutable audit trail.
          </p>
        </div>

        {/* Action Controls & Navigation */}
        <div className="flex flex-wrap items-center gap-2">
          <Link
            href="/admin"
            className="px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-black"
          >
            ← Admin Dashboard
          </Link>
          <Link
            href="/admin/mentors"
            className="px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-black"
          >
            Mentor Governance
          </Link>
          <Link
            href="/admin/audit-logs"
            className="px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-black"
          >
            Audit Logs
          </Link>
          <button
            onClick={loadSettings}
            disabled={isLoading || isSaving}
            className="px-3 py-1.5 text-xs font-medium text-white bg-black hover:bg-gray-800 disabled:opacity-50 rounded-lg transition-colors flex items-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-black"
          >
            <span className={`inline-block ${isLoading ? "animate-spin" : ""}`} aria-hidden="true">🔄</span>
            Refresh
          </button>
        </div>
      </div>

      {/* Feedback Banner */}
      {feedback && (
        <div
          role="alert"
          className={`p-4 rounded-xl border flex items-center justify-between gap-3 text-sm animate-in fade-in ${
            feedback.type === "success"
              ? "bg-emerald-50 text-emerald-800 border-emerald-200"
              : "bg-rose-50 text-rose-800 border-rose-200"
          }`}
        >
          <div className="flex items-center gap-2">
            <span aria-hidden="true">{feedback.type === "success" ? "✓" : "⚠️"}</span>
            <span className="font-medium">{feedback.message}</span>
          </div>
          <button
            onClick={() => setFeedback(null)}
            aria-label="Dismiss feedback notification"
            className="text-gray-400 hover:text-gray-600 text-xs px-2 py-1 rounded focus:outline-none focus:ring-2 focus:ring-black"
          >
            ✕
          </button>
        </div>
      )}

      {/* Configuration Status & Telemetry Card */}
      <div className="bg-white p-5 rounded-xl border border-gray-200/80 shadow-sm">
        <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
          Configuration Status & Telemetry
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
          <div className="p-3 bg-gray-50/75 rounded-lg border border-gray-100">
            <span className="text-xs text-gray-500 block mb-1">Last Modified</span>
            <span className="text-xs font-medium text-gray-900 block">
              {formatTimestamp(settings?.lastUpdatedAt)}
            </span>
          </div>
          <div className="p-3 bg-gray-50/75 rounded-lg border border-gray-100">
            <span className="text-xs text-gray-500 block mb-1">Modified By</span>
            <span className="text-xs font-mono font-medium text-gray-900 block truncate" title={settings?.lastUpdatedBy || "system"}>
              {settings?.lastUpdatedBy || "Initial Platform Defaults"}
            </span>
          </div>
          <div className="p-3 bg-gray-50/75 rounded-lg border border-gray-100">
            <span className="text-xs text-gray-500 block mb-1">Audit Trail Integration</span>
            <span className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200 inline-block">
              Active (PLATFORM_SETTING_UPDATED)
            </span>
          </div>
        </div>
      </div>

      {/* Purpose-Driven Navigation Tabs */}
      <div className="border-b border-gray-200 bg-white rounded-t-xl px-4 pt-2 flex items-center gap-2 overflow-x-auto" role="tablist" aria-label="Platform Settings Categories">
        <button
          role="tab"
          aria-selected={activeTab === "mentorship"}
          onClick={() => setActiveTab("mentorship")}
          className={`px-4 py-3 text-xs font-bold transition-colors border-b-2 flex items-center gap-2 whitespace-nowrap focus:outline-none focus:ring-2 focus:ring-black rounded-t-lg ${
            activeTab === "mentorship"
              ? "border-black text-black bg-gray-50/50"
              : "border-transparent text-gray-500 hover:text-gray-900 hover:bg-gray-50/25"
          }`}
        >
          <span>🎓</span>
          Mentorship Configuration
          {isDirty && (
            <span className="w-2 h-2 rounded-full bg-amber-500" title="Unsaved changes"></span>
          )}
        </button>

        <button
          role="tab"
          aria-selected={activeTab === "platform"}
          onClick={() => setActiveTab("platform")}
          className={`px-4 py-3 text-xs font-bold transition-colors border-b-2 flex items-center gap-2 whitespace-nowrap focus:outline-none focus:ring-2 focus:ring-black rounded-t-lg ${
            activeTab === "platform"
              ? "border-black text-black bg-gray-50/50"
              : "border-transparent text-gray-500 hover:text-gray-900 hover:bg-gray-50/25"
          }`}
        >
          <span>⚡</span>
          Platform Configuration
        </button>

        <button
          role="tab"
          aria-selected={activeTab === "security"}
          onClick={() => setActiveTab("security")}
          className={`px-4 py-3 text-xs font-bold transition-colors border-b-2 flex items-center gap-2 whitespace-nowrap focus:outline-none focus:ring-2 focus:ring-black rounded-t-lg ${
            activeTab === "security"
              ? "border-black text-black bg-gray-50/50"
              : "border-transparent text-gray-500 hover:text-gray-900 hover:bg-gray-50/25"
          }`}
        >
          <span>🔒</span>
          Security & Access
        </button>

        <button
          role="tab"
          aria-selected={activeTab === "system"}
          onClick={() => setActiveTab("system")}
          className={`px-4 py-3 text-xs font-bold transition-colors border-b-2 flex items-center gap-2 whitespace-nowrap focus:outline-none focus:ring-2 focus:ring-black rounded-t-lg ${
            activeTab === "system"
              ? "border-black text-black bg-gray-50/50"
              : "border-transparent text-gray-500 hover:text-gray-900 hover:bg-gray-50/25"
          }`}
        >
          <span>🖥️</span>
          Maintenance & Telemetry
        </button>
      </div>

      {/* Tab Panels */}
      {loadError ? (
        <div className="bg-white rounded-xl border border-gray-200/80 p-8 text-center shadow-sm">
          <div className="inline-flex p-3 rounded-full bg-rose-50 text-rose-600 mb-3 text-2xl">
            ⚠️
          </div>
          <h3 className="text-base font-semibold text-gray-900">Failed to load platform settings</h3>
          <p className="text-sm text-gray-500 mt-1 max-w-md mx-auto">{loadError}</p>
          <button
            onClick={loadSettings}
            className="mt-4 px-4 py-2 text-xs font-medium text-white bg-black hover:bg-gray-800 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-black"
          >
            Retry
          </button>
        </div>
      ) : isLoading ? (
        <div className="bg-white rounded-xl border border-gray-200/80 p-12 text-center text-gray-500 shadow-sm">
          <div className="w-8 h-8 border-4 border-black border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
          <p className="text-sm font-medium">Fetching configuration from MySQL...</p>
        </div>
      ) : (
        <>
          {/* TAB 1: MENTORSHIP CAPACITY CONFIGURATION (EDITABLE) */}
          {activeTab === "mentorship" && (
            <div className="bg-white rounded-b-xl border border-gray-200/80 shadow-sm overflow-hidden">
              <div className="px-6 py-5 border-b border-gray-100 bg-gray-50/50 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-lg bg-indigo-50 text-indigo-700 font-bold flex items-center justify-center text-lg">
                    🎓
                  </div>
                  <div>
                    <h2 className="text-base font-bold text-gray-900">
                      Mentorship Capacity Thresholds
                    </h2>
                    <p className="text-xs text-gray-500 mt-0.5">
                      Controls the min, max, and default student allocation limits per verified mentor.
                    </p>
                  </div>
                </div>
                {isDirty && (
                  <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-amber-50 text-amber-700 border border-amber-200 animate-pulse">
                    ● Unsaved Changes
                  </span>
                )}
              </div>

              <form onSubmit={handleSaveClick} className="p-6 space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  {/* Min Mentees */}
                  <div className="space-y-2">
                    <label htmlFor="input-min-mentees" className="text-sm font-semibold text-gray-900 block">
                      Minimum Mentee Capacity
                    </label>
                    <p className="text-xs text-gray-500 leading-relaxed">
                      Lower bound of students a mentor can accept (must be ≥ 1).
                    </p>
                    <div className="relative">
                      <input
                        id="input-min-mentees"
                        type="number"
                        min={1}
                        max={maxMentees}
                        value={minMentees}
                        onChange={(e) => setMinMentees(parseInt(e.target.value) || 1)}
                        className="w-full px-3.5 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-black font-medium"
                        required
                      />
                      <span className="absolute right-3 top-2.5 text-xs text-gray-400">
                        mentees
                      </span>
                    </div>
                  </div>

                  {/* Max Mentees */}
                  <div className="space-y-2">
                    <label htmlFor="input-max-mentees" className="text-sm font-semibold text-gray-900 block">
                      Maximum Mentee Capacity
                    </label>
                    <p className="text-xs text-gray-500 leading-relaxed">
                      Global ceiling for mentor capacity (product safety limit: 100).
                    </p>
                    <div className="relative">
                      <input
                        id="input-max-mentees"
                        type="number"
                        min={minMentees}
                        max={100}
                        value={maxMentees}
                        onChange={(e) => setMaxMentees(parseInt(e.target.value) || minMentees)}
                        className="w-full px-3.5 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-black font-medium"
                        required
                      />
                      <span className="absolute right-3 top-2.5 text-xs text-gray-400">
                        mentees
                      </span>
                    </div>
                  </div>

                  {/* Default Mentees */}
                  <div className="space-y-2">
                    <label htmlFor="input-default-mentees" className="text-sm font-semibold text-gray-900 block">
                      Default Mentee Capacity
                    </label>
                    <p className="text-xs text-gray-500 leading-relaxed">
                      Allocated to mentors who do not specify custom capacity.
                    </p>
                    <div className="relative">
                      <input
                        id="input-default-mentees"
                        type="number"
                        min={minMentees}
                        max={maxMentees}
                        value={defaultMentees}
                        onChange={(e) => setDefaultMentees(parseInt(e.target.value) || minMentees)}
                        className="w-full px-3.5 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-black font-medium"
                        required
                      />
                      <span className="absolute right-3 top-2.5 text-xs text-gray-400">
                        mentees
                      </span>
                    </div>
                  </div>
                </div>

                {/* Validation Notice */}
                {validationError && (
                  <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs font-medium text-rose-700 flex items-center gap-2">
                    <span>⚠️</span>
                    <span>{validationError}</span>
                  </div>
                )}

                {/* Policy Context Note */}
                <div className="p-4 bg-gray-50 rounded-xl border border-gray-200/70 text-xs text-gray-600 space-y-1.5">
                  <div className="font-semibold text-gray-800">
                    📌 Invariant & Relationship Safeguards:
                  </div>
                  <ul className="list-disc pl-5 space-y-1">
                    <li>Strict Invariant: <code>1 ≤ minMentees ≤ defaultMentees ≤ maxMentees ≤ 100</code> is validated server-side.</li>
                    <li>Existing mentor allocations and active mentorships remain untouched and protected.</li>
                    <li>Audit trail logging records all mutations as <code>PLATFORM_SETTING_UPDATED</code>.</li>
                  </ul>
                </div>

                {/* Form Footer Buttons */}
                <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
                  <button
                    type="button"
                    onClick={handleReset}
                    disabled={!isDirty || isSaving}
                    className="px-4 py-2 text-xs font-medium text-gray-700 hover:text-gray-900 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors focus:outline-none focus:ring-2 focus:ring-black"
                  >
                    Discard Changes
                  </button>
                  <button
                    type="submit"
                    disabled={!isDirty || isSaving || !!validationError}
                    className="px-5 py-2 text-xs font-semibold text-white bg-black hover:bg-gray-800 disabled:opacity-40 disabled:cursor-not-allowed rounded-lg transition-colors flex items-center gap-2 shadow-sm focus:outline-none focus:ring-2 focus:ring-black"
                  >
                    {isSaving && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                    {isSaving ? "Persisting to MySQL..." : "Save Configuration"}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* TAB 2: PLATFORM CONFIGURATION (GROUP A) */}
          {activeTab === "platform" && (
            <div className="bg-white rounded-b-xl border border-gray-200/80 shadow-sm p-6 space-y-6">
              <div className="flex items-center gap-3 pb-4 border-b border-gray-100">
                <div className="w-9 h-9 rounded-lg bg-amber-50 text-amber-700 font-bold flex items-center justify-center text-lg">
                  ⚡
                </div>
                <div>
                  <h2 className="text-base font-bold text-gray-900">
                    Platform & AI Configuration Telemetry
                  </h2>
                  <p className="text-xs text-gray-500">
                    Live operational parameters governing AI career advising, chat limits, and token budgets.
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 text-xs">
                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-1">
                  <span className="text-gray-400 font-semibold uppercase tracking-wider block">AI Engine Status</span>
                  <div className="text-sm font-bold text-gray-900 flex items-center gap-2 mt-1">
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
                    {settings?.platformConfig?.aiEnabled ? "Enabled & Active" : "Disabled"}
                  </div>
                  <p className="text-[11px] text-gray-500">Global AI advisor orchestration</p>
                </div>

                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-1">
                  <span className="text-gray-400 font-semibold uppercase tracking-wider block">AI Provider & Model</span>
                  <div className="text-sm font-bold text-gray-900 font-mono mt-1">
                    {settings?.platformConfig?.aiModel || "gemini-1.5-flash"}
                  </div>
                  <p className="text-[11px] text-gray-500">
                    Provider: <span className="font-medium text-gray-700">{settings?.platformConfig?.aiProvider || "mock"}</span>
                  </p>
                </div>

                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-1">
                  <span className="text-gray-400 font-semibold uppercase tracking-wider block">Generation Budget</span>
                  <div className="text-sm font-bold text-gray-900 mt-1">
                    {settings?.platformConfig?.maxTokens ?? 1000} Max Tokens
                  </div>
                  <p className="text-[11px] text-gray-500">
                    Sampling Temperature: <span className="font-mono text-gray-700">{settings?.platformConfig?.temperature ?? 0.7}</span>
                  </p>
                </div>

                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-1">
                  <span className="text-gray-400 font-semibold uppercase tracking-wider block">Chat History Window</span>
                  <div className="text-sm font-bold text-gray-900 mt-1">
                    {settings?.platformConfig?.chatHistoryLimit ?? 20} Messages
                  </div>
                  <p className="text-[11px] text-gray-500">Rolling contextual memory depth</p>
                </div>

                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-1">
                  <span className="text-gray-400 font-semibold uppercase tracking-wider block">Max Message Size</span>
                  <div className="text-sm font-bold text-gray-900 mt-1">
                    {settings?.platformConfig?.chatMaxMessageLength ?? 4000} Characters
                  </div>
                  <p className="text-[11px] text-gray-500">Payload input size limit</p>
                </div>

                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-1">
                  <span className="text-gray-400 font-semibold uppercase tracking-wider block">Credential Protection</span>
                  <div className="text-sm font-bold text-emerald-700 mt-1 flex items-center gap-1.5">
                    <span>🛡️</span> Zero Secret Exposure
                  </div>
                  <p className="text-[11px] text-gray-500">API keys never transmitted to browser</p>
                </div>
              </div>

              <div className="p-4 bg-gray-50 rounded-xl border border-gray-200/70 text-xs text-gray-600 space-y-1">
                <span className="font-semibold text-gray-800">Operational Notice:</span>
                <p>
                  Platform configuration parameters are derived from backend infrastructure properties and active Spring configuration beans. Sensitive environment credentials remain safely sealed on the server.
                </p>
              </div>
            </div>
          )}

          {/* TAB 3: SECURITY & ACCESS (GROUP C) */}
          {activeTab === "security" && (
            <div className="bg-white rounded-b-xl border border-gray-200/80 shadow-sm p-6 space-y-6">
              <div className="flex items-center gap-3 pb-4 border-b border-gray-100">
                <div className="w-9 h-9 rounded-lg bg-emerald-50 text-emerald-700 font-bold flex items-center justify-center text-lg">
                  🔒
                </div>
                <div>
                  <h2 className="text-base font-bold text-gray-900">
                    Security & Access Governance Architecture
                  </h2>
                  <p className="text-xs text-gray-500">
                    Authentication rules, administrative integrity safeguards, and role enforcement guarantees.
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="text-base">🔑</span>
                    <span className="font-bold text-gray-900 text-sm">Authentication Strategy</span>
                  </div>
                  <p className="text-gray-600 leading-relaxed">
                    {settings?.securityPolicy?.authenticationMethod || "Stateless JWT (HMAC-SHA256)"}
                  </p>
                  <div className="text-[11px] text-gray-500">
                    Token Expiry: <span className="font-semibold text-gray-700">{settings?.securityPolicy?.tokenExpiration || "24 Hours"}</span>
                  </div>
                </div>

                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="text-base">🛡️</span>
                    <span className="font-bold text-gray-900 text-sm">Multi-Admin Safeguard</span>
                  </div>
                  <p className="text-gray-600 leading-relaxed">
                    {settings?.securityPolicy?.adminSafeguard || "Last remaining active administrator cannot be suspended or deactivated."}
                  </p>
                  <div className="text-[11px] text-emerald-700 font-medium">
                    ✓ Prevents accidental administrative lockout
                  </div>
                </div>

                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="text-base">🚫</span>
                    <span className="font-bold text-gray-900 text-sm">Self-Modification Restriction</span>
                  </div>
                  <p className="text-gray-600 leading-relaxed">
                    {settings?.securityPolicy?.selfStatusModification || "Administrators cannot alter their own account status or elevate privileges."}
                  </p>
                  <div className="text-[11px] text-indigo-700 font-medium">
                    ✓ Enforced in AdminService.updateUserStatus
                  </div>
                </div>

                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="text-base">📋</span>
                    <span className="font-bold text-gray-900 text-sm">Immutable Audit Trail</span>
                  </div>
                  <p className="text-gray-600 leading-relaxed">
                    {settings?.securityPolicy?.auditTrailImmutability || "Audit records are strictly append-only in MySQL without delete or update endpoints."}
                  </p>
                  <div className="text-[11px] text-emerald-700 font-medium">
                    ✓ Guaranteed historical integrity
                  </div>
                </div>
              </div>

              <div className="p-4 bg-gray-50 rounded-xl border border-gray-200/70 text-xs text-gray-600 space-y-1">
                <span className="font-semibold text-gray-800">RBAC Hierarchy:</span>
                <p>
                  <code>ADMIN &gt; MENTOR &gt; STUDENT</code>. All administrative endpoints are guarded by <code>@PreAuthorize(&quot;hasRole(&#39;ADMIN&#39;)&quot;)</code> with server-derived actor identity resolution.
                </p>
              </div>
            </div>
          )}

          {/* TAB 4: SYSTEM & MAINTENANCE (GROUP D) */}
          {activeTab === "system" && (
            <div className="bg-white rounded-b-xl border border-gray-200/80 shadow-sm p-6 space-y-6">
              <div className="flex items-center gap-3 pb-4 border-b border-gray-100">
                <div className="w-9 h-9 rounded-lg bg-sky-50 text-sky-700 font-bold flex items-center justify-center text-lg">
                  🖥️
                </div>
                <div>
                  <h2 className="text-base font-bold text-gray-900">
                    System & Maintenance Telemetry
                  </h2>
                  <p className="text-xs text-gray-500">
                    Operational health, database persistence engine, and runtime platform metrics.
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 text-xs">
                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-1">
                  <span className="text-gray-400 font-semibold uppercase tracking-wider block">Application</span>
                  <div className="text-sm font-bold text-gray-900 mt-1">
                    {settings?.systemInfo?.applicationName || "Career Advisor"}
                  </div>
                  <p className="text-[11px] text-gray-500">
                    Service: {settings?.systemInfo?.serviceName || "Backend API Engine"}
                  </p>
                </div>

                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-1">
                  <span className="text-gray-400 font-semibold uppercase tracking-wider block">System Health</span>
                  <div className="text-sm font-bold text-emerald-700 flex items-center gap-2 mt-1">
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
                    {settings?.systemInfo?.serverStatus || "OPERATIONAL"}
                  </div>
                  <p className="text-[11px] text-gray-500">Tomcat HTTP Server on port 8080</p>
                </div>

                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-1">
                  <span className="text-gray-400 font-semibold uppercase tracking-wider block">Database Persistence</span>
                  <div className="text-sm font-bold text-gray-900 font-mono mt-1">
                    {settings?.systemInfo?.databaseEngine || "MySQL 8.0 (InnoDB)"}
                  </div>
                  <p className="text-[11px] text-gray-500">
                    Dialect: {settings?.systemInfo?.databaseDialect || "MySQLDialect"}
                  </p>
                </div>

                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-1">
                  <span className="text-gray-400 font-semibold uppercase tracking-wider block">Java Runtime</span>
                  <div className="text-sm font-bold text-gray-900 font-mono mt-1">
                    JDK {settings?.systemInfo?.javaVersion || "17"}
                  </div>
                  <p className="text-[11px] text-gray-500">Spring Boot 3.3.4 (Jakarta EE 10)</p>
                </div>

                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-1">
                  <span className="text-gray-400 font-semibold uppercase tracking-wider block">Host Environment</span>
                  <div className="text-sm font-bold text-gray-900 mt-1">
                    {settings?.systemInfo?.operatingSystem || "Windows"}
                  </div>
                  <p className="text-[11px] text-gray-500">Host OS platform</p>
                </div>

                <div className="p-4 rounded-xl bg-gray-50/75 border border-gray-100 space-y-1">
                  <span className="text-gray-400 font-semibold uppercase tracking-wider block">Persistent Settings</span>
                  <div className="text-sm font-bold text-indigo-700 mt-1">
                    platform_settings table
                  </div>
                  <p className="text-[11px] text-gray-500">Dynamic MySQL key-value persistence</p>
                </div>
              </div>
            </div>
          )}
        </>
      )}

      {/* Confirmation Modal */}
      {showConfirmModal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in"
          role="dialog"
          aria-modal="true"
          aria-labelledby="confirm-modal-title"
        >
          <div className="bg-white rounded-2xl max-w-md w-full border border-gray-200 shadow-2xl overflow-hidden p-6 space-y-4">
            <div className="w-10 h-10 rounded-full bg-amber-50 text-amber-600 flex items-center justify-center text-xl font-bold mx-auto" aria-hidden="true">
              ⚠️
            </div>
            <div className="text-center">
              <h3 id="confirm-modal-title" className="text-base font-bold text-gray-900">
                Confirm Capacity Reduction
              </h3>
              <p className="text-xs text-gray-500 mt-2 leading-relaxed">
                You are reducing the maximum mentee capacity from{" "}
                <span className="font-semibold text-gray-800">
                  {settings?.mentorshipCapacity.maxMentees}
                </span>{" "}
                to <span className="font-semibold text-gray-800">{maxMentees}</span>.
              </p>
              <p className="text-xs text-gray-500 mt-1 leading-relaxed">
                Existing accepted mentees will not be impacted, but mentors with active capacities above {maxMentees} will not be able to accept additional mentees until their active count drops below their limit.
              </p>
            </div>

            <div className="flex items-center gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowConfirmModal(false)}
                disabled={isSaving}
                className="flex-1 px-4 py-2 text-xs font-medium text-gray-700 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors focus:outline-none focus:ring-2 focus:ring-black"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={executeSave}
                disabled={isSaving}
                className="flex-1 px-4 py-2 text-xs font-semibold text-white bg-black hover:bg-gray-800 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-black"
              >
                {isSaving ? "Saving..." : "Confirm & Persist"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
