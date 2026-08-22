import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";
import { Career, careersData } from "./careersData";

export const careerService = {
  async getAllCareers(): Promise<Career[]> {
    try {
      const data = await apiRequest<Career[]>(API_ENDPOINTS.CAREERS.LIST);
      if (Array.isArray(data) && data.length > 0) {
        return data;
      }
    } catch {
      // Fallback to bundled data
    }
    return careersData;
  },

  async getCareerByIdOrTitle(idOrTitle: string): Promise<Career | null> {
    try {
      return await apiRequest<Career>(API_ENDPOINTS.CAREERS.DETAIL(idOrTitle));
    } catch {
      return (
        careersData.find(
          (c) =>
            c.id.toLowerCase() === idOrTitle.toLowerCase() ||
            c.title.toLowerCase() === idOrTitle.toLowerCase()
        ) || null
      );
    }
  },
};
