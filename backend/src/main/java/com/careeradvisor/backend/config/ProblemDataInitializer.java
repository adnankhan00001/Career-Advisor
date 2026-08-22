package com.careeradvisor.backend.config;

import com.careeradvisor.backend.model.CodingProblem;
import com.careeradvisor.backend.model.Difficulty;
import com.careeradvisor.backend.model.ProblemCategory;
import com.careeradvisor.backend.repository.CodingProblemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProblemDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProblemDataInitializer.class);

    private final CodingProblemRepository problemRepository;

    public ProblemDataInitializer(CodingProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    @Override
    public void run(String... args) {
        if (problemRepository.count() == 0) {
            logger.info("Seeding curated interview preparation coding problems...");

            List<CodingProblem> initialProblems = List.of(
                    // 1. Arrays
                    CodingProblem.builder()
                            .slug("two-sum")
                            .title("Two Sum")
                            .description("Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`. You may assume each input has exactly one solution.")
                            .difficulty(Difficulty.EASY)
                            .category(ProblemCategory.DSA)
                            .topic("Arrays")
                            .externalUrl("https://leetcode.com/problems/two-sum/")
                            .tags(List.of("Array", "Hash Table"))
                            .acceptanceRate("52.8%")
                            .orderIndex(1)
                            .starterCode("class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        // Write your solution here\n        return new int[]{};\n    }\n}")
                            .sampleInput("nums = [2,7,11,15], target = 9")
                            .sampleOutput("[0,1]")
                            .constraints("2 <= nums.length <= 10^4\n-10^9 <= nums[i] <= 10^9\n-10^9 <= target <= 10^9")
                            .explanation("Because nums[0] + nums[1] == 9, we return [0, 1].")
                            .build(),

                    CodingProblem.builder()
                            .slug("best-time-to-buy-and-sell-stock")
                            .title("Best Time to Buy and Sell Stock")
                            .description("You are given an array `prices` where `prices[i]` is the price of a given stock on the `i-th` day. Maximize your profit by choosing a single day to buy one stock and choosing a different day in the future to sell that stock.")
                            .difficulty(Difficulty.EASY)
                            .category(ProblemCategory.DSA)
                            .topic("Arrays")
                            .externalUrl("https://leetcode.com/problems/best-time-to-buy-and-sell-stock/")
                            .tags(List.of("Array", "Dynamic Programming"))
                            .acceptanceRate("54.2%")
                            .orderIndex(2)
                            .starterCode("class Solution {\n    public int maxProfit(int[] prices) {\n        // Write your solution here\n        return 0;\n    }\n}")
                            .sampleInput("prices = [7,1,5,3,6,4]")
                            .sampleOutput("5")
                            .constraints("1 <= prices.length <= 10^5\n0 <= prices[i] <= 10^4")
                            .explanation("Buy on day 2 (price = 1) and sell on day 5 (price = 6), profit = 6-1 = 5.")
                            .build(),

                    CodingProblem.builder()
                            .slug("maximum-subarray")
                            .title("Maximum Subarray")
                            .description("Given an integer array `nums`, find the subarray with the largest sum, and return its sum (Kadane's Algorithm).")
                            .difficulty(Difficulty.MEDIUM)
                            .category(ProblemCategory.DSA)
                            .topic("Arrays")
                            .externalUrl("https://leetcode.com/problems/maximum-subarray/")
                            .tags(List.of("Array", "Divide and Conquer", "Dynamic Programming"))
                            .acceptanceRate("50.5%")
                            .orderIndex(3)
                            .starterCode("class Solution {\n    public int maxSubArray(int[] nums) {\n        // Write your solution here\n        return 0;\n    }\n}")
                            .sampleInput("nums = [-2,1,-3,4,-1,2,1,-5,4]")
                            .sampleOutput("6")
                            .constraints("1 <= nums.length <= 10^5\n-10^4 <= nums[i] <= 10^4")
                            .explanation("The subarray [4,-1,2,1] has the largest sum 6.")
                            .build(),

                    // 2. Strings
                    CodingProblem.builder()
                            .slug("valid-anagram")
                            .title("Valid Anagram")
                            .description("Given two strings `s` and `t`, return `true` if `t` is an anagram of `s`, and `false` otherwise.")
                            .difficulty(Difficulty.EASY)
                            .category(ProblemCategory.DSA)
                            .topic("Strings")
                            .externalUrl("https://leetcode.com/problems/valid-anagram/")
                            .tags(List.of("Hash Table", "String", "Sorting"))
                            .acceptanceRate("64.1%")
                            .orderIndex(4)
                            .starterCode("class Solution {\n    public boolean isAnagram(String s, String t) {\n        // Write your solution here\n        return false;\n    }\n}")
                            .sampleInput("s = \"anagram\", t = \"nagaram\"")
                            .sampleOutput("true")
                            .constraints("1 <= s.length, t.length <= 5 * 10^4")
                            .explanation("Both strings contain the exact same frequency of characters.")
                            .build(),

                    CodingProblem.builder()
                            .slug("valid-palindrome")
                            .title("Valid Palindrome")
                            .description("A phrase is a palindrome if, after converting all uppercase letters into lowercase letters and removing all non-alphanumeric characters, it reads the same forward and backward.")
                            .difficulty(Difficulty.EASY)
                            .category(ProblemCategory.DSA)
                            .topic("Strings")
                            .externalUrl("https://leetcode.com/problems/valid-palindrome/")
                            .tags(List.of("Two Pointers", "String"))
                            .acceptanceRate("47.2%")
                            .orderIndex(5)
                            .starterCode("class Solution {\n    public boolean isPalindrome(String s) {\n        // Write your solution here\n        return false;\n    }\n}")
                            .sampleInput("s = \"A man, a plan, a canal: Panama\"")
                            .sampleOutput("true")
                            .constraints("1 <= s.length <= 2 * 10^5")
                            .explanation("\"amanaplanacanalpanama\" is a palindrome.")
                            .build(),

                    CodingProblem.builder()
                            .slug("longest-substring-without-repeating-characters")
                            .title("Longest Substring Without Repeating Characters")
                            .description("Given a string `s`, find the length of the longest substring without duplicate characters using a sliding window technique.")
                            .difficulty(Difficulty.MEDIUM)
                            .category(ProblemCategory.DSA)
                            .topic("Strings")
                            .externalUrl("https://leetcode.com/problems/longest-substring-without-repeating-characters/")
                            .tags(List.of("Hash Table", "String", "Sliding Window"))
                            .acceptanceRate("34.8%")
                            .orderIndex(6)
                            .starterCode("class Solution {\n    public int lengthOfLongestSubstring(String s) {\n        // Write your solution here\n        return 0;\n    }\n}")
                            .sampleInput("s = \"abcabcbb\"")
                            .sampleOutput("3")
                            .constraints("0 <= s.length <= 5 * 10^4")
                            .explanation("The answer is \"abc\", with the length of 3.")
                            .build(),

                    // 3. Linked List
                    CodingProblem.builder()
                            .slug("reverse-linked-list")
                            .title("Reverse Linked List")
                            .description("Given the `head` of a singly linked list, reverse the list, and return the reversed list iteratively or recursively.")
                            .difficulty(Difficulty.EASY)
                            .category(ProblemCategory.DSA)
                            .topic("Linked List")
                            .externalUrl("https://leetcode.com/problems/reverse-linked-list/")
                            .tags(List.of("Linked List", "Recursion"))
                            .acceptanceRate("76.2%")
                            .orderIndex(7)
                            .starterCode("/**\n * Definition for singly-linked list.\n * public class ListNode {\n *     int val;\n *     ListNode next;\n *     ListNode(int val) { this.val = val; }\n * }\n */\nclass Solution {\n    public ListNode reverseList(ListNode head) {\n        // Write your solution here\n        return null;\n    }\n}")
                            .sampleInput("head = [1,2,3,4,5]")
                            .sampleOutput("[5,4,3,2,1]")
                            .constraints("The number of nodes in the list is in the range [0, 5000].")
                            .explanation("Iteratively update next pointers backwards.")
                            .build(),

                    CodingProblem.builder()
                            .slug("merge-two-sorted-lists")
                            .title("Merge Two Sorted Lists")
                            .description("You are given the heads of two sorted linked lists `list1` and `list2`. Merge the two lists into one sorted list.")
                            .difficulty(Difficulty.EASY)
                            .category(ProblemCategory.DSA)
                            .topic("Linked List")
                            .externalUrl("https://leetcode.com/problems/merge-two-sorted-lists/")
                            .tags(List.of("Linked List", "Recursion"))
                            .acceptanceRate("64.5%")
                            .orderIndex(8)
                            .starterCode("class Solution {\n    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {\n        // Write your solution here\n        return null;\n    }\n}")
                            .sampleInput("list1 = [1,2,4], list2 = [1,3,4]")
                            .sampleOutput("[1,1,2,3,4,4]")
                            .constraints("The number of nodes in both lists is in the range [0, 50].")
                            .explanation("Compare node values and splice pointers in non-decreasing order.")
                            .build(),

                    // 4. Stack & Queue
                    CodingProblem.builder()
                            .slug("valid-parentheses")
                            .title("Valid Parentheses")
                            .description("Given a string `s` containing just the characters '(', ')', '{', '}', '[' and ']', determine if the input string is valid.")
                            .difficulty(Difficulty.EASY)
                            .category(ProblemCategory.DSA)
                            .topic("Stack")
                            .externalUrl("https://leetcode.com/problems/valid-parentheses/")
                            .tags(List.of("String", "Stack"))
                            .acceptanceRate("41.0%")
                            .orderIndex(9)
                            .starterCode("class Solution {\n    public boolean isValid(String s) {\n        // Write your solution here\n        return false;\n    }\n}")
                            .sampleInput("s = \"()[]{}\"")
                            .sampleOutput("true")
                            .constraints("1 <= s.length <= 10^4")
                            .explanation("Open brackets must be closed by the same type of brackets in the correct order.")
                            .build(),

                    // 5. Binary Search
                    CodingProblem.builder()
                            .slug("binary-search")
                            .title("Binary Search")
                            .description("Given an array of integers `nums` which is sorted in ascending order, and an integer `target`, write a function to search `target` in `nums` in O(log n) runtime.")
                            .difficulty(Difficulty.EASY)
                            .category(ProblemCategory.DSA)
                            .topic("Binary Search")
                            .externalUrl("https://leetcode.com/problems/binary-search/")
                            .tags(List.of("Array", "Binary Search"))
                            .acceptanceRate("58.1%")
                            .orderIndex(10)
                            .starterCode("class Solution {\n    public int search(int[] nums, int target) {\n        // Write your solution here\n        return -1;\n    }\n}")
                            .sampleInput("nums = [-1,0,3,5,9,12], target = 9")
                            .sampleOutput("4")
                            .constraints("1 <= nums.length <= 10^4\n-10^4 < nums[i], target < 10^4")
                            .explanation("9 exists in nums and its index is 4.")
                            .build(),

                    // 6. Trees & Graphs
                    CodingProblem.builder()
                            .slug("maximum-depth-of-binary-tree")
                            .title("Maximum Depth of Binary Tree")
                            .description("Given the `root` of a binary tree, return its maximum depth (the number of nodes along the longest path from root to farthest leaf node).")
                            .difficulty(Difficulty.EASY)
                            .category(ProblemCategory.DSA)
                            .topic("Trees")
                            .externalUrl("https://leetcode.com/problems/maximum-depth-of-binary-tree/")
                            .tags(List.of("Tree", "Depth-First Search", "Breadth-First Search", "Binary Tree"))
                            .acceptanceRate("75.4%")
                            .orderIndex(11)
                            .starterCode("class Solution {\n    public int maxDepth(TreeNode root) {\n        // Write your solution here\n        return 0;\n    }\n}")
                            .sampleInput("root = [3,9,20,null,null,15,7]")
                            .sampleOutput("3")
                            .constraints("The number of nodes in the tree is in the range [0, 10^4].")
                            .explanation("The longest path is 3 -> 20 -> 15 (depth 3).")
                            .build(),

                    // Core Technical Subjects
                    CodingProblem.builder()
                            .slug("java-hashmap-internal-working")
                            .title("Java HashMap Internal Working & Collisions")
                            .description("Explain and implement the core conceptual logic of HashMap `put()` and `get()` operations in Java 8+ including bucket indexing, hash collision chaining (LinkedList), and Treeification (Red-Black Tree when threshold >= 8).")
                            .difficulty(Difficulty.MEDIUM)
                            .category(ProblemCategory.JAVA)
                            .topic("Core Java")
                            .externalUrl("https://docs.oracle.com/javase/8/docs/api/java/util/HashMap.html")
                            .tags(List.of("Java", "HashMap", "Collections", "Hashing"))
                            .acceptanceRate("42.0%")
                            .orderIndex(12)
                            .starterCode("public class CustomHashMap<K, V> {\n    // Implement custom bucket array and hash function\n    public void put(K key, V value) {\n        // Logic here\n    }\n    public V get(K key) {\n        return null;\n    }\n}")
                            .sampleInput("put(\"apple\", 10); get(\"apple\")")
                            .sampleOutput("10")
                            .constraints("Handles hash collisions gracefully.")
                            .explanation("Uses (n - 1) & hash for index calculation and linked list node traversal for collisions.")
                            .build(),

                    CodingProblem.builder()
                            .slug("dbms-acid-properties-and-isolation-levels")
                            .title("DBMS ACID Properties & Transaction Isolation")
                            .description("Analyze transaction anomalies: Dirty Reads, Non-Repeatable Reads, and Phantom Reads. Detail how Read Committed, Repeatable Read, and Serializable isolation levels prevent each.")
                            .difficulty(Difficulty.MEDIUM)
                            .category(ProblemCategory.DBMS)
                            .topic("DBMS & SQL")
                            .externalUrl("https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html")
                            .tags(List.of("DBMS", "ACID", "Transactions", "Concurrency"))
                            .acceptanceRate("48.5%")
                            .orderIndex(13)
                            .starterCode("/*\n * Provide SQL transaction examples illustrating:\n * 1. READ COMMITTED vs REPEATABLE READ\n * 2. Pessimistic Locking with SELECT ... FOR UPDATE\n */")
                            .sampleInput("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;")
                            .sampleOutput("TRANSACTION APPLIED")
                            .constraints("Standard MySQL InnoDB transaction semantics.")
                            .explanation("Consistent non-locking reads using MVCC snapshot isolation.")
                            .build(),

                    CodingProblem.builder()
                            .slug("spring-boot-bean-lifecycle-and-scopes")
                            .title("Spring Boot Bean Lifecycle & Scopes")
                            .description("Demonstrate understanding of Spring IoC container initialization, BeanPostProcessor execution, `@PostConstruct`, `@PreDestroy`, and the differences between Singleton, Prototype, and Request scopes.")
                            .difficulty(Difficulty.MEDIUM)
                            .category(ProblemCategory.SPRING_BOOT)
                            .topic("Spring Boot")
                            .externalUrl("https://docs.spring.io/spring-framework/reference/core/beans.html")
                            .tags(List.of("Spring Boot", "IoC", "Dependency Injection", "Beans"))
                            .acceptanceRate("46.0%")
                            .orderIndex(14)
                            .starterCode("@Component\npublic class LifecycleBean {\n    @PostConstruct\n    public void init() {\n        // Initialized\n    }\n}")
                            .sampleInput("ApplicationContext.getBean(LifecycleBean.class)")
                            .sampleOutput("Bean initialized and injected.")
                            .constraints("Standard Spring Framework lifecycle contracts.")
                            .explanation("Instantiate -> Populate Properties -> BeanNameAware -> BeanPostProcessor -> @PostConstruct.")
                            .build()
            );

            problemRepository.saveAll(initialProblems);
            logger.info("Successfully seeded {} curated problems with starter templates and test cases.", initialProblems.size());
        }
    }
}
