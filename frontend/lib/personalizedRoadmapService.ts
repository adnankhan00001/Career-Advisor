import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";

export type StepPriority = "CORE" | "HIGH_PRIORITY" | "TESTED_OUT" | "RECOMMENDED";
export type MilestoneStatus = "COMPLETED" | "IN_PROGRESS" | "UPCOMING" | "SKIPPED";
export type StepState = "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED" | "VERIFICATION_PENDING";
export type EvidenceType = "GITHUB_REPOSITORY" | "DEPLOYED_PROJECT" | "NOTES" | "OTHER_LINK";

export interface PersonalizedStep {
  id: number;
  title: string;
  description: string;
  competencyKey: string;
  level: string;
  estimatedMinutes: number;
  sequenceOrder: number;
  isCompleted: boolean;
  state?: StepState;
  completedAt: string | null;
  isTestedOut: boolean;
  priority: StepPriority;
  evidenceStatus?: "ASSESSED" | "NOT_ASSESSED" | "INSUFFICIENT_EVIDENCE";
  assessmentScore: number | null;
}

export interface PersonalizedMilestone {
  id: number;
  title: string;
  technologyName: string;
  technologySlug: string;
  sequenceOrder: number;
  targetWeekNumber: number;
  estimatedHours: number;
  status: MilestoneStatus;
  isCustom?: boolean;
  isSkipped?: boolean;
  customNotes?: string;
  steps: PersonalizedStep[];
}

export interface PersonalizedRoadmap {
  id: number;
  careerTitle: string;
  careerSlug: string;
  totalEstimatedHours: number;
  completedStepsCount: number;
  totalStepsCount: number;
  progressPercentage: number;
  dailyStudyMinutes: number;
  targetCompletionDate: string;
  isTargetDateRealistic?: boolean;
  projectedCompletionDate?: string;
  testedOutCount: number;
  focusAreasCount: number;
  status: string;
  milestones: PersonalizedMilestone[];
}

export interface ProjectEvidence {
  id: number;
  milestoneId: number;
  milestoneTitle?: string;
  stepId?: number;
  stepTitle?: string;
  evidenceType: EvidenceType;
  url?: string;
  notes?: string;
  submissionStatus: string;
  reviewFeedback?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RoadmapRevision {
  id: number;
  revisionNumber: number;
  changeType: string;
  affectedMilestoneId?: number;
  affectedMilestoneTitle?: string;
  affectedStepId?: number;
  affectedStepTitle?: string;
  summary: string;
  previousValue?: string;
  newValue?: string;
  actorEmail?: string;
  createdAt: string;
}

export interface AddCustomMilestonePayload {
  title: string;
  technologyName?: string;
  estimatedHours?: number;
  customNotes?: string;
  stepTitles?: string[];
}

export interface UpdateMilestonePayload {
  title?: string;
  estimatedHours?: number;
  customNotes?: string;
  isSkipped?: boolean;
}

export interface SubmitEvidencePayload {
  stepId?: number;
  evidenceType: EvidenceType;
  url?: string;
  notes?: string;
}

export interface UpdateSchedulePayload {
  dailyStudyMinutes?: number;
  preferredDays?: string;
  targetCompletionDate?: string;
}

export const personalizedRoadmapService = {
  async getPersonalizedRoadmap(): Promise<PersonalizedRoadmap | null> {
    const res = await apiRequest<PersonalizedRoadmap | null>(API_ENDPOINTS.PERSONALIZED_ROADMAP.GET, {
      method: "GET",
    });
    if (!res || typeof res !== "object" || !("careerTitle" in (res as object))) {
      return null;
    }
    return res;
  },

  async generateRoadmap(forceRegenerate: boolean = true): Promise<PersonalizedRoadmap> {
    return apiRequest<PersonalizedRoadmap>(
      `${API_ENDPOINTS.PERSONALIZED_ROADMAP.GENERATE}?forceRegenerate=${forceRegenerate}`,
      {
        method: "POST",
      }
    );
  },

  async toggleStep(stepId: number): Promise<PersonalizedRoadmap> {
    return apiRequest<PersonalizedRoadmap>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.TOGGLE_STEP(stepId),
      {
        method: "POST",
      }
    );
  },

