"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import {
  ResumeAnalysis,
  ResumeSummary,
  uploadResume,
  getLatestResumeAnalysis,
  getUserResumes,
  deleteResume,
  syncResumeSkills,
} from "@/lib/resumeService";
import { ApiError } from "@/lib/apiClient";

export default function ResumePage() {
  const [analysis, setAnalysis] = useState<ResumeAnalysis | null>(null);
  const [history, setHistory] = useState<ResumeSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [selectedSkillsToSync, setSelectedSkillsToSync] = useState<Set<string>>(new Set());
  const [isSyncing, setIsSyncing] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    loadResumeData();
  }, []);

  const mapApiError = (err: any, fallbackMessage: string): string => {
    if (!err) return fallbackMessage;
    if (err instanceof ApiError || (err && typeof err.status === "number")) {
      if (err.status === 400) return err.message || "Invalid file or request data.";
      if (err.status === 401) return "Your session has expired. Please log in again.";
      if (err.status === 403) return "Access denied. You do not have permission to perform this action.";
      if (err.status === 404) return "Resume resource not found.";
      if (err.status === 413) return "Uploaded resume exceeds the 5MB file size limit. Please upload a smaller document.";
      if (err.status === 415) return err.message || "Unsupported file format. Only valid PDF and DOCX files are supported.";
      if (err.status === 422) return err.message || "Unable to parse text from this resume. Please ensure the document contains extractable text.";
      if (err.status === 500) return err.message || "Server encountered an error while processing the resume. Please try again.";
      if (err.message) return err.message;
    }
    return err.message || fallbackMessage;
  };

  const loadResumeData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [latest, resumes] = await Promise.all([
        getLatestResumeAnalysis(),
        getUserResumes().catch(() => []),
      ]);
      setAnalysis(latest);
      setHistory(resumes);

      if (latest && latest.extractedSkills) {
        // Pre-select all detected skills that are not already in profile
        const unprofiled = new Set<string>();
        latest.extractedSkills.forEach((s) => {
          if (!s.alreadyInProfile) {
            unprofiled.add(s.skillName);
          }
        });
        setSelectedSkillsToSync(unprofiled);
      }
    } catch (err: any) {
      if (err.status !== 404 && err.status !== 204) {
        setError(mapApiError(err, "Failed to load resume intelligence data."));
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleFileSelect = async (file: File) => {
    if (!file) return;

    const lowerName = file.name.toLowerCase();
    if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".docx")) {
      setError("Only PDF and DOCX documents are supported.");
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setError("File size exceeds 5MB limit. Please upload a smaller resume.");
      return;
    }

    setIsUploading(true);
    setUploadProgress("Reading document & extracting text...");
    setError(null);
    setSuccessMsg(null);

    try {
      setUploadProgress("Analyzing technical skills & matching career tracks...");
      const result = await uploadResume(file);
      setAnalysis(result);
      setSuccessMsg(`Resume "${file.name}" analyzed successfully! Detected ${result.extractedSkills.length} skills.`);

      // Update sync selection
      const unprofiled = new Set<string>();
      result.extractedSkills.forEach((s) => {
        if (!s.alreadyInProfile) {
          unprofiled.add(s.skillName);
        }
      });
      setSelectedSkillsToSync(unprofiled);

      // Refresh history
      const updatedHistory = await getUserResumes().catch(() => []);
      setHistory(updatedHistory);
    } catch (err: any) {
      console.error("Resume upload failed:", err);
      setError(mapApiError(err, "Failed to upload and parse resume. Please try again."));
    } finally {
      setIsUploading(false);
      setUploadProgress("");
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFileSelect(e.dataTransfer.files[0]);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const toggleSkillSelection = (skillName: string) => {
    const next = new Set(selectedSkillsToSync);
    if (next.has(skillName)) {
      next.delete(skillName);
    } else {
      next.add(skillName);
    }
    setSelectedSkillsToSync(next);
  };

  const handleSyncSkills = async (skillsToSync?: string[]) => {
    if (!analysis) return;
    const toSync = skillsToSync || Array.from(selectedSkillsToSync);
    if (toSync.length === 0) {
      setError("Please select at least one skill to synchronize.");
      return;
    }

    setIsSyncing(true);
    setError(null);
    try {
      const res = await syncResumeSkills(analysis.resumeId, toSync);
      setSuccessMsg(res.message || "Skills synchronized with your profile!");

      // Update local analysis state to show skills as alreadyInProfile
      const updatedSkills = analysis.extractedSkills.map((s) => ({
        ...s,
        alreadyInProfile: s.alreadyInProfile || toSync.includes(s.skillName),
      }));

      setAnalysis({
        ...analysis,
        extractedSkills: updatedSkills,
      });

      // Clear synced from selected set
      const nextSelected = new Set(selectedSkillsToSync);
      toSync.forEach((s) => nextSelected.delete(s));
      setSelectedSkillsToSync(nextSelected);
    } catch (err: any) {
      console.error("Skill sync failed:", err);
      setError(mapApiError(err, "Failed to synchronize skills."));
    } finally {
      setIsSyncing(false);
    }
  };

  const handleDeleteResume = async (id: number) => {
    if (!confirm("Are you sure you want to remove this resume analysis?")) return;
    setError(null);
    try {
      await deleteResume(id);
      setSuccessMsg("Resume deleted successfully.");
      if (analysis?.resumeId === id) {
        setAnalysis(null);
      }
      const updatedHistory = await getUserResumes().catch(() => []);
      setHistory(updatedHistory);
    } catch (err: any) {
      console.error("Resume deletion failed:", err);
      setError(mapApiError(err, "Failed to delete resume."));
    }
  };

  const formatFileSize = (bytes: number) => {
    if (!bytes || bytes === 0) return "0 KB";
    const k = 1024;
    if (bytes < k * k) {
      return (bytes / k).toFixed(1) + " KB";
    }
    return (bytes / (k * k)).toFixed(2) + " MB";
  };

  const formatDate = (dateStr: string) => {
    if (!dateStr) return "";
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString("en-US", {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="space-y-8 pb-12">
      {/* Hero Header */}
      <div className="bg-gradient-to-r from-gray-900 via-black to-blue-950 text-white rounded-3xl p-6 sm:p-8 border border-white/10 shadow-lg relative overflow-hidden">
        <div className="absolute right-0 top-0 w-80 h-80 bg-blue-600/10 rounded-full blur-3xl pointer-events-none" />
        <div className="relative z-10 max-w-3xl">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-500/15 border border-blue-400/30 text-blue-300 text-xs font-semibold uppercase tracking-wider mb-4">
            <span>📄 Resume Intelligence Engine</span>
          </div>
          <h1 className="text-2xl sm:text-4xl font-extrabold tracking-tight">
            Intelligent Skill Extraction & Career Match
          </h1>
          <p className="mt-2 text-sm sm:text-base text-gray-300 leading-relaxed">
            Upload your resume in PDF or DOCX format. Our server-side parser will automatically extract your technical skills, evaluate your alignment with industry career tracks, and uncover high-priority skill gaps.
          </p>
        </div>
      </div>

      {/* Alerts */}
      {error && (
        <div className="p-4 bg-red-500/10 border border-red-500/30 text-red-700 dark:text-red-300 rounded-2xl text-sm flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span>⚠️</span>
            <span>{error}</span>
          </div>
          <button
            onClick={() => setError(null)}
            className="text-xs font-bold hover:underline cursor-pointer"
          >
            Dismiss
          </button>
        </div>
      )}

      {successMsg && (
        <div className="p-4 bg-emerald-500/10 border border-emerald-500/30 text-emerald-800 dark:text-emerald-300 rounded-2xl text-sm flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span>✅</span>
            <span>{successMsg}</span>
          </div>
          <button
            onClick={() => setSuccessMsg(null)}
            className="text-xs font-bold hover:underline cursor-pointer"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Upload Component */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200 shadow-sm">
        <h2 className="text-lg font-bold text-gray-900 mb-2 flex items-center gap-2">
          <span>📤</span>
          <span>{analysis ? "Upload Updated Resume" : "Upload Your Resume"}</span>
        </h2>
        <p className="text-xs sm:text-sm text-gray-500 mb-6">
          Supported formats: <span className="font-semibold text-gray-700">PDF (.pdf)</span> and{" "}
          <span className="font-semibold text-gray-700">Word (.docx)</span> • Max size:{" "}
          <span className="font-semibold text-gray-700">5 MB</span>
        </p>

        <div
          onDrop={handleDrop}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onClick={() => fileInputRef.current?.click()}
          className={`border-2 border-dashed rounded-2xl p-8 sm:p-12 text-center cursor-pointer transition flex flex-col items-center justify-center gap-4 ${
            isDragging
              ? "border-blue-500 bg-blue-50/50"
              : "border-gray-300 hover:border-gray-400 bg-gray-50/50 hover:bg-gray-50"
          } ${isUploading ? "opacity-60 pointer-events-none" : ""}`}
        >
          <input
            type="file"
            ref={fileInputRef}
            onChange={(e) => {
              if (e.target.files && e.target.files.length > 0) {
                handleFileSelect(e.target.files[0]);
              }
            }}
            accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            className="hidden"
          />

          {isUploading ? (
            <div className="flex flex-col items-center gap-3">
              <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
              <p className="text-sm font-semibold text-gray-800">{uploadProgress}</p>
              <p className="text-xs text-gray-500">Please wait while the document is parsed...</p>
            </div>
          ) : (
            <>
              <div className="w-16 h-16 rounded-2xl bg-blue-100 text-blue-600 flex items-center justify-center text-2xl shadow-xs">
                📄
              </div>
              <div>
                <p className="text-sm sm:text-base font-bold text-gray-900">
                  Drag and drop your resume file here, or{" "}
                  <span className="text-blue-600 underline">browse</span>
                </p>
                <p className="text-xs text-gray-500 mt-1">
                  PDF or DOCX documents up to 5MB are parsed securely in user space.
                </p>
              </div>
              <div className="flex items-center gap-2 text-xs font-semibold text-gray-500">
                <span className="px-2 py-1 bg-gray-200 rounded-md">.PDF</span>
                <span className="px-2 py-1 bg-gray-200 rounded-md">.DOCX</span>
                <span className="px-2 py-1 bg-gray-200 rounded-md">🔒 End-to-End User Isolated</span>
              </div>
            </>
          )}
        </div>
      </div>

      {isLoading ? (
        <div className="p-12 text-center text-gray-500 flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-4 border-black border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm font-medium">Loading resume intelligence...</p>
        </div>
      ) : analysis ? (
        <>
          {/* Resume Overview Header Card */}
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200 shadow-sm space-y-6">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-gray-100">
              <div className="flex items-start gap-4">
                <div className="w-12 h-12 rounded-2xl bg-blue-600 text-white flex items-center justify-center text-xl font-bold shadow">
                  📄
                </div>
                <div>
                  <h3 className="text-lg font-bold text-gray-900">{analysis.fileName}</h3>
                  <div className="flex flex-wrap items-center gap-3 text-xs text-gray-500 mt-1">
                    <span>Size: {formatFileSize(analysis.fileSize)}</span>
                    <span>•</span>
                    <span>Uploaded: {formatDate(analysis.uploadTimestamp)}</span>
                    <span>•</span>
                    <span className="inline-flex items-center gap-1 font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md">
                      ✓ {analysis.parsingStatus}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <button
                  onClick={() => fileInputRef.current?.click()}
                  className="px-4 py-2 text-xs font-bold text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-xl transition cursor-pointer"
                >
                  Replace Resume
                </button>
                <button
                  onClick={() => handleDeleteResume(analysis.resumeId)}
                  className="px-3 py-2 text-xs font-bold text-red-600 hover:bg-red-50 border border-red-200 rounded-xl transition cursor-pointer"
                >
                  Delete
                </button>
              </div>
            </div>

            {/* Extracted Contact Info & Summary */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="p-4 rounded-2xl bg-gray-50 border border-gray-100">
                <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Detected Email</p>
                <p className="text-sm font-bold text-gray-800 mt-1 truncate">
                  {analysis.extractedEmail || "Not detected"}
                </p>
              </div>
              <div className="p-4 rounded-2xl bg-gray-50 border border-gray-100">
                <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Detected Phone</p>
                <p className="text-sm font-bold text-gray-800 mt-1">
                  {analysis.extractedPhone || "Not detected"}
                </p>
              </div>
              <div className="p-4 rounded-2xl bg-gray-50 border border-gray-100">
                <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Total Detected Skills</p>
                <p className="text-sm font-bold text-blue-600 mt-1">
                  {analysis.extractedSkills.length} Technical Skills
                </p>
              </div>
            </div>

            {analysis.summary && (
              <div className="p-4 rounded-2xl bg-blue-50/60 border border-blue-100">
                <p className="text-xs font-bold text-blue-900 uppercase tracking-wider mb-1">Executive Summary</p>
                <p className="text-xs sm:text-sm text-gray-700 leading-relaxed italic">
                  "{analysis.summary}"
                </p>
              </div>
            )}
          </div>

          {/* Extracted Skills Section with Confirmation and Sync */}
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200 shadow-sm space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                  <span>⚡</span>
                  <span>Skills Detected from Resume ({analysis.extractedSkills.length})</span>
                </h2>
                <p className="text-xs sm:text-sm text-gray-500 mt-1">
                  Review extracted skills. Click any skill chip to toggle confirmation, then sync to update your portfolio.
                </p>
              </div>

              {analysis.extractedSkills.some((s) => !s.alreadyInProfile) && (
                <button
                  onClick={() => handleSyncSkills()}
                  disabled={isSyncing || selectedSkillsToSync.size === 0}
                  className="px-5 py-2.5 bg-black text-white text-xs sm:text-sm font-bold rounded-xl hover:bg-gray-800 disabled:opacity-50 transition shadow-sm cursor-pointer flex items-center gap-2"
                >
                  {isSyncing ? (
                    <>
                      <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      <span>Syncing...</span>
                    </>
                  ) : (
                    <>
                      <span>Add Selected Skills to Profile</span>
                      <span className="px-1.5 py-0.5 bg-white/20 rounded-md text-[11px]">
                        {selectedSkillsToSync.size}
                      </span>
                    </>
                  )}
                </button>
              )}
            </div>

            {/* Skill Chips Grid */}
            <div className="flex flex-wrap gap-2.5">
              {analysis.extractedSkills.map((skill) => {
                const isSelected = selectedSkillsToSync.has(skill.skillName);
                const inProfile = skill.alreadyInProfile;

                return (
                  <div
                    key={skill.skillName}
                    onClick={() => {
                      if (!inProfile) {
                        toggleSkillSelection(skill.skillName);
                      }
                    }}
                    className={`inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold transition border select-none ${
                      inProfile
                        ? "bg-emerald-50 text-emerald-800 border-emerald-200 cursor-default"
                        : isSelected
                        ? "bg-blue-600 text-white border-blue-600 shadow-sm cursor-pointer"
                        : "bg-gray-50 hover:bg-gray-100 text-gray-800 border-gray-200 cursor-pointer"
                    }`}
                  >
                    <span>{skill.skillName}</span>
                    <span
                      className={`text-[10px] px-1.5 py-0.5 rounded-md font-bold ${
                        inProfile
                          ? "bg-emerald-200 text-emerald-900"
                          : isSelected
                          ? "bg-white/25 text-white"
                          : "bg-gray-200 text-gray-700"
                      }`}
                    >
                      {skill.confidence}%
                    </span>

                    {inProfile ? (
                      <span className="text-emerald-700 text-xs font-bold" title="Already in your profile">
                        ✓ In Profile
                      </span>
                    ) : isSelected ? (
                      <span className="text-white text-xs font-bold" title="Selected to sync">
                        ✓ Selected
                      </span>
                    ) : (
                      <span className="text-gray-400 text-xs font-bold" title="Click to select">
                        + Add
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Career Matches Grid */}
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200 shadow-sm space-y-6">
            <div>
              <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                <span>🎯</span>
                <span>Top Career Track Matches</span>
              </h2>
              <p className="text-xs sm:text-sm text-gray-500 mt-1">
                Based on technical skills extracted from your resume combined with your existing portfolio.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {analysis.matchedCareers.slice(0, 6).map((career) => (
                <div
                  key={career.title}
                  className={`p-5 rounded-2xl border transition flex flex-col justify-between gap-4 ${
                    career.targetGoal
                      ? "bg-blue-50/40 border-blue-300 ring-2 ring-blue-500/20"
                      : "bg-gray-50/60 border-gray-200 hover:border-gray-300"
                  }`}
                >
                  <div>
                    <div className="flex items-center justify-between gap-2 mb-2">
                      <span className="text-xs font-semibold px-2 py-0.5 rounded-md bg-gray-200 text-gray-700">
                        {career.category}
                      </span>
                      {career.targetGoal && (
                        <span className="text-[11px] font-extrabold text-blue-700 bg-blue-100 px-2 py-0.5 rounded-md">
                          ★ Target Goal
                        </span>
                      )}
                    </div>
                    <h3 className="text-base font-bold text-gray-900">{career.title}</h3>

                    {/* Match Score Bar */}
                    <div className="mt-3">
                      <div className="flex justify-between text-xs font-bold mb-1">
                        <span className="text-gray-600">Match Readiness</span>
                        <span
                          className={
                            career.matchScore >= 75
                              ? "text-emerald-700"
                              : career.matchScore >= 50
                              ? "text-blue-700"
                              : "text-amber-700"
                          }
                        >
                          {career.matchScore}%
                        </span>
                      </div>
                      <div className="w-full h-2 bg-gray-200 rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full transition-all duration-500 ${
                            career.matchScore >= 75
                              ? "bg-emerald-500"
                              : career.matchScore >= 50
                              ? "bg-blue-500"
                              : "bg-amber-500"
                          }`}
                          style={{ width: `${career.matchScore}%` }}
                        />
                      </div>
                    </div>

                    {/* Matched Skills */}
                    <div className="mt-4 space-y-2">
                      <p className="text-[11px] font-bold text-gray-500 uppercase tracking-wider">
                        Matched ({career.matchedSkills.length})
                      </p>
                      <div className="flex flex-wrap gap-1">
                        {career.matchedSkills.map((s) => (
                          <span
                            key={s}
                            className="text-[11px] font-medium px-2 py-0.5 bg-emerald-100/70 text-emerald-800 rounded-md"
                          >
                            ✓ {s}
                          </span>
                        ))}
                      </div>
                    </div>

                    {/* Missing Skills */}
                    {career.missingSkills.length > 0 && (
                      <div className="mt-3 space-y-2">
                        <p className="text-[11px] font-bold text-gray-500 uppercase tracking-wider">
                          Missing ({career.missingSkills.length})
                        </p>
                        <div className="flex flex-wrap gap-1">
                          {career.missingSkills.slice(0, 4).map((s) => (
                            <span
                              key={s}
                              className="text-[11px] font-medium px-2 py-0.5 bg-amber-100/70 text-amber-800 rounded-md"
                            >
                              + {s}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>

                  <Link
                    href="/roadmap"
                    className="w-full text-center py-2 text-xs font-bold text-gray-800 bg-white border border-gray-200 hover:bg-gray-100 rounded-xl transition"
                  >
                    View Roadmap →
                  </Link>
                </div>
              ))}
            </div>
          </div>

          {/* Skill Gap Analysis & Recommendations */}
          {analysis.skillGaps && (
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200 shadow-sm space-y-6">
              <div>
                <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                  <span>🚀</span>
                  <span>Target Career Skill Gaps ({analysis.skillGaps.targetCareer})</span>
                </h2>
                <p className="text-xs sm:text-sm text-gray-500 mt-1">
                  Bridging these high-priority missing skills will directly accelerate your target career readiness.
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {analysis.skillGaps.highPriorityMissing.map((missingSkill, idx) => (
                  <div
                    key={missingSkill}
                    className="p-5 rounded-2xl bg-amber-50/50 border border-amber-200 flex flex-col justify-between gap-4"
                  >
                    <div>
                      <div className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md bg-amber-200/60 text-amber-900 text-[11px] font-bold uppercase mb-2">
                        <span>⚠️ Priority #{idx + 1}</span>
                      </div>
                      <h3 className="text-base font-bold text-gray-900">{missingSkill}</h3>
                      <p className="text-xs text-gray-600 mt-1">
                        Required for {analysis.skillGaps.targetCareer}. Practice in the coding workspace or learn via milestone roadmap.
                      </p>
                    </div>

                    <div className="flex flex-col gap-2">
                      <Link
                        href="/roadmap"
                        className="w-full text-center py-2 text-xs font-bold text-white bg-black hover:bg-gray-800 rounded-xl transition"
                      >
                        Learn on Roadmap
                      </Link>
                      <Link
                        href="/practice"
                        className="w-full text-center py-2 text-xs font-bold text-gray-700 bg-white border border-gray-200 hover:bg-gray-50 rounded-xl transition"
                      >
                        Practice Challenge 💻
                      </Link>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Extracted Experience & Education Background */}
          {(analysis.extractedExperience.length > 0 ||
            analysis.extractedEducation.length > 0 ||
            analysis.extractedProjects.length > 0) && (
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200 shadow-sm space-y-6">
              <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                <span>📚</span>
                <span>Parsed Resume Background</span>
              </h2>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {analysis.extractedExperience.length > 0 && (
                  <div className="p-5 rounded-2xl bg-gray-50 border border-gray-100 space-y-3">
                    <p className="text-xs font-bold text-gray-700 uppercase tracking-wider">
                      Work Experience Highlights
                    </p>
                    <ul className="space-y-2 text-xs text-gray-600 list-disc list-inside">
                      {analysis.extractedExperience.map((item, i) => (
                        <li key={i} className="leading-relaxed">
                          {item}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {analysis.extractedEducation.length > 0 && (
                  <div className="p-5 rounded-2xl bg-gray-50 border border-gray-100 space-y-3">
                    <p className="text-xs font-bold text-gray-700 uppercase tracking-wider">
                      Academic Background
                    </p>
                    <ul className="space-y-2 text-xs text-gray-600 list-disc list-inside">
                      {analysis.extractedEducation.map((item, i) => (
                        <li key={i} className="leading-relaxed">
                          {item}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            </div>
          )}
        </>
      ) : (
        /* Empty State */
        <div className="bg-white rounded-3xl p-12 text-center border border-gray-200 shadow-sm space-y-4">
          <div className="w-16 h-16 rounded-3xl bg-gray-100 text-gray-400 mx-auto flex items-center justify-center text-3xl">
            📄
          </div>
          <h3 className="text-lg font-bold text-gray-900">No Resume Analyzed Yet</h3>
          <p className="text-xs sm:text-sm text-gray-500 max-w-md mx-auto">
            Upload your resume above to extract your skills, analyze gaps, and compare your background with 7 software career tracks.
          </p>
        </div>
      )}
    </div>
  );
}
