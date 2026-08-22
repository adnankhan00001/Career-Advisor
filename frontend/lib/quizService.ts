import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";

export interface QuizSubmissionResponse {
  score: number;
  totalQuestions: number;
  percentage: number;
  level: string;
  recommendedCareer: string;
  categoryScores?: Record<string, number>;
}

export const quizService = {
  async submitQuiz(
    answers: Record<number, string>
  ): Promise<QuizSubmissionResponse> {
    const formattedAnswers = Object.entries(answers).map(([qId, selected]) => ({
      questionId: Number(qId),
      selected,
    }));

    try {
      const response = await apiRequest<QuizSubmissionResponse>(
        API_ENDPOINTS.QUIZ.SUBMIT,
        {
          method: "POST",
          body: JSON.stringify({
            answers: formattedAnswers,
            answersMap: answers,
          }),
        }
      );

      if (response) {
        if (typeof window !== "undefined") {
          localStorage.setItem("userLevel", response.level);
          localStorage.setItem(
            "quizScore",
            `${response.score}/${response.totalQuestions} (${response.percentage}%)`
          );
        }
        return response;
      }
    } catch {
      // Fallback
    }

    // Fallback calculation
    let score = 0;
    const total = 8;
    const correctMap: Record<number, string> = {
      1: "O(log n)",
      2: "Queue",
      3: "Tree",
      4: "201 Created",
      5: "Atomicity, Consistency, Isolation, Durability",
      6: "useEffect",
      7: "HAVING",
      8: "Overfitting",
    };

    Object.entries(answers).forEach(([idStr, val]) => {
      const id = Number(idStr);
      if (correctMap[id] === val) {
        score++;
      }
    });

    const percentage = Math.round((score / total) * 100);
    const level =
      percentage >= 75
        ? "Advanced"
        : percentage >= 40
        ? "Intermediate"
        : "Beginner";
    const recommendedCareer = "Java Backend Developer";

    if (typeof window !== "undefined") {
      localStorage.setItem("userLevel", level);
      localStorage.setItem("quizScore", `${score}/${total} (${percentage}%)`);
    }

    return {
      score,
      totalQuestions: total,
      percentage,
      level,
      recommendedCareer,
    };
  },

  async getLatestQuiz(): Promise<QuizSubmissionResponse | null> {
    try {
      return await apiRequest<QuizSubmissionResponse>(API_ENDPOINTS.QUIZ.LATEST);
    } catch {
      return null;
    }
  },
};
