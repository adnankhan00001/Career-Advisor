import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";
import { roadmapData, RoadmapSection } from "./roadmapData";

export const roadmapService = {
  async getAllRoadmaps(): Promise<Record<string, RoadmapSection[]>> {
    try {
      const data = await apiRequest<Record<string, RoadmapSection[]>>(
        API_ENDPOINTS.ROADMAPS.LIST
      );
      if (data && typeof data === "object" && Object.keys(data).length > 0) {
        return data;
      }
    } catch {
      // Fallback
    }
    return roadmapData;
  },

  async getRoadmapForCareer(careerTitle: string): Promise<RoadmapSection[]> {
    try {
      const data = await apiRequest<RoadmapSection[]>(
        API_ENDPOINTS.ROADMAPS.DETAIL(careerTitle)
      );
      if (Array.isArray(data) && data.length > 0) {
        return data;
      }
    } catch {
      // Fallback
    }
    return roadmapData[careerTitle] || [];
  },
};
