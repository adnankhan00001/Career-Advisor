export const roadmapData: Record<
  string,
  { title: string; steps: string[] }[]
> = {
  "Backend Developer": [
    {
      title: "Fundamentals",
      steps: ["Java Basics", "OOP Concepts", "Data Structures"],
    },
    {
      title: "Backend Core",
      steps: ["Spring Boot", "REST APIs", "Authentication"],
    },
    {
      title: "Database",
      steps: ["SQL", "Database Design"],
    },
    {
      title: "Advanced",
      steps: ["Microservices", "System Design", "Deployment"],
    },
  ],

  "Frontend Developer": [
    {
      title: "Basics",
      steps: ["HTML", "CSS", "JavaScript"],
    },
    {
      title: "Framework",
      steps: ["React", "State Management"],
    },
    {
      title: "Advanced",
      steps: ["Performance Optimization", "Testing"],
    },
  ],

  "Data Analyst": [
    {
      title: "Basics",
      steps: ["Excel", "SQL"],
    },
    {
      title: "Programming",
      steps: ["Python", "Pandas"],
    },
    {
      title: "Visualization",
      steps: ["Power BI", "Tableau"],
    },
  ],
};