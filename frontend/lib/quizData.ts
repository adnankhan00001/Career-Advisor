export interface QuizQuestion {
  id: number;
  category: "General CS" | "Backend" | "Frontend" | "Data & Analytics" | "AI & ML";
  question: string;
  options: string[];
  answer: string;
  explanation: string;
}

export const quizQuestions: QuizQuestion[] = [
  {
    id: 1,
    category: "General CS",
    question: "What is the average time complexity of Binary Search in a sorted array?",
    options: ["O(n)", "O(log n)", "O(n log n)", "O(1)"],
    answer: "O(log n)",
    explanation: "Binary search divides the search space in half with each comparison, resulting in logarithmic time complexity O(log n).",
  },
  {
    id: 2,
    category: "General CS",
    question: "Which fundamental data structure operates on a First-In, First-Out (FIFO) principle?",
    options: ["Stack", "Queue", "Tree", "Graph"],
    answer: "Queue",
    explanation: "A Queue operates on FIFO order where elements inserted first are removed first.",
  },
  {
    id: 3,
    category: "General CS",
    question: "Which of the following is NOT a linear data structure?",
    options: ["Array", "Linked List", "Tree", "Stack"],
    answer: "Tree",
    explanation: "A Tree is a hierarchical non-linear data structure consisting of parent and child nodes.",
  },
  {
    id: 4,
    category: "Backend",
    question: "Which HTTP status code signifies that a requested resource was successfully created on the server?",
    options: ["200 OK", "201 Created", "204 No Content", "301 Moved Permanently"],
    answer: "201 Created",
    explanation: "HTTP status code 201 indicates that the request succeeded and a new resource was created.",
  },
  {
    id: 5,
    category: "Backend",
    question: "In relational database design, what does ACID stand for?",
    options: [
      "Atomicity, Consistency, Isolation, Durability",
      "Asynchronous, Concurrent, Isolated, Distributed",
      "Access, Control, Integrity, Data",
      "Authentication, Cryptography, Identity, Directory"
    ],
    answer: "Atomicity, Consistency, Isolation, Durability",
    explanation: "ACID properties ensure reliable processing of database transactions.",
  },
  {
    id: 6,
    category: "Frontend",
    question: "In React, what hook is used to handle side-effects such as data fetching, subscriptions, and DOM mutations?",
    options: ["useState", "useEffect", "useMemo", "useContext"],
    answer: "useEffect",
    explanation: "useEffect lets you perform side effects in functional components after rendering.",
  },
  {
    id: 7,
    category: "Data & Analytics",
    question: "In SQL, which clause is used to filter groups created by the GROUP BY clause?",
    options: ["WHERE", "HAVING", "ORDER BY", "FILTER"],
    answer: "HAVING",
    explanation: "The HAVING clause filters aggregated group results, whereas WHERE filters individual rows prior to grouping.",
  },
  {
    id: 8,
    category: "AI & ML",
    question: "What is the primary problem caused when a machine learning model learns training data noise too well, resulting in poor generalization on unseen data?",
    options: ["Underfitting", "Overfitting", "Data Drift", "Vanishing Gradient"],
    answer: "Overfitting",
    explanation: "Overfitting occurs when a model fits the training data too closely, capturing noise rather than the underlying pattern.",
  }
];

export function evaluateQuizResult(answers: Record<number, string>) {
  let score = 0;
  const categoryScores: Record<string, { total: number; correct: number }> = {};

  quizQuestions.forEach((q) => {
    if (!categoryScores[q.category]) {
      categoryScores[q.category] = { total: 0, correct: 0 };
    }
    categoryScores[q.category].total += 1;

    const selected = answers[q.id];
    if (selected === q.answer) {
      score += 1;
      categoryScores[q.category].correct += 1;
    }
  });

  const percentage = Math.round((score / quizQuestions.length) * 100);

  let level: "Beginner" | "Intermediate" | "Advanced" = "Beginner";
  if (percentage >= 75) {
    level = "Advanced";
  } else if (percentage >= 40) {
    level = "Intermediate";
  } else {
    level = "Beginner";
  }

  // Determine suggested career based on best category
  let bestCategory = "Software Engineering";
  let highestRatio = -1;
  Object.entries(categoryScores).forEach(([cat, { total, correct }]) => {
    const ratio = correct / total;
    if (ratio > highestRatio) {
      highestRatio = ratio;
      bestCategory = cat;
    }
  });

  let recommendedCareer = "Java Backend Developer";
  if (bestCategory === "Backend") {
    recommendedCareer = "Java Backend Developer";
  } else if (bestCategory === "Frontend") {
    recommendedCareer = "Frontend Developer";
  } else if (bestCategory === "Data & Analytics") {
    recommendedCareer = "Data Analyst";
  } else if (bestCategory === "AI & ML") {
    recommendedCareer = "AI/ML Engineer";
  } else {
    recommendedCareer = percentage >= 60 ? "Full Stack Developer" : "Frontend Developer";
  }

  return {
    score,
    totalQuestions: quizQuestions.length,
    percentage,
    level,
    categoryScores,
    recommendedCareer,
  };
}
