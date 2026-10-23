"use client";

import { useEffect, useState, useRef } from "react";
import {
  onboardingService,
  OnboardingStateDto,
  CatalogCareer,
  CatalogTechnology,
  AssessmentSession,
  AssessmentQuestion,
  AssessmentReport,
  StudyWindow,
} from "@/lib/onboardingService";
import { assessmentService } from "@/lib/assessmentService";

interface OnboardingWizardProps {
  initialState: OnboardingStateDto;
  onComplete: () => void;
}

const STUDY_WINDOWS: { value: StudyWindow; label: string; description: string }[] = [
  { value: "MORNING", label: "Morning", description: "6:00 AM - 12:00 PM" },
  { value: "AFTERNOON", label: "Afternoon", description: "12:00 PM - 5:00 PM" },
  { value: "EVENING", label: "Evening", description: "5:00 PM - 11:00 PM" },
  { value: "FLEXIBLE", label: "Flexible", description: "Anytime that works" },
];

const DAYS_OF_WEEK = [
  { id: "MONDAY", label: "Mon" },
  { id: "TUESDAY", label: "Tue" },
  { id: "WEDNESDAY", label: "Wed" },
  { id: "THURSDAY", label: "Thu" },
  { id: "FRIDAY", label: "Fri" },
  { id: "SATURDAY", label: "Sat" },
  { id: "SUNDAY", label: "Sun" },
];

