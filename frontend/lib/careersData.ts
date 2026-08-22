export interface Career {
  id: string;
  title: string;
  category: "Software Engineering" | "Data & AI" | "Cloud & DevOps";
  level: "Beginner" | "Intermediate" | "Advanced";
  duration: string;
  description: string;
  overview: string;
  requiredSkills: string[];
  technologies: string[];
  responsibilities: string[];
  salaryRange: string;
  roadmapSlug: string;
}

export const careersData: Career[] = [
  {
    id: "java-backend-dev",
    title: "Java Backend Developer",
    category: "Software Engineering",
    level: "Intermediate",
    duration: "4 - 6 Months",
    description: "Design and build scalable server-side systems, microservices, and secure RESTful APIs using Java and modern frameworks.",
    overview: "Java Backend Developers build enterprise-grade web applications, handle database interactions, orchestrate distributed transactions, and implement high-performance APIs.",
    requiredSkills: ["Java", "Spring Boot", "SQL", "REST APIs", "Git", "Data Structures"],
    technologies: ["Java 21", "Spring Boot 3", "PostgreSQL", "Hibernate/JPA", "Docker", "Kafka", "Maven"],
    responsibilities: [
      "Develop robust and maintainable backend services with Spring Boot",
      "Design relational schemas, write optimized SQL, and manage database migrations",
      "Build authentication and authorization mechanisms (JWT, OAuth2, Spring Security)",
      "Implement microservices architecture and message queues"
    ],
    salaryRange: "$90,000 - $140,000",
    roadmapSlug: "Java Backend Developer",
  },
  {
    id: "full-stack-dev",
    title: "Full Stack Developer",
    category: "Software Engineering",
    level: "Intermediate",
    duration: "6 - 8 Months",
    description: "Master both client-side and server-side engineering to build end-to-end modern web applications.",
    overview: "Full Stack Developers bridge the gap between user interfaces and backend logic, creating dynamic, responsive web experiences with end-to-end functionality.",
    requiredSkills: ["JavaScript", "TypeScript", "React", "Node.js", "SQL", "HTML/CSS", "Git"],
    technologies: ["React", "Next.js", "Node.js", "Express", "TypeScript", "PostgreSQL", "Tailwind CSS", "Prisma"],
    responsibilities: [
      "Build responsive UI components and interactive web interfaces",
      "Develop REST and GraphQL APIs for seamless client-server communication",
      "Manage database schemas and integrate third-party web services",
      "Deploy and maintain full-stack cloud applications"
    ],
    salaryRange: "$95,000 - $145,000",
    roadmapSlug: "Full Stack Developer",
  },
  {
    id: "frontend-dev",
    title: "Frontend Developer",
    category: "Software Engineering",
    level: "Beginner",
    duration: "3 - 5 Months",
    description: "Craft intuitive, accessible, and high-performance user interfaces and responsive web experiences.",
    overview: "Frontend Developers focus on the look, feel, and usability of web applications, transforming design mockups into interactive and animated web applications.",
    requiredSkills: ["HTML", "CSS", "JavaScript", "React", "Responsive Design", "Git"],
    technologies: ["React", "Next.js", "TypeScript", "Tailwind CSS", "Redux/Zustand", "Framer Motion", "Vite"],
    responsibilities: [
      "Build pixel-perfect, responsive layouts across devices and screen sizes",
      "Implement state management, dynamic routing, and API integration",
      "Ensure web accessibility (WCAG), cross-browser compatibility, and Core Web Vitals optimization",
      "Write unit and component tests with Jest and React Testing Library"
    ],
    salaryRange: "$80,000 - $125,000",
    roadmapSlug: "Frontend Developer",
  },
  {
    id: "data-analyst",
    title: "Data Analyst",
    category: "Data & AI",
    level: "Beginner",
    duration: "3 - 5 Months",
    description: "Transform complex raw data into actionable business insights, visual dashboards, and strategic reports.",
    overview: "Data Analysts collect, clean, and study data sets to help organizations make smarter decisions, identifying patterns and trends through statistical analysis and visualization.",
    requiredSkills: ["SQL", "Excel", "Python", "Data Visualization", "Critical Thinking"],
    technologies: ["Python", "Pandas", "NumPy", "Power BI", "Tableau", "SQL Server / PostgreSQL", "Jupyter"],
    responsibilities: [
      "Extract, clean, and transform data from diverse databases and business systems",
      "Build interactive KPI dashboards and visualization reports for stakeholders",
      "Conduct exploratory data analysis to detect business anomalies and opportunities",
      "Collaborate with product and operations teams to guide data-driven decision making"
    ],
    salaryRange: "$75,000 - $110,000",
    roadmapSlug: "Data Analyst",
  },
  {
    id: "data-scientist",
    title: "Data Scientist",
    category: "Data & AI",
    level: "Advanced",
    duration: "6 - 9 Months",
    description: "Apply statistical modeling, machine learning algorithms, and predictive analytics to solve complex business problems.",
    overview: "Data Scientists leverage mathematics, programming, and machine learning to build predictive models, forecast outcomes, and extract deep intelligence from structured and unstructured data.",
    requiredSkills: ["Python", "Statistics", "Machine Learning", "SQL", "Pandas", "Data Modeling"],
    technologies: ["Python", "Scikit-Learn", "TensorFlow / PyTorch", "Pandas", "NumPy", "SQL", "Jupyter Notebooks", "MLflow"],
    responsibilities: [
      "Formulate mathematical formulations and hypothesis testing strategies",
      "Train, evaluate, and tune supervised and unsupervised machine learning models",
      "Engineer features and build automated data pipelines for predictive modeling",
      "Present actionable insights and quantitative findings to executive teams"
    ],
    salaryRange: "$105,000 - $160,000",
    roadmapSlug: "Data Scientist",
  },
  {
    id: "ai-ml-engineer",
    title: "AI/ML Engineer",
    category: "Data & AI",
    level: "Advanced",
    duration: "6 - 9 Months",
    description: "Build, deploy, and scale deep learning models, LLM architectures, and generative AI solutions in production.",
    overview: "AI/ML Engineers bridge machine learning research and software engineering, creating production-grade AI pipelines, fine-tuning foundation models, and deploying intelligent systems.",
    requiredSkills: ["Python", "Deep Learning", "PyTorch / TensorFlow", "NLP / LLMs", "Cloud Deployment", "Math & Linear Algebra"],
    technologies: ["PyTorch", "Hugging Face", "LangChain / LlamaIndex", "OpenAI APIs", "Docker", "FastAPI", "Vector DBs (Pinecone/Chroma)"],
    responsibilities: [
      "Design and deploy production machine learning pipelines and real-time inference APIs",
      "Fine-tune Large Language Models (LLMs) and build Retrieval-Augmented Generation (RAG) workflows",
      "Optimize model inference latency, quantization, and resource utilization",
      "Monitor model drift, performance metrics, and safety in production environments"
    ],
    salaryRange: "$115,000 - $175,000",
    roadmapSlug: "AI/ML Engineer",
  },
  {
    id: "devops-engineer",
    title: "DevOps Engineer",
    category: "Cloud & DevOps",
    level: "Intermediate",
    duration: "4 - 6 Months",
    description: "Automate software delivery pipelines, manage cloud infrastructure, and ensure high availability and security.",
    overview: "DevOps Engineers streamline software delivery, manage cloud infrastructure as code, establish automated CI/CD pipelines, and monitor system reliability.",
    requiredSkills: ["Linux", "Docker", "Kubernetes", "CI/CD", "Cloud (AWS/GCP)", "Infrastructure as Code"],
    technologies: ["Docker", "Kubernetes", "GitHub Actions", "Terraform", "AWS", "Linux / Bash", "Prometheus", "Grafana"],
    responsibilities: [
      "Configure and maintain automated continuous integration and continuous deployment (CI/CD) pipelines",
      "Provision and manage scalable cloud infrastructure using Terraform and Ansible",
      "Deploy containerized microservices clusters using Kubernetes",
      "Set up distributed observability, log aggregation, and alerting solutions"
    ],
    salaryRange: "$100,000 - $155,000",
    roadmapSlug: "DevOps Engineer",
  }
];

export function calculateSkillMatch(userSkills: string[], requiredSkills: string[]): {
  percentage: number;
  matchedSkills: string[];
  missingSkills: string[];
} {
  if (!userSkills || userSkills.length === 0) {
    return {
      percentage: 0,
      matchedSkills: [],
      missingSkills: requiredSkills,
    };
  }

  const normalizedUserSkills = userSkills.map((s) => s.trim().toLowerCase());
  const matched: string[] = [];
  const missing: string[] = [];

  requiredSkills.forEach((skill) => {
    const isMatched = normalizedUserSkills.some(
      (userSkill) =>
        userSkill.includes(skill.toLowerCase()) ||
        skill.toLowerCase().includes(userSkill)
    );

    if (isMatched) {
      matched.push(skill);
    } else {
      missing.push(skill);
    }
  });

  const percentage = Math.round((matched.length / requiredSkills.length) * 100);

  return {
    percentage,
    matchedSkills: matched,
    missingSkills: missing,
  };
}
