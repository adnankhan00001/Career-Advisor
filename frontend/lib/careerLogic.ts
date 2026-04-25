export function getCareerSuggestions(skills: string[]) {
  const suggestions = [];

  const lowerSkills = skills.map((s) => s.toLowerCase());

  if (lowerSkills.includes("java") || lowerSkills.includes("spring")) {
    suggestions.push({ title: "Backend Developer", match: "90%" });
  }

  if (
    lowerSkills.includes("html") ||
    lowerSkills.includes("css") ||
    lowerSkills.includes("javascript") ||
    lowerSkills.includes("react")
  ) {
    suggestions.push({ title: "Frontend Developer", match: "92%" });
  }

  if (lowerSkills.includes("sql") || lowerSkills.includes("excel")) {
    suggestions.push({ title: "Data Analyst", match: "85%" });
  }

  // Default fallback
  if (suggestions.length === 0) {
    suggestions.push({ title: "Explore More Skills", match: "—" });
  }

  return suggestions;
}