import { careersData, calculateSkillMatch } from "./careersData";

export interface CareerSuggestion {
  title: string;
  match: string;
  matchedCount?: number;
  totalRequired?: number;
  careerId?: string;
}

export function getCareerSuggestions(skills: string[]): CareerSuggestion[] {
  if (!skills || skills.length === 0) {
    return [
      { title: "Frontend Developer", match: "Explore" },
      { title: "Java Backend Developer", match: "Explore" },
      { title: "Data Analyst", match: "Explore" },
    ];
  }

  const results = careersData
    .map((career) => {
      const matchResult = calculateSkillMatch(skills, career.requiredSkills);
      return {
        title: career.title,
        match: `${matchResult.percentage}%`,
        percentage: matchResult.percentage,
        matchedCount: matchResult.matchedSkills.length,
        totalRequired: career.requiredSkills.length,
        careerId: career.id,
      };
    })
    .filter((res) => res.percentage > 0)
    .sort((a, b) => b.percentage - a.percentage);

  if (results.length === 0) {
    return [{ title: "Explore More Skills", match: "—" }];
  }

  return results.map(({ title, match, matchedCount, totalRequired, careerId }) => ({
    title,
    match,
    matchedCount,
    totalRequired,
    careerId,
  }));
}