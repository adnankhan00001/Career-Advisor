"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import {
  mentorService,
  MentorApplicationDto,
  MentorProfileDto,
} from "@/lib/mentorService";
import { parseExpertiseAreas } from "@/lib/mentorRecommendation";
import { ApiError } from "@/lib/apiClient";

export default function MentorApplicationPage() {
  const router = useRouter();
  const { user } = useAuth();

  // Status check state
  const [existingProfile, setExistingProfile] = useState<MentorProfileDto | null>(null);
  const [isLoadingStatus, setIsLoadingStatus] = useState<boolean>(true);
  const [statusError, setStatusError] = useState<string | null>(null);

  // Form field state
  const [jobTitle, setJobTitle] = useState<string>("");
  const [company, setCompany] = useState<string>("");
  const [yearsOfExperience, setYearsOfExperience] = useState<string>("");
  const [expertiseAreas, setExpertiseAreas] = useState<string>("");
  const [maxMentees, setMaxMentees] = useState<number>(3);
  const [bio, setBio] = useState<string>("");
  const [linkedinUrl, setLinkedinUrl] = useState<string>("");
  const [githubUrl, setGithubUrl] = useState<string>("");
  const [codingProfileUrl, setCodingProfileUrl] = useState<string>("");
  const [portfolioUrl, setPortfolioUrl] = useState<string>("");

  // Submission state
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const [successBanner, setSuccessBanner] = useState<string | null>(null);

  // Check whether user already has an application or profile on mount
  const checkApplicationStatus = useCallback(async () => {
    setIsLoadingStatus(true);
    setStatusError(null);

    try {
      const profile = await mentorService.getMyProfile();
      setExistingProfile(profile);

      // If previous application was rejected, prefill the form so applicant can update & reapply
      if (profile.verificationStatus === "REJECTED") {
        setJobTitle(profile.jobTitle || "");
        setCompany(profile.company || "");
        setYearsOfExperience(profile.yearsOfExperience != null ? String(profile.yearsOfExperience) : "");
        setExpertiseAreas(profile.expertiseAreas || "");
        setMaxMentees(profile.maxMentees || 3);
        setBio(profile.bio || "");
        setLinkedinUrl(profile.linkedinUrl || "");
        setGithubUrl(profile.githubUrl || "");
        setCodingProfileUrl(profile.codingProfileUrl || "");
        setPortfolioUrl(profile.portfolioUrl || "");
      }
    } catch (err: any) {
      if (err instanceof ApiError && err.status === 404) {
        // HTTP 404 indicates user has not yet submitted an application
        setExistingProfile(null);
      } else if (err instanceof ApiError) {
        setStatusError(`Could not verify application status: ${err.message}`);
      } else {
        setStatusError("Unable to connect to mentorship services. Please check your connection.");
      }
    } finally {
      setIsLoadingStatus(false);
    }
  }, []);

  useEffect(() => {
    checkApplicationStatus();
  }, [checkApplicationStatus]);

  // Client-side form validation
  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!jobTitle.trim()) {
      errors.jobTitle = "Job title is required.";
    } else if (jobTitle.trim().length > 120) {
      errors.jobTitle = "Job title cannot exceed 120 characters.";
    }

    if (!company.trim()) {
      errors.company = "Company or organization is required.";
    } else if (company.trim().length > 120) {
      errors.company = "Company cannot exceed 120 characters.";
    }

    if (!yearsOfExperience.trim()) {
      errors.yearsOfExperience = "Years of experience is required.";
    } else {
      const expNum = Number(yearsOfExperience);
      if (isNaN(expNum) || expNum < 0) {
        errors.yearsOfExperience = "Years of experience must be a non-negative number.";
      } else if (expNum > 50) {
        errors.yearsOfExperience = "Please enter a realistic number of years (0–50).";
      }
    }

    if (!expertiseAreas.trim()) {
      errors.expertiseAreas = "At least one expertise area is required.";
    } else if (expertiseAreas.trim().length > 500) {
      errors.expertiseAreas = "Expertise areas cannot exceed 500 characters.";
    }

    if (maxMentees < 1 || maxMentees > 10) {
      errors.maxMentees = "Maximum mentees must be between 1 and 10.";
    }

    if (bio.trim().length > 2000) {
      errors.bio = "Bio cannot exceed 2000 characters.";
    }

    const validateRequiredUrlField = (val: string, fieldName: string, label: string) => {
      const trimmed = val.trim();
      if (!trimmed) {
        errors[fieldName] = `${label} is required.`;
      } else if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        errors[fieldName] = `${label} must start with http:// or https://`;
      } else if (trimmed.length > 255) {
        errors[fieldName] = `${label} cannot exceed 255 characters.`;
      }
    };

    const validateOptionalUrlField = (val: string, fieldName: string, label: string) => {
      const trimmed = val.trim();
      if (trimmed) {
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
          errors[fieldName] = `${label} must start with http:// or https://`;
        } else if (trimmed.length > 255) {
          errors[fieldName] = `${label} cannot exceed 255 characters.`;
        }
      }
    };

    validateRequiredUrlField(linkedinUrl, "linkedinUrl", "LinkedIn profile URL");
    validateRequiredUrlField(githubUrl, "githubUrl", "GitHub profile URL");
    validateRequiredUrlField(codingProfileUrl, "codingProfileUrl", "Coding practice profile URL");
    validateOptionalUrlField(portfolioUrl, "portfolioUrl", "Portfolio / personal website URL");

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Submit mentor application
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!validateForm()) {
      setFormError("Please resolve the highlighted validation errors before submitting.");
      return;
    }

    // Normalize and deduplicate expertise tags
    const parsedTags = parseExpertiseAreas(expertiseAreas);
    const cleanedExpertise = parsedTags.length > 0 ? parsedTags.join(", ") : expertiseAreas.trim();

    const payload: MentorApplicationDto = {
      jobTitle: jobTitle.trim(),
      company: company.trim(),
      yearsOfExperience: Math.floor(Number(yearsOfExperience)),
      expertiseAreas: cleanedExpertise,
      maxMentees: Number(maxMentees) || 3,
      bio: bio.trim() || undefined,
      linkedinUrl: linkedinUrl.trim(),
      githubUrl: githubUrl.trim(),
      codingProfileUrl: codingProfileUrl.trim(),
      portfolioUrl: portfolioUrl.trim() || undefined,
    };

    setIsSubmitting(true);

    try {
      const created = await mentorService.apply(payload);
      setExistingProfile(created);
      setSuccessBanner(
        existingProfile?.verificationStatus === "REJECTED"
          ? "Your updated application has been submitted successfully and is now under administrative review."
          : "Your mentor application has been submitted successfully! It is now pending administrative review."
      );
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (err: any) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setFormError(
            err.message ||
              "You have already submitted a mentor application or have an existing mentor profile."
          );
        } else if (err.status === 400) {
          setFormError(err.message || "Invalid application data. Please verify all fields.");
        } else {
          setFormError(err.message || `Submission failed with HTTP ${err.status}.`);
        }
      } else {
        setFormError("An unexpected network error occurred. Please try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // ------------------------------------------------------------------
  // RENDER: LOADING STATUS SKELETON
  // ------------------------------------------------------------------
  if (isLoadingStatus) {
    return (
      <div className="max-w-4xl mx-auto space-y-6 pb-12 animate-pulse">
        <div className="h-8 bg-gray-200 rounded w-1/3"></div>
        <div className="h-4 bg-gray-100 rounded w-2/3"></div>
        <div className="bg-white rounded-2xl border border-gray-200 p-8 space-y-6">
          <div className="h-6 bg-gray-200 rounded w-1/4"></div>
          <div className="space-y-3">
            <div className="h-10 bg-gray-100 rounded"></div>
            <div className="h-10 bg-gray-100 rounded"></div>
            <div className="h-24 bg-gray-100 rounded"></div>
          </div>
        </div>
      </div>
    );
  }

  // ------------------------------------------------------------------
  // RENDER: EXISTING APPLICATION VIEWS (APPLIED / APPROVED / SUSPENDED)
  // ------------------------------------------------------------------
  if (existingProfile && existingProfile.verificationStatus !== "REJECTED") {
    const isApplied = existingProfile.verificationStatus === "APPLIED";
    const isApproved = existingProfile.verificationStatus === "APPROVED";
    const isSuspended = existingProfile.verificationStatus === "SUSPENDED";
    const tags = parseExpertiseAreas(existingProfile.expertiseAreas);

    return (
      <div className="max-w-3xl mx-auto space-y-6 pb-12">
        {/* Navigation Breadcrumb */}
        <div className="flex items-center gap-2 text-xs font-semibold text-gray-500">
          <Link href="/mentorship" className="hover:text-indigo-600 transition">
            ← Back to Mentorship Hub
          </Link>
        </div>

        {/* Success Banner if just submitted */}
        {successBanner && (
          <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 text-sm flex items-start gap-2.5">
            <span className="text-emerald-600 font-bold">✓</span>
            <div>
              <p className="font-bold">Application Received</p>
              <p className="text-xs text-emerald-800">{successBanner}</p>
            </div>
          </div>
        )}

        {/* Status Card */}
        <div className="bg-white rounded-2xl border border-gray-200 shadow-xs p-6 sm:p-8 space-y-6">
          {/* Header & Status Badge */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-gray-100">
            <div className="space-y-1">
              <h1 className="text-xl sm:text-2xl font-extrabold text-gray-900">
                Mentor Application Status
              </h1>
              <p className="text-xs text-gray-500">
                Submitted by {user?.name || "Learner"} ({user?.email})
              </p>
            </div>

            <div>
              {isApplied && (
                <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-bold bg-amber-50 text-amber-800 border border-amber-200 shadow-xs">
                  <span>⏳</span>
                  <span>Under Administrative Review</span>
                </span>
              )}
              {isApproved && (
                <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-800 border border-emerald-200 shadow-xs">
                  <span>✅</span>
                  <span>Approved & Active Mentor</span>
                </span>
              )}
              {isSuspended && (
                <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-bold bg-gray-100 text-gray-800 border border-gray-300 shadow-xs">
                  <span>⚠️</span>
                  <span>Mentorship Suspended</span>
                </span>
              )}
            </div>
          </div>

          {/* Contextual Description */}
          {isApplied && (
            <div className="p-4 rounded-xl bg-amber-50/60 border border-amber-200 text-xs text-amber-900 space-y-1.5">
              <p className="font-bold">What happens next?</p>
              <p className="leading-relaxed">
                Your application is currently in the review queue. Platform administrators verify
                professional experience and technical expertise before approving mentor profiles to
                maintain mentorship quality. Once approved, your profile will immediately become
                discoverable to students in the Mentorship Hub.
              </p>
            </div>
          )}

          {isApproved && (
            <div className="p-4 rounded-xl bg-emerald-50/60 border border-emerald-200 text-xs text-emerald-900 space-y-1.5">
              <p className="font-bold">Congratulations!</p>
              <p className="leading-relaxed">
                Your mentor application has been approved. You are actively discoverable by students seeking
                guidance in your areas of expertise. Manage incoming requests anytime from your mentor dashboard.
              </p>
            </div>
          )}

          {isSuspended && (
            <div className="p-4 rounded-xl bg-gray-100 border border-gray-300 text-xs text-gray-800 space-y-1.5">
              <p className="font-bold">Mentorship Privileges Suspended</p>
              <p className="leading-relaxed">
                Your mentor profile is temporarily inactive. Mentees cannot submit new requests to your profile.
                Please reach out to support for resolution.
              </p>
            </div>
          )}

          {/* Submitted Profile Summary */}
          <div className="space-y-4 pt-2">
            <h2 className="text-sm font-bold text-gray-900 uppercase tracking-wider text-[11px]">
              Submitted Application Details
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div className="p-3.5 rounded-xl bg-gray-50 border border-gray-100 space-y-0.5">
                <span className="text-gray-400 font-medium">Job Title</span>
                <p className="font-bold text-gray-900">{existingProfile.jobTitle}</p>
              </div>

              <div className="p-3.5 rounded-xl bg-gray-50 border border-gray-100 space-y-0.5">
                <span className="text-gray-400 font-medium">Company</span>
                <p className="font-bold text-gray-900">{existingProfile.company}</p>
              </div>

              <div className="p-3.5 rounded-xl bg-gray-50 border border-gray-100 space-y-0.5">
                <span className="text-gray-400 font-medium">Years of Experience</span>
                <p className="font-bold text-gray-900">{existingProfile.yearsOfExperience} years</p>
              </div>

              <div className="p-3.5 rounded-xl bg-gray-50 border border-gray-100 space-y-0.5">
                <span className="text-gray-400 font-medium">Max Concurrent Mentees</span>
                <p className="font-bold text-gray-900">{existingProfile.maxMentees} mentees</p>
              </div>
            </div>

            {/* Expertise Tags */}
            <div className="space-y-1.5">
              <span className="text-xs font-semibold text-gray-700">Expertise Areas:</span>
              <div className="flex items-center gap-1.5 flex-wrap">
                {tags.map((tag) => (
                  <span
                    key={tag}
                    className="text-xs font-medium px-2.5 py-0.5 rounded-md bg-gray-100 text-gray-800 border border-gray-200"
                  >
                    {tag}
                  </span>
                ))}
              </div>
            </div>

            {/* Bio */}
            {existingProfile.bio && (
              <div className="space-y-1">
                <span className="text-xs font-semibold text-gray-700">Biography:</span>
                <p className="text-xs text-gray-600 p-3.5 rounded-xl bg-gray-50 border border-gray-100 leading-relaxed whitespace-pre-line">
                  {existingProfile.bio}
                </p>
              </div>
            )}

            {/* External Links */}
            <div className="space-y-1.5 pt-1">
              <span className="text-xs font-semibold text-gray-700">Profile & Portfolio Links:</span>
              <div className="flex flex-col sm:flex-row gap-3 text-xs">
                {existingProfile.linkedinUrl ? (
                  <a
                    href={existingProfile.linkedinUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-indigo-600 hover:underline flex items-center gap-1"
                  >
                    <span>🔗</span>
                    <span>LinkedIn Profile</span>
                  </a>
                ) : (
                  <span className="text-gray-400">LinkedIn: Not provided</span>
                )}

                {existingProfile.githubUrl ? (
                  <a
                    href={existingProfile.githubUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-indigo-600 hover:underline flex items-center gap-1"
                  >
                    <span>💻</span>
                    <span>GitHub Profile</span>
                  </a>
                ) : (
                  <span className="text-gray-400">GitHub: Not provided</span>
                )}

                {existingProfile.codingProfileUrl ? (
                  <a
                    href={existingProfile.codingProfileUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-amber-700 hover:underline flex items-center gap-1"
                  >
                    <span>⚡</span>
                    <span>Coding Profile</span>
                  </a>
                ) : (
                  <span className="text-gray-400">Coding Profile: Not provided</span>
                )}

                {existingProfile.portfolioUrl ? (
                  <a
                    href={existingProfile.portfolioUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-indigo-600 hover:underline flex items-center gap-1"
                  >
                    <span>🌐</span>
                    <span>Portfolio / Website</span>
                  </a>
                ) : (
                  <span className="text-gray-400">Portfolio: Not provided</span>
                )}
              </div>
            </div>
          </div>

          {/* Navigation Action Buttons */}
          <div className="pt-6 border-t border-gray-100 flex items-center justify-between">
            <Link
              href="/mentorship"
              className="px-4 py-2 text-xs font-semibold rounded-xl bg-gray-100 text-gray-700 hover:bg-gray-200 transition"
            >
              ← Return to Mentorship Hub
            </Link>

            {isApproved && (
              <Link
                href="/mentorship"
                className="px-4 py-2 text-xs font-semibold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 transition shadow-xs"
              >
                Browse Mentors & Mentees →
              </Link>
            )}
          </div>
        </div>
      </div>
    );
  }

  // ------------------------------------------------------------------
  // RENDER: APPLICATION FORM (FIRST-TIME APPLICANT)
  // ------------------------------------------------------------------
  return (
    <div className="max-w-3xl mx-auto space-y-6 pb-12">
      {/* Breadcrumb Navigation */}
      <div className="flex items-center gap-2 text-xs font-semibold text-gray-500">
        <Link href="/mentorship" className="hover:text-indigo-600 transition">
          Mentorship Hub
        </Link>
        <span>/</span>
        <span className="text-gray-900 font-bold">Apply to Become a Mentor</span>
      </div>

      {/* Header Banner */}
      <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-xs space-y-2">
        <div className="flex items-center gap-2.5">
          <span className="text-2xl">✨</span>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            {existingProfile?.verificationStatus === "REJECTED"
              ? "Update & Resubmit Mentor Application"
              : "Apply to Become a Mentor"}
          </h1>
        </div>
        <p className="text-sm text-gray-600 max-w-2xl leading-relaxed">
          Share your professional engineering journey, empower emerging developers, and help guide the
          next generation of tech talent. All mentor applications undergo administrative review.
        </p>
      </div>

      {/* Reapplication Banner for Previously Rejected Applicants */}
      {existingProfile?.verificationStatus === "REJECTED" && (
        <div className="p-5 rounded-2xl bg-amber-50 border border-amber-200 text-amber-950 space-y-2.5">
          <div className="flex items-center gap-2 font-bold text-sm text-amber-900">
            <span>⚠️</span>
            <span>Previous Application Status: Rejected</span>
          </div>
          {existingProfile.rejectionReason && (
            <div className="p-3.5 bg-white/90 rounded-xl border border-amber-200 text-xs">
              <span className="font-semibold text-gray-700 block mb-1">Feedback from Administrator:</span>
              <p className="text-gray-900 leading-relaxed font-medium">{existingProfile.rejectionReason}</p>
            </div>
          )}
          <p className="text-xs text-amber-900 leading-relaxed">
            Please address the constructive feedback above by updating your experience, expertise, or required profile URLs below, then submit your updated application for administrative review.
          </p>
        </div>
      )}

      {/* Global Status Error */}
      {statusError && (
        <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs">
          {statusError}
        </div>
      )}

      {/* Form Submission Error Banner */}
      {formError && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-900 text-xs space-y-1 animate-fade-in">
          <p className="font-bold flex items-center gap-1.5">
            <span>⚠️</span>
            <span>Unable to submit application</span>
          </p>
          <p>{formError}</p>
        </div>
      )}

      {/* Application Form */}
      <form
        onSubmit={handleSubmit}
        noValidate
        className="bg-white rounded-2xl border border-gray-200 shadow-xs p-6 sm:p-8 space-y-8"
      >
        {/* SECTION 1: Professional Background */}
        <div className="space-y-4">
          <div className="border-b border-gray-100 pb-2">
            <h2 className="text-base font-bold text-gray-900">
              1. Professional Background
            </h2>
            <p className="text-xs text-gray-500">
              Provide your current title, organization, and career longevity.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Job Title */}
            <div className="space-y-1.5">
              <label
                htmlFor="jobTitle"
                className="text-xs font-bold text-gray-800 flex items-center gap-1"
              >
                <span>Professional Job Title</span>
                <span className="text-red-500">*</span>
              </label>
              <input
                id="jobTitle"
                type="text"
                required
                maxLength={120}
                placeholder="e.g. Senior Software Engineer"
                value={jobTitle}
                onChange={(e) => {
                  setJobTitle(e.target.value);
                  if (validationErrors.jobTitle) {
                    setValidationErrors((prev) => ({ ...prev, jobTitle: "" }));
                  }
                }}
                className={`w-full px-3.5 py-2.5 rounded-xl text-xs sm:text-sm border transition text-gray-900 bg-white placeholder:text-gray-400 focus:outline-hidden focus:ring-2 focus:ring-indigo-100 ${
                  validationErrors.jobTitle
                    ? "border-rose-400 focus:border-rose-500"
                    : "border-gray-300 focus:border-indigo-600"
                }`}
              />
              {validationErrors.jobTitle && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {validationErrors.jobTitle}
                </p>
              )}
            </div>

            {/* Company */}
            <div className="space-y-1.5">
              <label
                htmlFor="company"
                className="text-xs font-bold text-gray-800 flex items-center gap-1"
              >
                <span>Current Company / Organization</span>
                <span className="text-red-500">*</span>
              </label>
              <input
                id="company"
                type="text"
                required
                maxLength={120}
                placeholder="e.g. Google, Microsoft, Startup"
                value={company}
                onChange={(e) => {
                  setCompany(e.target.value);
                  if (validationErrors.company) {
                    setValidationErrors((prev) => ({ ...prev, company: "" }));
                  }
                }}
                className={`w-full px-3.5 py-2.5 rounded-xl text-xs sm:text-sm border transition text-gray-900 bg-white placeholder:text-gray-400 focus:outline-hidden focus:ring-2 focus:ring-indigo-100 ${
                  validationErrors.company
                    ? "border-rose-400 focus:border-rose-500"
                    : "border-gray-300 focus:border-indigo-600"
                }`}
              />
              {validationErrors.company && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {validationErrors.company}
                </p>
              )}
            </div>

            {/* Years of Experience */}
            <div className="space-y-1.5 sm:col-span-2">
              <label
                htmlFor="yearsOfExperience"
                className="text-xs font-bold text-gray-800 flex items-center gap-1"
              >
                <span>Years of Professional Experience</span>
                <span className="text-red-500">*</span>
              </label>
              <input
                id="yearsOfExperience"
                type="number"
                min="0"
                max="50"
                required
                placeholder="e.g. 5"
                value={yearsOfExperience}
                onChange={(e) => {
                  setYearsOfExperience(e.target.value);
                  if (validationErrors.yearsOfExperience) {
                    setValidationErrors((prev) => ({ ...prev, yearsOfExperience: "" }));
                  }
                }}
                className={`w-full sm:w-1/2 px-3.5 py-2.5 rounded-xl text-xs sm:text-sm border transition text-gray-900 bg-white placeholder:text-gray-400 focus:outline-hidden focus:ring-2 focus:ring-indigo-100 ${
                  validationErrors.yearsOfExperience
                    ? "border-rose-400 focus:border-rose-500"
                    : "border-gray-300 focus:border-indigo-600"
                }`}
              />
              {validationErrors.yearsOfExperience ? (
                <p className="text-[11px] text-rose-600 font-medium">
                  {validationErrors.yearsOfExperience}
                </p>
              ) : (
                <p className="text-[11px] text-gray-400">
                  Total full-time professional experience in the technology industry.
                </p>
              )}
            </div>
          </div>
        </div>

        {/* SECTION 2: Expertise & Mentorship Capacity */}
        <div className="space-y-4">
          <div className="border-b border-gray-100 pb-2">
            <h2 className="text-base font-bold text-gray-900">
              2. Expertise & Capacity
            </h2>
            <p className="text-xs text-gray-500">
              Define the topics you can mentor in and how many mentees you can advise concurrently.
            </p>
          </div>

          <div className="space-y-4">
            {/* Expertise Areas */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="expertiseAreas"
                  className="text-xs font-bold text-gray-800 flex items-center gap-1"
                >
                  <span>Technical & Career Expertise Areas</span>
                  <span className="text-red-500">*</span>
                </label>
                <span className="text-[11px] text-gray-400">
                  {expertiseAreas.length}/500
                </span>
              </div>
              <input
                id="expertiseAreas"
                type="text"
                required
                maxLength={500}
                placeholder="e.g. Java, Spring Boot, Microservices, System Design, AWS, Docker"
                value={expertiseAreas}
                onChange={(e) => {
                  setExpertiseAreas(e.target.value);
                  if (validationErrors.expertiseAreas) {
                    setValidationErrors((prev) => ({ ...prev, expertiseAreas: "" }));
                  }
                }}
                className={`w-full px-3.5 py-2.5 rounded-xl text-xs sm:text-sm border transition text-gray-900 bg-white placeholder:text-gray-400 focus:outline-hidden focus:ring-2 focus:ring-indigo-100 ${
                  validationErrors.expertiseAreas
                    ? "border-rose-400 focus:border-rose-500"
                    : "border-gray-300 focus:border-indigo-600"
                }`}
              />
              {validationErrors.expertiseAreas ? (
                <p className="text-[11px] text-rose-600 font-medium">
                  {validationErrors.expertiseAreas}
                </p>
              ) : (
                <p className="text-[11px] text-gray-400">
                  Separate topics with commas. Used directly by the recommendation engine.
                </p>
              )}
            </div>

            {/* Max Mentees Capacity */}
            <div className="space-y-1.5">
              <label
                htmlFor="maxMentees"
                className="text-xs font-bold text-gray-800 flex items-center gap-1"
              >
                <span>Maximum Active Mentees (Capacity)</span>
                <span className="text-gray-400 font-normal">(Default: 3)</span>
              </label>
              <div className="flex items-center gap-3">
                <input
                  id="maxMentees"
                  type="number"
                  min="1"
                  max="10"
                  value={maxMentees}
                  onChange={(e) => {
                    const val = parseInt(e.target.value, 10);
                    setMaxMentees(isNaN(val) ? 1 : val);
                  }}
                  className="w-24 px-3 py-2 rounded-xl text-xs sm:text-sm border border-gray-300 bg-white text-gray-900 focus:outline-hidden focus:ring-2 focus:ring-indigo-100 focus:border-indigo-600 transition"
                />
                <span className="text-xs text-gray-500">
                  students concurrently (allowed range: 1 to 10)
                </span>
              </div>
              {validationErrors.maxMentees && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {validationErrors.maxMentees}
                </p>
              )}
            </div>
          </div>
        </div>

        {/* SECTION 3: Biography */}
        <div className="space-y-4">
          <div className="border-b border-gray-100 pb-2">
            <h2 className="text-base font-bold text-gray-900">
              3. Biography & Mentoring Approach
            </h2>
            <p className="text-xs text-gray-500">
              Share details about your professional trajectory and mentoring philosophy.
            </p>
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label htmlFor="bio" className="text-xs font-bold text-gray-800">
                Professional Bio & Guidance Philosophy
              </label>
              <span
                className={`text-[11px] ${
                  bio.length > 1900 ? "text-amber-600 font-bold" : "text-gray-400"
                }`}
              >
                {bio.length}/2000
              </span>
            </div>
            <textarea
              id="bio"
              rows={4}
              maxLength={2000}
              placeholder="Tell prospective mentees about your career milestones, what you enjoy guiding others through (e.g. system design interview prep, career transitions, clean code), and your mentoring style..."
              value={bio}
              onChange={(e) => {
                setBio(e.target.value);
                if (validationErrors.bio) {
                  setValidationErrors((prev) => ({ ...prev, bio: "" }));
                }
              }}
              className="w-full p-3.5 rounded-xl text-xs sm:text-sm border border-gray-300 bg-white text-gray-900 placeholder:text-gray-400 focus:outline-hidden focus:ring-2 focus:ring-indigo-100 focus:border-indigo-600 transition resize-y"
            />
            {validationErrors.bio && (
              <p className="text-[11px] text-rose-600 font-medium">
                {validationErrors.bio}
              </p>
            )}
          </div>
        </div>

        {/* SECTION 4: Professional & Profile Links */}
        <div className="space-y-4">
          <div className="border-b border-gray-100 pb-2">
            <h2 className="text-base font-bold text-gray-900">
              4. Profiles & Verification Links
            </h2>
            <p className="text-xs text-gray-500">
              URLs must start with http:// or https:// (max 255 characters).
            </p>
          </div>

          <div className="space-y-4">
            {/* LinkedIn */}
            <div className="space-y-1.5">
              <label htmlFor="linkedinUrl" className="text-xs font-bold text-gray-800 flex items-center gap-1">
                <span>LinkedIn Profile URL</span>
                <span className="text-red-500">*</span>
              </label>
              <input
                id="linkedinUrl"
                type="url"
                required
                maxLength={255}
                placeholder="https://linkedin.com/in/username"
                value={linkedinUrl}
                onChange={(e) => {
                  setLinkedinUrl(e.target.value);
                  if (validationErrors.linkedinUrl) {
                    setValidationErrors((prev) => ({ ...prev, linkedinUrl: "" }));
                  }
                }}
                className={`w-full px-3.5 py-2.5 rounded-xl text-xs sm:text-sm border transition text-gray-900 bg-white placeholder:text-gray-400 focus:outline-hidden focus:ring-2 focus:ring-indigo-100 ${
                  validationErrors.linkedinUrl
                    ? "border-rose-400 focus:border-rose-500"
                    : "border-gray-300 focus:border-indigo-600"
                }`}
              />
              {validationErrors.linkedinUrl && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {validationErrors.linkedinUrl}
                </p>
              )}
            </div>

            {/* GitHub */}
            <div className="space-y-1.5">
              <label htmlFor="githubUrl" className="text-xs font-bold text-gray-800 flex items-center gap-1">
                <span>GitHub Profile URL</span>
                <span className="text-red-500">*</span>
              </label>
              <input
                id="githubUrl"
                type="url"
                required
                maxLength={255}
                placeholder="https://github.com/username"
                value={githubUrl}
                onChange={(e) => {
                  setGithubUrl(e.target.value);
                  if (validationErrors.githubUrl) {
                    setValidationErrors((prev) => ({ ...prev, githubUrl: "" }));
                  }
                }}
                className={`w-full px-3.5 py-2.5 rounded-xl text-xs sm:text-sm border transition text-gray-900 bg-white placeholder:text-gray-400 focus:outline-hidden focus:ring-2 focus:ring-indigo-100 ${
                  validationErrors.githubUrl
                    ? "border-rose-400 focus:border-rose-500"
                    : "border-gray-300 focus:border-indigo-600"
                }`}
              />
              {validationErrors.githubUrl && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {validationErrors.githubUrl}
                </p>
              )}
            </div>

            {/* Coding Profile URL */}
            <div className="space-y-1.5">
              <label htmlFor="codingProfileUrl" className="text-xs font-bold text-gray-800 flex items-center gap-1">
                <span>Coding Practice Profile URL</span>
                <span className="text-red-500">*</span>
              </label>
              <input
                id="codingProfileUrl"
                type="url"
                required
                maxLength={255}
                placeholder="https://leetcode.com/username or https://hackerrank.com/username"
                value={codingProfileUrl}
                onChange={(e) => {
                  setCodingProfileUrl(e.target.value);
                  if (validationErrors.codingProfileUrl) {
                    setValidationErrors((prev) => ({ ...prev, codingProfileUrl: "" }));
                  }
                }}
                className={`w-full px-3.5 py-2.5 rounded-xl text-xs sm:text-sm border transition text-gray-900 bg-white placeholder:text-gray-400 focus:outline-hidden focus:ring-2 focus:ring-indigo-100 ${
                  validationErrors.codingProfileUrl
                    ? "border-rose-400 focus:border-rose-500"
                    : "border-gray-300 focus:border-indigo-600"
                }`}
              />
              {validationErrors.codingProfileUrl ? (
                <p className="text-[11px] text-rose-600 font-medium">
                  {validationErrors.codingProfileUrl}
                </p>
              ) : (
                <p className="text-[11px] text-gray-400">
                  Required coding-practice profile (e.g. LeetCode, HackerRank, Codeforces, Kaggle, CodeChef).
                </p>
              )}
            </div>

            {/* Portfolio / Personal Website */}
            <div className="space-y-1.5">
              <label htmlFor="portfolioUrl" className="text-xs font-bold text-gray-800">
                Portfolio or Personal Website (Optional)
              </label>
              <input
                id="portfolioUrl"
                type="url"
                maxLength={255}
                placeholder="https://yourportfolio.com"
                value={portfolioUrl}
                onChange={(e) => {
                  setPortfolioUrl(e.target.value);
                  if (validationErrors.portfolioUrl) {
                    setValidationErrors((prev) => ({ ...prev, portfolioUrl: "" }));
                  }
                }}
                className={`w-full px-3.5 py-2.5 rounded-xl text-xs sm:text-sm border transition text-gray-900 bg-white placeholder:text-gray-400 focus:outline-hidden focus:ring-2 focus:ring-indigo-100 ${
                  validationErrors.portfolioUrl
                    ? "border-rose-400 focus:border-rose-500"
                    : "border-gray-300 focus:border-indigo-600"
                }`}
              />
              {validationErrors.portfolioUrl ? (
                <p className="text-[11px] text-rose-600 font-medium">
                  {validationErrors.portfolioUrl}
                </p>
              ) : (
                <p className="text-[11px] text-gray-400">
                  Optional link to personal portfolio, engineering blog, or technical showcase.
                </p>
              )}
            </div>
          </div>
        </div>

        {/* Form Actions */}
        <div className="pt-6 border-t border-gray-100 flex items-center justify-between gap-4">
          <Link
            href="/mentorship"
            className="px-4 py-2.5 text-xs font-semibold rounded-xl text-gray-700 bg-gray-100 hover:bg-gray-200 transition"
          >
            Cancel
          </Link>

          <button
            type="submit"
            disabled={isSubmitting}
            className="px-6 py-2.5 text-xs sm:text-sm font-semibold rounded-xl text-white bg-indigo-600 hover:bg-indigo-700 transition disabled:opacity-50 flex items-center gap-2 cursor-pointer shadow-xs"
          >
            {isSubmitting ? (
              <>
                <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                <span>
                  {existingProfile?.verificationStatus === "REJECTED"
                    ? "Submitting Reapplication..."
                    : "Submitting Application..."}
                </span>
              </>
            ) : (
              <span>
                {existingProfile?.verificationStatus === "REJECTED"
                  ? "Submit Reapplication"
                  : "Submit Mentor Application"}
              </span>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
