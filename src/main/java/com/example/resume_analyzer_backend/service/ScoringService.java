package com.example.resume_analyzer_backend.service;

import com.example.resume_analyzer_backend.entity.AnalysisResult.ReadinessLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ScoringService {

    private static final Map<String, String> SKILL_CATEGORIES = new LinkedHashMap<>() {{
        put("java", "LANGUAGE");         put("python", "LANGUAGE");
        put("javascript", "LANGUAGE");   put("typescript", "LANGUAGE");
        put("kotlin", "LANGUAGE");       put("swift", "LANGUAGE");
        put("c++", "LANGUAGE");          put("c#", "LANGUAGE");
        put("go", "LANGUAGE");           put("rust", "LANGUAGE");
        put("php", "LANGUAGE");          put("ruby", "LANGUAGE");
        put("scala", "LANGUAGE");        put("dart", "LANGUAGE");
        put("r", "LANGUAGE");            put("matlab", "LANGUAGE");
        put("spring boot", "FRAMEWORK"); put("spring", "FRAMEWORK");
        put("react", "FRAMEWORK");       put("angular", "FRAMEWORK");
        put("vue", "FRAMEWORK");         put("django", "FRAMEWORK");
        put("flask", "FRAMEWORK");       put("fastapi", "FRAMEWORK");
        put("node.js", "FRAMEWORK");     put("nodejs", "FRAMEWORK");
        put("express", "FRAMEWORK");     put("laravel", "FRAMEWORK");
        put("hibernate", "FRAMEWORK");   put("tensorflow", "FRAMEWORK");
        put("pytorch", "FRAMEWORK");     put("scikit-learn", "FRAMEWORK");
        put("scikit", "FRAMEWORK");      put("keras", "FRAMEWORK");
        put("pandas", "FRAMEWORK");      put("numpy", "FRAMEWORK");
        put("flutter", "FRAMEWORK");
        put("docker", "TOOL");           put("kubernetes", "TOOL");
        put("jenkins", "TOOL");          put("git", "TOOL");
        put("maven", "TOOL");            put("gradle", "TOOL");
        put("jira", "TOOL");             put("postman", "TOOL");
        put("linux", "TOOL");            put("terraform", "TOOL");
        put("ansible", "TOOL");          put("github", "TOOL");
        put("gitlab", "TOOL");           put("bitbucket", "TOOL");
        put("mysql", "DATABASE");        put("postgresql", "DATABASE");
        put("mongodb", "DATABASE");      put("redis", "DATABASE");
        put("oracle", "DATABASE");       put("sql server", "DATABASE");
        put("elasticsearch", "DATABASE");put("cassandra", "DATABASE");
        put("sqlite", "DATABASE");       put("dynamodb", "DATABASE");
        put("sql", "DATABASE");          put("nosql", "DATABASE");
        put("aws", "CLOUD");             put("azure", "CLOUD");
        put("gcp", "CLOUD");             put("heroku", "CLOUD");
        put("firebase", "CLOUD");
        put("leadership", "SOFT");       put("communication", "SOFT");
        put("teamwork", "SOFT");         put("agile", "SOFT");
        put("scrum", "SOFT");            put("problem solving", "SOFT");
        put("mentoring", "SOFT");        put("analytical", "SOFT");
    }};

    private static final List<String> ACTION_VERBS = List.of(
        "developed","implemented","designed","led","managed","built","created",
        "improved","reduced","increased","optimized","delivered","architected",
        "deployed","collaborated","mentored","automated","integrated","migrated",
        "scaled","launched","maintained","engineered","analyzed","researched",
        "trained","tested","monitored","configured","coordinated","produced"
    );

    private static final List<String> DEGREE_KEYWORDS = List.of(
        "bachelor","master","phd","ph.d","doctorate","b.sc","m.sc","b.e","m.e",
        "b.tech","m.tech","mba","b.a","m.a","associate","diploma","b.s","m.s","degree"
    );

    private static final List<String> EDU_INSTITUTIONS = List.of(
        "university","college","institute","school","academy","polytechnic","iit","nit"
    );

    public Map<String, BigDecimal> scoreAll(String resumeText) {
        if (resumeText == null || resumeText.trim().isEmpty()) {
            log.warn("Resume text is empty");
            return zeroScores();
        }
        String lower = resumeText.toLowerCase();
        log.info("Scoring resume — length: {} chars", lower.length());

        BigDecimal skills     = scoreSkillsInternal(lower);
        BigDecimal experience = scoreExperienceInternal(lower);
        BigDecimal education  = scoreEducationInternal(lower);
        BigDecimal formatting = scoreFormattingInternal(lower);
        BigDecimal keywords   = scoreKeywordsInternal(lower);
        BigDecimal ats        = scoreATSInternal(resumeText, lower);

        BigDecimal overall = skills.multiply(new BigDecimal("0.30"))
            .add(experience.multiply(new BigDecimal("0.25")))
            .add(education.multiply(new BigDecimal("0.15")))
            .add(formatting.multiply(new BigDecimal("0.10")))
            .add(keywords.multiply(new BigDecimal("0.10")))
            .add(ats.multiply(new BigDecimal("0.10")))
            .setScale(2, RoundingMode.HALF_UP);

        log.info("SCORES overall:{} skills:{} exp:{} edu:{} fmt:{} kw:{} ats:{}",
            overall, skills, experience, education, formatting, keywords, ats);

        Map<String, BigDecimal> s = new LinkedHashMap<>();
        s.put("overall", overall); s.put("skills", skills);
        s.put("experience", experience); s.put("education", education);
        s.put("formatting", formatting); s.put("keywords", keywords);
        s.put("atsCompatibility", ats);
        return s;
    }

    private BigDecimal scoreSkillsInternal(String lower) {
        long count = SKILL_CATEGORIES.keySet().stream().filter(lower::contains).count();
        log.info("Skills matched: {}", count);
        double score = Math.min(100.0, 20.0 + (count * 5.0));
        if (count > 0 && score < 30) score = 30.0;
        return bd(score);
    }

    private BigDecimal scoreExperienceInternal(String lower) {
        double score = 35.0;
        for (String w : new String[]{"experience","work","employment","position","role",
                "responsibilities","achievements","worked","working","intern","internship"}) {
            if (lower.contains(w)) score += 5.0;
        }
        if (lower.contains("year"))  score += 8.0;
        if (lower.contains("month")) score += 4.0;
        if (containsAny(lower, "led","managed","headed","supervised")) score += 8.0;
        return bd(Math.min(100.0, score));
    }

    private BigDecimal scoreEducationInternal(String lower) {
        double score = 40.0;
        if (containsAny(lower,"phd","doctorate","ph.d"))                           score = 95.0;
        else if (containsAny(lower,"master","msc","mba","m.s","m.e","m.tech"))     score = 82.0;
        else if (containsAny(lower,"bachelor","b.sc","b.e","b.tech","b.s","undergraduate")) score = 72.0;
        else if (containsAny(lower,"associate","diploma"))                          score = 57.0;
        else if (containsAny(lower,"university","college","degree"))                score = 65.0;
        if (containsAny(lower,"gpa","cgpa","grade")) score = Math.min(100.0, score + 5.0);
        return bd(score);
    }

    private BigDecimal scoreFormattingInternal(String lower) {
        double score = 40.0;
        for (String s : new String[]{"education","experience","skills","summary",
                "objective","projects","certifications","achievements","profile","about"}) {
            if (lower.contains(s)) score += 5.0;
        }
        int len = lower.length();
        if (len > 200) score += 5.0;
        if (len > 500) score += 5.0;
        if (len > 1000) score += 5.0;
        if (lower.contains("-") || lower.contains("bullet")) score += 5.0;
        return bd(Math.min(100.0, score));
    }

    private BigDecimal scoreKeywordsInternal(String lower) {
        long found = ACTION_VERBS.stream().filter(lower::contains).count();
        return bd(Math.min(100.0, 25.0 + (found * 5.0)));
    }

    private BigDecimal scoreATSInternal(String original, String lower) {
        double score = 55.0;
        if (lower.contains("@"))   score += 15.0;
        if (lower.contains("phone") || lower.contains("mobile")
                || original.matches("(?s).*\\d{3}[-.\\s]?\\d{4}.*")) score += 10.0;
        if (original.length() > 200) score += 5.0;
        if (original.length() > 500) score += 5.0;
        if (lower.contains("skills")) score += 5.0;
        return bd(Math.min(100.0, score));
    }

    private Map<String, BigDecimal> zeroScores() {
        Map<String, BigDecimal> s = new LinkedHashMap<>();
        for (String k : List.of("overall","skills","experience","education","formatting","keywords","atsCompatibility"))
            s.put(k, bd(0));
        return s;
    }

    public ReadinessLevel determineLevel(BigDecimal overall) {
        double s = overall.doubleValue();
        if (s >= 85) return ReadinessLevel.EXPERT;
        if (s >= 75) return ReadinessLevel.SENIOR;
        if (s >= 65) return ReadinessLevel.MID;
        if (s >= 50) return ReadinessLevel.JUNIOR;
        return ReadinessLevel.ENTRY;
    }

    public List<Map<String, String>> extractSkills(String text) {
        String lower = text.toLowerCase();
        List<Map<String, String>> found = new ArrayList<>();
        for (Map.Entry<String, String> e : SKILL_CATEGORIES.entrySet()) {
            if (lower.contains(e.getKey())) {
                found.add(Map.of("name", capitalize(e.getKey()),
                    "category", e.getValue(), "proficiency", "INTERMEDIATE"));
            }
        }
        return found;
    }

    public List<Map<String, Object>> extractEducation(String text) {
        List<Map<String, Object>> educations = new ArrayList<>();
        String[] lines = text.split("\\n");
        String lower   = text.toLowerCase();
        Pattern yearPat = Pattern.compile("\\b(19|20)\\d{2}\\b");

        for (int i = 0; i < lines.length; i++) {
            String line  = lines[i].trim();
            String lLine = line.toLowerCase();
            if (line.isEmpty() || line.length() > 250) continue;
            if (!DEGREE_KEYWORDS.stream().anyMatch(lLine::contains)) continue;

            String degree = line.length() > 120 ? line.substring(0, 120) : line;
            String fieldOfStudy = "";
            int inIdx = lLine.indexOf(" in ");
            if (inIdx >= 0 && inIdx + 4 < line.length()) {
                fieldOfStudy = line.substring(inIdx + 4).trim();
                if (fieldOfStudy.length() > 80) fieldOfStudy = fieldOfStudy.substring(0, 80);
            }

            String institution = "";
            for (int j = Math.max(0, i - 3); j < Math.min(lines.length, i + 4); j++) {
                if (j == i) continue;
                String nb = lines[j].trim();
                if (EDU_INSTITUTIONS.stream().anyMatch(nb.toLowerCase()::contains)) {
                    institution = nb.length() > 120 ? nb.substring(0, 120) : nb;
                    break;
                }
            }
            if (institution.isEmpty()) {
                for (int j = i + 1; j < Math.min(lines.length, i + 3); j++) {
                    String nb = lines[j].trim();
                    if (!nb.isEmpty() && nb.length() < 120
                            && !DEGREE_KEYWORDS.stream().anyMatch(nb.toLowerCase()::contains)) {
                        institution = nb; break;
                    }
                }
            }

            Integer endYear = null;
            for (int j = Math.max(0, i - 3); j < Math.min(lines.length, i + 4); j++) {
                Matcher m = yearPat.matcher(lines[j]);
                while (m.find()) {
                    int yr = Integer.parseInt(m.group());
                    if (yr >= 1990 && yr <= 2030) { endYear = yr; break; }
                }
                if (endYear != null) break;
            }

            Map<String, Object> edu = new LinkedHashMap<>();
            edu.put("degree", degree);
            edu.put("institution", institution.isEmpty() ? "See Resume" : institution);
            edu.put("fieldOfStudy", fieldOfStudy);
            edu.put("startYear", endYear != null ? endYear - 4 : null);
            edu.put("endYear", endYear);
            educations.add(edu);
        }

        if (educations.isEmpty()) {
            String degree = "Not Specified";
            if (containsAny(lower,"phd","doctorate"))      degree = "PhD";
            else if (containsAny(lower,"master","msc"))    degree = "Master's Degree";
            else if (containsAny(lower,"bachelor","b.tech")) degree = "Bachelor's Degree";
            else if (containsAny(lower,"diploma"))         degree = "Diploma";
            if (!degree.equals("Not Specified") || lower.contains("education")) {
                Map<String, Object> edu = new LinkedHashMap<>();
                edu.put("degree", degree); edu.put("institution", "See Resume");
                edu.put("fieldOfStudy", ""); edu.put("startYear", null); edu.put("endYear", null);
                educations.add(edu);
            }
        }
        return educations;
    }

    public List<Map<String, Object>> extractExperience(String text) {
        List<Map<String, Object>> experiences = new ArrayList<>();
        String[] lines = text.split("\\n");
        String lower = text.toLowerCase();
        int eduStart = Integer.MAX_VALUE, eduEnd = Integer.MAX_VALUE;

        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].trim().toLowerCase();
            if (l.equals("education") || l.startsWith("education ")) {
                eduStart = i;
                for (int j = i + 1; j < lines.length; j++) {
                    String next = lines[j].trim().toLowerCase();
                    if (next.equals("experience") || next.startsWith("work")
                            || next.equals("skills") || next.startsWith("project")) {
                        eduEnd = j; break;
                    }
                }
                if (eduEnd == Integer.MAX_VALUE) eduEnd = eduStart + 15;
                break;
            }
        }

        Pattern rangePat = Pattern.compile(
            "(20\\d{2}|19\\d{2})\\s*[-\\u2013]\\s*(20\\d{2}|present|current|now|till date)",
            Pattern.CASE_INSENSITIVE);
        int currentYear = java.time.LocalDate.now().getYear();

        for (int i = 0; i < lines.length; i++) {
            if (i >= eduStart && i <= eduEnd) continue;
            String lLine = lines[i].trim().toLowerCase();
            if (DEGREE_KEYWORDS.stream().anyMatch(lLine::contains)) continue;
            if (EDU_INSTITUTIONS.stream().anyMatch(lLine::contains)) continue;

            Matcher m = rangePat.matcher(lines[i]);
            if (!m.find()) continue;

            try {
                int start = Integer.parseInt(m.group(1));
                String endStr = m.group(2).toLowerCase().trim();
                boolean isCurr = containsAny(endStr,"present","current","now","till");
                int end = isCurr ? currentYear : Integer.parseInt(m.group(2));
                if (end < start || end - start > 50) continue;

                String title = "Position";
                for (int j = i - 1; j >= Math.max(0, i - 3); j--) {
                    String prev = lines[j].trim();
                    if (!prev.isEmpty() && prev.length() > 2 && prev.length() < 100
                            && !prev.matches(".*\\d{4}.*")
                            && !DEGREE_KEYWORDS.stream().anyMatch(prev.toLowerCase()::contains)) {
                        title = prev; break;
                    }
                }

                String company = "Company";
                for (int j = i - 2; j >= Math.max(0, i - 4); j--) {
                    String prev = lines[j].trim();
                    if (!prev.isEmpty() && !prev.equals(title) && prev.length() > 2
                            && prev.length() < 100 && !prev.matches(".*\\d{4}.*")
                            && !DEGREE_KEYWORDS.stream().anyMatch(prev.toLowerCase()::contains)) {
                        company = prev; break;
                    }
                }

                StringBuilder desc = new StringBuilder();
                for (int j = i + 1; j < Math.min(lines.length, i + 5); j++) {
                    String next = lines[j].trim();
                    if (!next.isEmpty() && next.length() > 5 && !next.matches(".*\\d{4}\\s*[-\\u2013].*"))
                        desc.append(next).append(" ");
                }

                Map<String, Object> exp = new LinkedHashMap<>();
                exp.put("title",          title.length() > 120 ? "Position" : title);
                exp.put("company",        company.length() > 120 ? "Company" : company);
                exp.put("startYear",      start);
                exp.put("endYear",        end);
                exp.put("isCurrent",      isCurr);
                exp.put("durationMonths", Math.max(1, (end - start) * 12));
                exp.put("description",    desc.toString().trim());
                experiences.add(exp);
            } catch (NumberFormatException ignored) {}
        }

        if (experiences.isEmpty() && lower.contains("experience")) {
            Map<String, Object> exp = new LinkedHashMap<>();
            exp.put("title","Professional Experience"); exp.put("company","See Resume");
            exp.put("isCurrent",false); exp.put("durationMonths",12);
            exp.put("description","See resume for details");
            experiences.add(exp);
        }
        return experiences;
    }

    public String generateSummary(String name, BigDecimal years, String industry, String levelName) {
        String lvl = switch (levelName) {
            case "EXPERT" -> "seasoned expert"; case "SENIOR" -> "senior professional";
            case "MID" -> "mid-level professional"; case "JUNIOR" -> "junior professional";
            default -> "emerging professional";
        };
        double y = years != null ? years.doubleValue() : 0.0;
        String yearsText = y > 0
            ? String.format("approximately %.0f year%s of experience", y, y == 1 ? "" : "s")
            : "early-stage experience";
        return String.format("%s is a %s in %s with %s. Their resume demonstrates a solid foundation of technical skills relevant to the %s field.",
            name != null && !name.isBlank() ? name : "The candidate", lvl, industry, yearsText, industry);
    }

    public List<String> generateStrengths(Map<String, BigDecimal> scores, List<Map<String, String>> skills) {
        List<String> strengths = new ArrayList<>();
        if (scores.get("skills").doubleValue() >= 30)
            strengths.add("Demonstrates a strong and diverse technical skill set");
        if (scores.get("experience").doubleValue() >= 50)
            strengths.add("Has solid work experience with clear responsibilities");
        if (scores.get("education").doubleValue() >= 55)
            strengths.add("Strong educational background");
        if (scores.get("atsCompatibility").doubleValue() >= 55)
            strengths.add("Resume is well-formatted for ATS systems");
        if (skills.size() >= 4)
            strengths.add("Wide range of skills across multiple domains");
        if (strengths.isEmpty())
            strengths.add("Shows potential for growth with further development");
        return strengths;
    }

    public List<Map<String, Object>> generateImprovements(Map<String, BigDecimal> scores, String text) {
        List<Map<String, Object>> list = new ArrayList<>();
        String lower = text.toLowerCase();
        if (scores.get("skills").doubleValue() < 65)
            list.add(improvement("SKILLS","HIGH","Add more technical skills with proficiency levels.",8.5));
        if (scores.get("experience").doubleValue() < 65)
            list.add(improvement("EXPERIENCE","HIGH","Quantify achievements with metrics. Use strong action verbs.",9.0));
        if (!containsAny(lower,"summary","objective","profile"))
            list.add(improvement("FORMATTING","MEDIUM","Add a Professional Summary at the top.",7.0));
        if (scores.get("atsCompatibility").doubleValue() < 70)
            list.add(improvement("ATS","HIGH","Use standard section headings. Avoid tables and graphics.",8.0));
        if (!containsAny(lower,"github","linkedin"))
            list.add(improvement("GENERAL","LOW","Add links to GitHub, LinkedIn or portfolio.",5.5));
        return list;
    }

    public String extractEmail(String text) {
        Matcher m = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,6}").matcher(text);
        return m.find() ? m.group() : null;
    }

    public String extractPhone(String text) {
        Matcher m = Pattern.compile("(\\+?\\d[\\d\\s\\-().]{7,14}\\d)").matcher(text);
        return m.find() ? m.group().trim() : null;
    }

    public String extractName(String text) {
        for (String line : text.split("\\n")) {
            line = line.trim();
            if (line.length() > 3 && line.length() < 60 && line.matches("[A-Za-z .''\\-]+")
                    && !line.toLowerCase().contains("resume") && !line.toLowerCase().contains("curriculum"))
                return line;
        }
        return "Candidate";
    }

    public BigDecimal extractYearsOfExperience(String text) {
        Matcher m = Pattern.compile("(\\d+)\\+?\\s+years?\\s+(of\\s+)?(experience|exp)",
            Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return new BigDecimal(m.group(1));

        Matcher rm = Pattern.compile("(20\\d{2}|19\\d{2})\\s*[-\\u2013]\\s*(20\\d{2}|present|current)",
            Pattern.CASE_INSENSITIVE).matcher(text);
        int total = 0, cy = java.time.LocalDate.now().getYear();
        while (rm.find()) {
            try {
                int s = Integer.parseInt(rm.group(1));
                String es = rm.group(2).toLowerCase();
                int e = containsAny(es,"present","current") ? cy : Integer.parseInt(rm.group(2));
                if (e > s && e - s < 50) total += (e - s) * 12;
            } catch (NumberFormatException ignored) {}
        }
        if (total > 0) return BigDecimal.valueOf(total / 12.0).setScale(1, RoundingMode.HALF_UP);
        return BigDecimal.ONE;
    }

    public String detectEducationLevel(String text) {
        String lower = text.toLowerCase();
        if (containsAny(lower,"phd","ph.d","doctorate"))                          return "PhD";
        if (containsAny(lower,"master","msc","m.sc","mba","m.s","m.tech"))        return "Master's";
        if (containsAny(lower,"bachelor","b.sc","b.s","b.e","b.tech","b.a"))      return "Bachelor's";
        if (containsAny(lower,"associate","diploma","hnd"))                        return "Associate's / Diploma";
        return "Not Specified";
    }

    public String detectIndustry(String text) {
        String lower = text.toLowerCase();
        if (containsAny(lower,"machine learning","deep learning","data science","pandas","numpy","tensorflow","pytorch")) return "Data Science";
        if (containsAny(lower,"kubernetes","devops","ci/cd","terraform")) return "DevOps / Cloud";
        if (containsAny(lower,"cybersecurity","penetration","firewall","siem")) return "Cybersecurity";
        if (containsAny(lower,"react","angular","vue","frontend","html","css")) return "Frontend Engineering";
        if (containsAny(lower,"spring","microservices","rest api","backend")) return "Backend Engineering";
        if (containsAny(lower,"android","ios","flutter","mobile")) return "Mobile Development";
        return "Software Engineering";
    }

    private Map<String, Object> improvement(String cat, String pri, String sug, double impact) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("category",cat); m.put("priority",pri); m.put("suggestion",sug); m.put("impactScore",impact);
        return m;
    }

    private boolean containsAny(String text, String... terms) {
        for (String t : terms) if (text.contains(t)) return true;
        return false;
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}