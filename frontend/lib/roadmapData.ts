export interface RoadmapSection {
  title: string;
  steps: string[];
}

export const roadmapData: Record<string, RoadmapSection[]> = {
  "Java Backend Developer": [
    {
      title: "1. Core Java & Fundamentals",
      steps: [
        "Java Syntax, Data Types & Control Flow",
        "Object-Oriented Programming (OOP) Deep Dive",
        "Java Collections Framework (List, Set, Map)",
        "Exception Handling, Generics & Streams API",
        "Data Structures & Algorithms in Java",
      ],
    },
    {
      title: "2. Database & Persistence",
      steps: [
        "Relational Database Fundamentals & SQL",
        "PostgreSQL / MySQL Schema Design & Indexing",
        "JDBC & Spring Data JPA / Hibernate",
        "Transaction Management (ACID) & Connection Pooling",
      ],
    },
    {
      title: "3. Spring Boot & Enterprise Backend",
      steps: [
        "Spring Core, Inversion of Control (IoC) & DI",
        "Building RESTful APIs with Spring Boot 3",
        "Spring Security with JWT & OAuth2",
        "Validation, Error Handling & API Documentation (Swagger/OpenAPI)",
        "Unit & Integration Testing (JUnit 5 & Mockito)",
      ],
    },
    {
      title: "4. Architecture, Microservices & Cloud",
      steps: [
        "Microservices Architecture & Service Discovery (Eureka/Consul)",
        "Message Brokers (Apache Kafka or RabbitMQ)",
        "Caching Strategies with Redis",
        "Dockerizing Java Apps & CI/CD Pipelines",
        "System Design Principles & Scalability",
      ],
    },
  ],

  "Backend Developer": [
    {
      title: "1. Fundamentals",
      steps: ["Java/Node.js Basics", "OOP & Design Patterns", "Data Structures & Algorithms"],
    },
    {
      title: "2. Backend Core",
      steps: ["Frameworks (Spring Boot / Express)", "REST APIs & JSON", "Authentication & JWT"],
    },
    {
      title: "3. Database",
      steps: ["SQL & PostgreSQL / MySQL", "Database Design & Indexing", "ORM / JPA"],
    },
    {
      title: "4. Advanced",
      steps: ["Microservices", "System Design", "Docker & Cloud Deployment"],
    },
  ],

  "Full Stack Developer": [
    {
      title: "1. Web Foundations",
      steps: [
        "Semantic HTML5, CSS3 & Responsive Web Design",
        "Modern JavaScript (ES6+) & TypeScript Basics",
        "Git Version Control & GitHub Workflows",
      ],
    },
    {
      title: "2. Modern Frontend",
      steps: [
        "React Fundamentals, Hooks & Component Architecture",
        "Next.js App Router, SSR, SSG & Server Actions",
        "Tailwind CSS & Component UI Libraries",
        "Client-side State Management (Zustand/Redux)",
      ],
    },
    {
      title: "3. Backend & APIs",
      steps: [
        "Node.js & Express / NestJS Framework",
        "RESTful API & GraphQL Development",
        "Authentication (NextAuth, JWT, Session cookies)",
        "PostgreSQL / MongoDB & Prisma ORM",
      ],
    },
    {
      title: "4. Full-Stack Integration & Deployment",
      steps: [
        "End-to-End Type Safety (tRPC / TypeScript)",
        "Third-party API Integration & Webhooks",
        "Docker, CI/CD with GitHub Actions & Vercel/AWS Deployment",
        "Full-Stack Testing (Vitest, Cypress/Playwright)",
      ],
    },
  ],

  "Frontend Developer": [
    {
      title: "1. Web Basics & UI Foundations",
      steps: [
        "HTML5 Semantic Tags & Accessibility (a11y)",
        "CSS3 Flexbox, Grid, Responsive Layouts & Animations",
        "JavaScript Core (DOM, Async/Await, ES6+)",
      ],
    },
    {
      title: "2. React & Component Ecosystem",
      steps: [
        "TypeScript for Frontend Developers",
        "React Component Lifecycle, Hooks & Custom Hooks",
        "Styling with Tailwind CSS & CSS Modules",
        "Routing and Navigation (React Router / Next.js)",
      ],
    },
    {
      title: "3. Advanced State & Performance",
      steps: [
        "State Management (Zustand, TanStack Query)",
        "Core Web Vitals & Frontend Performance Optimization",
        "Cross-Browser Compatibility & Responsive Testing",
      ],
    },
    {
      title: "4. Production & Testing",
      steps: [
        "Unit & Component Testing (Jest, React Testing Library)",
        "E2E Testing with Playwright",
        "Build Tools (Vite, Turbopack, Webpack)",
        "Continuous Deployment (Vercel, Netlify, Cloudflare Pages)",
      ],
    },
  ],

  "Data Analyst": [
    {
      title: "1. Data Literacy & Spreadsheets",
      steps: [
        "Advanced Excel / Google Sheets (VLOOKUP, INDEX/MATCH, Pivot Tables)",
        "Descriptive Statistics & Business Metrics",
        "Data Hygiene & Data Cleaning Principles",
      ],
    },
    {
      title: "2. SQL & Relational Databases",
      steps: [
        "SQL Queries, Joins, Aggregations & Grouping",
        "Subqueries, CTEs (Common Table Expressions) & Window Functions",
        "Data Extraction & Transformation (ETL Basics)",
      ],
    },
    {
      title: "3. Python for Data Analytics",
      steps: [
        "Python Programming Fundamentals",
        "Data Manipulation with Pandas & NumPy",
        "Data Storytelling & Visualization with Matplotlib & Seaborn",
        "Jupyter Notebook Workflows",
      ],
    },
    {
      title: "4. Business Intelligence & Dashboards",
      steps: [
        "Power BI / Tableau Interactive Dashboard Design",
        "DAX / Calculated Fields & KPI Modeling",
        "Stakeholder Communication & Business Reporting",
      ],
    },
  ],

  "Data Scientist": [
    {
      title: "1. Mathematics & Programming Foundations",
      steps: [
        "Linear Algebra, Calculus & Multivariable Optimization",
        "Probability Theory & Inferential Statistics",
        "Advanced Python (NumPy, Pandas, SciPy)",
        "Data Wrangling, Imputation & Feature Engineering",
      ],
    },
    {
      title: "2. Machine Learning Algorithms",
      steps: [
        "Supervised Learning (Linear/Logistic Regression, Decision Trees, Random Forests)",
        "Unsupervised Learning (K-Means, PCA, Hierarchical Clustering)",
        "Model Evaluation Metrics (ROC-AUC, Precision/Recall, RMSE, Cross-Validation)",
        "Hyperparameter Tuning with Scikit-Learn",
      ],
    },
    {
      title: "3. Advanced Analytics & Deep Learning",
      steps: [
        "Time Series Forecasting & Anomaly Detection",
        "Neural Networks Basics with PyTorch / TensorFlow",
        "Natural Language Processing (NLP) & Feature Representations",
      ],
    },
    {
      title: "4. MLOps & Real-World Impact",
      steps: [
        "Experiment Tracking with MLflow / Weights & Biases",
        "Building ML Inference Pipelines & APIs with FastAPI",
        "A/B Testing Frameworks & Business Decision Science",
      ],
    },
  ],

  "AI/ML Engineer": [
    {
      title: "1. Deep Learning & Framework Foundations",
      steps: [
        "Neural Network Fundamentals & Backpropagation",
        "PyTorch / TensorFlow Deep Learning Architecture",
        "Convolutional Networks (CNNs) for Computer Vision",
        "Transformers & Attention Mechanisms",
      ],
    },
    {
      title: "2. Large Language Models & GenAI",
      steps: [
        "Hugging Face Transformers & Pre-trained Models",
        "Prompt Engineering & Structured Outputs",
        "Retrieval-Augmented Generation (RAG) Systems",
        "Vector Databases (Chroma, Pinecone, Qdrant)",
        "Fine-tuning Techniques (LoRA, QLoRA, PEFT)",
      ],
    },
    {
      title: "3. AI System Architecture & APIs",
      steps: [
        "FastAPI & Asynchronous Model Serving",
        "Model Optimization (Quantization, ONNX, TensorRT, vLLM)",
        "Agentic AI Frameworks (LangGraph, AutoGen)",
      ],
    },
    {
      title: "4. Production MLOps & Cloud Deployment",
      steps: [
        "Containerizing AI Workloads with GPU Support in Docker",
        "Cloud AI Services (AWS SageMaker, Google Vertex AI)",
        "LLM Observability, Evaluation & Guardrails",
      ],
    },
  ],

  "DevOps Engineer": [
    {
      title: "1. Linux, Networking & Scripting",
      steps: [
        "Linux OS Internals, Shell Scripting (Bash) & Automation",
        "Networking Fundamentals (DNS, TCP/IP, HTTP/HTTPS, SSL/TLS, SSH)",
        "Git & Advanced Version Control Workflows",
      ],
    },
    {
      title: "2. Containers & Orchestration",
      steps: [
        "Docker Architecture, Multi-Stage Builds & Container Security",
        "Kubernetes Core Concepts (Pods, Deployments, Services, Ingress)",
        "Helm Package Management for Kubernetes",
      ],
    },
    {
      title: "3. CI/CD & Infrastructure as Code",
      steps: [
        "Continuous Integration / Continuous Delivery with GitHub Actions",
        "Infrastructure as Code (IaC) with Terraform",
        "Cloud Provider Architecture (AWS / GCP Core Services)",
      ],
    },
    {
      title: "4. Observability, Security & Site Reliability",
      steps: [
        "Monitoring with Prometheus & Grafana",
        "Distributed Log Management (ELK Stack / Loki)",
        "DevSecOps (Vulnerability Scanning, Secrets Management with Vault)",
        "Disaster Recovery, High Availability & Chaos Engineering",
      ],
    },
  ],
};