  async updateStepState(stepId: number, state: StepState): Promise<PersonalizedRoadmap> {
    return apiRequest<PersonalizedRoadmap>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.STEP_STATE(stepId),
      {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ state }),
      }
    );
  },

  async updateStepPriority(stepId: number, priority: StepPriority): Promise<PersonalizedRoadmap> {
    return apiRequest<PersonalizedRoadmap>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.STEP_PRIORITY(stepId),
      {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ priority }),
      }
    );
  },

  async addCustomMilestone(payload: AddCustomMilestonePayload): Promise<PersonalizedRoadmap> {
    return apiRequest<PersonalizedRoadmap>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.ADD_CUSTOM_MILESTONE,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      }
    );
  },

  async updateMilestone(milestoneId: number, payload: UpdateMilestonePayload): Promise<PersonalizedRoadmap> {
    return apiRequest<PersonalizedRoadmap>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.UPDATE_MILESTONE(milestoneId),
      {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      }
    );
  },

  async deleteCustomMilestone(milestoneId: number): Promise<PersonalizedRoadmap> {
    return apiRequest<PersonalizedRoadmap>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.DELETE_CUSTOM_MILESTONE(milestoneId),
      {
        method: "DELETE",
      }
    );
  },

  async skipMilestone(milestoneId: number, skip: boolean): Promise<PersonalizedRoadmap> {
    return apiRequest<PersonalizedRoadmap>(
      `${API_ENDPOINTS.PERSONALIZED_ROADMAP.SKIP_MILESTONE(milestoneId)}?skip=${skip}`,
      {
        method: "POST",
      }
    );
  },

  async reorderMilestones(milestoneIds: number[]): Promise<PersonalizedRoadmap> {
    return apiRequest<PersonalizedRoadmap>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.REORDER_MILESTONES,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ milestoneIds }),
      }
    );
  },

  async submitEvidence(milestoneId: number, payload: SubmitEvidencePayload): Promise<ProjectEvidence> {
    return apiRequest<ProjectEvidence>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.SUBMIT_EVIDENCE(milestoneId),
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      }
    );
  },

  async getMilestoneEvidence(milestoneId: number): Promise<ProjectEvidence[]> {
    return apiRequest<ProjectEvidence[]>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.MILESTONE_EVIDENCE(milestoneId),
      {
        method: "GET",
      }
    );
  },

  async getRoadmapEvidence(): Promise<ProjectEvidence[]> {
    return apiRequest<ProjectEvidence[]>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.ALL_EVIDENCE,
      {
        method: "GET",
      }
    );
  },

  async updateSchedule(payload: UpdateSchedulePayload): Promise<PersonalizedRoadmap> {
    return apiRequest<PersonalizedRoadmap>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.UPDATE_SCHEDULE,
      {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      }
    );
  },

  async getRevisions(): Promise<RoadmapRevision[]> {
    return apiRequest<RoadmapRevision[]>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.REVISIONS,
      {
        method: "GET",
      }
    );
  },

  async startReassessment(
    technologySlugs: string[],
    baselineSessionId?: number,
    timeLimitMinutes?: number
  ): Promise<any> {
    return apiRequest<any>(API_ENDPOINTS.ASSESSMENTS.REASSESSMENTS, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        technologySlugs,
        baselineSessionId,
        timeLimitMinutes: timeLimitMinutes || 20,
      }),
    });
  },

  async getReassessment(id: number): Promise<any> {
    return apiRequest<any>(API_ENDPOINTS.ASSESSMENTS.REASSESSMENT(id), {
      method: "GET",
    });
  },

  async submitReassessment(id: number): Promise<any> {
    return apiRequest<any>(API_ENDPOINTS.ASSESSMENTS.REASSESSMENT_SUBMIT(id), {
      method: "POST",
    });
  },

  async getReassessmentComparison(
    id: number,
    baselineId?: number
  ): Promise<AssessmentComparison> {
    return apiRequest<AssessmentComparison>(
      API_ENDPOINTS.ASSESSMENTS.REASSESSMENT_COMPARISON(id, baselineId),
      {
        method: "GET",
      }
    );
  },

  async generateRoadmapPreview(
    reassessmentSessionId: number,
    baselineSessionId?: number
  ): Promise<RoadmapUpdateProposal> {
    return apiRequest<RoadmapUpdateProposal>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.ADAPTIVE_UPDATE_PREVIEW,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          reassessmentSessionId,
          baselineSessionId,
        }),
      }
    );
  },

  async getProposal(proposalId: number): Promise<RoadmapUpdateProposal> {
    return apiRequest<RoadmapUpdateProposal>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.ADAPTIVE_UPDATE_PROPOSAL(proposalId),
      {
        method: "GET",
      }
    );
  },

  async applyProposal(proposalId: number): Promise<PersonalizedRoadmap> {
    return apiRequest<PersonalizedRoadmap>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.ADAPTIVE_UPDATE_APPLY(proposalId),
      {
        method: "POST",
      }
    );
  },

  async rejectProposal(
    proposalId: number,
    reason?: string
  ): Promise<RoadmapUpdateProposal> {
    return apiRequest<RoadmapUpdateProposal>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.ADAPTIVE_UPDATE_REJECT(proposalId),
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ reason }),
      }
    );
  },

  async getProposalHistory(): Promise<ProposalSummary[]> {
    return apiRequest<ProposalSummary[]>(
      API_ENDPOINTS.PERSONALIZED_ROADMAP.ADAPTIVE_UPDATE_HISTORY,
      {
        method: "GET",
      }
    );
  },
};