export default function OnboardingWizard({ initialState, onComplete }: OnboardingWizardProps) {
  const [stateDto, setStateDto] = useState<OnboardingStateDto>(initialState);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Catalog Careers
  const [careers, setCareers] = useState<CatalogCareer[]>([]);
  const [selectedCareerSlug, setSelectedCareerSlug] = useState<string>(
    initialState.selectedCareer?.slug || ""
  );

  // Known Technologies
  const [selectedTechSlugs, setSelectedTechSlugs] = useState<string[]>(
    initialState.selectedTechnologies?.map((t) => t.slug) || []
  );

  // Study Preferences
  const [dailyMinutes, setDailyMinutes] = useState<number>(
    initialState.studyPreferences?.dailyStudyMinutes || 60
  );
  const [studyWindow, setStudyWindow] = useState<StudyWindow>(
    initialState.studyPreferences?.preferredStudyWindow || "FLEXIBLE"
  );
  const [preferredDays, setPreferredDays] = useState<string[]>(
    initialState.studyPreferences?.preferredDays && initialState.studyPreferences.preferredDays.length > 0
      ? initialState.studyPreferences.preferredDays
      : ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
  );
  const [targetDate, setTargetDate] = useState<string>(
    initialState.studyPreferences?.targetCompletionDate || ""
  );

  // Assessment Session & Questions
  const [activeSession, setActiveSession] = useState<AssessmentSession | null>(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState<number>(0);
  const [selectedOption, setSelectedOption] = useState<number | null>(null);
  const [assessmentReport, setAssessmentReport] = useState<AssessmentReport | null>(null);
  const [remainingSeconds, setRemainingSeconds] = useState<number>(0);

  // Timer reference
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  // Fetch catalog careers on mount
  useEffect(() => {
    async function loadCareers() {
      try {
        const list = await onboardingService.getCatalogCareers();
        setCareers(list);
        if (!selectedCareerSlug && list.length > 0) {
          setSelectedCareerSlug(list[0].slug);
        }
      } catch (err: any) {
        console.error("Failed to load catalog careers:", err);
      }
    }
    loadCareers();
  }, []);

  // Sync state if session exists or if assessment in progress
  useEffect(() => {
    if (
      stateDto.state === "ASSESSMENT_IN_PROGRESS" &&
      stateDto.currentAssessmentSessionId &&
      !activeSession
    ) {
      resumeAssessment(stateDto.currentAssessmentSessionId);
    } else if (
      (stateDto.state === "ASSESSMENT_COMPLETED" || stateDto.state === "ASSESSMENT_REVIEWED") &&
      stateDto.currentAssessmentSessionId &&
      !assessmentReport
    ) {
      loadReport(stateDto.currentAssessmentSessionId);
    }
  }, [stateDto.state, stateDto.currentAssessmentSessionId]);

  // Countdown timer effect
  useEffect(() => {
    if (activeSession && activeSession.status === "IN_PROGRESS" && remainingSeconds > 0) {
      timerRef.current = setInterval(() => {
        setRemainingSeconds((prev) => {
          if (prev <= 1) {
            clearInterval(timerRef.current!);
            handleTimeExpired();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);

      return () => {
        if (timerRef.current) clearInterval(timerRef.current);
      };
    }
  }, [activeSession?.status, remainingSeconds]);

  // Determine current active wizard step (1 to 6)
  const getStepNumber = (): number => {
    switch (stateDto.state) {
      case "NOT_STARTED":
        return 1;
      case "CAREER_SELECTED":
        return 2;
      case "TECHNOLOGIES_SELECTED":
        return 3;
      case "STUDY_PREFERENCES_CONFIGURED":
      case "ASSESSMENT_IN_PROGRESS":
        return 4;
      case "ASSESSMENT_COMPLETED":
        return 5;
      case "ASSESSMENT_REVIEWED":
      case "ROADMAP_GENERATED":
        return 6;
      default:
        return 1;
    }
  };

  const currentStep = getStepNumber();

  // Helper to resume active assessment
  const resumeAssessment = async (sessionId: number) => {
    setLoading(true);
    try {
      const session = await assessmentService.getSessionState(sessionId);
      setActiveSession(session);
      setRemainingSeconds(session.remainingSeconds || 0);

      // Find first unanswered question
      const firstUnanswered = session.questions.findIndex((q) => q.status === "UNANSWERED");
      setCurrentQuestionIndex(firstUnanswered >= 0 ? firstUnanswered : 0);
      if (firstUnanswered >= 0 && session.questions[firstUnanswered]?.selectedOptionIndex !== null) {
        setSelectedOption(session.questions[firstUnanswered].selectedOptionIndex);
      } else {
        setSelectedOption(null);
      }
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to resume assessment session");
    } finally {
      setLoading(false);
    }
  };

  // Helper to load assessment report
  const loadReport = async (sessionId: number) => {
    setLoading(true);
    try {
      const report = await assessmentService.getReport(sessionId);
      setAssessmentReport(report);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to load assessment report");
    } finally {
      setLoading(false);
    }
  };

  const handleTimeExpired = async () => {
    if (activeSession) {
      try {
        const report = await assessmentService.submitAssessment(activeSession.sessionId);
        setAssessmentReport(report);
        const updatedState = await onboardingService.getState();
        setStateDto(updatedState);
      } catch (err: any) {
        console.error("Failed to finalize expired session:", err);
      }
    }
  };

  // Step 1: Select Career
  const handleSelectCareer = async () => {
    if (!selectedCareerSlug) {
      setErrorMsg("Please select a career track to continue.");
      return;
    }
    setErrorMsg(null);
    setLoading(true);
    try {
      const updated = await onboardingService.selectCareer(selectedCareerSlug);
      setStateDto(updated);
      setSelectedTechSlugs([]);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to select career track.");
    } finally {
      setLoading(false);
    }
  };

  // Step 2: Select Technologies
  const handleToggleTech = (slug: string) => {
    if (selectedTechSlugs.includes(slug)) {
      setSelectedTechSlugs(selectedTechSlugs.filter((s) => s !== slug));
    } else {
      setSelectedTechSlugs([...selectedTechSlugs, slug]);
    }
  };

  const handleConfirmTechnologies = async () => {
    if (selectedTechSlugs.length === 0) {
      setErrorMsg("Please select at least one technology you have worked with or know.");
      return;
    }
    setErrorMsg(null);
    setLoading(true);
    try {
      const updated = await onboardingService.selectTechnologies(selectedTechSlugs);
      setStateDto(updated);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to save selected technologies.");
    } finally {
      setLoading(false);
    }
  };

  // Step 3: Study Preferences
  const handleToggleDay = (day: string) => {
    if (preferredDays.includes(day)) {
      if (preferredDays.length === 1) {
        setErrorMsg("Please keep at least one study day selected.");
        return;
      }
      setPreferredDays(preferredDays.filter((d) => d !== day));
    } else {
      setPreferredDays([...preferredDays, day]);
    }
  };

  const handleSavePreferences = async () => {
    setErrorMsg(null);
    setLoading(true);
    try {
      const updated = await onboardingService.saveStudyPreferences({
        dailyStudyMinutes: dailyMinutes,
        preferredStudyWindow: studyWindow,
        preferredDays,
        targetCompletionDate: targetDate || null,
      });
      setStateDto(updated);

      // Immediately start the assessment
      const session = await onboardingService.startAssessment();
      setActiveSession(session);
      setRemainingSeconds(session.remainingSeconds || 1200);
      setCurrentQuestionIndex(0);
      setSelectedOption(null);

      const refreshed = await onboardingService.getState();
      setStateDto(refreshed);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to save study preferences.");
    } finally {
      setLoading(false);
    }
  };

  // Step 4: Assessment Runner Handlers
  const handleOptionSelect = (optionIndex: number) => {
    setSelectedOption(optionIndex);
  };

  const handleSubmitAnswer = async () => {
    if (!activeSession || selectedOption === null) return;
    const currentQ = activeSession.questions[currentQuestionIndex];
    if (!currentQ) return;

    setErrorMsg(null);
    setLoading(true);
    try {
      await assessmentService.submitAnswer(activeSession.sessionId, {
        sessionQuestionId: currentQ.sessionQuestionId,
        selectedOptionIndex: selectedOption,
      });

      // Refresh session
      const refreshedSession = await assessmentService.getSessionState(activeSession.sessionId);
      setActiveSession(refreshedSession);

      // Advance to next question or stay
      if (currentQuestionIndex + 1 < refreshedSession.questions.length) {
        const nextIndex = currentQuestionIndex + 1;
        setCurrentQuestionIndex(nextIndex);
        setSelectedOption(refreshedSession.questions[nextIndex]?.selectedOptionIndex ?? null);
      } else {
        // Last question answered - check if student wants to submit
        setSelectedOption(null);
      }
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to submit answer");
    } finally {
      setLoading(false);
    }
  };

  const handleSkipQuestion = async () => {
    if (!activeSession) return;
    const currentQ = activeSession.questions[currentQuestionIndex];
    if (!currentQ) return;

    setErrorMsg(null);
    setLoading(true);
    try {
      await assessmentService.skipQuestion(activeSession.sessionId, currentQ.sessionQuestionId);
      const refreshedSession = await assessmentService.getSessionState(activeSession.sessionId);
      setActiveSession(refreshedSession);

      if (currentQuestionIndex + 1 < refreshedSession.questions.length) {
        const nextIndex = currentQuestionIndex + 1;
        setCurrentQuestionIndex(nextIndex);
        setSelectedOption(refreshedSession.questions[nextIndex]?.selectedOptionIndex ?? null);
      }
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to skip question");
    } finally {
      setLoading(false);
    }
  };

  const handleFinishAssessment = async () => {
    if (!activeSession) return;
    setErrorMsg(null);
    setLoading(true);
    try {
      const report = await assessmentService.submitAssessment(activeSession.sessionId);
      setAssessmentReport(report);
      const updatedState = await onboardingService.getState();
      setStateDto(updatedState);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to submit assessment");
    } finally {
      setLoading(false);
    }
  };

  // Step 5: Report Review Confirm
  const handleConfirmReport = async () => {
    setErrorMsg(null);
    setLoading(true);
    try {
      const updated = await onboardingService.reviewReport();
      setStateDto(updated);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to confirm assessment review");
    } finally {
      setLoading(false);
    }
  };

  // Step 6: Complete & Proceed to Roadmap
  const handleProceedToRoadmap = async () => {
    setErrorMsg(null);
    setLoading(true);
    try {
      await onboardingService.complete();
      onComplete();
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to advance to roadmap");
    } finally {
      setLoading(false);
    }
  };

  // Reset Onboarding
  const handleReset = async () => {
    if (
      !window.confirm(
        "Are you sure you want to reconfigure your setup? This will allow you to choose a new career track and retake the assessment."
      )
    ) {
      return;
    }
    setErrorMsg(null);
    setLoading(true);
    try {
      const resetState = await onboardingService.reset();
      setStateDto(resetState);
      setActiveSession(null);
      setAssessmentReport(null);
      setSelectedTechSlugs([]);
      setCurrentQuestionIndex(0);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to reset onboarding");
    } finally {
      setLoading(false);
    }
  };

  const selectedCareerObj =
    careers.find((c) => c.slug === selectedCareerSlug) || stateDto.selectedCareer;

  // Format timer
  const formatTimer = (secs: number) => {
    const mins = Math.floor(secs / 60);
    const s = secs % 60;
    return `${mins}:${s < 10 ? "0" : ""}${s}`;
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8 pb-16">
      {/* Step Progress Tracker */}
      <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-blue-600 bg-blue-50 px-2.5 py-0.5 rounded-md">
              Step {currentStep} of 6
            </span>
            <h2 className="text-xl font-extrabold text-gray-900 mt-1">
              Personalized Roadmap Onboarding
            </h2>
          </div>
          {currentStep > 1 && (
            <button
              onClick={handleReset}
              className="text-xs font-semibold text-gray-500 hover:text-red-600 transition underline cursor-pointer"
            >
              Start Over
            </button>
          )}
        </div>

        {/* Progress Bar */}
        <div className="grid grid-cols-6 gap-2">
          {[
            { num: 1, label: "Career Track" },
            { num: 2, label: "Known Tech" },
            { num: 3, label: "Study Schedule" },
            { num: 4, label: "Assessment" },
            { num: 5, label: "Report" },
            { num: 6, label: "Roadmap" },
          ].map((step) => {
            const isCompleted = currentStep > step.num;
            const isCurrent = currentStep === step.num;
            return (
              <div key={step.num} className="space-y-1.5">
                <div
                  className={`h-2 rounded-full transition-all duration-300 ${
                    isCompleted
                      ? "bg-emerald-500"
                      : isCurrent
                      ? "bg-blue-600"
                      : "bg-gray-200"
                  }`}
                />
                <p
                  className={`text-[11px] font-medium hidden sm:block truncate ${
                    isCurrent
                      ? "text-blue-600 font-bold"
                      : isCompleted
                      ? "text-emerald-700"
                      : "text-gray-400"
                  }`}
                >
                  {step.label}
                </p>
              </div>
            );
          })}
        </div>
      </div>

      {/* Error Notice */}
      {errorMsg && (
        <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded-xl text-sm flex items-start justify-between gap-3 animate-fade-in">
          <span>{errorMsg}</span>
          <button
            onClick={() => setErrorMsg(null)}
            className="text-red-500 hover:text-red-800 font-bold"
          >
            ✕
          </button>
        </div>
      )}

      {/* STAGE 1: CAREER SELECTION */}
      {currentStep === 1 && (
        <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-sm space-y-6">
          <div>
            <h3 className="text-2xl font-bold text-gray-900">Choose Your Career Goal</h3>
            <p className="text-gray-600 text-sm mt-1">
              Select the target software engineering role you want to prepare for. Your curriculum,
              assessments, and milestone schedule will be tailored to this track.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {careers.map((career) => {
              const isSelected = selectedCareerSlug === career.slug;
              return (
                <div
                  key={career.slug}
                  onClick={() => setSelectedCareerSlug(career.slug)}
                  className={`p-5 rounded-xl border-2 transition-all cursor-pointer flex flex-col justify-between ${
                    isSelected
                      ? "border-blue-600 bg-blue-50/40 shadow-sm"
                      : "border-gray-200 hover:border-gray-300 bg-white"
                  }`}
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold px-2 py-0.5 rounded bg-gray-100 text-gray-700">
                        {career.category || "Engineering"}
                      </span>
                      <span className="text-xs text-gray-500 font-medium">
                        {career.duration || "6-9 months"}
                      </span>
                    </div>
                    <h4 className="text-lg font-bold text-gray-900">{career.title}</h4>
                    <p className="text-xs text-gray-600 line-clamp-2">{career.description}</p>
                  </div>

                  <div className="mt-4 pt-3 border-t border-gray-100 flex items-center justify-between">
                    <span className="text-xs font-semibold text-gray-500">
                      {career.technologies?.length || 0} Technologies Included
                    </span>
                    <span
                      className={`text-xs font-bold px-2.5 py-1 rounded-lg ${
                        isSelected
                          ? "bg-blue-600 text-white"
                          : "bg-gray-100 text-gray-700"
                      }`}
                    >
                      {isSelected ? "Selected ✓" : "Select"}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>

          <div className="flex justify-end pt-4">
            <button
              onClick={handleSelectCareer}
              disabled={loading || !selectedCareerSlug}
              className="py-3 px-6 bg-blue-600 text-white font-bold rounded-xl hover:bg-blue-700 transition disabled:opacity-50 cursor-pointer shadow-sm"
            >
              {loading ? "Saving Career..." : "Continue to Technologies →"}
            </button>
          </div>
        </div>
      )}

      {/* STAGE 2: KNOWN TECHNOLOGY SELECTION */}
      {currentStep === 2 && (
        <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-sm space-y-6">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="text-xs font-bold text-blue-600 uppercase">
                {stateDto.selectedCareer?.title}
              </span>
            </div>
            <h3 className="text-2xl font-bold text-gray-900">Which Technologies Do You Know?</h3>
            <p className="text-gray-600 text-sm mt-1">
              Select the technologies you have experience with. We will run a brief diagnostic
              assessment on these to establish your baseline proficiency and skip what you already master.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
            {stateDto.selectedCareer?.technologies?.map((tech) => {
              const isChecked = selectedTechSlugs.includes(tech.slug);
              return (
                <div
                  key={tech.slug}
                  onClick={() => handleToggleTech(tech.slug)}
                  className={`p-4 rounded-xl border-2 transition-all cursor-pointer flex items-center justify-between ${
                    isChecked
                      ? "border-blue-600 bg-blue-50/50 shadow-xs"
                      : "border-gray-200 hover:border-gray-300 bg-white"
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <span className="text-2xl">{tech.icon || "💻"}</span>
                    <div>
                      <h5 className="font-bold text-sm text-gray-900">{tech.name}</h5>
                      <span className="text-[11px] text-gray-500">{tech.category}</span>
                    </div>
                  </div>
                  <input
                    type="checkbox"
                    checked={isChecked}
                    onChange={() => {}}
                    className="h-4 w-4 text-blue-600 rounded border-gray-300 pointer-events-none"
                  />
                </div>
              );
            })}
          </div>

          <div className="bg-blue-50/70 border border-blue-200 p-4 rounded-xl flex items-center justify-between">
            <span className="text-xs font-semibold text-blue-900">
              Selected: <strong className="font-bold">{selectedTechSlugs.length}</strong> technologies
              ({selectedTechSlugs.length * 3}-{selectedTechSlugs.length * 6} adaptive questions)
            </span>
            <button
              onClick={() => {
                if (stateDto.selectedCareer?.technologies) {
                  setSelectedTechSlugs(stateDto.selectedCareer.technologies.map((t) => t.slug));
                }
              }}
              className="text-xs font-bold text-blue-700 hover:underline cursor-pointer"
            >
              Select All
            </button>
          </div>

          <div className="flex items-center justify-between pt-4">
            <button
              onClick={() => setStateDto({ ...stateDto, state: "NOT_STARTED" })}
              className="py-2.5 px-4 text-gray-600 font-semibold text-sm hover:text-gray-900 cursor-pointer"
            >
              ← Back to Career
            </button>
            <button
              onClick={handleConfirmTechnologies}
              disabled={loading || selectedTechSlugs.length === 0}
              className="py-3 px-6 bg-blue-600 text-white font-bold rounded-xl hover:bg-blue-700 transition disabled:opacity-50 cursor-pointer shadow-sm"
            >
              {loading ? "Validating..." : "Continue to Study Schedule →"}
            </button>
          </div>
        </div>
      )}

      {/* STAGE 3: STUDY SCHEDULE CONFIGURATION */}
      {currentStep === 3 && (
        <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-sm space-y-6">
          <div>
            <h3 className="text-2xl font-bold text-gray-900">Set Your Study Availability</h3>
            <p className="text-gray-600 text-sm mt-1">
              Configure how much time you can realistically dedicate each day. We use this to balance
              daily milestones and estimate your curriculum timeline.
            </p>
          </div>

          {/* Daily Study Minutes */}
          <div className="space-y-3">
            <label className="block text-sm font-bold text-gray-900">
              Daily Study Time:{" "}
              <span className="text-blue-600 font-extrabold">{dailyMinutes} minutes</span> (
              {(dailyMinutes / 60).toFixed(1)} hrs/day)
            </label>
            <input
              type="range"
              min={15}
              max={240}
              step={15}
              value={dailyMinutes}
              onChange={(e) => setDailyMinutes(Number(e.target.value))}
              className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-blue-600"
            />
            <div className="flex gap-2">
              {[30, 45, 60, 90, 120].map((preset) => (
                <button
                  key={preset}
                  type="button"
                  onClick={() => setDailyMinutes(preset)}
                  className={`text-xs font-bold px-3 py-1.5 rounded-lg border transition ${
                    dailyMinutes === preset
                      ? "bg-blue-600 text-white border-blue-600"
                      : "bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100"
                  }`}
                >
                  {preset}m
                </button>
              ))}
            </div>
          </div>

          {/* Preferred Study Window */}
          <div className="space-y-2">
            <label className="block text-sm font-bold text-gray-900">Preferred Study Window</label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {STUDY_WINDOWS.map((window) => {
                const isSelected = studyWindow === window.value;
                return (
                  <div
                    key={window.value}
                    onClick={() => setStudyWindow(window.value)}
                    className={`p-3.5 rounded-xl border-2 cursor-pointer transition text-center ${
                      isSelected
                        ? "border-blue-600 bg-blue-50/50"
                        : "border-gray-200 hover:border-gray-300 bg-white"
                    }`}
                  >
                    <div className="font-bold text-sm text-gray-900">{window.label}</div>
                    <div className="text-[11px] text-gray-500 mt-0.5">{window.description}</div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Preferred Study Days */}
          <div className="space-y-2">
            <label className="block text-sm font-bold text-gray-900">Preferred Study Days</label>
            <div className="flex flex-wrap gap-2">
              {DAYS_OF_WEEK.map((day) => {
                const isSelected = preferredDays.includes(day.id);
                return (
                  <button
                    key={day.id}
                    type="button"
                    onClick={() => handleToggleDay(day.id)}
                    className={`py-2 px-4 rounded-xl text-xs font-bold transition border ${
                      isSelected
                        ? "bg-blue-600 text-white border-blue-600 shadow-xs"
                        : "bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100"
                    }`}
                  >
                    {day.label}
                  </button>
                );
              })}
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Active: {preferredDays.length} days/week (~
              {((dailyMinutes * preferredDays.length) / 60).toFixed(1)} hrs/week)
            </p>
          </div>

          {/* Target Completion Date (Optional) */}
          <div className="space-y-2">
            <label className="block text-sm font-bold text-gray-900">
              Target Completion Date <span className="text-gray-400 font-normal">(Optional)</span>
            </label>
            <input
              type="date"
              value={targetDate}
              onChange={(e) => setTargetDate(e.target.value)}
              className="w-full sm:w-72 p-2.5 rounded-xl border border-gray-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex items-center justify-between pt-4">
            <button
              onClick={() => setStateDto({ ...stateDto, state: "CAREER_SELECTED" })}
              className="py-2.5 px-4 text-gray-600 font-semibold text-sm hover:text-gray-900 cursor-pointer"
            >
              ← Back to Technologies
            </button>
            <button
              onClick={handleSavePreferences}
              disabled={loading}
              className="py-3 px-6 bg-blue-600 text-white font-bold rounded-xl hover:bg-blue-700 transition disabled:opacity-50 cursor-pointer shadow-sm"
            >
              {loading ? "Starting Assessment..." : "Begin Diagnostic Assessment →"}
            </button>
          </div>
        </div>
      )}

      {/* STAGE 4: ADAPTIVE ASSESSMENT RUNNER */}
      {currentStep === 4 && activeSession && (
        <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-sm space-y-6">
          {/* Runner Header */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-gray-100">
            <div>
              <span className="text-xs uppercase font-extrabold text-blue-600 bg-blue-50 px-2 py-0.5 rounded">
                Live Diagnostic Session #{activeSession.sessionId}
              </span>
              <h3 className="text-xl font-extrabold text-gray-900 mt-1">
                Question {currentQuestionIndex + 1} of {activeSession.questions.length}
              </h3>
            </div>

            {/* Timer */}
            <div
              className={`flex items-center gap-2 px-3 py-1.5 rounded-xl border font-mono font-bold text-sm ${
                remainingSeconds < 300
                  ? "bg-red-50 text-red-700 border-red-200 animate-pulse"
                  : "bg-gray-50 text-gray-800 border-gray-200"
              }`}
            >
              <span>⏱</span>
              <span>{formatTimer(remainingSeconds)} remaining</span>
            </div>
          </div>

          {/* Current Question */}
          {activeSession.questions[currentQuestionIndex] ? (
            (() => {
              const q = activeSession.questions[currentQuestionIndex];
              return (
                <div className="space-y-6">
                  {/* Badges */}
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-xs font-bold px-2.5 py-1 rounded-md bg-blue-100 text-blue-800">
                      {q.technologyName}
                    </span>
                    <span className="text-xs font-medium px-2 py-0.5 rounded bg-gray-100 text-gray-700">
                      Topic: {q.topic}
                    </span>
                    <span
                      className={`text-xs font-bold px-2 py-0.5 rounded ${
                        q.difficulty === "BEGINNER"
                          ? "bg-emerald-50 text-emerald-700"
                          : q.difficulty === "INTERMEDIATE"
                          ? "bg-amber-50 text-amber-700"
                          : "bg-purple-50 text-purple-700"
                      }`}
                    >
                      {q.difficulty}
                    </span>
                  </div>

                  {/* Question Prompt */}
                  <p className="text-base sm:text-lg font-bold text-gray-900 leading-relaxed">
                    {q.questionText}
                  </p>

                  {/* Options */}
                  <div className="space-y-3">
                    {q.options.map((opt, idx) => {
                      const isSelected = selectedOption === idx;
                      return (
                        <div
                          key={idx}
                          onClick={() => handleOptionSelect(idx)}
                          className={`p-4 rounded-xl border-2 transition-all cursor-pointer flex items-center justify-between ${
                            isSelected
                              ? "border-blue-600 bg-blue-50/50 text-blue-900 font-semibold"
                              : "border-gray-200 hover:border-gray-300 text-gray-800 bg-white"
                          }`}
                        >
                          <div className="flex items-center gap-3">
                            <span
                              className={`w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold ${
                                isSelected
                                  ? "bg-blue-600 text-white"
                                  : "bg-gray-100 text-gray-600"
                              }`}
                            >
                              {String.fromCharCode(65 + idx)}
                            </span>
                            <span className="text-sm">{opt}</span>
                          </div>
                          {isSelected && <span className="text-blue-600 font-bold">✓</span>}
                        </div>
                      );
                    })}
                  </div>

                  {/* Navigation & Submission Controls */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-4 border-t border-gray-100">
                    <button
                      onClick={handleSkipQuestion}
                      disabled={loading}
                      className="py-2.5 px-4 bg-gray-100 text-gray-700 font-semibold text-xs sm:text-sm rounded-xl hover:bg-gray-200 transition cursor-pointer"
                    >
                      Skip Question ↷
                    </button>

                    <div className="flex items-center gap-3">
                      <button
                        onClick={handleSubmitAnswer}
                        disabled={loading || selectedOption === null}
                        className="py-2.5 px-6 bg-blue-600 text-white font-bold text-sm rounded-xl hover:bg-blue-700 transition disabled:opacity-50 cursor-pointer shadow-sm"
                      >
                        {loading ? "Submitting..." : "Submit Answer"}
                      </button>

                      <button
                        onClick={handleFinishAssessment}
                        disabled={loading}
                        className="py-2.5 px-4 bg-black text-white font-bold text-sm rounded-xl hover:bg-gray-800 transition cursor-pointer"
                      >
                        Finish & View Report →
                      </button>
                    </div>
                  </div>
                </div>
              );
            })()
          ) : (
            <div className="text-center py-12 space-y-4">
              <p className="text-gray-600 font-medium">All loaded questions have been answered!</p>
              <button
                onClick={handleFinishAssessment}
                className="py-3 px-6 bg-blue-600 text-white font-bold rounded-xl hover:bg-blue-700 transition cursor-pointer shadow-sm"
              >
                Generate Assessment Report →
              </button>
            </div>
          )}
        </div>
      )}

      {/* STAGE 5: ASSESSMENT REPORT REVIEW */}
      {currentStep === 5 && assessmentReport && (
        <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-sm space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-gray-100">
            <div>
              <span className="text-xs uppercase font-extrabold text-emerald-700 bg-emerald-50 px-2.5 py-0.5 rounded">
                Assessment Complete
              </span>
              <h3 className="text-2xl font-extrabold text-gray-900 mt-1">
                Your Baseline Proficiency Report
              </h3>
            </div>

            <div className="flex items-center gap-3">
              <div className="text-right">
                <div className="text-xs text-gray-500 font-medium">Overall Score</div>
                <div className="text-2xl font-black text-blue-600">
                  {assessmentReport.overallScore}%
                </div>
              </div>
              <span className="px-3 py-1 rounded-xl bg-blue-100 text-blue-800 font-bold text-sm">
                {assessmentReport.overallProficiency}
              </span>
            </div>
          </div>

          {/* Technology Results Grid */}
          <div className="space-y-4">
            <h4 className="text-sm font-bold uppercase tracking-wider text-gray-500">
              Technology Skill Breakdown
            </h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {assessmentReport.technologyResults.map((tr) => (
                <div
                  key={tr.technologySlug}
                  className="p-5 rounded-xl border border-gray-200 bg-gray-50/50 space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <h5 className="font-extrabold text-gray-900 text-base">
                      {tr.technologyName}
                    </h5>
                    <span
                      className={`text-xs font-bold px-2.5 py-0.5 rounded-full ${
                        tr.proficiencyLevel === "Advanced" || tr.proficiencyLevel === "Expert"
                          ? "bg-emerald-100 text-emerald-800"
                          : tr.proficiencyLevel === "Intermediate"
                          ? "bg-blue-100 text-blue-800"
                          : "bg-amber-100 text-amber-800"
                      }`}
                    >
                      {tr.proficiencyLevel} ({tr.score}%)
                    </span>
                  </div>

                  <div className="text-xs text-gray-600 flex items-center justify-between">
                    <span>
                      Correct: {tr.correctQuestions}/{tr.totalQuestions}
                    </span>
                    <span>Confidence: {tr.confidenceScore}%</span>
                  </div>

                  {/* Strengths */}
                  {tr.strengths && tr.strengths.length > 0 && (
                    <div className="text-xs">
                      <span className="font-bold text-emerald-700">✓ Strengths: </span>
                      <span className="text-gray-700">{tr.strengths.join(", ")}</span>
                    </div>
                  )}

                  {/* Weaknesses */}
                  {tr.weaknesses && tr.weaknesses.length > 0 && (
                    <div className="text-xs">
                      <span className="font-bold text-amber-700">▲ Areas to Grow: </span>
                      <span className="text-gray-700">{tr.weaknesses.join(", ")}</span>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Question Reviews */}
          <div className="space-y-4 pt-2">
            <h4 className="text-sm font-bold uppercase tracking-wider text-gray-500">
              Question Review & Explanations ({assessmentReport.questionReviews.length})
            </h4>
            <div className="space-y-3 max-h-96 overflow-y-auto pr-2">
              {assessmentReport.questionReviews.map((qr, idx) => (
                <div
                  key={idx}
                  className="p-4 rounded-xl border border-gray-200 bg-white text-xs space-y-2"
                >
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-gray-900">
                      Q{idx + 1}. {qr.technologyName} ({qr.topic})
                    </span>
                    <span
                      className={`font-bold px-2 py-0.5 rounded ${
                        qr.isCorrect
                          ? "bg-emerald-50 text-emerald-700"
                          : "bg-red-50 text-red-700"
                      }`}
                    >
                      {qr.isCorrect ? "Correct ✓" : "Incorrect ✕"}
                    </span>
                  </div>
                  <p className="text-gray-700 font-medium">{qr.questionText}</p>
                  <p className="text-gray-600 italic bg-gray-50 p-2.5 rounded-lg border border-gray-100">
                    💡 <strong>Explanation:</strong> {qr.explanation}
                  </p>
                </div>
              ))}
            </div>
          </div>

          <div className="flex justify-end pt-4">
            <button
              onClick={handleConfirmReport}
              disabled={loading}
              className="py-3 px-6 bg-blue-600 text-white font-bold rounded-xl hover:bg-blue-700 transition disabled:opacity-50 cursor-pointer shadow-sm"
            >
              {loading ? "Processing..." : "Confirm & Proceed to Roadmap Setup →"}
            </button>
          </div>
        </div>
      )}

      {/* STAGE 6: READY FOR ROADMAP GENERATION */}
      {currentStep === 6 && (
        <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-sm space-y-6 text-center">
          <div className="w-16 h-16 bg-emerald-100 text-emerald-700 rounded-full flex items-center justify-center text-3xl mx-auto">
            🎯
          </div>

          <div className="space-y-2 max-w-lg mx-auto">
            <h3 className="text-2xl font-black text-gray-900">Your Profile is Ready!</h3>
            <p className="text-gray-600 text-sm">
              We have calibrated your baseline skills for{" "}
              <strong className="text-gray-900 font-bold">
                {stateDto.selectedCareer?.title || "your selected career"}
              </strong>
              . You are ready to explore your personalized learning milestones.
            </p>
          </div>

          <div className="bg-gray-50 rounded-xl p-5 max-w-lg mx-auto text-left text-xs space-y-2 border border-gray-200">
            <div className="flex justify-between py-1 border-b border-gray-200">
              <span className="text-gray-500 font-semibold">Target Career Track:</span>
              <span className="font-bold text-gray-900">
                {stateDto.selectedCareer?.title}
              </span>
            </div>
            <div className="flex justify-between py-1 border-b border-gray-200">
              <span className="text-gray-500 font-semibold">Daily Study Commitment:</span>
              <span className="font-bold text-gray-900">
                {stateDto.studyPreferences?.dailyStudyMinutes} mins / day (
                {stateDto.studyPreferences?.preferredStudyWindow} window)
              </span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-gray-500 font-semibold">Assessed Technologies:</span>
              <span className="font-bold text-gray-900">
                {stateDto.selectedTechnologies?.map((t) => t.name).join(", ") || "General"}
              </span>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
            <button
              onClick={handleReset}
              className="py-3 px-6 bg-gray-100 text-gray-800 font-bold text-sm rounded-xl hover:bg-gray-200 transition cursor-pointer"
            >
              Reconfigure Track & Preferences
            </button>
            <button
              onClick={handleProceedToRoadmap}
              disabled={loading}
              className="py-3 px-8 bg-blue-600 text-white font-bold text-sm rounded-xl hover:bg-blue-700 transition disabled:opacity-50 cursor-pointer shadow-md"
            >
              {loading ? "Opening Roadmap..." : "Enter My Interactive Roadmap →"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
