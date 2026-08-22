package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.CareerDto;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CareerService {

    private final List<CareerDto> careers = new ArrayList<>();

    public CareerService() {
        initCareers();
    }

    private void initCareers() {
        careers.add(CareerDto.builder()
                .id("java-backend-dev")
                .title("Java Backend Developer")
                .category("Software Engineering")
                .level("Intermediate")
                .duration("4 - 6 Months")
                .description("Build robust, scalable enterprise server-side microservices, REST APIs, and database architectures using Java and Spring Boot.")
                .overview("Java Backend Developers power mission-critical software systems across banking, e-commerce, and enterprise SaaS. This path masteries Core Java, Spring Boot microservices, ORM with Hibernate/JPA, relational database optimization, caching with Redis, and containerized cloud deployment.")
                .requiredSkills(List.of("Java", "Spring Boot", "SQL", "PostgreSQL", "REST APIs", "Docker", "Git"))
                .technologies(List.of("Java 17+", "Spring Boot 3", "Spring Data JPA", "Hibernate", "PostgreSQL", "Docker", "JUnit", "Redis", "Kafka"))
                .responsibilities(List.of(
                        "Design and build secure, high-throughput RESTful microservices",
                        "Architect database schemas, transactions, and performance indexes",
                        "Implement JWT authentication, authorization, and OAuth2 security",
                        "Write automated unit and integration tests with JUnit and Mockito",
                        "Containerize and deploy backend services using Docker and CI/CD pipelines"
                ))
                .salaryRange("$85,000 - $145,000 / year")
                .roadmapSlug("java-backend-developer")
                .build());

        careers.add(CareerDto.builder()
                .id("frontend-dev")
                .title("Frontend Developer")
                .category("Software Engineering")
                .level("Beginner")
                .duration("3 - 5 Months")
                .description("Craft fast, responsive, and delightful user interfaces for modern web applications using React, Next.js, and TypeScript.")
                .overview("Frontend Developers turn design concepts into fast, interactive web applications. You will learn modern JavaScript/TypeScript, React 19, Next.js App Router, Tailwind CSS, state management, accessibility, and client-side performance optimization.")
                .requiredSkills(List.of("JavaScript", "TypeScript", "React", "Next.js", "HTML", "CSS", "Tailwind CSS", "Git"))
                .technologies(List.of("HTML5", "CSS3", "JavaScript (ES6+)", "TypeScript", "React", "Next.js", "Tailwind CSS", "Framer Motion", "Vite"))
                .responsibilities(List.of(
                        "Develop accessible and responsive web interfaces matching Figma designs",
                        "Manage asynchronous data fetching, caching, and state management",
                        "Optimize Core Web Vitals (LCP, FID, CLS) and render performance",
                        "Integrate RESTful and GraphQL APIs seamlessly with client apps",
                        "Implement cross-browser compatibility and responsive viewports"
                ))
                .salaryRange("$75,000 - $130,000 / year")
                .roadmapSlug("frontend-developer")
                .build());

        careers.add(CareerDto.builder()
                .id("full-stack-dev")
                .title("Full Stack Developer")
                .category("Software Engineering")
                .level("Advanced")
                .duration("6 - 9 Months")
                .description("Master both client and server development to build complete end-to-end production web applications and cloud services.")
                .overview("Full Stack Developers possess the versatility to build entire applications from interactive React frontends to resilient Java/Node backend APIs and relational database architectures.")
                .requiredSkills(List.of("Java", "Spring Boot", "React", "Next.js", "TypeScript", "SQL", "Docker", "REST APIs"))
                .technologies(List.of("React", "Next.js", "TypeScript", "Java / Node.js", "Spring Boot", "PostgreSQL", "Docker", "AWS", "Git"))
                .responsibilities(List.of(
                        "Architect end-to-end web applications from UI to database",
                        "Develop secure REST and GraphQL API services",
                        "Design normalized database schemas and optimize query performance",
                        "Deploy full-stack applications with Docker and modern CI/CD",
                        "Bridge UX designs and backend architecture effectively"
                ))
                .salaryRange("$95,000 - $160,000 / year")
                .roadmapSlug("full-stack-developer")
                .build());

        careers.add(CareerDto.builder()
                .id("ai-ml-engineer")
                .title("AI/ML Engineer")
                .category("Data & AI")
                .level("Advanced")
                .duration("6 - 10 Months")
                .description("Develop machine learning models, train deep neural networks, and deploy LLM applications into production pipelines.")
                .overview("AI and Machine Learning Engineers create predictive models and intelligent automated systems. You will learn Python, data pipelines with Pandas/NumPy, machine learning algorithms with Scikit-Learn, deep learning with PyTorch, and fine-tuning/RAG for Large Language Models.")
                .requiredSkills(List.of("Python", "Machine Learning", "PyTorch", "Pandas", "SQL", "Docker", "Git"))
                .technologies(List.of("Python", "PyTorch", "TensorFlow", "Scikit-Learn", "Pandas", "NumPy", "LangChain", "FastAPI", "Hugging Face", "MLflow"))
                .responsibilities(List.of(
                        "Train, evaluate, and fine-tune machine learning and deep learning models",
                        "Build data processing pipelines and feature engineering systems",
                        "Develop Retrieval-Augmented Generation (RAG) and LLM-powered services",
                        "Deploy model inference microservices with FastAPI and Docker",
                        "Monitor model performance, data drift, and latency in production"
                ))
                .salaryRange("$110,000 - $180,000 / year")
                .roadmapSlug("ai-ml-engineer")
                .build());

        careers.add(CareerDto.builder()
                .id("data-analyst")
                .title("Data Analyst")
                .category("Data & AI")
                .level("Beginner")
                .duration("3 - 4 Months")
                .description("Transform raw business data into actionable visual insights, KPI dashboards, and statistical decision frameworks.")
                .overview("Data Analysts empower organizational leadership to make data-driven decisions. You will master SQL querying, data cleaning with Python/Pandas, exploratory data analysis, and interactive dashboard creation with Power BI and Tableau.")
                .requiredSkills(List.of("SQL", "Python", "Pandas", "Power BI", "Excel", "Data Visualization"))
                .technologies(List.of("SQL (PostgreSQL/MySQL)", "Python", "Pandas", "Power BI", "Tableau", "Excel / Google Sheets", "Matplotlib / Seaborn"))
                .responsibilities(List.of(
                        "Write complex SQL queries to extract and aggregate business metrics",
                        "Clean, transform, and validate messy datasets using Python and Pandas",
                        "Design dynamic, executive-facing dashboards in Power BI or Tableau",
                        "Perform exploratory data analysis and statistical hypothesis testing",
                        "Present actionable recommendations to cross-functional teams"
                ))
                .salaryRange("$65,000 - $110,000 / year")
                .roadmapSlug("data-analyst")
                .build());

        careers.add(CareerDto.builder()
                .id("devops-engineer")
                .title("DevOps Engineer")
                .category("Cloud & DevOps")
                .level("Intermediate")
                .duration("5 - 7 Months")
                .description("Automate cloud infrastructure provisioning, CI/CD pipelines, container orchestration, and system reliability monitoring.")
                .overview("DevOps Engineers bridge software development and operations to achieve rapid, reliable software releases. You will master Linux administration, Docker containerization, Kubernetes orchestration, Infrastructure as Code with Terraform, CI/CD automation, and Prometheus/Grafana observability.")
                .requiredSkills(List.of("Docker", "Kubernetes", "AWS", "Linux", "CI/CD", "Git", "Terraform"))
                .technologies(List.of("Docker", "Kubernetes", "AWS / GCP", "Terraform", "GitHub Actions", "Linux / Bash", "Prometheus", "Grafana", "ArgoCD"))
                .responsibilities(List.of(
                        "Build and maintain automated CI/CD release pipelines",
                        "Manage production Kubernetes clusters and container workloads",
                        "Provision and audit cloud infrastructure using Terraform (IaC)",
                        "Implement system monitoring, alerting, and log aggregation",
                        "Ensure high availability, disaster recovery, and zero-downtime deployments"
                ))
                .salaryRange("$95,000 - $155,000 / year")
                .roadmapSlug("devops-engineer")
                .build());

        careers.add(CareerDto.builder()
                .id("cloud-solutions-architect")
                .title("Cloud Solutions Architect")
                .category("Cloud & DevOps")
                .level("Advanced")
                .duration("6 - 8 Months")
                .description("Design secure, resilient, and cost-effective cloud enterprise infrastructure and multi-region distributed system architectures.")
                .overview("Cloud Solutions Architects design scalable distributed architectures on major cloud platforms like AWS and Azure. You will master cloud computing fundamentals, high-availability architecture, networking (VPC, DNS, CDN), serverless paradigms, security compliance, and disaster recovery.")
                .requiredSkills(List.of("AWS", "Cloud Architecture", "Docker", "Kubernetes", "Linux", "Networking", "Security"))
                .technologies(List.of("AWS (EC2, S3, RDS, Lambda, ECS, EKS)", "Terraform", "Docker", "Kubernetes", "CloudFront", "Route 53", "IAM", "OpenSearch"))
                .responsibilities(List.of(
                        "Design scalable, multi-region cloud architectures for high availability",
                        "Evaluate cost optimization, resource sizing, and security compliance",
                        "Implement cloud networking, VPC topologies, and firewall policies",
                        "Architect disaster recovery strategies and automated backup pipelines",
                        "Guide development teams on cloud-native best practices and migration"
                ))
                .salaryRange("$120,000 - $190,000 / year")
                .roadmapSlug("cloud-solutions-architect")
                .build());
    }

    public List<CareerDto> getAllCareers() {
        return Collections.unmodifiableList(careers);
    }

    public Optional<CareerDto> getCareerByIdOrTitle(String idOrTitle) {
        return careers.stream()
                .filter(c -> c.getId().equalsIgnoreCase(idOrTitle) || c.getTitle().equalsIgnoreCase(idOrTitle))
                .findFirst();
    }
}