export type CompetencyChangeClassification =
  | "IMPROVED"
  | "DECLINED"
  | "UNCHANGED"
  | "NEW_EVIDENCE"
  | "INSUFFICIENT_EVIDENCE"
  | "NOT_REASSESSED";

export interface CompetencyChange {
  competencyKey: string;
  competencyTitle: string;
  technologySlug: string;
  level: string;
  classification: CompetencyChangeClassification;
  previousStatus: string;
  newStatus: string;
  previousScore?: number;
  newScore?: number;
  scoreDelta?: number;
  isComparable: boolean;
  explanation: string;
}

export interface TechnologyComparison {
  technologySlug: string;
  technologyName: string;
  previousScore?: number;
  newScore: number;
  scoreDelta?: number;
  previousProficiency?: string;
  newProficiency: string;
  previousConfidence?: string;
  newConfidence: string;
  isComparable: boolean;
  incomparableReason?: string;
  competencies: CompetencyChange[];
}

export interface AssessmentComparison {
  reassessmentSessionId: number;
  baselineSessionId?: number;
  reassessmentDate: string;
  baselineDate?: string;
  technologyComparisons: TechnologyComparison[];
  improvedCompetenciesCount: number;
  declinedCompetenciesCount: number;
  unchangedCompetenciesCount: number;
  newEvidenceCompetenciesCount: number;
  insufficientEvidenceCount: number;
  unassessedCompetenciesCount: number;
  summary: string;
}

export type RoadmapChangeAction =
  | "KEEP_EXISTING_STEP"
  | "ADD_NEW_STEP"
  | "UPDATE_RECOMMENDED_PRIORITY"
  | "UPDATE_ESTIMATED_EFFORT"
  | "RECOMMEND_REASSESSMENT"
  | "MARK_AS_POTENTIALLY_TESTED_OUT"
  | "NO_CHANGE_REQUIRED";

export type ProposalStatus =
  | "PENDING"
  | "APPLIED"
  | "REJECTED"
  | "EXPIRED"
  | "SUPERSEDED";

export interface RoadmapProposalChangeItem {
  id: number;
  actionType: RoadmapChangeAction;
  milestoneId: number;
  milestoneTitle: string;
  technologySlug: string;
  stepId: number;
  stepTitle: string;
  competencyKey: string;
  currentPriority: string;
  proposedPriority: string;
  currentEstimatedMinutes: number;
  proposedEstimatedMinutes: number;
  currentTestedOut: boolean;
  proposedTestedOut: boolean;
  isCompleted: boolean;
  state: string;
  reasoning: string;
  sequenceOrder: number;
}

export interface RoadmapUpdateProposal {
  id: number;
  roadmapId: number;
  roadmapVersion: number;
  sourceSessionId: number;
  baselineSessionId?: number;
  status: ProposalStatus;
  currentRoadmapTitle: string;
  currentCompletionPercentage: number;
  currentRemainingHours: number;
  proposedRemainingHours: number;
  currentEstimatedWeeks: number;
  proposedEstimatedWeeks: number;
  currentDailyStudyMinutes: number;
  proposedTargetCompletionDate: string;
  testedOutCandidatesCount: number;
  priorityUpdatesCount: number;
  preservedCompletedCount: number;
  comparison?: AssessmentComparison;
  changeItems: RoadmapProposalChangeItem[];
  preservationNotice: string;
  createdAt: string;
  appliedAt?: string;
  rejectedAt?: string;
  rejectionReason?: string;
  isStale: boolean;
}

export interface ProposalSummary {
  id: number;
  sourceSessionId: number;
  status: string;
  testedOutCandidatesCount: number;
  priorityUpdatesCount: number;
  preservedCompletedCount: number;
  proposedRemainingHours: number;
  createdAt: string;
  appliedAt?: string;
}

