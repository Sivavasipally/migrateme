package com.example.gitmigrator.controller.component;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for syntax highlighting different file types
 */
public class SyntaxHighlighter {
    
    // Dockerfile patterns
    private static final String[] DOCKERFILE_KEYWORDS = {
        "FROM", "RUN", "CMD", "LABEL", "EXPOSE", "ENV", "ADD", "COPY",
        "ENTRYPOINT", "VOLUME", "USER", "WORKDIR", "ARG", "ONBUILD",
        "STOPSIGNAL", "HEALTHCHECK", "SHELL"
    };
    
    private static final String DOCKERFILE_KEYWORD_PATTERN = "\\b(" + String.join("|", DOCKERFILE_KEYWORDS) + ")\\b";
    private static final String DOCKERFILE_COMMENT_PATTERN = "#[^\r\n]*";
    private static final String DOCKERFILE_STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    
    private static final Pattern DOCKERFILE_PATTERN = Pattern.compile(
        "(?<KEYWORD>" + DOCKERFILE_KEYWORD_PATTERN + ")" +
        "|(?<COMMENT>" + DOCKERFILE_COMMENT_PATTERN + ")" +
        "|(?<STRING>" + DOCKERFILE_STRING_PATTERN + ")"
    );
    
    // YAML patterns
    private static final String YAML_KEY_PATTERN = "^\\s*[a-zA-Z_][a-zA-Z0-9_-]*\\s*:";
    private static final String YAML_COMMENT_PATTERN = "#[^\r\n]*";
    private static final String YAML_STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
    private static final String YAML_NUMBER_PATTERN = "\\b\\d+(\\.\\d+)?\\b";
    private static final String YAML_BOOLEAN_PATTERN = "\\b(true|false|yes|no|on|off)\\b";
    
    private static final Pattern YAML_PATTERN = Pattern.compile(
        "(?<KEY>" + YAML_KEY_PATTERN + ")" +
        "|(?<COMMENT>" + YAML_COMMENT_PATTERN + ")" +
        "|(?<STRING>" + YAML_STRING_PATTERN + ")" +
        "|(?<NUMBER>" + YAML_NUMBER_PATTERN + ")" +
        "|(?<BOOLEAN>" + YAML_BOOLEAN_PATTERN + ")",
        Pattern.MULTILINE
    );
    
    // JSON patterns
    private static final String JSON_STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String JSON_NUMBER_PATTERN = "-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?";
    private static final String JSON_BOOLEAN_PATTERN = "\\b(true|false|null)\\b";
    private static final String JSON_BRACE_PATTERN = "[{}\\[\\]]";
    
    private static final Pattern JSON_PATTERN = Pattern.compile(
        "(?<STRING>" + JSON_STRING_PATTERN + ")" +
        "|(?<NUMBER>" + JSON_NUMBER_PATTERN + ")" +
        "|(?<BOOLEAN>" + JSON_BOOLEAN_PATTERN + ")" +
        "|(?<BRACE>" + JSON_BRACE_PATTERN + ")"
    );
    
    // XML patterns
    private static final String XML_TAG_PATTERN = "</?\\b[a-zA-Z][a-zA-Z0-9_-]*\\b[^>]*>";
    private static final String XML_ATTRIBUTE_PATTERN = "\\b[a-zA-Z][a-zA-Z0-9_-]*(?=\\s*=)";
    private static final String XML_STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
    private static final String XML_COMMENT_PATTERN = "<!--[^>]*-->";
    
    private static final Pattern XML_PATTERN = Pattern.compile(
        "(?<TAG>" + XML_TAG_PATTERN + ")" +
        "|(?<ATTRIBUTE>" + XML_ATTRIBUTE_PATTERN + ")" +
        "|(?<STRING>" + XML_STRING_PATTERN + ")" +
        "|(?<COMMENT>" + XML_COMMENT_PATTERN + ")"
    );
    
    // Shell patterns
    private static final String[] SHELL_KEYWORDS = {
        "if", "then", "else", "elif", "fi", "case", "esac", "for", "while",
        "until", "do", "done", "function", "return", "exit", "break", "continue",
        "echo", "printf", "read", "cd", "pwd", "ls", "cp", "mv", "rm", "mkdir",
        "chmod", "chown", "grep", "sed", "awk", "sort", "uniq", "head", "tail"
    };
    
    private static final String SHELL_KEYWORD_PATTERN = "\\b(" + String.join("|", SHELL_KEYWORDS) + ")\\b";
    private static final String SHELL_COMMENT_PATTERN = "#[^\r\n]*";
    private static final String SHELL_STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
    private static final String SHELL_VARIABLE_PATTERN = "\\$\\{?[a-zA-Z_][a-zA-Z0-9_]*\\}?";
    
    private static final Pattern SHELL_PATTERN = Pattern.compile(
        "(?<KEYWORD>" + SHELL_KEYWORD_PATTERN + ")" +
        "|(?<COMMENT>" + SHELL_COMMENT_PATTERN + ")" +
        "|(?<STRING>" + SHELL_STRING_PATTERN + ")" +
        "|(?<VARIABLE>" + SHELL_VARIABLE_PATTERN + ")"
    );
    
    public static StyleSpans<Collection<String>> computeDockerfileHighlighting(String text) {
        return computeHighlighting(text, DOCKERFILE_PATTERN);
    }
    
    public static StyleSpans<Collection<String>> computeYamlHighlighting(String text) {
        return computeHighlighting(text, YAML_PATTERN);
    }
    
    public static StyleSpans<Collection<String>> computeJsonHighlighting(String text) {
        return computeHighlighting(text, JSON_PATTERN);
    }
    
    public static StyleSpans<Collection<String>> computeXmlHighlighting(String text) {
        return computeHighlighting(text, XML_PATTERN);
    }
    
    public static StyleSpans<Collection<String>> computeShellHighlighting(String text) {
        return computeHighlighting(text, SHELL_PATTERN);
    }
    
    private static StyleSpans<Collection<String>> computeHighlighting(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        
        while (matcher.find()) {
            String styleClass = getStyleClass(matcher);
            
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
    
    private static String getStyleClass(Matcher matcher) {
        if (matcher.group("KEYWORD") != null) {
            return "keyword";
        } else if (matcher.group("COMMENT") != null) {
            return "comment";
        } else if (matcher.group("STRING") != null) {
            return "string";
        } else if (matcher.group("NUMBER") != null) {
            return "number";
        } else if (matcher.group("BOOLEAN") != null) {
            return "boolean";
        } else if (matcher.group("KEY") != null) {
            return "key";
        } else if (matcher.group("TAG") != null) {
            return "tag";
        } else if (matcher.group("ATTRIBUTE") != null) {
            return "attribute";
        } else if (matcher.group("VARIABLE") != null) {
            return "variable";
        } else if (matcher.group("BRACE") != null) {
            return "brace";
        }
        return "default";
    }
}