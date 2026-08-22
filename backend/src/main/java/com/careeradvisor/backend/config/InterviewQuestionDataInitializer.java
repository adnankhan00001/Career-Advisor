package com.careeradvisor.backend.config;

import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.InterviewQuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InterviewQuestionDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(InterviewQuestionDataInitializer.class);

    private final InterviewQuestionRepository questionRepository;

    public InterviewQuestionDataInitializer(InterviewQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public void run(String... args) {
        if (questionRepository.count() == 0) {
            logger.info("Seeding curated Mock Interview Question bank...");

            List<InterviewQuestion> initialQuestions = List.of(
                    // 1. JAVA (5 questions)
                    InterviewQuestion.builder()
                            .question("Why is String immutable in Java, and what role does the String Constant Pool play?")
                            .category(ProblemCategory.JAVA)
                            .topic("Core Java")
                            .difficulty(Difficulty.MEDIUM)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) For security, synchronization, caching hashcode, and string pool memory optimization",
                                    "B) Because Java does not allow dynamic memory allocation for text",
                                    "C) To prevent classes from inheriting from java.lang.Object",
                                    "D) Because primitive data types cannot be stored on the Heap"
                            ))
                            .correctAnswer("A) For security, synchronization, caching hashcode, and string pool memory optimization")
                            .explanation("Immutability allows safe multi-threading, cached hashCode for fast Map lookups, and memory sharing via String Pool.")
                            .expectedConcepts(List.of("String Pool", "Immutability", "Thread Safety", "HashCode Caching"))
                            .orderIndex(1)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What happens when a hash collision occurs in Java 8+ HashMap, and when does treeification trigger?")
                            .category(ProblemCategory.JAVA)
                            .topic("Collections")
                            .difficulty(Difficulty.MEDIUM)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) An array resize occurs immediately without chaining",
                                    "B) Entries are chained in a LinkedList; converts to a Red-Black Tree when bin count >= 8 and table capacity >= 64",
                                    "C) The colliding key is overwritten with the newest value",
                                    "D) A ConcurrentModificationException is thrown immediately"
                            ))
                            .correctAnswer("B) Entries are chained in a LinkedList; converts to a Red-Black Tree when bin count >= 8 and table capacity >= 64")
                            .explanation("Java 8 replaces LinkedList with a Red-Black Tree (O(log n)) when bucket size reaches TREEIFY_THRESHOLD (8) and min table capacity is 64.")
                            .expectedConcepts(List.of("HashMap", "Hash Collisions", "Treeification", "Red-Black Tree"))
                            .orderIndex(2)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What is the key difference between Checked and Unchecked Exceptions in Java?")
                            .category(ProblemCategory.JAVA)
                            .topic("Exception Handling")
                            .difficulty(Difficulty.EASY)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Checked exceptions subclass RuntimeException and are ignored by the compiler",
                                    "B) Checked exceptions must be caught or declared with 'throws' at compile-time; Unchecked (RuntimeExceptions) do not",
                                    "C) Unchecked exceptions can only be caught using try-with-resources",
                                    "D) Checked exceptions only occur inside static methods"
                            ))
                            .correctAnswer("B) Checked exceptions must be caught or declared with 'throws' at compile-time; Unchecked (RuntimeExceptions) do not")
                            .explanation("Checked exceptions inherit directly from Exception and are enforced at compile-time for recoverable conditions.")
                            .expectedConcepts(List.of("Checked Exceptions", "RuntimeException", "Compiler Verification"))
                            .orderIndex(3)
                            .build(),

                    InterviewQuestion.builder()
                            .question("Where are Java objects and local variables stored in memory?")
                            .category(ProblemCategory.JAVA)
                            .topic("JVM Memory Model")
                            .difficulty(Difficulty.EASY)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Objects and method references are in Stack; primitives are in Heap",
                                    "B) All objects reside in Heap memory; method execution frames and primitive local variables reside on Stack memory",
                                    "C) Both objects and primitives are stored in Metaspace",
                                    "D) Objects reside on Stack; local variables reside in Native Memory"
                            ))
                            .correctAnswer("B) All objects reside in Heap memory; method execution frames and primitive local variables reside on Stack memory")
                            .explanation("Heap is shared across threads for object storage; each thread has its own Stack holding local variable frames.")
                            .expectedConcepts(List.of("Heap Memory", "Stack Memory", "Garbage Collection"))
                            .orderIndex(4)
                            .build(),

                    InterviewQuestion.builder()
                            .question("How does ReentrantLock differ from the 'synchronized' keyword in Java?")
                            .category(ProblemCategory.JAVA)
                            .topic("Concurrency")
                            .difficulty(Difficulty.HARD)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Synchronized supports fairness and timeout acquisition; ReentrantLock does not",
                                    "B) ReentrantLock provides tryLock() with timeouts, fairness policies, and interruptible locks; synchronized is block-scoped",
                                    "C) ReentrantLock cannot be used across multiple threads",
                                    "D) Synchronized works only on primitive types"
                            ))
                            .correctAnswer("B) ReentrantLock provides tryLock() with timeouts, fairness policies, and interruptible locks; synchronized is block-scoped")
                            .explanation("ReentrantLock from java.util.concurrent.locks provides timed lock polling, fair queue ordering, and interruptible locking.")
                            .expectedConcepts(List.of("ReentrantLock", "Synchronized", "Thread Concurrency", "Fairness"))
                            .orderIndex(5)
                            .build(),

                    // 2. OOP (5 questions)
                    InterviewQuestion.builder()
                            .question("Which SOLID principle states that 'High-level modules should not depend on low-level modules; both should depend on abstractions'?")
                            .category(ProblemCategory.OOP)
                            .topic("SOLID Principles")
                            .difficulty(Difficulty.MEDIUM)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Single Responsibility Principle (SRP)",
                                    "B) Open-Closed Principle (OCP)",
                                    "C) Liskov Substitution Principle (LSP)",
                                    "D) Dependency Inversion Principle (DIP)"
                            ))
                            .correctAnswer("D) Dependency Inversion Principle (DIP)")
                            .explanation("DIP in SOLID advocates decoupling high-level business rules from low-level implementation details via interfaces.")
                            .expectedConcepts(List.of("DIP", "SOLID", "Abstraction", "Decoupling"))
                            .orderIndex(6)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What distinguishes runtime polymorphism (dynamic method dispatch) from compile-time polymorphism?")
                            .category(ProblemCategory.OOP)
                            .topic("Polymorphism")
                            .difficulty(Difficulty.EASY)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Runtime polymorphism uses method overloading; compile-time uses method overriding",
                                    "B) Runtime polymorphism resolves method overriding at runtime based on the actual object; compile-time resolves overloading at compile time",
                                    "C) Compile-time polymorphism requires abstract classes",
                                    "D) Runtime polymorphism only works with static methods"
                            ))
                            .correctAnswer("B) Runtime polymorphism resolves method overriding at runtime based on the actual object; compile-time resolves overloading at compile time")
                            .explanation("Dynamic dispatch uses vtable lookups at runtime to invoke overridden child class implementations.")
                            .expectedConcepts(List.of("Method Overriding", "Method Overloading", "Dynamic Dispatch"))
                            .orderIndex(7)
                            .build(),

                    InterviewQuestion.builder()
                            .question("Why is 'Composition over Inheritance' generally recommended in object-oriented design?")
                            .category(ProblemCategory.OOP)
                            .topic("Design Principles")
                            .difficulty(Difficulty.MEDIUM)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Inheritance violates type safety and is banned in production",
                                    "B) Composition provides loose coupling, dynamic runtime behavior swapping, and avoids fragile base class hierarchies",
                                    "C) Composition runs faster because it bypasses CPU caches",
                                    "D) Inheritance requires multiple inheritance which Java does not support"
                            ))
                            .correctAnswer("B) Composition provides loose coupling, dynamic runtime behavior swapping, and avoids fragile base class hierarchies")
                            .explanation("Composition enables HAS-A relationships that can be altered or mocked dynamically without rigid compile-time IS-A coupling.")
                            .expectedConcepts(List.of("Composition", "Inheritance", "Loose Coupling", "Design Patterns"))
                            .orderIndex(8)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What is the primary benefit of Encapsulation in software engineering?")
                            .category(ProblemCategory.OOP)
                            .topic("Encapsulation")
                            .difficulty(Difficulty.EASY)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Restricting direct access to state and bundling data with methods to protect object invariants",
                                    "B) Allowing any class to mutate internal variables without getters",
                                    "C) Increasing code execution speed by inlining variables",
                                    "D) Generating automated unit tests for class properties"
                            ))
                            .correctAnswer("A) Restricting direct access to state and bundling data with methods to protect object invariants")
                            .explanation("Encapsulation hides internal representation and exposes controlled contracts through access modifiers and methods.")
                            .expectedConcepts(List.of("Encapsulation", "Information Hiding", "Invariants"))
                            .orderIndex(9)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What occurs if a subclass violates the Liskov Substitution Principle (LSP)?")
                            .category(ProblemCategory.OOP)
                            .topic("SOLID Principles")
                            .difficulty(Difficulty.HARD)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Subclass cannot be substituted for its base type without breaking program correctness or altering expectations",
                                    "B) The compiler rejects the class due to package naming",
                                    "C) Garbage collection fails on subclass instances",
                                    "D) Database transactions are automatically aborted"
                            ))
                            .correctAnswer("A) Subclass cannot be substituted for its base type without breaking program correctness or altering expectations")
                            .explanation("LSP ensures subtypes honor the behavioral contracts and invariants established by the supertype.")
                            .expectedConcepts(List.of("LSP", "Subtyping", "Behavioral Contracts"))
                            .orderIndex(10)
                            .build(),

                    // 3. DBMS (5 questions)
                    InterviewQuestion.builder()
                            .question("Which transaction anomaly is prevented by REPEATABLE READ isolation level that occurs in READ COMMITTED?")
                            .category(ProblemCategory.DBMS)
                            .topic("ACID & Concurrency")
                            .difficulty(Difficulty.MEDIUM)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Dirty Read",
                                    "B) Non-Repeatable Read (reading altered values on re-query within the same transaction)",
                                    "C) Write Skew in Serializable",
                                    "D) Hard Disk Crash"
                            ))
                            .correctAnswer("B) Non-Repeatable Read (reading altered values on re-query within the same transaction)")
                            .explanation("REPEATABLE READ ensures that if a transaction reads a row, subsequent reads in that transaction see the identical snapshot.")
                            .expectedConcepts(List.of("Isolation Levels", "Non-Repeatable Read", "MVCC", "ACID"))
                            .orderIndex(11)
                            .build(),

                    InterviewQuestion.builder()
                            .question("Why do relational databases commonly utilize B+ Tree indexes over Hash indexes for primary keys and lookups?")
                            .category(ProblemCategory.DBMS)
                            .topic("Indexing")
                            .difficulty(Difficulty.MEDIUM)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Hash indexes do not support O(1) lookups",
                                    "B) B+ Trees support efficient range queries (BETWEEN, <, >), sequential scans via leaf node pointers, and sorted retrieval",
                                    "C) B+ Trees consume zero disk storage space",
                                    "D) Hash indexes cannot store integer data types"
                            ))
                            .correctAnswer("B) B+ Trees support efficient range queries (BETWEEN, <, >), sequential scans via leaf node pointers, and sorted retrieval")
                            .explanation("B+ Trees store all data in linked leaf nodes, making range scans and ORDER BY queries highly efficient.")
                            .expectedConcepts(List.of("B+ Tree", "Index Optimization", "Range Scans", "Database Engine"))
                            .orderIndex(12)
                            .build(),

                    InterviewQuestion.builder()
                            .question("A database table is in Third Normal Form (3NF) if it is in 2NF and what additional condition is met?")
                            .category(ProblemCategory.DBMS)
                            .topic("Normalization")
                            .difficulty(Difficulty.MEDIUM)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) It contains no foreign keys",
                                    "B) Every non-key attribute is non-transitively dependent on the primary key (no transitive dependencies)",
                                    "C) It only stores numeric data types",
                                    "D) It contains a maximum of 10 columns"
                            ))
                            .correctAnswer("B) Every non-key attribute is non-transitively dependent on the primary key (no transitive dependencies)")
                            .explanation("3NF eliminates transitive functional dependencies where X -> Y and Y -> Z.")
                            .expectedConcepts(List.of("3NF", "Transitive Dependency", "Relational Design"))
                            .orderIndex(13)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What is the key functional difference between a LEFT JOIN and an INNER JOIN?")
                            .category(ProblemCategory.DBMS)
                            .topic("SQL Queries")
                            .difficulty(Difficulty.EASY)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) INNER JOIN returns only matched rows from both tables; LEFT JOIN returns all rows from left table and matched rows or NULLs from right table",
                                    "B) LEFT JOIN returns only rows that do not exist in the right table",
                                    "C) INNER JOIN creates duplicate indexes on joined keys",
                                    "D) There is no difference between them"
                            ))
                            .correctAnswer("A) INNER JOIN returns only matched rows from both tables; LEFT JOIN returns all rows from left table and matched rows or NULLs from right table")
                            .explanation("LEFT JOIN preserves all records from the left relation, substituting NULL for missing right matches.")
                            .expectedConcepts(List.of("SQL Joins", "Relational Algebra", "Query Optimization"))
                            .orderIndex(14)
                            .build(),

                    InterviewQuestion.builder()
                            .question("When is Optimistic Locking preferred over Pessimistic Locking in concurrent database applications?")
                            .category(ProblemCategory.DBMS)
                            .topic("Concurrency Control")
                            .difficulty(Difficulty.HARD)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) High write contention where transactions must block each other immediately",
                                    "B) Read-heavy workloads with low collision probability, using version columns (@Version) to avoid heavy database locks",
                                    "C) Only when operating without a primary key",
                                    "D) When all queries use subqueries"
                            ))
                            .correctAnswer("B) Read-heavy workloads with low collision probability, using version columns (@Version) to avoid heavy database locks")
                            .explanation("Optimistic locking avoids locking rows during read and validates version integrity upon commit.")
                            .expectedConcepts(List.of("Optimistic Locking", "Pessimistic Locking", "@Version", "Lock Contention"))
                            .orderIndex(15)
                            .build(),

                    // 4. SPRING BOOT (5 questions)
                    InterviewQuestion.builder()
                            .question("How does Dependency Injection (DI) in Spring Boot invert control (IoC)?")
                            .category(ProblemCategory.SPRING_BOOT)
                            .topic("Spring Core")
                            .difficulty(Difficulty.EASY)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) The framework container instantiates, configures, and injects object dependencies instead of components creating dependencies directly with 'new'",
                                    "B) Developers manually construct singleton threads in main()",
                                    "C) By serializing beans into JSON before execution",
                                    "D) By preventing classes from having constructors"
                            ))
                            .correctAnswer("A) The framework container instantiates, configures, and injects object dependencies instead of components creating dependencies directly with 'new'")
                            .explanation("Spring IoC container manages bean lifecycles and injects required collaborators via constructor or field injection.")
                            .expectedConcepts(List.of("IoC", "Dependency Injection", "ApplicationContext", "Bean Management"))
                            .orderIndex(16)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What is the default scope of a Spring Bean, and how does it behave in a multi-threaded application?")
                            .category(ProblemCategory.SPRING_BOOT)
                            .topic("Bean Lifecycles")
                            .difficulty(Difficulty.MEDIUM)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Prototype scope; new instance per method call",
                                    "B) Singleton scope; one shared instance per Spring ApplicationContext, requiring thread-safe stateless design",
                                    "C) Request scope; created per HTTP socket connection",
                                    "D) Session scope; stored in browser cookies"
                            ))
                            .correctAnswer("B) Singleton scope; one shared instance per Spring ApplicationContext, requiring thread-safe stateless design")
                            .explanation("Spring Singleton beans are shared across all concurrent request threads and should not maintain mutable shared instance state.")
                            .expectedConcepts(List.of("Singleton Scope", "Thread Safety", "Stateless Services"))
                            .orderIndex(17)
                            .build(),

                    InterviewQuestion.builder()
                            .question("How does @Transactional handle unchecked exceptions vs checked exceptions by default in Spring?")
                            .category(ProblemCategory.SPRING_BOOT)
                            .topic("Transactions")
                            .difficulty(Difficulty.MEDIUM)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Rolls back on RuntimeException and Error by default, but does NOT roll back on checked exceptions unless configured with rollbackFor",
                                    "B) Rolls back on all exceptions regardless of type",
                                    "C) Ignores RuntimeExceptions and commits regardless",
                                    "D) Aborts database connection on any exception"
                            ))
                            .correctAnswer("A) Rolls back on RuntimeException and Error by default, but does NOT roll back on checked exceptions unless configured with rollbackFor")
                            .explanation("Spring's transactional proxy defaults to rollback on unhandled RuntimeExceptions (and Errors), requiring rollbackFor = Exception.class for checked types.")
                            .expectedConcepts(List.of("@Transactional", "Transaction Rollback", "Proxy Mechanism"))
                            .orderIndex(18)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What is the primary purpose of Spring Security Filter Chain in a stateless JWT architecture?")
                            .category(ProblemCategory.SPRING_BOOT)
                            .topic("Spring Security")
                            .difficulty(Difficulty.MEDIUM)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Intercepting requests, validating JWT Bearer token signature, and setting Authentication in SecurityContextHolder",
                                    "B) Storing user passwords in clear text in session memory",
                                    "C) Encrypting frontend HTML files on server startup",
                                    "D) Connecting directly to DNS root servers"
                            ))
                            .correctAnswer("A) Intercepting requests, validating JWT Bearer token signature, and setting Authentication in SecurityContextHolder")
                            .explanation("JwtAuthenticationFilter parses the Authorization header, validates claims/expiration, and builds the SecurityContext principal.")
                            .expectedConcepts(List.of("Spring Security", "JWT Authentication", "SecurityContextHolder"))
                            .orderIndex(19)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What is the difference between FetchType.LAZY and FetchType.EAGER in JPA/Hibernate relationships?")
                            .category(ProblemCategory.SPRING_BOOT)
                            .topic("Spring Data JPA")
                            .difficulty(Difficulty.EASY)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) LAZY loads associated entities on-demand when accessed; EAGER loads them immediately with the parent entity",
                                    "B) EAGER causes Hibernate to delete associated records",
                                    "C) LAZY is only supported on primary key columns",
                                    "D) EAGER disables SQL query caching"
                            ))
                            .correctAnswer("A) LAZY loads associated entities on-demand when accessed; EAGER loads them immediately with the parent entity")
                            .explanation("LAZY avoids unnecessary JOIN queries until the relationship getter is accessed, preventing N+1 performance bottlenecks.")
                            .expectedConcepts(List.of("JPA", "Hibernate", "Lazy Loading", "Eager Fetching"))
                            .orderIndex(20)
                            .build(),

                    // 5. DSA (5 questions)
                    InterviewQuestion.builder()
                            .question("What is the time complexity of Binary Search on a sorted array of size N, and what is the primary loop invariant?")
                            .category(ProblemCategory.DSA)
                            .topic("Binary Search")
                            .difficulty(Difficulty.EASY)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) O(N) where the target is searched sequentially",
                                    "B) O(log N) where the search interval [low, high] is halved at each comparison step",
                                    "C) O(N^2) using nested comparison loops",
                                    "D) O(1) using direct address hashing"
                            ))
                            .correctAnswer("B) O(log N) where the search interval [low, high] is halved at each comparison step")
                            .explanation("Binary search achieves O(log N) by halving the search space through mid index comparisons on sorted elements.")
                            .expectedConcepts(List.of("Binary Search", "Time Complexity", "Divide and Conquer"))
                            .orderIndex(21)
                            .build(),

                    InterviewQuestion.builder()
                            .question("In Breadth-First Search (BFS) vs Depth-First Search (DFS) on trees and graphs, which data structures are used?")
                            .category(ProblemCategory.DSA)
                            .topic("Trees & Graphs")
                            .difficulty(Difficulty.EASY)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) BFS uses a Queue (FIFO); DFS uses a Stack (LIFO) or recursion",
                                    "B) BFS uses a Heap; DFS uses a LinkedList",
                                    "C) BFS uses a Binary Search Tree; DFS uses an Array",
                                    "D) Both use priority queues"
                            ))
                            .correctAnswer("A) BFS uses a Queue (FIFO); DFS uses a Stack (LIFO) or recursion")
                            .explanation("BFS traverses level-by-level using a Queue; DFS traverses deep along paths using a Stack or system call stack.")
                            .expectedConcepts(List.of("BFS", "DFS", "Queue", "Stack"))
                            .orderIndex(22)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What are the two core properties required to solve a problem with Dynamic Programming?")
                            .category(ProblemCategory.DSA)
                            .topic("Dynamic Programming")
                            .difficulty(Difficulty.MEDIUM)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Unsorted input and greedy choice property",
                                    "B) Optimal Substructure and Overlapping Subproblems",
                                    "C) Non-linear time complexity and recursive depth limit",
                                    "D) Prime factorization and modulus arithmetic"
                            ))
                            .correctAnswer("B) Optimal Substructure and Overlapping Subproblems")
                            .explanation("Dynamic programming applies memoization or tabulation when optimal solutions to subproblems compose the global optimal answer.")
                            .expectedConcepts(List.of("Optimal Substructure", "Overlapping Subproblems", "Memoization"))
                            .orderIndex(23)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What is the worst-case time complexity of QuickSort and how can it be mitigated?")
                            .category(ProblemCategory.DSA)
                            .topic("Sorting Algorithms")
                            .difficulty(Difficulty.MEDIUM)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) O(N^2) when pivot is poorly chosen; mitigated by randomized pivot selection or Median-of-Three",
                                    "B) O(log N); mitigated by using merge steps",
                                    "C) O(N log N) in all cases unconditionally",
                                    "D) O(N!) without mitigation"
                            ))
                            .correctAnswer("A) O(N^2) when pivot is poorly chosen; mitigated by randomized pivot selection or Median-of-Three")
                            .explanation("QuickSort degrades to O(N^2) on sorted data when selecting leftmost/rightmost pivot; randomized pivot restores O(N log N) expected time.")
                            .expectedConcepts(List.of("QuickSort", "Pivot Selection", "Worst-case Complexity"))
                            .orderIndex(24)
                            .build(),

                    InterviewQuestion.builder()
                            .question("Which data structure allows finding the median of a streaming data flow in O(log N) insertion time?")
                            .category(ProblemCategory.DSA)
                            .topic("Heaps & Priority Queues")
                            .difficulty(Difficulty.HARD)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Two Heaps: Max-Heap for lower half and Min-Heap for upper half",
                                    "B) A single Singly Linked List",
                                    "C) A circular queue of fixed size",
                                    "D) An adjacency matrix"
                            ))
                            .correctAnswer("A) Two Heaps: Max-Heap for lower half and Min-Heap for upper half")
                            .explanation("Maintaining two balanced heaps (Max-Heap & Min-Heap) gives O(log N) insert and O(1) median lookup.")
                            .expectedConcepts(List.of("Max-Heap", "Min-Heap", "Streaming Data", "Median Maintenance"))
                            .orderIndex(25)
                            .build(),

                    // 6. OPERATING SYSTEMS (3 questions)
                    InterviewQuestion.builder()
                            .question("What are the 4 Coffman conditions required for a Deadlock to occur in an operating system?")
                            .category(ProblemCategory.OS)
                            .topic("Concurrency & Deadlocks")
                            .difficulty(Difficulty.HARD)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Mutual Exclusion, Hold and Wait, No Preemption, and Circular Wait",
                                    "B) Paging, Segmentation, Swapping, and Virtual Memory",
                                    "C) Read, Write, Execute, and Delete permissions",
                                    "D) CPU, RAM, Disk, and Network bottlenecking"
                            ))
                            .correctAnswer("A) Mutual Exclusion, Hold and Wait, No Preemption, and Circular Wait")
                            .explanation("All four Coffman conditions must hold simultaneously for a system deadlock to exist; breaking any one prevents deadlock.")
                            .expectedConcepts(List.of("Deadlock", "Coffman Conditions", "Mutual Exclusion", "Circular Wait"))
                            .orderIndex(26)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What is the key difference between a Process and a Thread?")
                            .category(ProblemCategory.OS)
                            .topic("Processes & Threads")
                            .difficulty(Difficulty.EASY)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) A process is an independent executing program with its own memory address space; threads share the memory space of their parent process",
                                    "B) Threads have completely isolated heap memory",
                                    "C) Processes share the same stack frames",
                                    "D) Threads cannot execute concurrently on multi-core CPUs"
                            ))
                            .correctAnswer("A) A process is an independent executing program with its own memory address space; threads share the memory space of their parent process")
                            .explanation("Processes have isolated virtual memory; threads within the same process share code, data, and heap but retain private stacks.")
                            .expectedConcepts(List.of("Process vs Thread", "Address Space", "Context Switching"))
                            .orderIndex(27)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What is a Page Fault in Virtual Memory management?")
                            .category(ProblemCategory.OS)
                            .topic("Virtual Memory")
                            .difficulty(Difficulty.MEDIUM)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) A trap raised by the MMU when a program accesses a memory page mapped in virtual address space but not currently loaded in physical RAM",
                                    "B) A corrupted sector on physical hard disk",
                                    "C) An illegal null pointer dereference in C/C++",
                                    "D) A syntax error in HTML parsing"
                            ))
                            .correctAnswer("A) A trap raised by the MMU when a program accesses a memory page mapped in virtual address space but not currently loaded in physical RAM")
                            .explanation("When a valid page is missing from RAM, the OS handles the page fault by swapping the block from disk to physical memory.")
                            .expectedConcepts(List.of("Page Fault", "MMU", "Virtual Memory", "Paging"))
                            .orderIndex(28)
                            .build(),

                    // 7. COMPUTER NETWORKS (3 questions)
                    InterviewQuestion.builder()
                            .question("What sequence of packets is exchanged in the standard TCP 3-Way Handshake connection establishment?")
                            .category(ProblemCategory.CN)
                            .topic("Transport Layer")
                            .difficulty(Difficulty.EASY)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) SYN -> SYN-ACK -> ACK",
                                    "B) ACK -> SYN -> FIN",
                                    "C) PING -> PONG -> OK",
                                    "D) RST -> SYN -> DATA"
                            ))
                            .correctAnswer("A) SYN -> SYN-ACK -> ACK")
                            .explanation("Client sends SYN; Server responds with SYN-ACK; Client replies with ACK to establish reliable full-duplex communication.")
                            .expectedConcepts(List.of("TCP Handshake", "SYN", "ACK", "Connection Reliability"))
                            .orderIndex(29)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What major protocol change distinguishes HTTP/3 from HTTP/2 and HTTP/1.1?")
                            .category(ProblemCategory.CN)
                            .topic("Application Protocols")
                            .difficulty(Difficulty.HARD)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) HTTP/3 runs on top of QUIC (built on UDP) to eliminate Head-of-Line blocking at the transport layer",
                                    "B) HTTP/3 eliminates TLS encryption",
                                    "C) HTTP/3 requires plain text XML payloads",
                                    "D) HTTP/3 only functions over Bluetooth connections"
                            ))
                            .correctAnswer("A) HTTP/3 runs on top of QUIC (built on UDP) to eliminate Head-of-Line blocking at the transport layer")
                            .explanation("HTTP/3 replaces TCP with QUIC over UDP, resolving TCP packet loss head-of-line blocking across multiplexed streams.")
                            .expectedConcepts(List.of("HTTP/3", "QUIC", "UDP", "Head-of-Line Blocking"))
                            .orderIndex(30)
                            .build(),

                    InterviewQuestion.builder()
                            .question("What is the primary role of DNS (Domain Name System) in internet routing?")
                            .category(ProblemCategory.CN)
                            .topic("Network Infrastructure")
                            .difficulty(Difficulty.EASY)
                            .questionType(QuestionType.MCQ)
                            .options(List.of(
                                    "A) Translating human-readable domain names (e.g. google.com) into machine-routable IP addresses (e.g. 142.250.190.46)",
                                    "B) Assigning local MAC addresses to WiFi routers",
                                    "C) Encrypting HTTPS passwords during login",
                                    "D) Measuring internet upload and download speeds"
                            ))
                            .correctAnswer("A) Translating human-readable domain names (e.g. google.com) into machine-routable IP addresses (e.g. 142.250.190.46)")
                            .explanation("DNS is the internet phonebook resolving hierarchical domain lookups through root, TLD, and authoritative nameservers.")
                            .expectedConcepts(List.of("DNS", "IP Address", "TLD", "Authoritative Nameserver"))
                            .orderIndex(31)
                            .build()
            );

            questionRepository.saveAll(initialQuestions);
            logger.info("Successfully seeded {} curated Mock Interview questions across 7 core technical categories.", initialQuestions.size());
        }
    }
}
