package com.careeradvisor.backend.config;

import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.AssessmentQuestionRepository;
import com.careeradvisor.backend.repository.CompetencyNodeRepository;
import com.careeradvisor.backend.repository.TechnologyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@Order(20)
public class AssessmentQuestionDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentQuestionDataInitializer.class);

    private final AssessmentQuestionRepository questionRepository;
    private final TechnologyRepository technologyRepository;
    private final CompetencyNodeRepository competencyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssessmentQuestionDataInitializer(AssessmentQuestionRepository questionRepository,
                                             TechnologyRepository technologyRepository,
                                             CompetencyNodeRepository competencyRepository) {
        this.questionRepository = questionRepository;
        this.technologyRepository = technologyRepository;
        this.competencyRepository = competencyRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Initializing Curated Assessment Question Bank...");

        List<QuestionSpec> specs = buildQuestionBankSpecs();
        int createdCount = 0;

        for (QuestionSpec spec : specs) {
            if (questionRepository.findByQuestionKey(spec.key).isPresent()) {
                continue;
            }

            Optional<Technology> techOpt = technologyRepository.findBySlug(spec.techSlug);
            if (techOpt.isEmpty()) {
                logger.warn("Skipping question {} because technology {} not found", spec.key, spec.techSlug);
                continue;
            }

            CompetencyNode comp = null;
            if (spec.competencyKey != null) {
                comp = competencyRepository.findByCompetencyKey(spec.competencyKey).orElse(null);
            }

            String optionsJson;
            try {
                optionsJson = objectMapper.writeValueAsString(spec.options);
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize options for question {}", spec.key, e);
                continue;
            }

            int weight = switch (spec.difficulty) {
                case ADVANCED -> 3;
                case INTERMEDIATE -> 2;
                default -> 1;
            };

            AssessmentQuestion question = AssessmentQuestion.builder()
                    .questionKey(spec.key)
                    .technology(techOpt.get())
                    .competency(comp)
                    .topic(spec.topic)
                    .difficulty(spec.difficulty)
                    .questionType(AssessmentQuestionType.MULTIPLE_CHOICE)
                    .questionText(spec.questionText)
                    .optionsJson(optionsJson)
                    .correctOptionIndex(spec.correctIndex)
                    .explanation(spec.explanation)
                    .scoringWeight(weight)
                    .active(true)
                    .build();

            questionRepository.save(question);
            createdCount++;
        }

        logger.info("Assessment Question Bank initialized. Seeded {} new questions. Total active: {}",
                createdCount, questionRepository.countByActiveTrue());
    }

    private static class QuestionSpec {
        String key;
        String techSlug;
        String competencyKey;
        String topic;
        AssessmentDifficulty difficulty;
        String questionText;
        List<String> options;
        int correctIndex;
        String explanation;

        QuestionSpec(String key, String techSlug, String competencyKey, String topic,
                     AssessmentDifficulty difficulty, String questionText,
                     List<String> options, int correctIndex, String explanation) {
            this.key = key;
            this.techSlug = techSlug;
            this.competencyKey = competencyKey;
            this.topic = topic;
            this.difficulty = difficulty;
            this.questionText = questionText;
            this.options = options;
            this.correctIndex = correctIndex;
            this.explanation = explanation;
        }
    }

    private List<QuestionSpec> buildQuestionBankSpecs() {
        List<QuestionSpec> list = new ArrayList<>();

        // ========================== JAVA ==========================
        list.add(new QuestionSpec(
                "java-syntax-01", "java", "java-syntax-basics", "Syntax & Types",
                AssessmentDifficulty.BEGINNER,
                "What is the default value of a boolean variable declared as a class field in Java?",
                List.of("true", "false", "null", "0"),
                1,
                "In Java, primitive boolean fields default to false when not explicitly initialized."
        ));

        list.add(new QuestionSpec(
                "java-syntax-02", "java", "java-syntax-basics", "Control Flow",
                AssessmentDifficulty.BEGINNER,
                "Which keyword is used to prevent method overriding or class inheritance in Java?",
                List.of("static", "abstract", "final", "const"),
                2,
                "The 'final' keyword prevents classes from being extended and methods from being overridden."
        ));

        list.add(new QuestionSpec(
                "java-oop-01", "java", "java-oop-fundamentals", "OOP Fundamentals",
                AssessmentDifficulty.BEGINNER,
                "Which OOP principle allows a subclass to provide a specific implementation of a method defined in its superclass?",
                List.of("Polymorphism", "Encapsulation", "Abstraction", "Aggregation"),
                0,
                "Method overriding is runtime (dynamic) polymorphism where the call is resolved at runtime based on the actual object type."
        ));

        list.add(new QuestionSpec(
                "java-collections-01", "java", "java-collections-framework", "Collections",
                AssessmentDifficulty.INTERMEDIATE,
                "Which Java Collection class implements a hash table and allows one null key and multiple null values?",
                List.of("Hashtable", "ConcurrentHashMap", "HashMap", "TreeMap"),
                2,
                "HashMap permits one null key and multiple null values. Hashtable and ConcurrentHashMap do not allow null keys or values."
        ));

        list.add(new QuestionSpec(
                "java-collections-02", "java", "java-collections-framework", "Collections & Generics",
                AssessmentDifficulty.INTERMEDIATE,
                "What is the average time complexity of get(key) and put(key, value) in a well-distributed Java HashMap?",
                List.of("O(n)", "O(log n)", "O(1)", "O(n log n)"),
                2,
                "With a proper hash function and low collision distribution, HashMap operations operate in constant O(1) time."
        ));

        list.add(new QuestionSpec(
                "java-threads-01", "java", "java-concurrency-multithreading", "Concurrency",
                AssessmentDifficulty.INTERMEDIATE,
                "What guarantee does the 'volatile' keyword provide in Java?",
                List.of("Atomicity of compound operations", "Visibility of writes across threads and happens-before ordering", "Automatic mutual exclusion locking", "Prevents memory leaks in garbage collection"),
                1,
                "Volatile ensures reads and writes go directly to main memory with memory barrier semantics, but does NOT guarantee atomicity for compound actions like count++."
        ));

        list.add(new QuestionSpec(
                "java-threads-02", "java", "java-concurrency-multithreading", "Advanced Concurrency",
                AssessmentDifficulty.ADVANCED,
                "How does Java's ConcurrentHashMap achieve high write concurrency in modern JDK versions (8+)?",
                List.of("Synchronizing the entire map on every operation", "Using CAS (Compare-And-Swap) for empty bin insertion and synchronized locking per bucket bin tree/list head", "Using ReadWriteLock across table segments", "Copy-on-write array cloning on every mutation"),
                1,
                "JDK 8+ ConcurrentHashMap utilizes CAS for the first node insertion in a bin and fine-grained synchronized locking on the individual head node of that bin."
        ));

        list.add(new QuestionSpec(
                "java-jvm-01", "java", "java-concurrency-multithreading", "JVM Internals",
                AssessmentDifficulty.ADVANCED,
                "In JVM memory architecture, what triggers an OutOfMemoryError: Metaspace?",
                List.of("Excessive heap allocations of large byte arrays", "Infinite recursion exhausting call stack frames", "Exhausting native memory due to excessive class loading and dynamic bytecode generation", "Unclosed JDBC database connections"),
                2,
                "Metaspace stores class metadata in native process memory. Classloader leaks or uncontrolled dynamic proxy generation exhaust Metaspace."
        ));

        // ========================== SPRING BOOT ==========================
        list.add(new QuestionSpec(
                "spring-boot-01", "spring-boot", "spring-core-di", "Dependency Injection",
                AssessmentDifficulty.BEGINNER,
                "Which Spring annotation is used to mark a class as a service provider containing business logic?",
                List.of("@Repository", "@Component", "@Service", "@Controller"),
                2,
                "@Service is a specialization of @Component indicating that the class provides business service operations."
        ));

        list.add(new QuestionSpec(
                "spring-boot-02", "spring-boot", "spring-boot-rest-apis", "REST APIs",
                AssessmentDifficulty.BEGINNER,
                "Which annotation is a shorthand combining @Controller and @ResponseBody in Spring Boot?",
                List.of("@Endpoint", "@RestController", "@WebService", "@Api"),
                1,
                "@RestController eliminates the need to annotate every controller method with @ResponseBody for JSON/XML responses."
        ));

        list.add(new QuestionSpec(
                "spring-boot-03", "spring-boot", "spring-boot-rest-apis", "REST Exception Handling",
                AssessmentDifficulty.INTERMEDIATE,
                "Which annotation is commonly used on a global class to intercept and handle exceptions thrown by REST controllers?",
                List.of("@ExceptionHandler", "@ControllerAdvice", "@ErrorMapping", "@ResponseFilter"),
                1,
                "@ControllerAdvice (or @RestControllerAdvice) provides centralized, global exception handling across all controllers."
        ));

        list.add(new QuestionSpec(
                "spring-boot-04", "spring-boot", "spring-data-jpa-hibernate", "Spring Data JPA",
                AssessmentDifficulty.INTERMEDIATE,
                "What is the default isolation level for methods annotated with @Transactional in Spring if not explicitly specified?",
                List.of("READ_UNCOMMITTED", "READ_COMMITTED", "DEFAULT (determined by underlying database)", "SERIALIZABLE"),
                2,
                "Spring's @Transactional default isolation is Isolation.DEFAULT, which delegates to the underlying database transaction manager's default."
        ));

        list.add(new QuestionSpec(
                "spring-boot-05", "spring-boot", "spring-security-jwt", "Security & Filters",
                AssessmentDifficulty.ADVANCED,
                "In Spring Security, where should a custom OncePerRequestFilter for Bearer JWT validation typically be placed?",
                List.of("After FilterSecurityInterceptor", "Before UsernamePasswordAuthenticationFilter", "After SessionManagementFilter", "Directly in the DispatcherServlet"),
                1,
                "JWT filters are typically placed before UsernamePasswordAuthenticationFilter to populate the SecurityContextHolder before downstream authorization checks."
        ));

        list.add(new QuestionSpec(
                "spring-boot-06", "spring-boot", "spring-data-jpa-hibernate", "JPA Performance",
                AssessmentDifficulty.ADVANCED,
                "How does the N+1 select problem typically occur in Hibernate/JPA, and how is it resolved in repository queries?",
                List.of("Fetching lazy collections without eager caching; resolved by enabling second-level cache", "Executing 1 query for parents plus N separate queries for lazy children; resolved using JOIN FETCH in JPQL or EntityGraphs", "Opening N connections in a pool; resolved by HikariCP sizing", "Using native SQL; resolved by converting to CriteriaBuilder"),
                1,
                "The N+1 problem occurs when iterating child associations triggers a query per parent; JOIN FETCH or @EntityGraph fetches associations in a single query."
        ));

        // ========================== JAVASCRIPT ==========================
        list.add(new QuestionSpec(
                "js-syntax-01", "javascript", "js-syntax-data-types", "Data Types",
                AssessmentDifficulty.BEGINNER,
                "What does the expression typeof null evaluate to in JavaScript?",
                List.of("\"null\"", "\"undefined\"", "\"object\"", "\"number\""),
                2,
                "In JavaScript, typeof null returns 'object', an artifact dating back to the initial implementation where object tags were 0."
        ));

        list.add(new QuestionSpec(
                "js-syntax-02", "javascript", "js-es6-modern-features", "Scoping & Variables",
                AssessmentDifficulty.BEGINNER,
                "What is the key scoping difference between 'var' and 'let' in JavaScript?",
                List.of("'var' is block-scoped, whereas 'let' is function-scoped", "'let' is block-scoped, whereas 'var' is function-scoped", "Neither has any scope limitations", "'let' variables can be redeclared in the same scope, 'var' cannot"),
                1,
                "'let' and 'const' are block-scoped to the nearest curly braces, while 'var' is function-scoped and hoisted with undefined."
        ));

        list.add(new QuestionSpec(
                "js-async-01", "javascript", "js-async-promises-fetch", "Event Loop & Async",
                AssessmentDifficulty.INTERMEDIATE,
                "In JavaScript's event loop, what is the execution priority between Microtasks (e.g., Promise.then) and Macrotasks (e.g., setTimeout)?",
                List.of("Macrotasks run before all microtasks", "All microtasks in the queue are executed immediately after the current script and before the next macrotask", "They alternate in round-robin fashion", "setTimeout runs synchronously before promises"),
                1,
                "The microtask queue is drained completely after the synchronous call stack empties and before the event loop picks the next macrotask."
        ));

        list.add(new QuestionSpec(
                "js-async-02", "javascript", "js-async-promises-fetch", "Closures & Context",
                AssessmentDifficulty.INTERMEDIATE,
                "What is a closure in JavaScript?",
                List.of("A method that closes a database connection", "The combination of a function bundled together with references to its surrounding lexical state (scope)", "A function that has no return value", "An object that cannot be modified after creation"),
                1,
                "A closure gives an inner function access to an outer function's scope even after the outer function has finished executing."
        ));

        list.add(new QuestionSpec(
                "js-adv-01", "javascript", "js-es6-modern-features", "Prototypes & Engine",
                AssessmentDifficulty.ADVANCED,
                "How does property resolution work along the prototype chain when accessing obj.prop?",
                List.of("It searches obj, and if not found, checks obj.__proto__, continuing up to Object.prototype until null is reached", "It checks Object.prototype first, then cascades down to obj", "It scans global window object properties immediately", "It throws a ReferenceError if the property is not directly on obj"),
                0,
                "JavaScript looks up the prototype chain from the instance to Object.prototype and finally null before returning undefined."
        ));

        // ========================== TYPESCRIPT ==========================
        list.add(new QuestionSpec(
                "ts-types-01", "typescript", "ts-type-annotations", "Basic Types",
                AssessmentDifficulty.BEGINNER,
                "Which TypeScript type should be used when a value can be of any type, but requires type-checking before performing operations on it?",
                List.of("any", "unknown", "never", "void"),
                1,
                "'unknown' is the type-safe counterpart of 'any'. Anything is assignable to 'unknown', but operations require narrowing/type assertions."
        ));

        list.add(new QuestionSpec(
                "ts-types-02", "typescript", "ts-interfaces-generics", "Generics",
                AssessmentDifficulty.INTERMEDIATE,
                "What does the TypeScript utility type 'Readonly<T>' do?",
                List.of("Freezes the object at runtime with Object.freeze", "Constructs a type with all properties of T set to readonly at compile time", "Prevents reassignment of local let variables", "Deletes all methods from type T"),
                1,
                "Readonly<T> marks all properties of type T with the 'readonly' modifier, preventing compile-time mutations."
        ));

        list.add(new QuestionSpec(
                "ts-adv-01", "typescript", "ts-interfaces-generics", "Advanced Types",
                AssessmentDifficulty.ADVANCED,
                "What is the return type of the conditional type: type ExcludeNull<T> = T extends null | undefined ? never : T?",
                List.of("A union type stripped of null and undefined members", "A compile-time syntax error", "Always 'any'", "An array of non-null objects"),
                0,
                "Distributive conditional types over unions filter out types that match the condition by resolving to 'never'."
        ));

        // ========================== REACT ==========================
        list.add(new QuestionSpec(
                "react-hooks-01", "react", "react-hooks-state", "Hooks Fundamentals",
                AssessmentDifficulty.BEGINNER,
                "Which React hook is used to persist mutable values across renders without triggering a re-render when changed?",
                List.of("useState", "useMemo", "useRef", "useCallback"),
                2,
                "useRef returns a mutable ref object whose .current property can be updated without triggering a component re-render."
        ));

        list.add(new QuestionSpec(
                "react-hooks-02", "react", "react-hooks-state", "Lifecycle & Effects",
                AssessmentDifficulty.INTERMEDIATE,
                "When does the cleanup function returned by useEffect execute?",
                List.of("Only when the browser window closes", "Before the component unmounts and before re-running the effect on dependency change", "Immediately after the JSX renders to the DOM", "Synchronously before any child component renders"),
                1,
                "The cleanup function runs when the component unmounts and prior to running the next effect when dependencies update."
        ));

        list.add(new QuestionSpec(
                "react-perf-01", "react", "react-components-jsx", "Rendering Optimization",
                AssessmentDifficulty.ADVANCED,
                "What is the purpose of React's useTransition hook introduced in React 18?",
                List.of("To create CSS transition animations", "To mark state updates as non-urgent transitions, keeping the UI responsive during expensive renders", "To transition between different page URLs in Next.js", "To handle WebSocket state streaming"),
                1,
                "useTransition lets you mark updates as non-blocking transitions so urgent interactions (e.g. typing) are not stalled by heavy renders."
        ));

        // ========================== NEXT.JS ==========================
        list.add(new QuestionSpec(
                "nextjs-app-01", "nextjs", "nextjs-app-router", "App Router Basics",
                AssessmentDifficulty.BEGINNER,
                "In Next.js App Router, which file name defines the unique UI route component for a segment folder?",
                List.of("route.ts", "index.tsx", "page.tsx", "layout.tsx"),
                2,
                "In Next.js App Router, 'page.tsx' makes a route segment publicly accessible and renders its primary UI."
        ));

        list.add(new QuestionSpec(
                "nextjs-ssr-01", "nextjs", "nextjs-server-actions-ssr", "Server Components",
                AssessmentDifficulty.INTERMEDIATE,
                "What is the default rendering paradigm for components inside the Next.js App Router directory?",
                List.of("Client Components with 'use client'", "React Server Components (RSC) rendered on the server", "Static HTML with no hydration", "Single-Page Application client-only rendering"),
                1,
                "By default, all components in the Next.js App Router are React Server Components unless opted-in via 'use client'."
        ));

        list.add(new QuestionSpec(
                "nextjs-adv-01", "nextjs", "nextjs-server-actions-ssr", "Data & Caching",
                AssessmentDifficulty.ADVANCED,
                "How does Next.js handle revalidation on-demand for cached data in Server Actions?",
                List.of("By reloading the browser window via window.location.reload()", "Using revalidatePath() or revalidateTag() to purge specific cache entries without a full rebuild", "Deleting the .next/cache directory from disk", "Sending a WebSocket broadcast to all connected clients"),
                1,
                "Next.js provides revalidatePath and revalidateTag functions to purge cached fetch requests or routes on-demand."
        ));

        // ========================== MYSQL & SQL ==========================
        list.add(new QuestionSpec(
                "mysql-sql-01", "mysql", "sql-crud-queries", "SQL Basics",
                AssessmentDifficulty.BEGINNER,
                "Which SQL clause is used to filter records returned by a GROUP BY aggregate query?",
                List.of("WHERE", "HAVING", "LIMIT", "ORDER BY"),
                1,
                "HAVING filters groups created by GROUP BY based on aggregate values, whereas WHERE filters individual rows before grouping."
        ));

        list.add(new QuestionSpec(
                "mysql-sql-02", "mysql", "sql-joins-indexes", "Joins & Constraints",
                AssessmentDifficulty.INTERMEDIATE,
                "What is the difference between an INNER JOIN and a LEFT JOIN in SQL?",
                List.of("INNER JOIN returns rows only when there is a match in both tables; LEFT JOIN returns all rows from the left table and matched rows from the right", "LEFT JOIN only returns rows from the left table that have NO match on the right", "There is no difference in MySQL", "INNER JOIN returns all rows from both tables including nulls"),
                0,
                "INNER JOIN requires matching keys in both tables; LEFT JOIN preserves all records from the left table, filling missing right-side attributes with NULL."
        ));

        list.add(new QuestionSpec(
                "mysql-idx-01", "mysql", "sql-joins-indexes", "Indexing & Execution Plan",
                AssessmentDifficulty.ADVANCED,
                "In MySQL InnoDB, why is a covering index beneficial for query performance?",
                List.of("It encrypts table data at rest", "The query can be satisfied entirely from the B-tree index without performing secondary lookups to the clustered table rows", "It automatically generates foreign key constraints", "It compresses text columns using zlib"),
                1,
                "A covering index contains all columns requested by the SELECT, WHERE, and JOIN clauses, eliminating expensive row lookups from the clustered index."
        ));

        // ========================== DOCKER ==========================
        list.add(new QuestionSpec(
                "docker-basics-01", "docker", "docker-containers-images", "Docker Concepts",
                AssessmentDifficulty.BEGINNER,
                "What is the fundamental difference between a Docker image and a Docker container?",
                List.of("An image is a running instance; a container is the static file", "An image is an immutable blueprint/template; a container is a running, writable instance of that image", "Containers only run on Linux, while images only run on Windows", "There is no distinction between them"),
                1,
                "An image is a read-only template with layers; a container is a runnable, isolated instance with a thin writable layer on top."
        ));

        list.add(new QuestionSpec(
                "docker-compose-01", "docker", "docker-compose-orchestration", "Docker Compose",
                AssessmentDifficulty.INTERMEDIATE,
                "How do containers in the same user-defined Docker network discover and communicate with each other?",
                List.of("By hardcoding host localhost IP addresses", "Using internal automatic DNS resolution by service or container name", "Through shared filesystem folders only", "Docker requires external public IP routing"),
                1,
                "Docker includes an embedded DNS server that resolves container and service names to their assigned internal network IPs."
        ));

        list.add(new QuestionSpec(
                "docker-adv-01", "docker", "docker-containers-images", "Multi-stage Builds",
                AssessmentDifficulty.ADVANCED,
                "Why are multi-stage Docker builds recommended for production Spring Boot and Next.js applications?",
                List.of("They allow compiling code and copying only the runtime artifacts into a lean final image, drastically reducing image size and attack surface", "They run tests in parallel across multiple virtual machines", "They bypass Docker daemon security restrictions", "They convert Java JAR files directly into binary machine code"),
                0,
                "Multi-stage builds leave behind heavyweight SDKs, build tools, and source files, yielding small, secure production images."
        ));

        // ========================== GIT ==========================
        list.add(new QuestionSpec(
                "git-basics-01", "git", "git-cli-branching", "Git Basics",
                AssessmentDifficulty.BEGINNER,
                "Which Git command stages modified files to prepare them for the next commit?",
                List.of("git push", "git commit", "git add", "git checkout"),
                2,
                "git add stages file modifications to the index, preparing them to be committed."
        ));

        list.add(new QuestionSpec(
                "git-workflow-01", "git", "git-workflows-merge", "Merge vs Rebase",
                AssessmentDifficulty.INTERMEDIATE,
                "What is the main difference between 'git merge' and 'git rebase' when integrating changes from a feature branch?",
                List.of("'git merge' creates a 3-way merge commit preserving true history; 'git rebase' rewrites commit history onto the target tip for a linear graph", "'git rebase' permanently deletes uncommitted changes", "'git merge' can only be run on remote servers", "'git rebase' creates duplicate commits on the remote repository"),
                0,
                "Merge preserves branch topology with a merge commit; rebase replays your commits sequentially on top of the base branch for a clean, linear history."
        ));

        return list;
    }
}
