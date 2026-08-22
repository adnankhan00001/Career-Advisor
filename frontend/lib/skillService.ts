import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";

export const skillService = {
  async getUserSkills(): Promise<string[]> {
    try {
      const skills = await apiRequest<string[]>(API_ENDPOINTS.SKILLS.BASE);
      if (Array.isArray(skills)) {
        if (typeof window !== "undefined") {
          localStorage.setItem("skills", JSON.stringify(skills));
        }
        return skills;
      }
    } catch {
      // Fallback to local storage if offline or not authenticated
    }

    if (typeof window !== "undefined") {
      try {
        return JSON.parse(localStorage.getItem("skills") || "[]");
      } catch {
        return [];
      }
    }
    return [];
  },

  async addSkill(skill: string): Promise<string[]> {
    try {
      const updated = await apiRequest<string[]>(API_ENDPOINTS.SKILLS.BASE, {
        method: "POST",
        body: JSON.stringify({ skill }),
      });
      if (Array.isArray(updated)) {
        if (typeof window !== "undefined") {
          localStorage.setItem("skills", JSON.stringify(updated));
        }
        return updated;
      }
    } catch {
      // Fallback local update
    }

    if (typeof window !== "undefined") {
      const current = JSON.parse(localStorage.getItem("skills") || "[]");
      if (!current.includes(skill)) {
        const next = [...current, skill];
        localStorage.setItem("skills", JSON.stringify(next));
        return next;
      }
      return current;
    }
    return [skill];
  },

  async removeSkill(skill: string): Promise<string[]> {
    try {
      const updated = await apiRequest<string[]>(
        API_ENDPOINTS.SKILLS.DELETE(skill),
        {
          method: "DELETE",
        }
      );
      if (Array.isArray(updated)) {
        if (typeof window !== "undefined") {
          localStorage.setItem("skills", JSON.stringify(updated));
        }
        return updated;
      }
    } catch {
      // Fallback local update
    }

    if (typeof window !== "undefined") {
      const current = JSON.parse(localStorage.getItem("skills") || "[]");
      const next = current.filter((s: string) => s !== skill);
      localStorage.setItem("skills", JSON.stringify(next));
      return next;
    }
    return [];
  },
};
