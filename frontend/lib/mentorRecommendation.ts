import { MentorProfileDto } from "./mentorService";

export interface StudentProfileContext {
  careerGoal?: string | null;
  skills?: string[] | null;
}

export interface RecommendedMentor {
  mentor: MentorProfileDto;
  score: number;
  matchedSkills: string[];
  matchedGoalTerms: string[];
  recommendationReason: string;
}

/**
 * Normalizes a single keyword or phrase:
 * trims, converts to lowercase, strips punctuation.
 */
function normalizeTerm(term: string): string {
  return term
    .toLowerCase()
    .trim()
    .replace(/[^\w\s+#.-]/g, "");
}

/**
 * Parses comma/semicolon/newline-separated expertise areas into clean, unique tokens.
 */
export function parseExpertiseAreas(expertise: string | null | undefined): string[] {
  if (!expertise) return [];
  const parts = expertise.split(/[,;\n/|]+/);
  const set = new Set<string>();
  for (const part of parts) {
    const trimmed = part.trim();
    if (trimmed.length > 0) {
      set.add(trimmed);
    }
  }
  return Array.from(set);
}

/**
 * Extracts substantive keywords from a career goal string,
 * ignoring common stop words like "engineer", "developer", "and", "in", "for".
 */
const GOAL_STOP_WORDS = new Set([
  "a", "an", "the", "and", "or", "in", "at", "to", "for", "of", "with",
  "as", "by", "is", "on", "developer", "engineer", "specialist", "architect",
  "role", "job", "career", "professional", "junior", "mid", "senior", "lead"
]);

function extractGoalKeywords(careerGoal: string): string[] {
  const normalized = normalizeTerm(careerGoal);
  const words = normalized.split(/\s+/).filter((w) => w.length > 1);
  return words.filter((word) => !GOAL_STOP_WORDS.has(word));
}

/**
 * Computes deterministic recommendation scores for approved mentors
 * based on student career goals and existing skills.
 */
export function rankMentors(
  mentors: MentorProfileDto[],
  context: StudentProfileContext
): RecommendedMentor[] {
  if (!Array.isArray(mentors) || mentors.length === 0) {
    return [];
  }

  const rawCareerGoal = (context.careerGoal || "").trim();
  const rawSkills = Array.isArray(context.skills) ? context.skills : [];

  // Deduplicate and normalize student skills
  const normalizedStudentSkills = Array.from(
    new Set(rawSkills.map((s) => s.trim()).filter((s) => s.length > 0))
  );

  const goalKeywords = rawCareerGoal ? extractGoalKeywords(rawCareerGoal) : [];
  const normalizedGoalPhrase = normalizeTerm(rawCareerGoal);

  // If no career goal AND no skills exist, we cannot make evidence-based recommendations
  if (normalizedStudentSkills.length === 0 && goalKeywords.length === 0 && !normalizedGoalPhrase) {
    return [];
  }

  const recommendations: RecommendedMentor[] = [];

  for (const mentor of mentors) {
    // Only approved and active mentors should be recommended
    if (mentor.verificationStatus !== "APPROVED" || mentor.isActive === false) {
      continue;
    }

    let score = 0;
    const matchedSkills: string[] = [];
    const matchedGoalTerms: string[] = [];

    const mentorExpertiseList = parseExpertiseAreas(mentor.expertiseAreas);
    const normalizedMentorExpertise = mentorExpertiseList.map((e) => ({
      original: e,
      normalized: normalizeTerm(e),
    }));

    const normalizedJobTitle = normalizeTerm(mentor.jobTitle || "");
    const normalizedBio = normalizeTerm(mentor.bio || "");

    // 1. DIRECT SKILL MATCHING (Highest Weight: 10 pts exact, 6 pts partial)
    for (const skill of normalizedStudentSkills) {
      const normSkill = normalizeTerm(skill);
      if (!normSkill) continue;

      let skillMatched = false;
      // Check against explicit expertise tags
      for (const exp of normalizedMentorExpertise) {
        if (exp.normalized === normSkill) {
          score += 10;
          matchedSkills.push(exp.original);
          skillMatched = true;
          break;
        } else if (
          normSkill.length >= 3 &&
          (exp.normalized.includes(normSkill) || normSkill.includes(exp.normalized))
        ) {
          score += 6;
          matchedSkills.push(exp.original);
          skillMatched = true;
          break;
        }
      }

      // If not in expertise tags, check job title or bio (Lower weight: 2-3 pts)
      if (!skillMatched && normSkill.length >= 3) {
        if (normalizedJobTitle.includes(normSkill)) {
          score += 3;
          matchedSkills.push(skill);
        } else if (normalizedBio.includes(normSkill)) {
          score += 1;
        }
      }
    }

    // 2. CAREER GOAL MATCHING (High Weight: 15 pts direct title match, 4-5 pts keyword)
    if (normalizedGoalPhrase && normalizedGoalPhrase.length >= 3) {
      if (
        normalizedJobTitle.includes(normalizedGoalPhrase) ||
        normalizedGoalPhrase.includes(normalizedJobTitle)
      ) {
        score += 15;
        matchedGoalTerms.push(mentor.jobTitle);
      } else {
        for (const kw of goalKeywords) {
          let kwMatched = false;
          if (normalizedJobTitle.includes(kw)) {
            score += 5;
            matchedGoalTerms.push(kw);
            kwMatched = true;
          }
          if (!kwMatched) {
            for (const exp of normalizedMentorExpertise) {
              if (exp.normalized.includes(kw)) {
                score += 4;
                matchedGoalTerms.push(exp.original);
                break;
              }
            }
          }
        }
      }
    }

    const uniqueSkills = Array.from(new Set(matchedSkills));
    const uniqueGoalTerms = Array.from(new Set(matchedGoalTerms));

    if (score > 0) {
      // Slight bonus for having available slots so actionable mentors rank slightly higher for equal scores
      if (mentor.availableSlots > 0) {
        score += 1;
      }

      const reasons: string[] = [];
      if (uniqueSkills.length > 0) {
        const topSkills = uniqueSkills.slice(0, 3).join(", ");
        reasons.push(`Matches your skills in ${topSkills}`);
      }
      if (uniqueGoalTerms.length > 0 && rawCareerGoal) {
        reasons.push(`Aligns with your career goal (${rawCareerGoal})`);
      }
      if (reasons.length === 0) {
        reasons.push("Aligns with your profile background");
      }

      recommendations.push({
        mentor,
        score,
        matchedSkills: uniqueSkills,
        matchedGoalTerms: uniqueGoalTerms,
        recommendationReason: reasons.join(" • "),
      });
    }
  }

  // Stable deterministic sorting:
  // 1. Score descending
  // 2. Available slots > 0 first
  // 3. Years of experience descending
  // 4. Mentor ID ascending (stable tie-breaker)
  recommendations.sort((a, b) => {
    if (b.score !== a.score) return b.score - a.score;
    const aAvail = a.mentor.availableSlots > 0 ? 1 : 0;
    const bAvail = b.mentor.availableSlots > 0 ? 1 : 0;
    if (bAvail !== aAvail) return bAvail - aAvail;
    if (b.mentor.yearsOfExperience !== a.mentor.yearsOfExperience) {
      return b.mentor.yearsOfExperience - a.mentor.yearsOfExperience;
    }
    return a.mentor.id - b.mentor.id;
  });

  return recommendations;
}
