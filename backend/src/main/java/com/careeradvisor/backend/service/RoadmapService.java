package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.RoadmapSectionDto;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RoadmapService {

    private final Map<String, List<RoadmapSectionDto>> roadmaps = new LinkedHashMap<>();

    public RoadmapService() {
        initRoadmaps();
    }

    private void initRoadmaps() {
        // 1. Java Backend Developer
        roadmaps.put("Java Backend Developer", List.of(
                new RoadmapSectionDto("1. Java Fundamentals & OOP", List.of(
                        "Java Syntax & Data Types",
                        "Object-Oriented Programming (OOP)",
                        "Exception Handling",
                        "Java Collections Framework (List, Set, Map)",
                        "Java 8+ Features (Lambdas, Streams, Optional)"
                )),
                new RoadmapSectionDto("2. Relational Databases & SQL", List.of(
                        "Relational Database Concepts (PostgreSQL / MySQL)",
                        "SQL Queries (Joins, Aggregations, Subqueries)",
                        "Database Normalization & Indexing",
                        "JDBC & Connection Pooling"
                )),
                new RoadmapSectionDto("3. Spring Boot Framework", List.of(
                        "Spring Core & Dependency Injection",
                        "Spring Boot Auto-configuration & Starter POMs",
                        "Building RESTful APIs with Spring MVC",
                        "Spring Data JPA & Hibernate ORM",
                        "Spring Security & JWT Authentication"
                )),
                new RoadmapSectionDto("4. API Design & Architecture", List.of(
                        "RESTful API Best Practices & OpenAPI (Swagger)",
                        "Pagination, Sorting, and DTO Validation",
                        "Global Exception Handling & Structured JSON Responses",
                        "Asynchronous Processing & Scheduled Tasks"
                )),
                new RoadmapSectionDto("5. Testing & Code Quality", List.of(
                        "Unit Testing with JUnit 5 & AssertJ",
                        "Mocking Dependencies with Mockito",
                        "Integration Testing with @SpringBootTest & Testcontainers",
                        "Code Formatting & Static Code Analysis"
                )),
                new RoadmapSectionDto("6. Deployment & Microservices Basics", List.of(
                        "Containerization with Docker & Docker Compose",
                        "Caching Strategies with Redis",
                        "Message Queues Basics (Kafka / RabbitMQ)",
                        "CI/CD with GitHub Actions & Cloud Deployment"
                ))
        ));

        // 2. Frontend Developer
        roadmaps.put("Frontend Developer", List.of(
                new RoadmapSectionDto("1. Web Foundations", List.of(
                        "Semantic HTML5 & Accessibility (ARIA)",
                        "Modern CSS3 (Flexbox, CSS Grid, Responsive Design)",
                        "JavaScript Fundamentals (ES6+, DOM, Fetch API)",
                        "Git & Version Control Workflow"
                )),
                new RoadmapSectionDto("2. Modern UI Frameworks (React & Next.js)", List.of(
                        "React Components, Props & State",
                        "React Hooks (useState, useEffect, useMemo, useCallback)",
                        "Next.js App Router & Server Components",
                        "Tailwind CSS & Component Styling"
                )),
                new RoadmapSectionDto("3. State & Data Flow", List.of(
                        "Client State Management (Zustand / Context API)",
                        "Server State & Data Fetching (TanStack Query / SWR)",
                        "Form Validation with Zod & React Hook Form",
                        "Routing, Dynamic Segments & Layouts"
                )),
                new RoadmapSectionDto("4. TypeScript for Web Development", List.of(
                        "TypeScript Basics & Primitive Types",
                        "Interfaces, Types, Generics & Utility Types",
                        "Type-Safe Component Props & Event Handlers",
                        "Next.js TypeScript Best Practices"
                )),
                new RoadmapSectionDto("5. Performance & Production Deployment", List.of(
                        "Core Web Vitals & Image Optimization",
                        "Lighthouse Audits & Accessibility Compliance",
                        "Unit & Component Testing with Vitest / Jest",
                        "Vercel / Cloudflare Pages Deployment"
                ))
        ));

        // 3. Full Stack Developer
        roadmaps.put("Full Stack Developer", List.of(
                new RoadmapSectionDto("1. Frontend Core (React & TypeScript)", List.of(
                        "Modern TypeScript & React Ecosystem",
                        "Next.js App Router Architecture",
                        "Tailwind CSS & Responsive Layout Systems",
                        "Client-side State & API Integration"
                )),
                new RoadmapSectionDto("2. Server & API Engineering", List.of(
                        "RESTful API Design & Contract Standards",
                        "Backend Frameworks (Spring Boot / Express / NestJS)",
                        "JWT Authentication & Session Management",
                        "Middleware, Interceptors & Error Pipelines"
                )),
                new RoadmapSectionDto("3. Database Architecture & Storage", List.of(
                        "PostgreSQL Schema Design & Migrations (Flyway)",
                        "JPA / Prisma ORM & Connection Optimization",
                        "Redis Caching & Session Storage",
                        "S3-compatible Object Storage for Media"
                )),
                new RoadmapSectionDto("4. Full-Stack Integration & Security", List.of(
                        "CORS, CSRF & Content Security Policies",
                        "Role-Based Access Control (RBAC)",
                        "WebSockets & Real-Time Notifications",
                        "End-to-End Type Safety (tRPC / OpenAPI Generator)"
                )),
                new RoadmapSectionDto("5. Testing & Observability", List.of(
                        "Frontend Unit & Integration Testing",
                        "Backend Unit & Integration Testing with Mockito",
                        "End-to-End Testing with Playwright",
                        "Structured Logging & Health Monitoring"
                )),
                new RoadmapSectionDto("6. Cloud Deployment & CI/CD", List.of(
                        "Multi-stage Docker Builds for Frontend & Backend",
                        "Docker Compose Orchestration",
                        "Automated CI/CD Pipelines (GitHub Actions)",
                        "Cloud Hosting (AWS ECS / Render / Railway)"
                ))
        ));

        // 4. AI/ML Engineer
        roadmaps.put("AI/ML Engineer", List.of(
                new RoadmapSectionDto("1. Mathematics & Python Essentials", List.of(
                        "Linear Algebra, Calculus & Probability for ML",
                        "Advanced Python (OOP, Typing, Generators)",
                        "NumPy & Vectorized Matrix Operations",
                        "Pandas Data Wrangling & Feature Transformation"
                )),
                new RoadmapSectionDto("2. Classical Machine Learning", List.of(
                        "Supervised Learning (Regression, Trees, Ensemble Models)",
                        "Unsupervised Learning (K-Means, PCA, Clustering)",
                        "Model Evaluation Metrics (Precision, Recall, ROC-AUC)",
                        "Scikit-Learn Pipelines & Hyperparameter Tuning"
                )),
                new RoadmapSectionDto("3. Deep Learning Foundations", List.of(
                        "Neural Networks & Backpropagation from Scratch",
                        "PyTorch Fundamentals (Tensors, Autograd, Modules)",
                        "Convolutional Neural Networks (CNN) for Computer Vision",
                        "Recurrent Architectures & Transformers Basics"
                )),
                new RoadmapSectionDto("4. Generative AI & Large Language Models", List.of(
                        "Transformer Architecture (Self-Attention, Encoders, Decoders)",
                        "Hugging Face Transformers & Pretrained Models",
                        "Retrieval-Augmented Generation (RAG) Architecture",
                        "Vector Databases (Pinecone / Chroma / pgvector)",
                        "LangChain / LlamaIndex Frameworks"
                )),
                new RoadmapSectionDto("5. MLOps & Production Inference", List.of(
                        "FastAPI Inference Microservices",
                        "Model Versioning & Experiment Tracking (MLflow / W&B)",
                        "Dockerizing Machine Learning Workloads",
                        "GPU-Accelerated Inference & Cloud Serving"
                ))
        ));

        // 5. Data Analyst
        roadmaps.put("Data Analyst", List.of(
                new RoadmapSectionDto("1. SQL Mastery for Data Extraction", List.of(
                        "Relational Schema Navigation & ER Diagrams",
                        "Advanced Joins, Grouping & Set Operations",
                        "SQL Window Functions (ROW_NUMBER, RANK, LAG/LEAD)",
                        "Common Table Expressions (CTEs) & Subqueries",
                        "Query Performance & Indexing Basics"
                )),
                new RoadmapSectionDto("2. Spreadsheets & Business Intelligence", List.of(
                        "Advanced Excel (XLOOKUP, Pivot Tables, Power Query)",
                        "Data Modeling & Relationships in Power BI / Tableau",
                        "DAX Formulas & Calculated Measures",
                        "Interactive Executive Dashboard Design"
                )),
                new RoadmapSectionDto("3. Python for Data Analytics", List.of(
                        "Python Data Types, Loops & Functions",
                        "Pandas Series & DataFrames Operations",
                        "Handling Missing Values, Duplicates & Outliers",
                        "Exploratory Data Analysis (EDA) Workflow"
                )),
                new RoadmapSectionDto("4. Data Visualization & Storytelling", List.of(
                        "Data Visualization Principles (Chart Selection, Color Theory)",
                        "Matplotlib & Seaborn Plotting",
                        "Building KPI Tracking Systems",
                        "Translating Data Metrics into Executive Summaries"
                )),
                new RoadmapSectionDto("5. Applied Statistics & Experimentation", List.of(
                        "Descriptive & Inferential Statistics",
                        "A/B Testing Foundations & Hypothesis Testing",
                        "Correlation vs. Causation Analysis",
                        "End-to-End Business Case Study Presentation"
                ))
        ));

        // 6. DevOps Engineer
        roadmaps.put("DevOps Engineer", List.of(
                new RoadmapSectionDto("1. Operating Systems & Networking", List.of(
                        "Linux Administration, Shell Scripting & Bash",
                        "Networking Fundamentals (TCP/IP, DNS, HTTP/HTTPS, SSH)",
                        "Process Management, Memory & System Performance",
                        "Security Basics (SSH Keys, File Permissions, Firewalls)"
                )),
                new RoadmapSectionDto("2. Containers & Microservices", List.of(
                        "Docker Core Concepts (Images, Containers, Layers)",
                        "Writing Optimized, Multi-Stage Dockerfiles",
                        "Docker Compose Multi-Container Orchestration",
                        "Container Security & Image Scanning"
                )),
                new RoadmapSectionDto("3. CI/CD Pipeline Automation", List.of(
                        "Git Branching Strategies (Trunk-based, Gitflow)",
                        "GitHub Actions Workflow Authoring",
                        "Automated Testing & Linting in CI",
                        "Artifact Storage & Container Registry Publishing"
                )),
                new RoadmapSectionDto("4. Infrastructure as Code (IaC) & Cloud", List.of(
                        "Terraform Fundamentals (Providers, Resources, State)",
                        "Modular Infrastructure Provisioning on AWS",
                        "AWS Core Services (VPC, EC2, S3, RDS, IAM)",
                        "Cloud Cost Management & Resource Lifecycle"
                )),
                new RoadmapSectionDto("5. Kubernetes & Observability", List.of(
                        "Kubernetes Architecture (Pods, Deployments, Services)",
                        "Ingress Controllers, ConfigMaps & Secrets",
                        "Prometheus Metrics & Grafana Dashboards",
                        "Log Aggregation with EFK / Loki"
                ))
        ));

        // 7. Cloud Solutions Architect
        roadmaps.put("Cloud Solutions Architect", List.of(
                new RoadmapSectionDto("1. Cloud Fundamentals & Architecture Principles", List.of(
                        "Cloud Service Models (IaaS, PaaS, SaaS, Serverless)",
                        "AWS Well-Architected Framework (6 Pillars)",
                        "High Availability, Fault Tolerance & Redundancy",
                        "Multi-Region & Hybrid Cloud Strategies"
                )),
                new RoadmapSectionDto("2. Cloud Networking & Security", List.of(
                        "VPC Design, Subnets, Route Tables & NAT Gateways",
                        "DNS Routing with Route 53 & CDN with CloudFront",
                        "IAM Policies, Roles, MFA & Least Privilege Access",
                        "Data Encryption at Rest and in Transit (KMS, TLS)"
                )),
                new RoadmapSectionDto("3. Compute, Storage & Databases", List.of(
                        "EC2 Auto Scaling Groups & Elastic Load Balancing",
                        "Serverless Compute with AWS Lambda & API Gateway",
                        "S3 Storage Classes, Lifecycle Policies & Replication",
                        "Relational (RDS/Aurora) vs. NoSQL (DynamoDB) Architecture"
                )),
                new RoadmapSectionDto("4. Scalability, Caching & Messaging", List.of(
                        "Decoupled Microservices with SQS & SNS",
                        "Event-Driven Architectures with EventBridge",
                        "Distributed In-Memory Caching with ElastiCache (Redis)",
                        "Microservice Container Orchestration with ECS & EKS"
                )),
                new RoadmapSectionDto("5. Governance, Cost & Disaster Recovery", List.of(
                        "Disaster Recovery Strategies (Backup/Restore, Warm Standby, Multi-Site)",
                        "Infrastructure as Code with Terraform & AWS CDK",
                        "CloudWatch, CloudTrail & Centralized Observability",
                        "AWS Cost Explorer & FinOps Optimization"
                ))
        ));
    }

    public Map<String, List<RoadmapSectionDto>> getAllRoadmaps() {
        return Collections.unmodifiableMap(roadmaps);
    }

    public List<RoadmapSectionDto> getRoadmapForCareer(String careerTitleOrSlug) {
        if (careerTitleOrSlug == null || careerTitleOrSlug.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String query = careerTitleOrSlug.trim().replace("-", " ").toLowerCase();
        for (Map.Entry<String, List<RoadmapSectionDto>> entry : roadmaps.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(careerTitleOrSlug) ||
                entry.getKey().toLowerCase().replace("-", " ").equals(query)) {
                return entry.getValue();
            }
        }

        return Collections.emptyList();
    }

    public int getTotalStepsForCareer(String careerTitleOrSlug) {
        List<RoadmapSectionDto> sections = getRoadmapForCareer(careerTitleOrSlug);
        return sections.stream().mapToInt(s -> s.getSteps().size()).sum();
    }
}
