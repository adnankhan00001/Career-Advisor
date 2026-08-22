package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.ExtractedSkillDto;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ResumeSkillExtractor {

    public record SkillDefinition(String canonicalName, String category, List<Pattern> patterns) {}

    private final List<SkillDefinition> skillDefinitions = new ArrayList<>();

    public ResumeSkillExtractor() {
        initSkillDefinitions();
    }

    private void initSkillDefinitions() {
        // Programming Languages
        addSkill("Java", "Programming Languages", "\\bjava\\b(?!\\s*script)", "\\bcore\\s+java\\b", "\\bjava\\s*1[17]\\b", "\\bjava\\s*8\\b");
        addSkill("Python", "Programming Languages", "\\bpython\\b", "\\bpython3\\b");
        addSkill("JavaScript", "Programming Languages", "\\bjavascript\\b", "\\bjs\\b", "\\bes6\\b", "\\becmascript\\b");
        addSkill("TypeScript", "Programming Languages", "\\btypescript\\b", "\\bts\\b");
        addSkill("C++", "Programming Languages", "\\bc\\+\\+\\b", "\\bcpp\\b");
        addSkill("C#", "Programming Languages", "\\bc#\\b", "\\bcsharp\\b", "\\b\\.net\\b");
        addSkill("Go", "Programming Languages", "\\bgolang\\b", "\\bgo\\s+language\\b");
        addSkill("SQL", "Programming Languages", "\\bsql\\b(?!\\s*server)", "\\bstructured\\s+query\\s+language\\b");
        addSkill("HTML", "Programming Languages", "\\bhtml\\b", "\\bhtml5\\b");
        addSkill("CSS", "Programming Languages", "\\bcss\\b", "\\bcss3\\b");

        // Frameworks & Libraries
        addSkill("Spring Boot", "Frameworks & Libraries", "\\bspring\\s*boot\\b", "\\bspringboot\\b", "\\bspring\\s+framework\\b");
        addSkill("Spring Security", "Frameworks & Libraries", "\\bspring\\s+security\\b");
        addSkill("Hibernate", "Frameworks & Libraries", "\\bhibernate\\b", "\\borm\\b");
        addSkill("JPA", "Frameworks & Libraries", "\\bjpa\\b", "\\bspring\\s+data\\s+jpa\\b", "\\bjakarta\\s+persistence\\b");
        addSkill("React", "Frameworks & Libraries", "\\breact\\b", "\\breactjs\\b", "\\breact\\.js\\b");
        addSkill("Next.js", "Frameworks & Libraries", "\\bnextjs\\b", "\\bnext\\.js\\b", "\\bnext\\s+js\\b");
        addSkill("Node.js", "Frameworks & Libraries", "\\bnodejs\\b", "\\bnode\\.js\\b", "\\bnode\\b");
        addSkill("Tailwind CSS", "Frameworks & Libraries", "\\btailwind\\b", "\\btailwind\\s*css\\b");
        addSkill("Express", "Frameworks & Libraries", "\\bexpress\\b", "\\bexpressjs\\b", "\\bexpress\\.js\\b");
        addSkill("PyTorch", "Frameworks & Libraries", "\\bpytorch\\b");
        addSkill("TensorFlow", "Frameworks & Libraries", "\\btensorflow\\b", "\\btf\\b");
        addSkill("Pandas", "Frameworks & Libraries", "\\bpandas\\b");
        addSkill("NumPy", "Frameworks & Libraries", "\\bnumpy\\b");
        addSkill("Scikit-Learn", "Frameworks & Libraries", "\\bscikit-learn\\b", "\\bprompt-learn\\b", "\\bsklearn\\b");

        // Databases & Caching
        addSkill("MySQL", "Databases", "\\bmysql\\b", "\\bmy\\s+sql\\b");
        addSkill("PostgreSQL", "Databases", "\\bpostgresql\\b", "\\bpostgres\\b");
        addSkill("MongoDB", "Databases", "\\bmongodb\\b", "\\bmongo\\b");
        addSkill("Redis", "Databases", "\\bredis\\b");
        addSkill("Kafka", "Databases", "\\bkafka\\b", "\\bapache\\s+kafka\\b");

        // Cloud & DevOps
        addSkill("Docker", "Cloud & DevOps", "\\bdocker\\b", "\\bcontainerization\\b", "\\bcontainers\\b");
        addSkill("Kubernetes", "Cloud & DevOps", "\\bkubernetes\\b", "\\bk8s\\b");
        addSkill("AWS", "Cloud & DevOps", "\\baws\\b", "\\bamazon\\s+web\\s+services\\b", "\\bec2\\b", "\\bs3\\b", "\\blambda\\b");
        addSkill("Linux", "Cloud & DevOps", "\\blinux\\b", "\\bubuntu\\b", "\\bbash\\b", "\\bshell\\s+scripting\\b");
        addSkill("Git", "Cloud & DevOps", "\\bgit\\b", "\\bgithub\\b", "\\bgitlab\\b", "\\bversion\\s+control\\b");
        addSkill("CI/CD", "Cloud & DevOps", "\\bci/cd\\b", "\\bci-cd\\b", "\\bcontinuous\\s+integration\\b", "\\bgithub\\s+actions\\b", "\\bjenkins\\b");
        addSkill("Terraform", "Cloud & DevOps", "\\bterraform\\b", "\\binfrastructure\\s+as\\s+code\\b", "\\biac\\b");
        addSkill("Cloud Architecture", "Cloud & DevOps", "\\bcloud\\s+architecture\\b", "\\bcloud\\s+computing\\b", "\\bhigh\\s+availability\\b");

        // Architecture & Core CS
        addSkill("REST APIs", "Architecture & Core CS", "\\brest\\s*apis?\\b", "\\brestful\\b", "\\brestful\\s*apis?\\b", "\\bweb\\s*apis?\\b", "\\brest\\b");
        addSkill("GraphQL", "Architecture & Core CS", "\\bgraphql\\b");
        addSkill("Microservices", "Architecture & Core CS", "\\bmicroservices?\\b", "\\bdistributed\\s+systems?\\b");
        addSkill("DSA", "Architecture & Core CS", "\\bdsa\\b", "\\bdata\\s+structures\\b", "\\balgorithms\\b", "\\bdata\\s+structures\\s+and\\s+algorithms\\b");
        addSkill("Operating Systems", "Architecture & Core CS", "\\boperating\\s+systems?\\b", "\\bos\\s+concepts\\b", "\\bmultithreading\\b", "\\bconcurrency\\b");
        addSkill("Computer Networks", "Architecture & Core CS", "\\bcomputer\\s+networks?\\b", "\\bnetworking\\b", "\\btcp/ip\\b", "\\bhttp/https\\b");
        addSkill("DBMS", "Architecture & Core CS", "\\bdbms\\b", "\\bdatabase\\s+management\\b", "\\bacid\\s+properties\\b", "\\bdatabase\\s+indexing\\b");
        addSkill("System Design", "Architecture & Core CS", "\\bsystem\\s+design\\b", "\\bscalable\\s+systems\\b", "\\bload\\s+balancing\\b");
        addSkill("Security", "Architecture & Core CS", "\\bjwt\\b", "\\boauth2?\\b", "\\bauthentication\\b", "\\bcyber\\s*security\\b", "\\bweb\\s+security\\b");

        // Data & AI
        addSkill("Machine Learning", "Data & AI", "\\bmachine\\s+learning\\b", "\\bml\\b", "\\bdeep\\s+learning\\b", "\\bneural\\s+networks?\\b");
        addSkill("Power BI", "Data & AI", "\\bpower\\s*bi\\b", "\\btableau\\b");
        addSkill("Excel", "Data & AI", "\\bexcel\\b", "\\badvanced\\s+excel\\b", "\\bgoogle\\s+sheets\\b");
        addSkill("Data Visualization", "Data & AI", "\\bdata\\s+visualization\\b", "\\bmatplotlib\\b", "\\bseaborn\\b");
    }

    private void addSkill(String name, String category, String... regexes) {
        List<Pattern> patterns = Arrays.stream(regexes)
                .map(r -> Pattern.compile(r, Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());
        skillDefinitions.add(new SkillDefinition(name, category, patterns));
    }

    public List<ExtractedSkillDto> extractSkills(String text, Set<String> existingUserSkills) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String lowerText = text.toLowerCase();
        Set<String> normalizedUserSkills = existingUserSkills != null
                ? existingUserSkills.stream().map(String::toLowerCase).collect(Collectors.toSet())
                : Collections.emptySet();

        Map<String, ExtractedSkillDto> matchedSkills = new LinkedHashMap<>();

        // Identify if a dedicated "skills" section exists in text
        String skillsSection = extractSection(text, "SKILLS", "TECHNICAL SKILLS", "KEY SKILLS", "EXPERTISE", "TECHNOLOGIES");

        for (SkillDefinition def : skillDefinitions) {
            int matchCount = 0;
            boolean inSkillsSection = false;

            for (Pattern pattern : def.patterns()) {
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    matchCount++;
                }

                if (skillsSection != null && pattern.matcher(skillsSection).find()) {
                    inSkillsSection = true;
                }
            }

            if (matchCount > 0) {
                // Confidence scoring
                int confidence = 80;
                if (inSkillsSection) confidence += 10;
                if (matchCount >= 3) confidence += 5;
                if (matchCount >= 5) confidence += 3;
                confidence = Math.min(99, confidence);

                boolean alreadyInProfile = normalizedUserSkills.contains(def.canonicalName().toLowerCase());

                matchedSkills.put(def.canonicalName(), ExtractedSkillDto.builder()
                        .skillName(def.canonicalName())
                        .category(def.category())
                        .confidence(confidence)
                        .alreadyInProfile(alreadyInProfile)
                        .build());
            }
        }

        // Sort: by category, then by confidence descending
        return matchedSkills.values().stream()
                .sorted((a, b) -> {
                    if (a.getConfidence() != b.getConfidence()) {
                        return Integer.compare(b.getConfidence(), a.getConfidence());
                    }
                    return a.getSkillName().compareToIgnoreCase(b.getSkillName());
                })
                .collect(Collectors.toList());
    }

    public String extractEmail(String text) {
        if (text == null) return null;
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher matcher = emailPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    public String extractPhone(String text) {
        if (text == null) return null;
        Pattern phonePattern = Pattern.compile("(\\+?\\d{1,3}[-.\\s]?)?(\\(?\\d{3}\\)?[-.\\s]?)?\\d{3}[-.\\s]?\\d{4}");
        Matcher matcher = phonePattern.matcher(text);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    public String extractSummary(String text) {
        String summarySection = extractSection(text, "SUMMARY", "PROFESSIONAL SUMMARY", "PROFILE", "ABOUT ME", "CAREER OBJECTIVE");
        if (summarySection != null && !summarySection.trim().isEmpty()) {
            return summarySection.trim();
        }

        // Fallback: extract the first 3 non-empty lines that don't look like headers
        String[] lines = text.split("\n");
        List<String> validLines = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 30 && !trimmed.contains("@") && !trimmed.matches(".*\\d{10}.*")) {
                validLines.add(trimmed);
                if (validLines.size() >= 2) break;
            }
        }

        return validLines.isEmpty()
                ? "Candidate resume parsed with extracted technical skills and background."
                : String.join(" ", validLines);
    }

    public List<String> extractEducation(String text) {
        String eduSection = extractSection(text, "EDUCATION", "ACADEMIC BACKGROUND", "QUALIFICATIONS", "ACADEMICS");
        if (eduSection == null || eduSection.trim().isEmpty()) {
            // Regex search for degree keywords
            List<String> degrees = new ArrayList<>();
            Pattern p = Pattern.compile("(?i)\\b(bachelor|master|b\\.tech|m\\.tech|b\\.e|m\\.e|b\\.sc|m\\.sc|bca|mca|phd|diploma)\\b[^\\n.]+");
            Matcher m = p.matcher(text);
            while (m.find() && degrees.size() < 4) {
                degrees.add(m.group().trim());
            }
            return degrees;
        }

        return splitSectionItems(eduSection);
    }

    public List<String> extractExperience(String text) {
        String expSection = extractSection(text, "EXPERIENCE", "WORK EXPERIENCE", "EMPLOYMENT HISTORY", "PROFESSIONAL EXPERIENCE", "WORK HISTORY");
        if (expSection == null || expSection.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return splitSectionItems(expSection);
    }

    public List<String> extractProjects(String text) {
        String projSection = extractSection(text, "PROJECTS", "PERSONAL PROJECTS", "ACADEMIC PROJECTS", "KEY PROJECTS");
        if (projSection == null || projSection.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return splitSectionItems(projSection);
    }

    private String extractSection(String text, String... headers) {
        if (text == null) return null;

        for (String header : headers) {
            Pattern p = Pattern.compile("(?i)(?:^|\\n)\\s*" + Pattern.quote(header) + "\\s*[:\\-]?\\s*\\n([\\s\\S]*?)(?=(?:\\n\\s*[A-Z\\s]{3,20}\\s*[:\\-]?\\s*\\n|$))");
            Matcher m = p.matcher(text);
            if (m.find()) {
                String content = m.group(1).trim();
                if (!content.isEmpty()) {
                    return content;
                }
            }
        }
        return null;
    }

    private List<String> splitSectionItems(String sectionText) {
        if (sectionText == null || sectionText.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] lines = sectionText.split("\n");
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.matches("^\\d+[.)].*")) {
                if (current.length() > 0) {
                    items.add(current.toString().trim());
                    current.setLength(0);
                }
                current.append(trimmed.replaceFirst("^[•\\-*\\d.)\\s]+", ""));
            } else {
                if (current.length() > 0) {
                    current.append(" ").append(trimmed);
                } else {
                    current.append(trimmed);
                }
            }
        }

        if (current.length() > 0) {
            items.add(current.toString().trim());
        }

        return items.stream()
                .filter(s -> s.length() >= 5)
                .limit(6)
                .collect(Collectors.toList());
    }
}
