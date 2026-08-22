import { apiRequest } from "./apiClient";
import { API_ENDPOINTS } from "./config";

export interface ExtractedSkill {
  skillName: string;
  category: string;
  confidence: number;
  alreadyInProfile: boolean;
}

export interface CareerMatch {
  title: string;
  category: string;
  matchScore: number;
  skillScore: number;
  goalScore: number;
  quizScore: number;
  matchedSkills: string[];
  missingSkills: string[];
  targetGoal: boolean;
}

export interface SkillGap {
  targetCareer: string;
  matchPercentage: number;
  acquiredSkills: string[];
  missingSkills: string[];
  highPriorityMissing: string[];
}

export interface RecommendationAction {
  id: string;
  priority: string;
  type: string;
  title: string;
  description: string;
  actionUrl: string;
  actionText: string;
  score: number;
}

export interface ResumeSummary {
  id: number;
  originalFileName: string;
  fileType: string;
  fileSize: number;
  uploadTimestamp: string;
  parsingStatus: "UPLOADED" | "PROCESSING" | "COMPLETED" | "FAILED";
  skillsCount: number;
  extractedSummary: string;
  extractedEmail?: string;
  extractedPhone?: string;
}

export interface ResumeAnalysis {
  resumeId: number;
  fileName: string;
  fileSize: number;
  fileType: string;
  parsingStatus: "UPLOADED" | "PROCESSING" | "COMPLETED" | "FAILED";
  uploadTimestamp: string;
  summary: string;
  extractedEmail?: string;
  extractedPhone?: string;
  extractedSkills: ExtractedSkill[];
  extractedEducation: string[];
  extractedExperience: string[];
  extractedProjects: string[];
  matchedCareers: CareerMatch[];
  skillGaps: SkillGap;
  recommendations: RecommendationAction[];
}

export interface SyncSkillsResponse {
  syncedCount: number;
  userSkills: string[];
  message: string;
}

export async function uploadResume(file: File): Promise<ResumeAnalysis> {
  const formData = new FormData();
  formData.append("file", file);

  return apiRequest<ResumeAnalysis>(API_ENDPOINTS.RESUMES.UPLOAD, {
    method: "POST",
    body: formData,
  });
}

export async function getUserResumes(): Promise<ResumeSummary[]> {
  return apiRequest<ResumeSummary[]>(API_ENDPOINTS.RESUMES.BASE);
}

export async function getResumeAnalysis(id: number): Promise<ResumeAnalysis> {
  return apiRequest<ResumeAnalysis>(API_ENDPOINTS.RESUMES.ANALYSIS(id));
}

export async function getLatestResumeAnalysis(): Promise<ResumeAnalysis | null> {
  try {
    return await apiRequest<ResumeAnalysis>(API_ENDPOINTS.RESUMES.LATEST_ANALYSIS);
  } catch (err: any) {
    if (err.status === 404 || err.status === 204) {
      return null;
    }
    throw err;
  }
}

export async function deleteResume(id: number): Promise<{ message: string }> {
  return apiRequest<{ message: string }>(API_ENDPOINTS.RESUMES.DETAIL(id), {
    method: "DELETE",
  });
}

export async function syncResumeSkills(id: number, skills: string[]): Promise<SyncSkillsResponse> {
  return apiRequest<SyncSkillsResponse>(API_ENDPOINTS.RESUMES.SYNC_SKILLS(id), {
    method: "POST",
    body: JSON.stringify({ skills }),
  });
}
