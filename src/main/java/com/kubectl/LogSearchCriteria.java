package com.kubectl;

import java.util.regex.Pattern;

public class LogSearchCriteria {
    private final String text;
    private final String operation;
    private final boolean caseSensitive;
    private final boolean useRegex;
    private Pattern pattern;

    public LogSearchCriteria(String text, String operation, boolean caseSensitive, boolean useRegex) {
        this.text = text;
        this.operation = operation;
        this.caseSensitive = caseSensitive;
        this.useRegex = useRegex;

        if (useRegex) {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            pattern = Pattern.compile(text, flags);
        }
    }

    public boolean matches(String line) {
        if (useRegex) {
            return pattern.matcher(line).find();
        } else if (caseSensitive) {
            return line.contains(text);
        } else {
            return line.toLowerCase().contains(text.toLowerCase());
        }
    }

    public String getOperation() {
        return operation;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(text).append("\"");
        if (useRegex) sb.append(" (regex)");
        if (caseSensitive) sb.append(" (case sensitive)");
        sb.append(" [").append(operation).append("]");
        return sb.toString();
    }
}
