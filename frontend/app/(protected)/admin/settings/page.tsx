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

export default function AdminSettingsPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();

  // Settings State
  const [settings, setSettings] = useState<PlatformSettings | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Form State
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
            Configure system-wide constraints and runtime parameters. All modifications are persisted to MySQL and recorded in the audit trail.
          </p>
        </div>

        {/* Action Controls & Navigation */}
        <div className="flex flex-wrap items-center gap-2">
          <Link
            href="/admin"
            className="px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
          >
            ← Admin Dashboard
          </Link>
          <Link
            href="/admin/mentors"
            className="px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
          >
            Mentor Governance
          </Link>
          <Link
            href="/admin/audit-logs"
            className="px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
          >
            Audit Logs
          </Link>
          <button
            onClick={loadSettings}
            disabled={isLoading || isSaving}
            className="px-3 py-1.5 text-xs font-medium text-white bg-black hover:bg-gray-800 disabled:opacity-50 rounded-lg transition-colors flex items-center gap-1.5"
          >
            <span className={`inline-block ${isLoading ? "animate-spin" : ""}`}>🔄</span>
            Refresh
          </button>
        </div>
      </div>

      {/* Feedback Banner */}
      {feedback && (
        <div
          className={`p-4 rounded-xl border flex items-center justify-between gap-3 text-sm animate-in fade-in ${
            feedback.type === "success"
              ? "bg-emerald-50 text-emerald-800 border-emerald-200"
              : "bg-rose-50 text-rose-800 border-rose-200"
          }`}
        >
          <div className="flex items-center gap-2">
            <span>{feedback.type === "success" ? "✓" : "⚠️"}</span>
            <span className="font-medium">{feedback.message}</span>
          </div>
          <button
            onClick={() => setFeedback(null)}
            className="text-gray-400 hover:text-gray-600 text-xs px-2 py-1 rounded"
          >
            ✕
          </button>
        </div>
      )}

      {/* Metadata Overview Card */}
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

      {/* Mentorship Capacity Configuration Card */}
      <div className="bg-white rounded-xl border border-gray-200/80 shadow-sm overflow-hidden">
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

        {loadError ? (
          <div className="p-8 text-center">
            <div className="inline-flex p-3 rounded-full bg-rose-50 text-rose-600 mb-3">
              ⚠️
            </div>
            <h3 className="text-base font-semibold text-gray-900">Failed to load platform settings</h3>
            <p className="text-sm text-gray-500 mt-1 max-w-md mx-auto">{loadError}</p>
            <button
              onClick={loadSettings}
              className="mt-4 px-4 py-2 text-xs font-medium text-white bg-black hover:bg-gray-800 rounded-lg transition-colors"
            >
              Retry
            </button>
          </div>
        ) : isLoading ? (
          <div className="p-12 text-center text-gray-500">
            <div className="w-8 h-8 border-4 border-black border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
            <p className="text-sm font-medium">Fetching configuration from MySQL...</p>
          </div>
        ) : (
          <form onSubmit={handleSaveClick} className="p-6 space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Min Mentees */}
              <div className="space-y-2">
                <label className="text-sm font-semibold text-gray-900 block">
                  Minimum Mentee Capacity
                </label>
                <p className="text-xs text-gray-500 leading-relaxed">
                  Lower bound of students a mentor can accept (must be ≥ 1).
                </p>
                <div className="relative">
                  <input
                    type="number"
                    min={1}
                    max={maxMentees}
                    value={minMentees}
                    onChange={(e) => setMinMentees(parseInt(e.target.value) || 1)}
                    className="w-full px-3.5 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-black/10 focus:border-black font-medium"
                    required
                  />
                  <span className="absolute right-3 top-2.5 text-xs text-gray-400">
                    mentees
                  </span>
                </div>
              </div>

              {/* Max Mentees */}
              <div className="space-y-2">
                <label className="text-sm font-semibold text-gray-900 block">
                  Maximum Mentee Capacity
                </label>
                <p className="text-xs text-gray-500 leading-relaxed">
                  Global ceiling for mentor capacity (product safety limit: 100).
                </p>
                <div className="relative">
                  <input
                    type="number"
                    min={minMentees}
                    max={100}
                    value={maxMentees}
                    onChange={(e) => setMaxMentees(parseInt(e.target.value) || minMentees)}
                    className="w-full px-3.5 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-black/10 focus:border-black font-medium"
                    required
                  />
                  <span className="absolute right-3 top-2.5 text-xs text-gray-400">
                    mentees
                  </span>
                </div>
              </div>

              {/* Default Mentees */}
              <div className="space-y-2">
                <label className="text-sm font-semibold text-gray-900 block">
                  Default Mentee Capacity
                </label>
                <p className="text-xs text-gray-500 leading-relaxed">
                  Allocated to mentors who do not specify custom capacity.
                </p>
                <div className="relative">
                  <input
                    type="number"
                    min={minMentees}
                    max={maxMentees}
                    value={defaultMentees}
                    onChange={(e) => setDefaultMentees(parseInt(e.target.value) || minMentees)}
                    className="w-full px-3.5 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-black/10 focus:border-black font-medium"
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
                📌 Existing Mentors & Relationship Safety:
              </div>
              <ul className="list-disc pl-5 space-y-1">
                <li>Existing mentor-specific capacities are preserved and not automatically modified.</li>
                <li>Currently accepted mentorship requests and relationships remain active and protected.</li>
                <li>When mentors update their profiles or new candidates apply, their requested capacity is validated against these bounds.</li>
              </ul>
            </div>

            {/* Form Footer Buttons */}
            <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
              <button
                type="button"
                onClick={handleReset}
                disabled={!isDirty || isSaving}
                className="px-4 py-2 text-xs font-medium text-gray-700 hover:text-gray-900 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              >
                Discard Changes
              </button>
              <button
                type="submit"
                disabled={!isDirty || isSaving || !!validationError}
                className="px-5 py-2 text-xs font-semibold text-white bg-black hover:bg-gray-800 disabled:opacity-40 disabled:cursor-not-allowed rounded-lg transition-colors flex items-center gap-2 shadow-sm"
              >
                {isSaving && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                {isSaving ? "Persisting to MySQL..." : "Save Configuration"}
              </button>
            </div>
          </form>
        )}
      </div>

      {/* Confirmation Modal */}
      {showConfirmModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-md w-full border border-gray-200 shadow-2xl overflow-hidden p-6 space-y-4">
            <div className="w-10 h-10 rounded-full bg-amber-50 text-amber-600 flex items-center justify-center text-xl font-bold mx-auto">
              ⚠️
            </div>
            <div className="text-center">
              <h3 className="text-base font-bold text-gray-900">
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
                className="flex-1 px-4 py-2 text-xs font-medium text-gray-700 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={executeSave}
                disabled={isSaving}
                className="flex-1 px-4 py-2 text-xs font-semibold text-white bg-black hover:bg-gray-800 rounded-lg transition-colors"
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
