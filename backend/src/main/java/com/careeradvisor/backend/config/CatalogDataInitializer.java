package com.careeradvisor.backend.config;

import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.*;
import com.careeradvisor.backend.service.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@Order(10)
public class CatalogDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CatalogDataInitializer.class);

    private final CareerTrackRepository careerTrackRepository;
    private final TechnologyRepository technologyRepository;
    private final CompetencyNodeRepository competencyNodeRepository;
    private final CatalogService catalogService;

    public CatalogDataInitializer(CareerTrackRepository careerTrackRepository,
                                  TechnologyRepository technologyRepository,
                                  CompetencyNodeRepository competencyNodeRepository,
                                  CatalogService catalogService) {
        this.careerTrackRepository = careerTrackRepository;
        this.technologyRepository = technologyRepository;
        this.competencyNodeRepository = competencyNodeRepository;
        this.catalogService = catalogService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Initializing Career Advisor Catalog Foundation...");

        // 1. Seed Technologies
        Map<String, Technology> techMap = seedTechnologies();

        // 2. Seed Career Tracks and associate with Technologies
        seedCareerTracks(techMap);

        // 3. Seed Canonical Competencies
        Map<String, CompetencyNode> compMap = seedCompetencyNodes(techMap);

        // 4. Seed Validated Prerequisite Relationships
        seedPrerequisites(compMap);

        logger.info("Career Advisor Catalog Foundation initialization completed successfully.");
    }

    private Map<String, Technology> seedTechnologies() {
        Map<String, Technology> map = new HashMap<>();

        List<TechSpec> specs = List.of(
                new TechSpec("Java", "java", "Programming Language", "java", "Core object-oriented, cross-platform enterprise programming language."),
                new TechSpec("Spring Boot", "spring-boot", "Backend Framework", "spring", "Convention-over-configuration enterprise microservices and REST API framework."),
                new TechSpec("SQL", "sql", "Database", "database", "Structured Query Language for relational database querying and schema manipulation."),
                new TechSpec("PostgreSQL", "postgresql", "Database", "postgresql", "Powerful, open-source object-relational database management system."),
                new TechSpec("Docker", "docker", "DevOps & Cloud", "docker", "Operating system-level virtualization for packaging containerized software applications."),
                new TechSpec("Kubernetes", "kubernetes", "DevOps & Cloud", "kubernetes", "Automated container orchestration, scaling, and operational management system."),
                new TechSpec("Git", "git", "Tooling & VCS", "git", "Distributed version control system for tracking changes in source code."),
                new TechSpec("JavaScript", "javascript", "Programming Language", "javascript", "High-level, interpreted scripting language powering modern web applications."),
                new TechSpec("TypeScript", "typescript", "Programming Language", "typescript", "Strongly-typed superset of JavaScript that compiles to plain JavaScript."),
                new TechSpec("React", "react", "Frontend Framework", "react", "Declarative, component-based user interface rendering library."),
                new TechSpec("Next.js", "nextjs", "Frontend Framework", "nextjs", "Full-stack React framework featuring App Router, SSR, and Turbopack."),
                new TechSpec("Tailwind CSS", "tailwindcss", "Styling & UI", "tailwindcss", "Utility-first CSS framework for rapid and responsive UI development."),
                new TechSpec("HTML5 & CSS3", "html-css", "Web Foundations", "html", "Foundational web markup and styling standards for accessible web applications."),
                new TechSpec("Python", "python", "Programming Language", "python", "Versatile high-level programming language renowned for data science and AI/ML."),
                new TechSpec("Pandas & NumPy", "pandas-numpy", "Data Science", "python", "High-performance Python libraries for numerical computing and tabular data analysis."),
                new TechSpec("PyTorch", "pytorch", "AI & Deep Learning", "pytorch", "Tensors and dynamic neural networks framework for deep learning research and production."),
                new TechSpec("Linux & Bash", "linux-bash", "Operating Systems", "terminal", "Open-source Unix-like operating system and command line shell scripting."),
                new TechSpec("Terraform", "terraform", "Infrastructure as Code", "cloud", "Infrastructure-as-code software tool for provisioning cloud infrastructure."),
                new TechSpec("AWS Cloud", "aws", "Cloud Platform", "aws", "Comprehensive on-demand cloud computing services and distributed architectures."),
                new TechSpec("Redis", "redis", "Database & Caching", "redis", "In-memory data structure store used as distributed cache, database, and message broker."),
                new TechSpec("Apache Kafka", "kafka", "Messaging & Streaming", "server", "Distributed event streaming platform for high-throughput, fault-tolerant pipelines."),
                new TechSpec("Excel & BI", "excel-bi", "Business Intelligence", "chart", "Spreadsheet analysis, Power BI, and interactive executive data dashboards.")
        );

        for (TechSpec spec : specs) {
            Technology tech = technologyRepository.findBySlug(spec.slug)
                    .orElseGet(() -> technologyRepository.save(
                            Technology.builder()
                                    .name(spec.name)
                                    .slug(spec.slug)
                                    .category(spec.category)
                                    .icon(spec.icon)
                                    .description(spec.description)
                                    .active(true)
                                    .build()
                    ));
            map.put(spec.slug, tech);
        }

        return map;
    }

    private void seedCareerTracks(Map<String, Technology> techMap) {
        List<CareerSpec> careers = List.of(
                new CareerSpec(
                        "Java Backend Developer",
                        "java-backend-developer",
                        "Software Engineering",
                        "Intermediate",
                        "4 - 6 Months",
                        "$85,000 - $145,000 / year",
                        "Build robust, scalable enterprise server-side microservices, REST APIs, and database architectures using Java and Spring Boot.",
                        "Java Backend Developers power mission-critical software systems across banking, e-commerce, and enterprise SaaS. This path covers Core Java, Spring Boot microservices, ORM with Hibernate/JPA, relational database optimization, caching with Redis, and containerized cloud deployment.",
                        List.of("java", "spring-boot", "sql", "postgresql", "docker", "git", "redis", "kafka")
                ),
                new CareerSpec(
                        "Frontend Developer",
                        "frontend-developer",
                        "Software Engineering",
                        "Beginner",
                        "3 - 5 Months",
                        "$75,000 - $130,000 / year",
                        "Craft fast, responsive, and delightful user interfaces for modern web applications using React, Next.js, and TypeScript.",
                        "Frontend Developers turn design concepts into fast, interactive web applications. You will learn modern JavaScript/TypeScript, React 19, Next.js App Router, Tailwind CSS, state management, accessibility, and client-side performance optimization.",
                        List.of("javascript", "typescript", "react", "nextjs", "html-css", "tailwindcss", "git")
                ),
                new CareerSpec(
                        "Full Stack Developer",
                        "full-stack-developer",
                        "Software Engineering",
                        "Advanced",
                        "6 - 9 Months",
                        "$95,000 - $160,000 / year",
                        "Master both client and server development to build complete end-to-end production web applications and cloud services.",
                        "Full Stack Developers possess the versatility to build entire applications from interactive React frontends to resilient Java/Node backend APIs and relational database architectures.",
                        List.of("java", "spring-boot", "react", "nextjs", "typescript", "sql", "docker", "aws", "git")
                ),
                new CareerSpec(
                        "AI/ML Engineer",
                        "ai-ml-engineer",
                        "Data & AI",
                        "Advanced",
                        "6 - 10 Months",
                        "$110,000 - $180,000 / year",
                        "Develop machine learning models, train deep neural networks, and deploy LLM applications into production pipelines.",
                        "AI and Machine Learning Engineers create predictive models and intelligent automated systems. You will learn Python, data pipelines with Pandas/NumPy, machine learning algorithms, deep learning with PyTorch, and RAG architectures.",
                        List.of("python", "pandas-numpy", "pytorch", "sql", "docker", "git")
                ),
                new CareerSpec(
                        "Data Analyst",
                        "data-analyst",
                        "Data & AI",
                        "Beginner",
                        "3 - 5 Months",
                        "$65,000 - $115,000 / year",
                        "Transform raw business data into actionable executive insights through SQL queries, statistical modeling, and interactive dashboards.",
                        "Data Analysts bridge data engineering and business decision-making. You will master SQL data extraction, exploratory data analysis with Python, and visual storytelling with modern Business Intelligence dashboards.",
                        List.of("sql", "excel-bi", "python", "pandas-numpy", "git")
                ),
                new CareerSpec(
                        "DevOps Engineer",
                        "devops-engineer",
                        "Cloud & DevOps",
                        "Intermediate",
                        "5 - 8 Months",
                        "$90,000 - $155,000 / year",
                        "Automate deployment pipelines, orchestrate container clusters, and manage scalable cloud infrastructure reliability.",
                        "DevOps Engineers empower modern continuous delivery. You will master Linux systems administration, Docker containerization, Kubernetes cluster management, CI/CD with GitHub Actions, and Infrastructure as Code with Terraform.",
                        List.of("linux-bash", "docker", "kubernetes", "terraform", "aws", "git")
                ),
                new CareerSpec(
                        "Cloud Solutions Architect",
                        "cloud-solutions-architect",
                        "Cloud & Architecture",
                        "Advanced",
                        "7 - 12 Months",
                        "$120,000 - $190,000 / year",
                        "Architect fault-tolerant, highly available, secure, and cost-optimized multi-region distributed cloud systems.",
                        "Cloud Solutions Architects design enterprise cloud infrastructures adhering to the AWS Well-Architected Framework, covering VPC networking, high-throughput caching with Redis, microservice event streaming with Kafka, and cloud governance.",
                        List.of("aws", "terraform", "docker", "kubernetes", "linux-bash", "redis", "kafka", "postgresql")
                )
        );

        for (CareerSpec spec : careers) {
            CareerTrack track = careerTrackRepository.findBySlug(spec.slug).orElse(null);
            if (track == null) {
                Set<Technology> technologies = new HashSet<>();
                for (String techSlug : spec.techSlugs) {
                    Technology tech = techMap.get(techSlug);
                    if (tech != null) {
                        technologies.add(tech);
                    }
                }

                track = CareerTrack.builder()
                        .title(spec.title)
                        .slug(spec.slug)
                        .category(spec.category)
                        .level(spec.level)
                        .duration(spec.duration)
                        .salaryRange(spec.salaryRange)
                        .description(spec.description)
                        .overview(spec.overview)
                        .technologies(technologies)
                        .active(true)
                        .build();

                careerTrackRepository.save(track);
            }
        }
    }

    private Map<String, CompetencyNode> seedCompetencyNodes(Map<String, Technology> techMap) {
        Map<String, CompetencyNode> map = new HashMap<>();

        List<CompSpec> specs = List.of(
                // Java
                new CompSpec("java", "java-syntax", "Java Syntax & Data Types", "Core primitive data types, variables, loops, control structures, and methods.", "Beginner", 1),
                new CompSpec("java", "java-oop", "Object-Oriented Programming (OOP)", "Inheritance, Polymorphism, Encapsulation, Abstraction, and Interfaces.", "Beginner", 2),
                new CompSpec("java", "java-collections", "Java Collections Framework", "Lists, Sets, Maps, Queues, iteration contracts, and Big-O efficiency.", "Intermediate", 3),
                new CompSpec("java", "java-streams", "Lambdas & Streams API", "Functional interfaces, Stream pipelines, map-filter-reduce, and Optional.", "Intermediate", 4),

                // SQL
                new CompSpec("sql", "sql-basics", "Relational Database Concepts", "Tables, primary keys, foreign keys, constraints, and relational schemas.", "Beginner", 1),
                new CompSpec("sql", "sql-queries", "SQL Queries, Joins & Aggregations", "INNER/LEFT/RIGHT JOINs, GROUP BY, HAVING, subqueries, and CTEs.", "Intermediate", 2),
                new CompSpec("sql", "sql-indexing", "Normalization & Indexing", "Database normalization forms, B-Tree indexes, and execution query plans.", "Advanced", 3),

                // Spring Boot
                new CompSpec("spring-boot", "spring-core-di", "Spring Core & Dependency Injection", "Inversion of Control (IoC), ApplicationContext, Bean lifecycle, and autowiring.", "Intermediate", 1),
                new CompSpec("spring-boot", "spring-boot-rest", "RESTful APIs with Spring MVC", "Request mappings, HTTP verbs, response status codes, DTOs, and global exception handling.", "Intermediate", 2),
                new CompSpec("spring-boot", "spring-data-jpa", "Spring Data JPA & Hibernate ORM", "Entity mapping, repositories, transaction management, lazy loading, and caching.", "Intermediate", 3),
                new CompSpec("spring-boot", "spring-security-jwt", "Spring Security & JWT Authentication", "SecurityFilterChain, authentication providers, JWT claim generation, and role authorization.", "Advanced", 4),

                // JavaScript
                new CompSpec("javascript", "js-fundamentals", "JavaScript ES6+ Fundamentals", "Let/const, arrow functions, destructuring, modules, DOM APIs, and Fetch.", "Beginner", 1),
                new CompSpec("javascript", "js-async", "Asynchronous JavaScript & Promises", "Event loop, Promises, async/await, and error handling in asynchronous code.", "Intermediate", 2),

                // TypeScript
                new CompSpec("typescript", "ts-basics", "TypeScript Fundamentals & Types", "Static typing, Interfaces, Type aliases, Union types, and Type inference.", "Beginner", 1),
                new CompSpec("typescript", "ts-generics", "Generics & Advanced Types", "Generic functions/classes, Utility types (Partial, Pick, Omit), and type guards.", "Intermediate", 2),

                // React
                new CompSpec("react", "react-components", "React Components & State", "JSX, functional components, props, state, conditional rendering, and keys.", "Beginner", 1),
                new CompSpec("react", "react-hooks", "React Hooks & Lifecycle", "useState, useEffect, useMemo, useCallback, useRef, and custom hooks.", "Intermediate", 2),

                // Next.js
                new CompSpec("nextjs", "nextjs-app-router", "Next.js App Router & Server Components", "Layouts, pages, server vs client components, dynamic routes, and navigation.", "Intermediate", 1),
                new CompSpec("nextjs", "nextjs-data-fetching", "Server Actions & Caching", "Data fetching patterns, Server Actions, route handlers, and revalidation.", "Advanced", 2),

                // Docker
                new CompSpec("docker", "docker-containers", "Docker Containers & Images", "Writing Dockerfiles, layers, image building, container lifecycles, and ports.", "Beginner", 1),
                new CompSpec("docker", "docker-compose", "Docker Compose Multi-Container Orchestration", "Defining services, networks, volumes, and multi-container coordination.", "Intermediate", 2),

                // Python
                new CompSpec("python", "python-basics", "Python Syntax & OOP", "Variables, lists, dicts, functions, OOP classes, and package management.", "Beginner", 1),
                new CompSpec("pandas-numpy", "pandas-data-wrangling", "Pandas Data Wrangling & Analysis", "DataFrames, Series, missing data imputation, grouping, and aggregations.", "Intermediate", 1),

                // PyTorch
                new CompSpec("pytorch", "pytorch-tensors", "Tensors & Automatic Differentiation", "PyTorch Tensors, Autograd, GPU acceleration, and tensor manipulation.", "Intermediate", 1),
                new CompSpec("pytorch", "pytorch-neural-nets", "Building Neural Network Architectures", "nn.Module, loss functions, optimizers, and model training loops.", "Advanced", 2),

                // Linux
                new CompSpec("linux-bash", "linux-shell", "Linux Shell & Command Line Mastery", "File system navigation, permissions, process monitoring, piping, and Bash.", "Beginner", 1),

                // AWS
                new CompSpec("aws", "aws-core-services", "AWS Core Cloud Services", "VPC networking, EC2 compute instances, S3 storage buckets, and RDS databases.", "Intermediate", 1)
        );

        for (CompSpec spec : specs) {
            Technology tech = techMap.get(spec.techSlug);
            if (tech != null) {
                CompetencyNode node = competencyNodeRepository.findByCompetencyKey(spec.key)
                        .orElseGet(() -> competencyNodeRepository.save(
                                CompetencyNode.builder()
                                        .technology(tech)
                                        .competencyKey(spec.key)
                                        .title(spec.title)
                                        .description(spec.description)
                                        .level(spec.level)
                                        .sequenceOrder(spec.sequence)
                                        .active(true)
                                        .build()
                        ));
                map.put(spec.key, node);
            }
        }

        return map;
    }

    private void seedPrerequisites(Map<String, CompetencyNode> compMap) {
        List<PrereqSpec> prereqs = List.of(
                // Java Progression
                new PrereqSpec("java-oop", "java-syntax", "Core syntax and control structures are required to model object-oriented classes."),
                new PrereqSpec("java-collections", "java-oop", "Collections Framework heavily utilizes OOP interfaces, polymorphism, and generics."),
                new PrereqSpec("java-streams", "java-collections", "Stream pipelines operate directly on Collection data sources."),

                // SQL Progression
                new PrereqSpec("sql-queries", "sql-basics", "Relational schema understanding precedes complex multi-table joins."),
                new PrereqSpec("sql-indexing", "sql-queries", "Query execution knowledge is required to analyze query plans and apply B-Tree indexes."),

                // Spring Boot Progression
                new PrereqSpec("spring-core-di", "java-oop", "Spring Inversion of Control manages object-oriented bean dependencies."),
                new PrereqSpec("spring-boot-rest", "spring-core-di", "REST controllers and services are configured as Spring IoC beans."),
                new PrereqSpec("spring-data-jpa", "spring-boot-rest", "JPA repositories are injected into Spring service components."),
                new PrereqSpec("spring-data-jpa", "sql-queries", "Relational querying and database concepts are essential for entity mapping."),
                new PrereqSpec("spring-security-jwt", "spring-boot-rest", "Spring Security filter chains protect REST endpoint controllers."),

                // Frontend Progression
                new PrereqSpec("ts-basics", "js-fundamentals", "TypeScript extends JavaScript syntax with static typing."),
                new PrereqSpec("ts-generics", "ts-basics", "Generics require prior mastery of fundamental TypeScript types and interfaces."),
                new PrereqSpec("react-components", "js-fundamentals", "React component declaration relies on modern JavaScript ES6+."),
                new PrereqSpec("react-hooks", "react-components", "Hooks are executed within functional React components."),
                new PrereqSpec("nextjs-app-router", "react-components", "Next.js App Router structures and renders React component trees."),
                new PrereqSpec("nextjs-app-router", "ts-basics", "Next.js App Router enforces type-safe page props and layouts."),

                // Containers & AI
                new PrereqSpec("docker-compose", "docker-containers", "Compose coordinates multi-container Docker workloads."),
                new PrereqSpec("pandas-data-wrangling", "python-basics", "Pandas data wrangling requires foundational Python programming."),
                new PrereqSpec("pytorch-neural-nets", "pytorch-tensors", "Neural network layer operations are computed using PyTorch tensors.")
        );

        for (PrereqSpec spec : prereqs) {
            CompetencyNode competency = compMap.get(spec.competencyKey);
            CompetencyNode prerequisite = compMap.get(spec.prerequisiteKey);

            if (competency != null && prerequisite != null) {
                try {
                    catalogService.addPrerequisite(competency, prerequisite, spec.reason);
                } catch (Exception e) {
                    logger.warn("Skipping prerequisite {} -> {}: {}", spec.competencyKey, spec.prerequisiteKey, e.getMessage());
                }
            }
        }
    }

    private record TechSpec(String name, String slug, String category, String icon, String description) {}
    private record CareerSpec(String title, String slug, String category, String level, String duration, String salaryRange, String description, String overview, List<String> techSlugs) {}
    private record CompSpec(String techSlug, String key, String title, String description, String level, int sequence) {}
    private record PrereqSpec(String competencyKey, String prerequisiteKey, String reason) {}
}
