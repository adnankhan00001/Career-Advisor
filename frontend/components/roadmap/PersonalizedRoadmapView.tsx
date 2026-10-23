"use client";

import { useState } from "react";
import {
  PersonalizedRoadmap,
  PersonalizedMilestone,
  PersonalizedStep,
  ProjectEvidence,
  RoadmapRevision,
  StepState,
  StepPriority,
  EvidenceType,
  AssessmentComparison,
  RoadmapUpdateProposal,
  ProposalSummary,
  RoadmapProposalChangeItem,
  personalizedRoadmapService,
} from "@/lib/personalizedRoadmapService";
import { assessmentService } from "@/lib/assessmentService";

interface PersonalizedRoadmapViewProps {
  initialRoadmap: PersonalizedRoadmap;
  onReconfigure: () => void;
  onRefresh: () => void;
}

export default function PersonalizedRoadmapView({
  initialRoadmap,
  onReconfigure,
  onRefresh,
}: PersonalizedRoadmapViewProps) {
  const [roadmap, setRoadmap] = useState<PersonalizedRoadmap>(initialRoadmap);
  const [togglingStepId, setTogglingStepId] = useState<number | null>(null);
  const [regenerating, setRegenerating] = useState(false);
  const [collapsedMilestones, setCollapsedMilestones] = useState<Record<number, boolean>>({});
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Modals state
  const [showAddMilestoneModal, setShowAddMilestoneModal] = useState(false);
  const [newMilestoneTitle, setNewMilestoneTitle] = useState("");
  const [newMilestoneTech, setNewMilestoneTech] = useState("");
  const [newMilestoneHours, setNewMilestoneHours] = useState(4.0);
  const [newMilestoneNotes, setNewMilestoneNotes] = useState("");
  const [newMilestoneSteps, setNewMilestoneSteps] = useState("");

  const [editingMilestone, setEditingMilestone] = useState<PersonalizedMilestone | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editHours, setEditHours] = useState(3.0);
  const [editNotes, setEditNotes] = useState("");

  const [evidenceMilestone, setEvidenceMilestone] = useState<PersonalizedMilestone | null>(null);
  const [evidenceStepId, setEvidenceStepId] = useState<number | undefined>(undefined);
  const [evidenceType, setEvidenceType] = useState<EvidenceType>("GITHUB_REPOSITORY");
  const [evidenceUrl, setEvidenceUrl] = useState("");
  const [evidenceNotes, setEvidenceNotes] = useState("");
  const [existingEvidences, setExistingEvidences] = useState<ProjectEvidence[]>([]);
  const [loadingEvidence, setLoadingEvidence] = useState(false);

  const [showScheduleModal, setShowScheduleModal] = useState(false);
  const [schedMinutes, setSchedMinutes] = useState(roadmap.dailyStudyMinutes || 60);
  const [schedDays, setSchedDays] = useState<string[]>(["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]);
  const [schedTargetDate, setSchedTargetDate] = useState(roadmap.targetCompletionDate || "");

  const [showRevisionsModal, setShowRevisionsModal] = useState(false);
  const [revisions, setRevisions] = useState<RoadmapRevision[]>([]);
  const [loadingRevisions, setLoadingRevisions] = useState(false);

  // Phase 6: Reassessment and Adaptive Updates State
  const [showReassessModal, setShowReassessModal] = useState(false);
  const [availableTechs, setAvailableTechs] = useState<{ slug: string; name: string }[]>([]);
  const [selectedTechSlugs, setSelectedTechSlugs] = useState<string[]>([]);
  const [reassessTimeLimit, setReassessTimeLimit] = useState(20);
  const [startingReassessment, setStartingReassessment] = useState(false);

  // Active in-modal assessment session
  const [activeSession, setActiveSession] = useState<any | null>(null);
  const [sessionQuestionIndex, setSessionQuestionIndex] = useState(0);
  const [selectedOption, setSelectedOption] = useState<number | null>(null);
  const [submittingAnswer, setSubmittingAnswer] = useState(false);
  const [finishingSession, setFinishingSession] = useState(false);

  // Comparison State
  const [showComparisonModal, setShowComparisonModal] = useState(false);
  const [comparison, setComparison] = useState<AssessmentComparison | null>(null);
  const [loadingComparison, setLoadingComparison] = useState(false);

  // Proposal State
  const [showProposalModal, setShowProposalModal] = useState(false);
  const [proposal, setProposal] = useState<RoadmapUpdateProposal | null>(null);
  const [generatingPreview, setGeneratingPreview] = useState(false);
  const [applyingProposal, setApplyingProposal] = useState(false);
  const [rejectingProposal, setRejectingProposal] = useState(false);
  const [loadingProposal, setLoadingProposal] = useState(false);

  // Proposal History State
  const [showProposalHistoryModal, setShowProposalHistoryModal] = useState(false);
  const [proposalHistory, setProposalHistory] = useState<ProposalSummary[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(false);

  const notifySuccess = (msg: string) => {
    setSuccessMsg(msg);
    setTimeout(() => setSuccessMsg(null), 4000);
  };

  const toggleMilestoneCollapse = (milestoneId: number) => {
    setCollapsedMilestones((prev) => ({
      ...prev,
      [milestoneId]: !prev[milestoneId],
    }));
  };

  const handleToggleStep = async (stepId: number) => {
    setTogglingStepId(stepId);
    setErrorMsg(null);
    try {
      const updated = await personalizedRoadmapService.toggleStep(stepId);
      setRoadmap(updated);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to update step progress");
    } finally {
      setTogglingStepId(null);
    }
  };

  const handleUpdateStepState = async (stepId: number, state: StepState) => {
    setErrorMsg(null);
    try {
      const updated = await personalizedRoadmapService.updateStepState(stepId, state);
      setRoadmap(updated);
      notifySuccess(`Step updated to ${state.replace(/_/g, " ")}`);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to update step state");
    }
  };

  const handleUpdateStepPriority = async (stepId: number, priority: StepPriority) => {
    setErrorMsg(null);
    try {
      const updated = await personalizedRoadmapService.updateStepPriority(stepId, priority);
      setRoadmap(updated);
      notifySuccess(`Priority set to ${priority.replace(/_/g, " ")}`);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to update step priority");
    }
  };

  const handleRegenerate = async () => {
    if (
      !window.confirm(
        "Regenerate your personalized roadmap? This will recalibrate your milestones with your latest assessments and preferences."
      )
    ) {
      return;
    }

    setRegenerating(true);
    setErrorMsg(null);
    try {
      const updated = await personalizedRoadmapService.generateRoadmap(true);
      setRoadmap(updated);
      notifySuccess("Roadmap successfully regenerated and recalibrated.");
      onRefresh();
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to regenerate roadmap");
    } finally {
      setRegenerating(false);
    }
  };

  const handleAddCustomMilestone = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMilestoneTitle.trim()) return;

    setErrorMsg(null);
    try {
      const stepTitles = newMilestoneSteps
        .split("\n")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);

      const updated = await personalizedRoadmapService.addCustomMilestone({
        title: newMilestoneTitle.trim(),
        technologyName: newMilestoneTech.trim() || undefined,
        estimatedHours: newMilestoneHours,
        customNotes: newMilestoneNotes.trim() || undefined,
        stepTitles: stepTitles.length > 0 ? stepTitles : undefined,
      });

      setRoadmap(updated);
      setShowAddMilestoneModal(false);
      setNewMilestoneTitle("");
      setNewMilestoneTech("");
      setNewMilestoneNotes("");
      setNewMilestoneSteps("");
      notifySuccess("Custom milestone added to your roadmap track!");
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to add custom milestone");
    }
  };

  const handleEditMilestoneSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingMilestone) return;

    setErrorMsg(null);
    try {
      const updated = await personalizedRoadmapService.updateMilestone(editingMilestone.id, {
        title: editingMilestone.isCustom ? editTitle.trim() : undefined,
        estimatedHours: editHours,
        customNotes: editNotes.trim() || undefined,
      });
      setRoadmap(updated);
      setEditingMilestone(null);
      notifySuccess("Milestone details updated.");
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to update milestone");
    }
  };

  const handleDeleteCustomMilestone = async (milestone: PersonalizedMilestone) => {
    if (!milestone.isCustom) {
      alert("Canonical required milestones cannot be deleted from the career track.");
      return;
    }
    if (!window.confirm(`Delete custom milestone "${milestone.title}"?`)) {
      return;
    }

    setErrorMsg(null);
    try {
      const updated = await personalizedRoadmapService.deleteCustomMilestone(milestone.id);
      setRoadmap(updated);
      notifySuccess("Custom milestone deleted.");
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to delete milestone");
    }
  };

  const handleSkipToggle = async (milestone: PersonalizedMilestone) => {
    const willSkip = !milestone.isSkipped;
    const action = willSkip ? "Skip" : "Restore";
    if (
      !window.confirm(
        `${action} milestone "${milestone.title}"? ${
          willSkip ? "Its hours will be excluded from your active study schedule." : "It will be re-added to your schedule."
        }`
      )
    ) {
      return;
    }

    setErrorMsg(null);
    try {
      const updated = await personalizedRoadmapService.skipMilestone(milestone.id, willSkip);
      setRoadmap(updated);
      notifySuccess(`Milestone ${willSkip ? "skipped" : "restored"}.`);
    } catch (err: any) {
      setErrorMsg(err.message || `Failed to ${action.toLowerCase()} milestone`);
    }
  };

  const handleMoveMilestone = async (index: number, direction: "up" | "down") => {
    const newIndex = direction === "up" ? index - 1 : index + 1;
    if (newIndex < 0 || newIndex >= roadmap.milestones.length) return;

    const list = [...roadmap.milestones];
    const temp = list[index];
    list[index] = list[newIndex];
    list[newIndex] = temp;

    const milestoneIds = list.map((m) => m.id);

    setErrorMsg(null);
    try {
      const updated = await personalizedRoadmapService.reorderMilestones(milestoneIds);
      setRoadmap(updated);
      notifySuccess("Milestone order updated successfully.");
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to reorder milestones (check prerequisite dependencies)");
    }
  };

  const openEvidenceModal = async (milestone: PersonalizedMilestone, stepId?: number) => {
    setEvidenceMilestone(milestone);
    setEvidenceStepId(stepId);
    setEvidenceUrl("");
    setEvidenceNotes("");
    setLoadingEvidence(true);
    setErrorMsg(null);

    try {
      const evs = await personalizedRoadmapService.getMilestoneEvidence(milestone.id);
      setExistingEvidences(evs);
    } catch {
      setExistingEvidences([]);
    } finally {
      setLoadingEvidence(false);
    }
  };

  const handleSubmitEvidence = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!evidenceMilestone) return;

    setErrorMsg(null);
    try {
      await personalizedRoadmapService.submitEvidence(evidenceMilestone.id, {
        stepId: evidenceStepId,
        evidenceType,
        url: evidenceUrl.trim() || undefined,
        notes: evidenceNotes.trim() || undefined,
      });

      // Refresh roadmap to show verification pending status if step updated
      const refreshed = await personalizedRoadmapService.getPersonalizedRoadmap();
      if (refreshed) setRoadmap(refreshed);

      const evs = await personalizedRoadmapService.getMilestoneEvidence(evidenceMilestone.id);
      setExistingEvidences(evs);
      setEvidenceUrl("");
      setEvidenceNotes("");
      notifySuccess("Project evidence submitted (marked as Self-Reported / Pending Review).");
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to submit project evidence");
    }
  };

  const handleUpdateSchedule = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    try {
      const updated = await personalizedRoadmapService.updateSchedule({
        dailyStudyMinutes: schedMinutes,
        preferredDays: schedDays.join(","),
        targetCompletionDate: schedTargetDate || undefined,
      });
      setRoadmap(updated);
      setShowScheduleModal(false);
      notifySuccess("Study schedule availability updated and milestones recalculated!");
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to update study schedule");
    }
  };

  const openRevisionsModal = async () => {
    setShowRevisionsModal(true);
    setLoadingRevisions(true);
    try {
      const revs = await personalizedRoadmapService.getRevisions();
      setRevisions(revs);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to load revision history");
    } finally {
      setLoadingRevisions(false);
    }
  };

  const openReassessModal = () => {
    const techMap = new Map<string, string>();
    for (const m of roadmap.milestones) {
      if (!m.isCustom && m.technologySlug && m.technologyName) {
        techMap.set(m.technologySlug, m.technologyName);
      }
    }
    const list = Array.from(techMap.entries()).map(([slug, name]) => ({ slug, name }));
    if (list.length === 0 && roadmap.careerSlug) {
      list.push({ slug: roadmap.careerSlug, name: roadmap.careerTitle });
    }
    setAvailableTechs(list);
    setSelectedTechSlugs(list.map((t) => t.slug));
    setShowReassessModal(true);
  };

  const handleToggleTechSlug = (slug: string) => {
    setSelectedTechSlugs((prev) =>
      prev.includes(slug) ? prev.filter((s) => s !== slug) : [...prev, slug]
    );
  };

  const handleStartReassessment = async () => {
    if (selectedTechSlugs.length === 0) {
      setErrorMsg("Please select at least one technology to reassess.");
      return;
    }
    setStartingReassessment(true);
    setErrorMsg(null);
    try {
      const session = await personalizedRoadmapService.startReassessment(
        selectedTechSlugs,
        undefined,
        reassessTimeLimit
      );
      setActiveSession(session);
      setSessionQuestionIndex(0);
      setSelectedOption(null);
      setShowReassessModal(false);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to start reassessment");
    } finally {
      setStartingReassessment(false);
    }
  };

  const handleAnswerQuestion = async () => {
    if (!activeSession || selectedOption === null) return;
    const currentQ = activeSession.questions?.[sessionQuestionIndex];
    if (!currentQ) return;

    setSubmittingAnswer(true);
    setErrorMsg(null);
    try {
      await assessmentService.submitAnswer(activeSession.sessionId, {
        sessionQuestionId: currentQ.sessionQuestionId,
        selectedOptionIndex: selectedOption,
      });
      const refreshed = await assessmentService.getSessionState(activeSession.sessionId);
      setActiveSession(refreshed);
      setSelectedOption(null);
      if (sessionQuestionIndex < (refreshed.questions?.length || 1) - 1) {
        setSessionQuestionIndex((prev) => prev + 1);
      }
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to submit answer");
    } finally {
      setSubmittingAnswer(false);
    }
  };

  const handleSkipQuestion = async () => {
    if (!activeSession) return;
    const currentQ = activeSession.questions?.[sessionQuestionIndex];
    if (!currentQ) return;

    setSubmittingAnswer(true);
    setErrorMsg(null);
    try {
      await assessmentService.skipQuestion(activeSession.sessionId, currentQ.sessionQuestionId);
      const refreshed = await assessmentService.getSessionState(activeSession.sessionId);
      setActiveSession(refreshed);
      setSelectedOption(null);
      if (sessionQuestionIndex < (refreshed.questions?.length || 1) - 1) {
        setSessionQuestionIndex((prev) => prev + 1);
      }
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to skip question");
    } finally {
      setSubmittingAnswer(false);
    }
  };

  const handleFinishReassessment = async () => {
    if (!activeSession) return;
    setFinishingSession(true);
    setErrorMsg(null);
    try {
      await personalizedRoadmapService.submitReassessment(activeSession.sessionId);
      const comp = await personalizedRoadmapService.getReassessmentComparison(activeSession.sessionId);
      setComparison(comp);
      setActiveSession(null);
      setShowComparisonModal(true);
      notifySuccess("Reassessment submitted! Viewing before-and-after comparison.");
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to finalize reassessment");
    } finally {
      setFinishingSession(false);
    }
  };

  const handleGeneratePreviewFromComparison = async () => {
    if (!comparison) return;
    setGeneratingPreview(true);
    setErrorMsg(null);
    try {
      const prop = await personalizedRoadmapService.generateRoadmapPreview(
        comparison.reassessmentSessionId,
        comparison.baselineSessionId
      );
      setProposal(prop);
      setShowComparisonModal(false);
      setShowProposalModal(true);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to generate roadmap update preview");
    } finally {
      setGeneratingPreview(false);
    }
  };

  const handleApplyProposal = async () => {
    if (!proposal) return;
    setApplyingProposal(true);
    setErrorMsg(null);
    try {
      const updated = await personalizedRoadmapService.applyProposal(proposal.id);
      setRoadmap(updated);
      setShowProposalModal(false);
      setProposal(null);
      notifySuccess("Adaptive roadmap update successfully applied! Your schedule and curriculum have been updated.");
      onRefresh();
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to apply roadmap update");
    } finally {
      setApplyingProposal(false);
    }
  };

  const handleRejectProposal = async () => {
    if (!proposal) return;
    setRejectingProposal(true);
    setErrorMsg(null);
    try {
      await personalizedRoadmapService.rejectProposal(proposal.id, "Rejected by student");
      setShowProposalModal(false);
      setProposal(null);
      notifySuccess("Roadmap update proposal rejected. Your active roadmap remains unchanged.");
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to reject proposal");
    } finally {
      setRejectingProposal(false);
    }
  };

  const openProposalHistoryModal = async () => {
    setShowProposalHistoryModal(true);
    setLoadingHistory(true);
    try {
      const list = await personalizedRoadmapService.getProposalHistory();
      setProposalHistory(list);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to load update proposals history");
    } finally {
      setLoadingHistory(false);
    }
  };

  const handleViewHistoricalProposal = async (propId: number) => {
    setLoadingProposal(true);
    setErrorMsg(null);
    try {
      const p = await personalizedRoadmapService.getProposal(propId);
      setProposal(p);
      setShowProposalHistoryModal(false);
      setShowProposalModal(true);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to load proposal details");
    } finally {
      setLoadingProposal(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Alert Notices */}
      {errorMsg && (
        <div className="bg-red-50 border border-red-200 text-red-800 p-4 rounded-xl text-sm flex items-center justify-between shadow-2xs">
          <div className="flex items-center gap-2">
            <span className="text-red-600 font-bold">⚠️ Error:</span>
            <span>{errorMsg}</span>
          </div>
          <button onClick={() => setErrorMsg(null)} className="text-red-500 hover:text-red-800 font-bold">
            ✕
          </button>
        </div>
      )}

      {successMsg && (
        <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 p-4 rounded-xl text-sm flex items-center justify-between shadow-2xs">
          <div className="flex items-center gap-2">
            <span className="text-emerald-600 font-bold">✓</span>
            <span>{successMsg}</span>
          </div>
          <button onClick={() => setSuccessMsg(null)} className="text-emerald-500 hover:text-emerald-800 font-bold">
            ✕
          </button>
        </div>
      )}

      {/* Target Date Realism Advisory */}
      {roadmap.isTargetDateRealistic === false && roadmap.projectedCompletionDate && (
        <div className="bg-amber-50 border border-amber-200 text-amber-900 p-4 rounded-xl text-xs sm:text-sm flex items-start gap-3 shadow-2xs">
          <span className="text-amber-600 text-base leading-none">⚠️</span>
          <div className="flex-1">
            <span className="font-bold">Target Date Advisory: </span>
            Your target completion date of <span className="font-semibold underline">{roadmap.targetCompletionDate}</span> requires a faster pace than your configured {roadmap.dailyStudyMinutes} min/day commitment. Based on curriculum hours, a realistic projected completion date is <span className="font-bold text-amber-900">{roadmap.projectedCompletionDate}</span>.
          </div>
          <button
            onClick={() => setShowScheduleModal(true)}
            className="text-xs bg-amber-200/70 hover:bg-amber-300 text-amber-900 font-bold px-2.5 py-1 rounded-md"
          >
            Adjust Schedule
          </button>
        </div>
      )}

      {/* Top Banner: Personalized Overview */}
      <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-sm space-y-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <div className="flex flex-wrap items-center gap-2 mb-2">
              <span className="text-xs uppercase tracking-wider text-blue-600 font-extrabold bg-blue-50 px-2.5 py-0.5 rounded-md border border-blue-100">
                ★ Personalized Adaptive Track
              </span>
              <span
                className={`text-xs font-bold px-2.5 py-0.5 rounded-md ${
                  roadmap.status === "COMPLETED"
                    ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                    : "bg-gray-100 text-gray-700"
                }`}
              >
                {roadmap.status}
              </span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900">
              {roadmap.careerTitle}
            </h1>
            <p className="text-gray-600 text-sm mt-1 max-w-2xl">
              Calibrated based on your diagnostic assessment and study capacity. Your tested skills
              have been credited, and identified growth areas are highlighted.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2 sm:gap-3">
            <button
              onClick={openReassessModal}
              className="py-2 px-3 sm:px-4 bg-indigo-600 text-white text-xs sm:text-sm font-bold rounded-xl hover:bg-indigo-700 transition shadow-xs cursor-pointer flex items-center gap-1.5"
            >
              <span>🎯</span>
              <span>Reassess Skills</span>
            </button>
            <button
              onClick={openProposalHistoryModal}
              className="py-2 px-3 sm:px-4 bg-indigo-50 text-indigo-700 text-xs sm:text-sm font-semibold rounded-xl hover:bg-indigo-100 transition border border-indigo-200 cursor-pointer"
            >
              📊 Updates
            </button>
            <button
              onClick={() => setShowScheduleModal(true)}
              className="py-2 px-3 sm:px-4 bg-gray-100 text-gray-800 text-xs sm:text-sm font-semibold rounded-xl hover:bg-gray-200 transition border border-gray-200 cursor-pointer"
            >
              📅 Schedule ({roadmap.dailyStudyMinutes}m/d)
            </button>
            <button
              onClick={() => setShowAddMilestoneModal(true)}
              className="py-2 px-3 sm:px-4 bg-emerald-50 text-emerald-700 text-xs sm:text-sm font-semibold rounded-xl hover:bg-emerald-100 transition border border-emerald-200 cursor-pointer"
            >
              ➕ Custom Milestone
            </button>
            <button
              onClick={openRevisionsModal}
              className="py-2 px-3 sm:px-4 bg-purple-50 text-purple-700 text-xs sm:text-sm font-semibold rounded-xl hover:bg-purple-100 transition border border-purple-200 cursor-pointer"
            >
              📜 History
            </button>
            <button
              onClick={handleRegenerate}
              disabled={regenerating}
              className="py-2 px-3 sm:px-4 bg-gray-100 text-gray-800 text-xs sm:text-sm font-semibold rounded-xl hover:bg-gray-200 transition border border-gray-200 cursor-pointer disabled:opacity-50"
            >
              {regenerating ? "Recalibrating..." : "🔄 Regenerate"}
            </button>
            <button
              onClick={onReconfigure}
              className="py-2 px-3 sm:px-4 bg-blue-50 text-blue-700 text-xs sm:text-sm font-semibold rounded-xl hover:bg-blue-100 transition border border-blue-200 cursor-pointer"
            >
              Reconfigure ⚙
            </button>
          </div>
        </div>

        {/* Progress Bar & Key Metrics */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-4 border-t border-gray-100">
          <div className="p-4 bg-gray-50/80 rounded-xl border border-gray-100">
            <div className="text-xs text-gray-500 font-medium">Curriculum Progress</div>
            <div className="text-2xl font-black text-gray-900 mt-0.5">
              {roadmap.progressPercentage}%
            </div>
            <div className="text-[11px] text-gray-500 mt-1">
              {roadmap.completedStepsCount} of {roadmap.totalStepsCount} topics
            </div>
          </div>

          <div className="p-4 bg-gray-50/80 rounded-xl border border-gray-100">
            <div className="text-xs text-gray-500 font-medium">Tested Out ✓</div>
            <div className="text-2xl font-black text-emerald-600 mt-0.5">
              {roadmap.testedOutCount}
            </div>
            <div className="text-[11px] text-emerald-700 font-semibold mt-1">
              Bypassed via diagnostic
            </div>
          </div>

          <div className="p-4 bg-gray-50/80 rounded-xl border border-gray-100">
            <div className="text-xs text-gray-500 font-medium">Focus Areas ⚡</div>
            <div className="text-2xl font-black text-amber-600 mt-0.5">
              {roadmap.focusAreasCount}
            </div>
            <div className="text-[11px] text-amber-700 font-semibold mt-1">
              Targeted skill gaps
            </div>
          </div>

          <div className="p-4 bg-gray-50/80 rounded-xl border border-gray-100">
            <div className="text-xs text-gray-500 font-medium">Required Effort</div>
            <div className="text-2xl font-black text-blue-600 mt-0.5">
              {roadmap.totalEstimatedHours?.toFixed(1) || "0.0"}h
            </div>
            <div className="text-[11px] text-gray-500 mt-1">
              Target: {roadmap.targetCompletionDate || "Self-Paced"}
            </div>
          </div>
        </div>

        {/* Linear Progress Bar */}
        <div className="space-y-1.5">
          <div className="w-full bg-gray-200 h-2.5 rounded-full overflow-hidden">
            <div
              className="bg-blue-600 h-2.5 rounded-full transition-all duration-500"
              style={{ width: `${roadmap.progressPercentage}%` }}
            />
          </div>
        </div>
      </div>

      {/* Milestones List */}
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h3 className="text-xl font-extrabold text-gray-900">
            Learning Milestones ({roadmap.milestones?.length || 0})
          </h3>
          <span className="text-xs text-gray-500">
            Use ▲/▼ to reorder (prerequisites validated automatically)
          </span>
        </div>

        {roadmap.milestones && roadmap.milestones.length > 0 ? (
          roadmap.milestones.map((milestone, idx) => {
            const isCollapsed = !!collapsedMilestones[milestone.id];
            const completedCount = milestone.steps.filter((s) => s.isCompleted).length;
            const totalCount = milestone.steps.length;

            return (
              <div
                key={milestone.id}
                className={`bg-white rounded-2xl border transition overflow-hidden shadow-xs ${
                  milestone.isSkipped
                    ? "border-dashed border-gray-300 opacity-60 bg-gray-50/50"
                    : "border-gray-200"
                }`}
              >
                {/* Milestone Header */}
                <div className="p-5 sm:p-6 bg-white hover:bg-gray-50/60 transition flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-gray-100">
                  <div
                    onClick={() => toggleMilestoneCollapse(milestone.id)}
                    className="flex-1 space-y-1 cursor-pointer"
                  >
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-xs font-bold px-2.5 py-0.5 rounded-md bg-blue-50 text-blue-700 border border-blue-100">
                        Week {milestone.targetWeekNumber}
                      </span>
                      {milestone.technologyName && (
                        <span className="text-xs font-semibold px-2 py-0.5 rounded bg-gray-100 text-gray-700">
                          {milestone.technologyName}
                        </span>
                      )}
                      {milestone.isCustom && (
                        <span className="text-xs font-bold px-2 py-0.5 rounded bg-purple-50 text-purple-700 border border-purple-200">
                          Custom ✦
                        </span>
                      )}
                      <span
                        className={`text-xs font-bold px-2.5 py-0.5 rounded-full ${
                          milestone.isSkipped
                            ? "bg-gray-200 text-gray-700"
                            : milestone.status === "COMPLETED"
                            ? "bg-emerald-100 text-emerald-800"
                            : milestone.status === "IN_PROGRESS"
                            ? "bg-blue-100 text-blue-800"
                            : "bg-gray-100 text-gray-600"
                        }`}
                      >
                        {milestone.isSkipped
                          ? "Skipped (Excluded)"
                          : milestone.status === "COMPLETED"
                          ? "Completed ✓"
                          : milestone.status === "IN_PROGRESS"
                          ? "In Progress"
                          : "Upcoming"}
                      </span>
                    </div>

                    <h4 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                      <span>{milestone.title}</span>
                      {milestone.customNotes && (
                        <span className="text-xs font-normal text-gray-500 italic">
                          ({milestone.customNotes})
                        </span>
                      )}
                    </h4>
                  </div>

                  {/* Actions & Ordering */}
                  <div className="flex flex-wrap items-center gap-2 sm:gap-3">
                    <div className="text-right text-xs mr-2">
                      <div className="font-bold text-gray-900">
                        {completedCount} / {totalCount} done
                      </div>
                      <div className="text-gray-500">
                        ~{milestone.estimatedHours?.toFixed(1) || "2.0"} hrs
                      </div>
                    </div>

                    {/* Reorder Buttons */}
                    <div className="flex items-center border border-gray-200 rounded-lg overflow-hidden bg-gray-50">
                      <button
                        onClick={() => handleMoveMilestone(idx, "up")}
                        disabled={idx === 0}
                        title="Move Milestone Earlier"
                        className="p-1.5 hover:bg-gray-200 text-gray-700 disabled:opacity-30 disabled:cursor-not-allowed text-xs font-bold"
                      >
                        ▲
                      </button>
                      <button
                        onClick={() => handleMoveMilestone(idx, "down")}
                        disabled={idx === roadmap.milestones.length - 1}
                        title="Move Milestone Later"
                        className="p-1.5 hover:bg-gray-200 text-gray-700 disabled:opacity-30 disabled:cursor-not-allowed text-xs font-bold"
                      >
                        ▼
                      </button>
                    </div>

                    {/* Evidence Button */}
                    <button
                      onClick={() => openEvidenceModal(milestone)}
                      title="Submit or View Project Evidence"
                      className="px-2.5 py-1 text-xs font-semibold rounded-lg bg-indigo-50 text-indigo-700 hover:bg-indigo-100 transition border border-indigo-200"
                    >
                      📁 Evidence
                    </button>

                    {/* Edit Milestone */}
                    <button
                      onClick={() => {
                        setEditingMilestone(milestone);
                        setEditTitle(milestone.title);
                        setEditHours(milestone.estimatedHours || 3.0);
                        setEditNotes(milestone.customNotes || "");
                      }}
                      title="Edit Milestone"
                      className="px-2.5 py-1 text-xs font-semibold rounded-lg bg-gray-100 text-gray-700 hover:bg-gray-200 transition"
                    >
                      ✏️ Edit
                    </button>

                    {/* Skip / Restore */}
                    <button
                      onClick={() => handleSkipToggle(milestone)}
                      title={milestone.isSkipped ? "Restore Milestone" : "Skip Milestone"}
                      className={`px-2.5 py-1 text-xs font-semibold rounded-lg transition ${
                        milestone.isSkipped
                          ? "bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200"
                          : "bg-amber-50 text-amber-700 hover:bg-amber-100 border border-amber-200"
                      }`}
                    >
                      {milestone.isSkipped ? "Restore" : "Skip"}
                    </button>

                    {/* Delete Custom Milestone */}
                    {milestone.isCustom && (
                      <button
                        onClick={() => handleDeleteCustomMilestone(milestone)}
                        title="Delete Custom Milestone"
                        className="px-2 py-1 text-xs font-bold text-red-600 hover:bg-red-50 rounded-lg transition"
                      >
                        🗑️
                      </button>
                    )}

                    {/* Collapse chevron */}
                    <button
                      onClick={() => toggleMilestoneCollapse(milestone.id)}
                      className="text-gray-400 hover:text-gray-700 font-bold text-sm px-1"
                    >
                      {isCollapsed ? "▼" : "▲"}
                    </button>
                  </div>
                </div>

                {/* Steps List */}
                {!isCollapsed && (
                  <div className="divide-y divide-gray-100 p-2 sm:p-4 bg-gray-50/30">
                    {milestone.steps.map((step) => {
                      const isToggling = togglingStepId === step.id;
                      const currentState = step.state || (step.isCompleted ? "COMPLETED" : "NOT_STARTED");

                      return (
                        <div
                          key={step.id}
                          className={`p-3.5 sm:p-4 rounded-xl transition flex flex-col sm:flex-row sm:items-start justify-between gap-4 ${
                            step.isCompleted
                              ? "bg-white/80 opacity-90"
                              : "bg-white shadow-2xs hover:bg-blue-50/20"
                          }`}
                        >
                          {/* Step Content */}
                          <div className="flex items-start gap-3 flex-1">
                            <input
                              type="checkbox"
                              checked={step.isCompleted}
                              disabled={isToggling}
                              onChange={() => handleToggleStep(step.id)}
                              className="mt-1 h-5 w-5 text-blue-600 rounded border-gray-300 focus:ring-blue-500 cursor-pointer"
                            />

                            <div className="space-y-1 flex-1">
                              <div className="flex flex-wrap items-center gap-2">
                                <h5
                                  className={`text-sm font-bold ${
                                    step.isCompleted
                                      ? "line-through text-gray-500"
                                      : "text-gray-900"
                                  }`}
                                >
                                  {step.title}
                                </h5>

                                {/* Badges */}
                                {step.isTestedOut && (
                                  <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
                                    Tested Out ✓
                                  </span>
                                )}
                                {step.priority === "HIGH_PRIORITY" && (
                                  <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800">
                                    Focus Area ⚡
                                  </span>
                                )}
                                {step.priority === "CORE" && !step.isTestedOut && (
                                  <span className="text-[11px] font-semibold px-2 py-0.5 rounded bg-blue-50 text-blue-700">
                                    Core
                                  </span>
                                )}
                                {currentState === "VERIFICATION_PENDING" && (
                                  <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-800">
                                    Evidence Submitted ⏳
                                  </span>
                                )}
                                {step.evidenceStatus === "INSUFFICIENT_EVIDENCE" && (
                                  <span className="text-[11px] font-medium px-2 py-0.5 rounded bg-amber-50 text-amber-700 border border-amber-200">
                                    Needs Diagnostic
                                  </span>
                                )}
                                {step.evidenceStatus === "NOT_ASSESSED" && (
                                  <span className="text-[11px] font-medium px-2 py-0.5 rounded bg-gray-100 text-gray-500">
                                    Unassessed
                                  </span>
                                )}

                                <span className="text-[11px] font-medium text-gray-400">
                                  • {step.estimatedMinutes} mins
                                </span>
                                <span className="text-[11px] text-gray-400">
                                  • {step.level}
                                </span>
                              </div>

                              {step.description && (
                                <p className="text-xs text-gray-600 leading-relaxed">
                                  {step.description}
                                </p>
                              )}

                              {step.isTestedOut && step.assessmentScore && (
                                <p className="text-[11px] text-emerald-700 font-medium italic">
                                  ★ Credited from diagnostic assessment ({step.assessmentScore}% score)
                                </p>
                              )}
                            </div>
                          </div>

                          {/* Step Granular Controls */}
                          <div className="flex flex-wrap items-center gap-2 text-xs">
                            {/* Learning State Dropdown */}
                            <select
                              value={currentState}
                              onChange={(e) => handleUpdateStepState(step.id, e.target.value as StepState)}
                              className="bg-gray-50 border border-gray-200 rounded-lg px-2 py-1 text-gray-700 font-medium"
                            >
                              <option value="NOT_STARTED">Not Started</option>
                              <option value="IN_PROGRESS">In Progress</option>
                              <option value="VERIFICATION_PENDING">Pending Evidence</option>
                              <option value="COMPLETED">Completed</option>
                            </select>

                            {/* Priority Dropdown */}
                            <select
                              value={step.priority}
                              onChange={(e) => handleUpdateStepPriority(step.id, e.target.value as StepPriority)}
                              className="bg-gray-50 border border-gray-200 rounded-lg px-2 py-1 text-gray-700 font-medium"
                            >
                              <option value="CORE">Core</option>
                              <option value="HIGH_PRIORITY">High Priority</option>
                              <option value="RECOMMENDED">Recommended</option>
                              <option value="TESTED_OUT">Tested Out</option>
                            </select>

                            {/* Attach Evidence for Step */}
                            <button
                              onClick={() => openEvidenceModal(milestone, step.id)}
                              title="Attach Evidence for this Step"
                              className="px-2 py-1 bg-indigo-50 text-indigo-700 hover:bg-indigo-100 rounded-lg border border-indigo-200"
                            >
                              📁
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })
        ) : (
          <div className="bg-white p-12 text-center rounded-2xl border border-gray-200 space-y-3">
            <p className="text-gray-600 font-medium">No personalized milestones found.</p>
            <button
              onClick={handleRegenerate}
              className="py-2.5 px-6 bg-blue-600 text-white text-sm font-bold rounded-xl hover:bg-blue-700 transition"
            >
              Generate Personalized Roadmap
            </button>
          </div>
        )}
      </div>

      {/* ================= MODAL: ADD CUSTOM MILESTONE ================= */}
      {showAddMilestoneModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 space-y-5 shadow-2xl">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-lg font-bold text-gray-900">Add Custom Learning Milestone</h3>
              <button
                onClick={() => setShowAddMilestoneModal(false)}
                className="text-gray-400 hover:text-gray-700 font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleAddCustomMilestone} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">
                  Milestone Title *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Docker & Cloud Deployment"
                  value={newMilestoneTitle}
                  onChange={(e) => setNewMilestoneTitle(e.target.value)}
                  className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase mb-1">
                    Technology / Domain
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Docker"
                    value={newMilestoneTech}
                    onChange={(e) => setNewMilestoneTech(e.target.value)}
                    className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase mb-1">
                    Estimated Hours
                  </label>
                  <input
                    type="number"
                    step="0.5"
                    min="0.5"
                    max="100"
                    value={newMilestoneHours}
                    onChange={(e) => setNewMilestoneHours(parseFloat(e.target.value) || 2)}
                    className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">
                  Custom Notes (Optional)
                </label>
                <input
                  type="text"
                  placeholder="e.g. Focus on containerizing Spring Boot microservices"
                  value={newMilestoneNotes}
                  onChange={(e) => setNewMilestoneNotes(e.target.value)}
                  className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">
                  Sub-Topics / Steps (1 per line)
                </label>
                <textarea
                  rows={3}
                  placeholder={"Docker Fundamentals & Images\nMulti-Stage Dockerfiles\nCompose for Local Development"}
                  value={newMilestoneSteps}
                  onChange={(e) => setNewMilestoneSteps(e.target.value)}
                  className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:ring-blue-500 font-mono text-xs"
                />
              </div>

              <div className="flex justify-end gap-3 pt-2 border-t">
                <button
                  type="button"
                  onClick={() => setShowAddMilestoneModal(false)}
                  className="px-4 py-2 border rounded-xl text-sm font-semibold text-gray-600 hover:bg-gray-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-sm font-bold shadow"
                >
                  Add Milestone
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ================= MODAL: EDIT MILESTONE ================= */}
      {editingMilestone && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-5 shadow-2xl">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-lg font-bold text-gray-900">
                Edit Milestone {editingMilestone.isCustom ? "(Custom)" : "(Canonical)"}
              </h3>
              <button
                onClick={() => setEditingMilestone(null)}
                className="text-gray-400 hover:text-gray-700 font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleEditMilestoneSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">
                  Title
                </label>
                <input
                  type="text"
                  disabled={!editingMilestone.isCustom}
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                  className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:text-gray-500"
                />
                {!editingMilestone.isCustom && (
                  <p className="text-[11px] text-gray-500 mt-1">
                    Canonical career requirement title cannot be altered.
                  </p>
                )}
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">
                  Estimated Hours
                </label>
                <input
                  type="number"
                  step="0.5"
                  min="0.5"
                  max="100"
                  value={editHours}
                  onChange={(e) => setEditHours(parseFloat(e.target.value) || 2)}
                  className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">
                  Custom Notes
                </label>
                <input
                  type="text"
                  placeholder="Notes or study focus"
                  value={editNotes}
                  onChange={(e) => setEditNotes(e.target.value)}
                  className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div className="flex justify-end gap-3 pt-2 border-t">
                <button
                  type="button"
                  onClick={() => setEditingMilestone(null)}
                  className="px-4 py-2 border rounded-xl text-sm font-semibold text-gray-600 hover:bg-gray-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-bold shadow"
                >
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ================= MODAL: PROJECT EVIDENCE ================= */}
      {evidenceMilestone && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-2xl max-w-xl w-full p-6 space-y-5 shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b pb-3">
              <div>
                <h3 className="text-lg font-bold text-gray-900">Project Evidence Submission</h3>
                <p className="text-xs text-gray-500">
                  Milestone: <span className="font-semibold text-gray-800">{evidenceMilestone.title}</span>
                </p>
              </div>
              <button
                onClick={() => setEvidenceMilestone(null)}
                className="text-gray-400 hover:text-gray-700 font-bold"
              >
                ✕
              </button>
            </div>

            {/* Submission Form */}
            <form onSubmit={handleSubmitEvidence} className="space-y-4 bg-gray-50/80 p-4 rounded-xl border border-gray-200">
              <h4 className="text-xs font-bold uppercase tracking-wider text-gray-700">Submit New Evidence</h4>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase mb-1">
                    Evidence Type *
                  </label>
                  <select
                    value={evidenceType}
                    onChange={(e) => setEvidenceType(e.target.value as EvidenceType)}
                    className="w-full border border-gray-300 rounded-xl px-3 py-2 text-xs bg-white focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="GITHUB_REPOSITORY">GitHub Repository</option>
                    <option value="DEPLOYED_PROJECT">Deployed Live Project</option>
                    <option value="NOTES">Study Notes / Solution</option>
                    <option value="OTHER_LINK">Other Resource Link</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase mb-1">
                    Specific Step (Optional)
                  </label>
                  <select
                    value={evidenceStepId || ""}
                    onChange={(e) => setEvidenceStepId(e.target.value ? parseInt(e.target.value) : undefined)}
                    className="w-full border border-gray-300 rounded-xl px-3 py-2 text-xs bg-white focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">Whole Milestone</option>
                    {evidenceMilestone.steps.map((st) => (
                      <option key={st.id} value={st.id}>
                        {st.title}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">
                  URL (HTTP / HTTPS)
                </label>
                <input
                  type="url"
                  placeholder="https://github.com/username/project"
                  value={evidenceUrl}
                  onChange={(e) => setEvidenceUrl(e.target.value)}
                  className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-xs bg-white focus:ring-2 focus:ring-blue-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">
                  Submission Notes / Rationale
                </label>
                <textarea
                  rows={2}
                  placeholder="Brief summary of implemented features and architecture..."
                  value={evidenceNotes}
                  onChange={(e) => setEvidenceNotes(e.target.value)}
                  className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-xs bg-white focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div className="flex items-center justify-between pt-1">
                <span className="text-[11px] text-gray-500 italic">
                  * Evidence is marked as Self-Reported / Pending Review (not auto-verified).
                </span>
                <button
                  type="submit"
                  className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow"
                >
                  Submit Evidence
                </button>
              </div>
            </form>

            {/* Existing Submissions List */}
            <div className="space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-gray-700">
                Submitted Evidence History ({existingEvidences.length})
              </h4>

              {loadingEvidence ? (
                <div className="text-center py-4 text-xs text-gray-500">Loading evidence...</div>
              ) : existingEvidences.length > 0 ? (
                <div className="space-y-2 max-h-56 overflow-y-auto">
                  {existingEvidences.map((ev) => (
                    <div key={ev.id} className="p-3 bg-white border border-gray-200 rounded-xl text-xs space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-indigo-700">
                          {ev.evidenceType.replace(/_/g, " ")}
                        </span>
                        <span className="bg-amber-100 text-amber-800 text-[10px] font-bold px-2 py-0.5 rounded-full">
                          {ev.submissionStatus}
                        </span>
                      </div>
                      {ev.url && (
                        <a
                          href={ev.url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-blue-600 hover:underline block truncate font-mono text-[11px]"
                        >
                          {ev.url}
                        </a>
                      )}
                      {ev.notes && <p className="text-gray-600 italic">{ev.notes}</p>}
                      <div className="text-[10px] text-gray-400">
                        Submitted: {new Date(ev.createdAt).toLocaleString()}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-gray-500 italic">No project evidence submitted yet.</p>
              )}
            </div>

            <div className="flex justify-end pt-2 border-t">
              <button
                type="button"
                onClick={() => setEvidenceMilestone(null)}
                className="px-4 py-2 border rounded-xl text-xs font-semibold text-gray-600 hover:bg-gray-100"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ================= MODAL: ADJUST STUDY SCHEDULE ================= */}
      {showScheduleModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-5 shadow-2xl">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-lg font-bold text-gray-900">Adjust Study Availability</h3>
              <button
                onClick={() => setShowScheduleModal(false)}
                className="text-gray-400 hover:text-gray-700 font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleUpdateSchedule} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">
                  Daily Study Time: {schedMinutes} minutes ({Math.round((schedMinutes / 60) * 10) / 10} hrs/day)
                </label>
                <input
                  type="range"
                  min="15"
                  max="480"
                  step="15"
                  value={schedMinutes}
                  onChange={(e) => setSchedMinutes(parseInt(e.target.value))}
                  className="w-full accent-blue-600 cursor-pointer"
                />
                <div className="flex justify-between text-[10px] text-gray-400">
                  <span>15m (Light)</span>
                  <span>60m (Default)</span>
                  <span>120m</span>
                  <span>480m (Full-Time)</span>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1.5">
                  Study Days per Week ({schedDays.length} days selected)
                </label>
                <div className="grid grid-cols-7 gap-1">
                  {["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"].map((day) => {
                    const isSelected = schedDays.includes(day);
                    return (
                      <button
                        type="button"
                        key={day}
                        onClick={() => {
                          if (isSelected) {
                            if (schedDays.length > 1) {
                              setSchedDays(schedDays.filter((d) => d !== day));
                            }
                          } else {
                            setSchedDays([...schedDays, day]);
                          }
                        }}
                        className={`py-1.5 text-xs font-bold rounded-lg transition border ${
                          isSelected
                            ? "bg-blue-600 text-white border-blue-600"
                            : "bg-gray-50 text-gray-600 border-gray-200 hover:bg-gray-100"
                        }`}
                      >
                        {day.slice(0, 3)}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">
                  Target Completion Date (Optional)
                </label>
                <input
                  type="date"
                  value={schedTargetDate}
                  onChange={(e) => setSchedTargetDate(e.target.value)}
                  className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                />
                <p className="text-[11px] text-gray-500 mt-1">
                  Calculated projected date: <span className="font-bold text-gray-800">{roadmap.projectedCompletionDate || "Determined automatically"}</span>
                </p>
              </div>

              <div className="flex justify-end gap-3 pt-2 border-t">
                <button
                  type="button"
                  onClick={() => setShowScheduleModal(false)}
                  className="px-4 py-2 border rounded-xl text-sm font-semibold text-gray-600 hover:bg-gray-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-bold shadow"
                >
                  Recalculate Schedule
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ================= MODAL: REVISION HISTORY ================= */}
      {showRevisionsModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-2xl max-w-xl w-full p-6 space-y-5 shadow-2xl max-h-[85vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-lg font-bold text-gray-900">Roadmap Revision History</h3>
              <button
                onClick={() => setShowRevisionsModal(false)}
                className="text-gray-400 hover:text-gray-700 font-bold"
              >
                ✕
              </button>
            </div>

            {loadingRevisions ? (
              <div className="text-center py-6 text-sm text-gray-500">Loading revisions...</div>
            ) : revisions.length > 0 ? (
              <div className="space-y-3">
                {revisions.map((rev) => (
                  <div key={rev.id} className="p-3.5 bg-gray-50/80 border border-gray-200 rounded-xl text-xs space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-purple-700">
                        Rev #{rev.revisionNumber}: {rev.changeType.replace(/_/g, " ")}
                      </span>
                      <span className="text-[10px] text-gray-400">
                        {new Date(rev.createdAt).toLocaleString()}
                      </span>
                    </div>
                    <p className="text-gray-800 font-medium">{rev.summary}</p>
                    {rev.newValue && (
                      <p className="text-[11px] text-gray-500 font-mono truncate">
                        New: {rev.newValue}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-gray-500 italic py-4 text-center">
                No revisions recorded yet.
              </p>
            )}

            <div className="flex justify-end pt-2 border-t">
              <button
                type="button"
                onClick={() => setShowRevisionsModal(false)}
                className="px-4 py-2 border rounded-xl text-xs font-semibold text-gray-600 hover:bg-gray-100"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 1: Reassessment Setup Modal */}
      {showReassessModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white w-full max-w-lg rounded-2xl p-6 shadow-xl space-y-6 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b">
              <div>
                <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                  <span>🎯</span> Adaptive Skill Reassessment
                </h3>
                <p className="text-xs text-gray-500 mt-0.5">
                  Re-evaluate your knowledge. Results will be compared with your baseline without erasing unassessed competencies.
                </p>
              </div>
              <button
                onClick={() => setShowReassessModal(false)}
                className="text-gray-400 hover:text-gray-600 font-bold text-lg"
              >
                ✕
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                  Select Technologies to Reassess
                </label>
                <div className="space-y-2 max-h-48 overflow-y-auto p-1">
                  {availableTechs.map((tech) => (
                    <label
                      key={tech.slug}
                      className="flex items-center gap-3 p-2.5 rounded-xl border border-gray-200 hover:bg-gray-50 cursor-pointer text-sm"
                    >
                      <input
                        type="checkbox"
                        checked={selectedTechSlugs.includes(tech.slug)}
                        onChange={() => handleToggleTechSlug(tech.slug)}
                        className="rounded text-indigo-600 focus:ring-indigo-500 w-4 h-4"
                      />
                      <span className="font-semibold text-gray-800">{tech.name}</span>
                    </label>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                  Time Limit (Minutes)
                </label>
                <input
                  type="number"
                  min="5"
                  max="60"
                  value={reassessTimeLimit}
                  onChange={(e) => setReassessTimeLimit(parseInt(e.target.value) || 20)}
                  className="w-full p-2.5 border rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-hidden"
                />
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t">
              <button
                type="button"
                onClick={() => setShowReassessModal(false)}
                className="px-4 py-2 border rounded-xl text-xs font-semibold text-gray-600 hover:bg-gray-100"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={startingReassessment || selectedTechSlugs.length === 0}
                onClick={handleStartReassessment}
                className="px-5 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold hover:bg-indigo-700 disabled:opacity-50"
              >
                {startingReassessment ? "Starting..." : "Begin Reassessment ➔"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 2: Active Reassessment Test Flow Modal */}
      {activeSession && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-white w-full max-w-2xl rounded-2xl p-6 sm:p-8 shadow-2xl space-y-6 max-h-[92vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b">
              <div>
                <span className="text-xs uppercase tracking-wider font-extrabold text-indigo-600 bg-indigo-50 px-2.5 py-0.5 rounded-md">
                  Active Adaptive Session #{activeSession.sessionId}
                </span>
                <h3 className="text-lg font-bold text-gray-900 mt-1">
                  Question {sessionQuestionIndex + 1} of {activeSession.questions?.length || 1}
                </h3>
              </div>
              <button
                onClick={() => {
                  if (confirm("Abandon active reassessment view? You can finalize or return to it later.")) {
                    setActiveSession(null);
                  }
                }}
                className="text-gray-400 hover:text-gray-600 text-lg font-bold"
              >
                ✕
              </button>
            </div>

            {activeSession.questions && activeSession.questions[sessionQuestionIndex] ? (
              <div className="space-y-5">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-gray-500">
                    Technology: <span className="text-gray-800 font-bold">{activeSession.questions[sessionQuestionIndex].technologyName}</span>
                  </span>
                  <span className="text-[11px] font-bold px-2 py-0.5 rounded bg-gray-100 text-gray-700">
                    {activeSession.questions[sessionQuestionIndex].difficulty}
                  </span>
                  <span className="text-[11px] font-semibold text-gray-500">
                    Topic: {activeSession.questions[sessionQuestionIndex].topic}
                  </span>
                </div>

                <div className="p-4 bg-gray-50 rounded-xl border border-gray-200">
                  <p className="text-sm sm:text-base text-gray-900 font-semibold leading-relaxed">
                    {activeSession.questions[sessionQuestionIndex].questionText}
                  </p>
                </div>

                <div className="space-y-2.5">
                  {activeSession.questions[sessionQuestionIndex].options?.map((opt: string, idx: number) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => setSelectedOption(idx)}
                      className={`w-full text-left p-3.5 rounded-xl border text-xs sm:text-sm transition flex items-start gap-3 cursor-pointer ${
                        selectedOption === idx
                          ? "border-indigo-600 bg-indigo-50/70 text-indigo-950 font-bold ring-2 ring-indigo-500/20"
                          : "border-gray-200 hover:bg-gray-50 text-gray-700"
                      }`}
                    >
                      <span className={`w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${
                        selectedOption === idx ? "bg-indigo-600 text-white" : "bg-gray-200 text-gray-700"
                      }`}>
                        {String.fromCharCode(65 + idx)}
                      </span>
                      <span>{opt}</span>
                    </button>
                  ))}
                </div>

                <div className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t">
                  <button
                    type="button"
                    onClick={handleSkipQuestion}
                    disabled={submittingAnswer || finishingSession}
                    className="px-3.5 py-2 border rounded-xl text-xs font-semibold text-gray-600 hover:bg-gray-100 disabled:opacity-50"
                  >
                    Skip Question
                  </button>

                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={handleAnswerQuestion}
                      disabled={selectedOption === null || submittingAnswer || finishingSession}
                      className="px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold hover:bg-indigo-700 disabled:opacity-50"
                    >
                      {submittingAnswer ? "Submitting..." : "Submit Answer"}
                    </button>
                    <button
                      type="button"
                      onClick={handleFinishReassessment}
                      disabled={finishingSession}
                      className="px-4 py-2 bg-emerald-600 text-white rounded-xl text-xs font-bold hover:bg-emerald-700 disabled:opacity-50"
                    >
                      {finishingSession ? "Finalizing..." : "Finish & View Results ➔"}
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              <div className="text-center py-8 text-gray-500 text-sm">
                No questions pending. You can finalize your assessment session now.
                <div className="mt-4">
                  <button
                    type="button"
                    onClick={handleFinishReassessment}
                    disabled={finishingSession}
                    className="px-5 py-2.5 bg-emerald-600 text-white rounded-xl text-sm font-bold hover:bg-emerald-700"
                  >
                    {finishingSession ? "Finalizing..." : "Finalize & Compare Results ➔"}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* MODAL 3: Before-and-After Assessment Comparison Modal */}
      {showComparisonModal && comparison && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white w-full max-w-3xl rounded-2xl p-6 sm:p-8 shadow-2xl space-y-6 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b">
              <div>
                <span className="text-xs uppercase tracking-wider font-extrabold text-emerald-700 bg-emerald-50 px-2.5 py-0.5 rounded-md border border-emerald-100">
                  Assessment Comparison Report
                </span>
                <h3 className="text-xl font-black text-gray-900 mt-1">
                  Skills Progression Analysis
                </h3>
              </div>
              <button
                onClick={() => setShowComparisonModal(false)}
                className="text-gray-400 hover:text-gray-600 text-lg font-bold"
              >
                ✕
              </button>
            </div>

            {/* Overall summary stats */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div className="p-3 bg-emerald-50/70 border border-emerald-100 rounded-xl">
                <span className="text-xs font-medium text-emerald-800">Improved</span>
                <div className="text-xl font-black text-emerald-700 mt-0.5">
                  +{comparison.improvedCompetenciesCount}
                </div>
              </div>
              <div className="p-3 bg-rose-50/70 border border-rose-100 rounded-xl">
                <span className="text-xs font-medium text-rose-800">Declined</span>
                <div className="text-xl font-black text-rose-700 mt-0.5">
                  {comparison.declinedCompetenciesCount}
                </div>
              </div>
              <div className="p-3 bg-blue-50/70 border border-blue-100 rounded-xl">
                <span className="text-xs font-medium text-blue-800">New Evidence</span>
                <div className="text-xl font-black text-blue-700 mt-0.5">
                  {comparison.newEvidenceCompetenciesCount}
                </div>
              </div>
              <div className="p-3 bg-gray-50/80 border border-gray-200 rounded-xl">
                <span className="text-xs font-medium text-gray-600">Unchanged</span>
                <div className="text-xl font-black text-gray-800 mt-0.5">
                  {comparison.unchangedCompetenciesCount}
                </div>
              </div>
            </div>

            {/* Per-Technology breakdown */}
            <div className="space-y-4">
              {comparison.technologyComparisons?.map((tech) => (
                <div key={tech.technologySlug} className="border border-gray-200 rounded-xl p-4 space-y-3">
                  <div className="flex flex-wrap items-center justify-between gap-2 pb-2 border-b border-gray-100">
                    <div>
                      <h4 className="font-extrabold text-gray-900">{tech.technologyName}</h4>
                      <p className="text-xs text-gray-500">
                        {tech.previousProficiency ? `Previous: ${tech.previousProficiency} (${tech.previousScore}%)` : "No prior baseline"}
                        {" ➔ "}
                        <span className="font-bold text-gray-800">New: {tech.newProficiency} ({tech.newScore}%)</span>
                      </p>
                    </div>

                    {tech.isComparable && tech.scoreDelta !== undefined && (
                      <div className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                        tech.scoreDelta > 0
                          ? "bg-emerald-100 text-emerald-800"
                          : tech.scoreDelta < 0
                          ? "bg-rose-100 text-rose-800"
                          : "bg-gray-100 text-gray-700"
                      }`}>
                        {tech.scoreDelta > 0 ? `+${tech.scoreDelta}%` : `${tech.scoreDelta}%`} Delta
                      </div>
                    )}
                  </div>

                  {/* Competency changes list */}
                  <div className="space-y-2">
                    {tech.competencies?.map((comp) => {
                      const badgeClasses =
                        comp.classification === "IMPROVED"
                          ? "bg-emerald-100 text-emerald-800"
                          : comp.classification === "DECLINED"
                          ? "bg-rose-100 text-rose-800"
                          : comp.classification === "NEW_EVIDENCE"
                          ? "bg-blue-100 text-blue-800"
                          : comp.classification === "INSUFFICIENT_EVIDENCE"
                          ? "bg-amber-100 text-amber-800"
                          : "bg-gray-100 text-gray-600";

                      return (
                        <div
                          key={comp.competencyKey}
                          className="flex flex-col sm:flex-row sm:items-center justify-between p-2.5 bg-gray-50/70 rounded-lg text-xs gap-2"
                        >
                          <div>
                            <span className="font-bold text-gray-800">{comp.competencyTitle}</span>
                            <span className="text-[10px] text-gray-400 ml-1.5">({comp.level})</span>
                            <p className="text-[11px] text-gray-500 mt-0.5">{comp.explanation}</p>
                          </div>
                          <div className="shrink-0">
                            <span className={`px-2 py-0.5 rounded-md font-bold uppercase tracking-wider text-[10px] ${badgeClasses}`}>
                              {comp.classification.replace(/_/g, " ")}
                            </span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t">
              <button
                type="button"
                onClick={() => setShowComparisonModal(false)}
                className="px-4 py-2 border rounded-xl text-xs font-semibold text-gray-600 hover:bg-gray-100"
              >
                Close
              </button>
              <button
                type="button"
                disabled={generatingPreview}
                onClick={handleGeneratePreviewFromComparison}
                className="px-5 py-2.5 bg-indigo-600 text-white rounded-xl text-xs font-bold hover:bg-indigo-700 disabled:opacity-50 flex items-center gap-2"
              >
                {generatingPreview ? "Analyzing Impact..." : "Preview Roadmap Impact ➔"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 4: Roadmap Update Proposal Preview Modal */}
      {showProposalModal && proposal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-white w-full max-w-3xl rounded-2xl p-6 sm:p-8 shadow-2xl space-y-6 max-h-[92vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b">
              <div>
                <span className="text-xs uppercase tracking-wider font-extrabold text-indigo-600 bg-indigo-50 px-2.5 py-0.5 rounded-md border border-indigo-100">
                  Adaptive Update Proposal #{proposal.id} • {proposal.status}
                </span>
                <h3 className="text-xl font-black text-gray-900 mt-1">
                  Proposed Curriculum & Schedule Update
                </h3>
              </div>
              <button
                onClick={() => setShowProposalModal(false)}
                className="text-gray-400 hover:text-gray-600 text-lg font-bold"
              >
                ✕
              </button>
            </div>

            {/* Preservation Guarantee Banner */}
            <div className="p-4 bg-blue-50 border border-blue-200 text-blue-900 rounded-xl text-xs sm:text-sm flex items-start gap-3 shadow-2xs">
              <span className="text-blue-600 text-base leading-none">🛡️</span>
              <div>
                <span className="font-extrabold">Preservation Guarantee: </span>
                {proposal.preservationNotice} Completed topics ({proposal.preservedCompletedCount}), in-progress work, and submitted project evidence remain untouched.
              </div>
            </div>

            {proposal.isStale && (
              <div className="p-3.5 bg-amber-50 border border-amber-300 text-amber-900 rounded-xl text-xs flex items-center gap-2">
                <span className="font-bold">⚠️ Concurrency Notice:</span>
                <span>Your roadmap was modified since this proposal was generated. Please reject or preview again to ensure latest alignment.</span>
              </div>
            )}

            {/* Schedule & Workload comparison metrics */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div className="p-3 bg-gray-50/80 border border-gray-100 rounded-xl text-xs">
                <span className="text-gray-500 font-medium">Workload Remaining</span>
                <div className="text-base font-bold text-gray-900 mt-1">
                  {proposal.currentRemainingHours.toFixed(1)}h ➔ <span className="text-indigo-600">{proposal.proposedRemainingHours.toFixed(1)}h</span>
                </div>
              </div>

              <div className="p-3 bg-gray-50/80 border border-gray-100 rounded-xl text-xs">
                <span className="text-gray-500 font-medium">Estimated Weeks</span>
                <div className="text-base font-bold text-gray-900 mt-1">
                  {proposal.currentEstimatedWeeks} wks ➔ <span className="text-indigo-600">{proposal.proposedEstimatedWeeks} wks</span>
                </div>
              </div>

              <div className="p-3 bg-emerald-50/70 border border-emerald-100 rounded-xl text-xs">
                <span className="text-emerald-700 font-medium">Test Out Candidates</span>
                <div className="text-base font-bold text-emerald-800 mt-1">
                  {proposal.testedOutCandidatesCount} Topics
                </div>
              </div>

              <div className="p-3 bg-amber-50/70 border border-amber-100 rounded-xl text-xs">
                <span className="text-amber-700 font-medium">Priority Updates</span>
                <div className="text-base font-bold text-amber-800 mt-1">
                  {proposal.priorityUpdatesCount} Topics
                </div>
              </div>
            </div>

            {/* Change Items List */}
            <div className="space-y-3">
              <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider">
                Proposed Changes ({proposal.changeItems?.length || 0} Items)
              </h4>

              <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
                {proposal.changeItems?.map((item) => {
                  const actionBadge =
                    item.actionType === "MARK_AS_POTENTIALLY_TESTED_OUT"
                      ? "bg-emerald-100 text-emerald-800 border-emerald-200"
                      : item.actionType === "UPDATE_RECOMMENDED_PRIORITY"
                      ? "bg-amber-100 text-amber-800 border-amber-200"
                      : item.actionType === "KEEP_EXISTING_STEP"
                      ? "bg-blue-100 text-blue-800 border-blue-200"
                      : item.actionType === "RECOMMEND_REASSESSMENT"
                      ? "bg-purple-100 text-purple-800 border-purple-200"
                      : "bg-gray-100 text-gray-600 border-gray-200";

                  return (
                    <div
                      key={item.id || item.stepId}
                      className="p-3 bg-gray-50/70 border border-gray-200 rounded-xl text-xs space-y-1"
                    >
                      <div className="flex flex-wrap items-center justify-between gap-1.5">
                        <span className="font-bold text-gray-900">{item.stepTitle}</span>
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold border ${actionBadge}`}>
                          {item.actionType.replace(/_/g, " ")}
                        </span>
                      </div>
                      <div className="text-[11px] text-gray-600 flex items-center gap-2">
                        <span>Milestone: {item.milestoneTitle}</span>
                        {item.proposedPriority && item.currentPriority !== item.proposedPriority && (
                          <span className="font-semibold text-indigo-700">
                            • Priority: {item.currentPriority} ➔ {item.proposedPriority}
                          </span>
                        )}
                      </div>
                      <p className="text-[11px] text-gray-500 italic">{item.reasoning}</p>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Footer with Apply & Reject Actions */}
            <div className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t">
              <button
                type="button"
                onClick={() => setShowProposalModal(false)}
                className="px-4 py-2 border rounded-xl text-xs font-semibold text-gray-600 hover:bg-gray-100"
              >
                Close Preview
              </button>

              {proposal.status === "PENDING" && (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    disabled={rejectingProposal || applyingProposal}
                    onClick={handleRejectProposal}
                    className="px-4 py-2 border border-rose-300 text-rose-700 hover:bg-rose-50 rounded-xl text-xs font-bold disabled:opacity-50"
                  >
                    {rejectingProposal ? "Rejecting..." : "❌ Reject Proposal"}
                  </button>
                  <button
                    type="button"
                    disabled={applyingProposal || rejectingProposal || proposal.isStale}
                    onClick={handleApplyProposal}
                    className="px-5 py-2 bg-emerald-600 text-white rounded-xl text-xs font-bold hover:bg-emerald-700 disabled:opacity-50 shadow-xs"
                  >
                    {applyingProposal ? "Applying Changes..." : "✅ Apply Approved Update"}
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* MODAL 5: Proposals History Modal */}
      {showProposalHistoryModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white w-full max-w-xl rounded-2xl p-6 shadow-xl space-y-5 max-h-[85vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b">
              <div>
                <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                  <span>📜</span> Adaptive Update Proposals History
                </h3>
                <p className="text-xs text-gray-500 mt-0.5">
                  Historical record of all generated roadmap update proposals and their application outcomes.
                </p>
              </div>
              <button
                onClick={() => setShowProposalHistoryModal(false)}
                className="text-gray-400 hover:text-gray-600 text-lg font-bold"
              >
                ✕
              </button>
            </div>

            {loadingHistory ? (
              <div className="text-center py-6 text-sm text-gray-500">Loading history...</div>
            ) : proposalHistory.length > 0 ? (
              <div className="space-y-3">
                {proposalHistory.map((p) => {
                  const statusColor =
                    p.status === "APPLIED"
                      ? "bg-emerald-100 text-emerald-800 border-emerald-200"
                      : p.status === "REJECTED"
                      ? "bg-rose-100 text-rose-800 border-rose-200"
                      : p.status === "PENDING"
                      ? "bg-amber-100 text-amber-800 border-amber-200"
                      : "bg-gray-100 text-gray-600 border-gray-200";

                  return (
                    <div
                      key={p.id}
                      className="p-3.5 bg-gray-50 border border-gray-200 rounded-xl text-xs space-y-1.5 flex items-center justify-between"
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-extrabold text-gray-900">Proposal #{p.id}</span>
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold border ${statusColor}`}>
                            {p.status}
                          </span>
                        </div>
                        <p className="text-[11px] text-gray-500 mt-0.5">
                          {p.testedOutCandidatesCount} tested-out candidates • {p.priorityUpdatesCount} priority updates
                        </p>
                        <p className="text-[10px] text-gray-400">
                          Created: {new Date(p.createdAt).toLocaleString()}
                        </p>
                      </div>

                      <button
                        type="button"
                        onClick={() => handleViewHistoricalProposal(p.id)}
                        className="px-3 py-1.5 bg-indigo-50 text-indigo-700 hover:bg-indigo-100 border border-indigo-200 rounded-lg text-xs font-bold"
                      >
                        View
                      </button>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="text-sm text-gray-500 italic py-6 text-center">
                No adaptive update proposals generated yet.
              </p>
            )}

            <div className="flex justify-end pt-2 border-t">
              <button
                type="button"
                onClick={() => setShowProposalHistoryModal(false)}
                className="px-4 py-2 border rounded-xl text-xs font-semibold text-gray-600 hover:bg-gray-100"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
