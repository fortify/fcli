package com.fortify.cli.aviator.fpr.filter.engine;


import com.fortify.cli.aviator.fpr.filter.AttributeMapper;
import com.fortify.cli.aviator.fpr.filter.SearchQuery;
import com.fortify.cli.aviator.fpr.filter.SearchTree;
import com.fortify.cli.aviator.fpr.filter.comparer.BooleanComparer;
import com.fortify.cli.aviator.fpr.filter.comparer.ContainsSearchComparer;
import com.fortify.cli.aviator.fpr.filter.comparer.ExactMatchComparer;
import com.fortify.cli.aviator.fpr.filter.comparer.IsNotSearchComparer;
import com.fortify.cli.aviator.fpr.filter.comparer.NumberRangeComparer;
import com.fortify.cli.aviator.fpr.filter.comparer.RegexComparer;
import com.fortify.cli.aviator.fpr.filter.comparer.SearchComparer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FilterParser {

    private static final String ALL_MODIFIER = "all_search_fields";
    private static final Pattern LEGACY_MODIFIER_FINDER = Pattern.compile("([^\\s\\\\:]+|\\[[^\\]]+?\\]):");

    /**
     * Main entry point for parsing modern filter queries.
     */
    public static SearchTree parse(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new SearchTree(null);
        }
        List<Object> tokens = tokenizeAndBuildModern(query);
        SearchTree tree = buildTreeFromTokens(tokens);
        return tree;
    }

    private static List<Object> tokenizeAndBuildModern(String query) {
        List<String> rawTokens = tokenizeModern(query);
        List<Object> logicalTokens = new ArrayList<>();
        Object lastLogicalToken = null;

        for (int i = 0; i < rawTokens.size(); i++) {
            String current = rawTokens.get(i);

            if ("AND".equalsIgnoreCase(current) || "OR".equalsIgnoreCase(current)) {
                logicalTokens.add("AND".equalsIgnoreCase(current) ? SearchTree.LogicalOperator.AND : SearchTree.LogicalOperator.OR);
                lastLogicalToken = null;
                continue;
            }

            if (lastLogicalToken instanceof SearchQuery) {
                logicalTokens.add(SearchTree.LogicalOperator.AND);
            }

            StringBuilder fullTermBuilder = new StringBuilder(current);
            // Greedily consume subsequent tokens if they don't look like an operator or a new modifier
            while (i + 1 < rawTokens.size() && !isOperatorOrNewModifier(rawTokens.get(i + 1))) {
                fullTermBuilder.append(" ").append(rawTokens.get(i + 1));
                i++;
            }

            SearchQuery term = parseTerm(fullTermBuilder.toString());
            logicalTokens.add(term);
            lastLogicalToken = term;
        }
        return logicalTokens;
    }

    private static List<String> tokenizeModern(String query) {
        List<String> tokens = new ArrayList<>();
        // Improved tokenizer: Handles escaped quotes inner
        Pattern tokenizerPattern = Pattern.compile("\"(?:\\\\.|[^\"])*\"|\\bAND\\b|\\bOR\\b|\\S+");
        Matcher matcher = tokenizerPattern.matcher(query);
        while (matcher.find()) {
            String token = matcher.group();
            // Unescape inner for quotes
            if (token.startsWith("\"") && token.endsWith("\"")) {
                token = token.substring(1, token.length() - 1).replace("\\\"", "\"").replace("\\:", ":"); // like library
            }
            tokens.add(token);
        }
        return tokens;
    }

    private static boolean isOperatorOrNewModifier(String token) {
        if ("AND".equalsIgnoreCase(token) || "OR".equalsIgnoreCase(token)) {
            return true;
        }
        return token.matches("!?\\s*(\\[[^\\]]+\\]|\\w+):.*");
    }

    /**
     * Parses a query string using legacy syntax (implicit AND/OR).
     */
    public static SearchTree parseLegacy(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new SearchTree(null);
        }

        Map<String, List<String>> termsByModifier = new LinkedHashMap<>();
        Matcher matcher = LEGACY_MODIFIER_FINDER.matcher(query);

        List<Integer> modifierStarts = new ArrayList<>();
        List<String> modifierNames = new ArrayList<>();
        List<Integer> modifierEnds = new ArrayList<>();

        while (matcher.find()) {
            modifierStarts.add(matcher.start());
            modifierNames.add(cleanseModifier(matcher.group()));
            modifierEnds.add(matcher.end());
        }

        if (modifierStarts.isEmpty()) {
            if (!query.trim().isEmpty()) {
                termsByModifier.computeIfAbsent(ALL_MODIFIER, k -> new ArrayList<>()).add(query.trim());
            }
        } else {
            if (modifierStarts.get(0) > 0) {
                String term = query.substring(0, modifierStarts.get(0)).trim();
                if (!term.isEmpty()) {
                    termsByModifier.computeIfAbsent(ALL_MODIFIER, k -> new ArrayList<>()).add(term);
                }
            }
            for (int i = 0; i < modifierNames.size(); i++) {
                int valueStart = modifierEnds.get(i);
                int valueEnd = (i + 1 < modifierNames.size()) ? modifierStarts.get(i + 1) : query.length();
                String value = query.substring(valueStart, valueEnd).trim();
                if (!value.isEmpty()) {
                    termsByModifier.computeIfAbsent(modifierNames.get(i), k -> new ArrayList<>()).add(value);
                }
            }
        }

        SearchTree.Node rootNode = null;
        for (Map.Entry<String, List<String>> entry : termsByModifier.entrySet()) {
            String modifier = entry.getKey();
            List<String> values = entry.getValue();
            SearchQuery currentQuery = buildLegacyQuery(modifier, values);
            if (rootNode == null) {
                rootNode = new SearchTree.Node(currentQuery);
            } else {
                rootNode = new SearchTree.Node(SearchTree.LogicalOperator.AND, new SearchTree.Node(currentQuery), rootNode);
            }
        }
        return new SearchTree(rootNode);
    }

    private static SearchQuery buildLegacyQuery(String modifier, List<String> values) {
        String attributeName = AttributeMapper.getAttributeName(modifier.toLowerCase());
        if (attributeName == null) {
            attributeName = modifier;
        }
        if (values.size() == 1) {
            return new SearchQuery(attributeName, createComparer(values.get(0)));
        }
        BooleanComparer combinedComparer = new BooleanComparer();
        for (String value : values) {
            combinedComparer.addComparer(createComparer(value));
        }
        if (isSpecialModifier(modifier)) {
            attributeName = "confidence";
        }
        return new SearchQuery(attributeName, combinedComparer);
    }

    private static String cleanseModifier(String modifierWithColon) {
        String cleaned = modifierWithColon.substring(0, modifierWithColon.length() - 1).trim();
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            return cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned;
    }

    private static SearchQuery parseTerm(String term) {
        int colonIndex = -1;
        int searchIndex = 0;
        while ((colonIndex = term.indexOf(':', searchIndex)) != -1) {
            if (colonIndex > 0 && term.charAt(colonIndex - 1) != '\\') {
                break;
            }
            searchIndex = colonIndex + 1;
        }

        if (colonIndex == -1) {
            return new SearchQuery(ALL_MODIFIER, createComparer(term));
        }

        String modifierPart = term.substring(0, colonIndex).trim();
        String valuePart = term.substring(colonIndex + 1).trim();

        String cleanedModifier = cleanseModernModifier(modifierPart);
        String attributeName = AttributeMapper.getAttributeName(cleanedModifier.toLowerCase());
        if (attributeName == null) {
            attributeName = cleanedModifier;
        }

        SearchComparer comparer = createComparer(valuePart);
        if (isSpecialModifier(cleanedModifier)) {
            comparer = createSpecialComparer(cleanedModifier, valuePart);
            attributeName = "confidence"; // like library
        }
        return new SearchQuery(attributeName, comparer);
    }

    private static String cleanseModernModifier(String modifierPart) {
        if (modifierPart.startsWith("!")) {
            modifierPart = modifierPart.substring(1).trim();
        }
        if (modifierPart.startsWith("[") && modifierPart.endsWith("]")) {
            return modifierPart.substring(1, modifierPart.length() - 1);
        }
        return modifierPart;
    }

    private static SearchComparer createComparer(String value) {
        String trimmedValue = value.trim();
        boolean isNegated = trimmedValue.startsWith("!");
        if (isNegated) {
            trimmedValue = trimmedValue.substring(1).trim();
        }

        SearchComparer baseComparer;

        if (trimmedValue.startsWith("\"") && trimmedValue.endsWith("\"")) {
            String innerValue = trimmedValue.substring(1, trimmedValue.length() - 1).replace("\\\"", "\"").replace("\\:", ":"); // unescape like library
            baseComparer = new ExactMatchComparer(innerValue);
        } else if ((trimmedValue.startsWith("[") || trimmedValue.startsWith("(")) && (trimmedValue.endsWith("]") || trimmedValue.endsWith(")"))) {
            if (trimmedValue.matches("[\\[(].*[,\\-].*[\\])]")) {
                baseComparer = new NumberRangeComparer(trimmedValue);
            } else {
                baseComparer = new ContainsSearchComparer(trimmedValue);
            }
        } else if (trimmedValue.startsWith("/") && trimmedValue.endsWith("/")) {
            String regex = trimmedValue.substring(1, trimmedValue.length() - 1);
            baseComparer = new RegexComparer(regex);
        } else if ("<none>".equalsIgnoreCase(trimmedValue)) {
            baseComparer = new ExactMatchComparer(trimmedValue);
        } else {
            String unescapedValue = trimmedValue.replace("\\:", ":"); // like library
            baseComparer = new ContainsSearchComparer(unescapedValue);
        }

        return isNegated ? new IsNotSearchComparer(baseComparer) : baseComparer;
    }

    private static SearchTree buildTreeFromTokens(List<Object> tokens) {
        if (tokens.isEmpty()) return new SearchTree(null);
        Stack<SearchTree.Node> values = new Stack<>();
        Stack<SearchTree.LogicalOperator> ops = new Stack<>();
        for (Object token : tokens) {
            if (token instanceof SearchQuery) {
                values.push(new SearchTree.Node((SearchQuery) token));
            } else if (token instanceof SearchTree.LogicalOperator) {
                SearchTree.LogicalOperator currentOp = (SearchTree.LogicalOperator) token;
                while (!ops.isEmpty() && hasPrecedence(ops.peek(), currentOp)) {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                }
                ops.push(currentOp);
            }
        }
        while (!ops.isEmpty()) {
            values.push(applyOp(ops.pop(), values.pop(), values.pop()));
        }
        return new SearchTree(values.pop());
    }

    private static boolean hasPrecedence(SearchTree.LogicalOperator op1, SearchTree.LogicalOperator op2) {
        return op1 == SearchTree.LogicalOperator.AND && op2 == SearchTree.LogicalOperator.OR;
    }

    private static SearchTree.Node applyOp(SearchTree.LogicalOperator op, SearchTree.Node b, SearchTree.Node a) {
        return new SearchTree.Node(op, a, b);
    }

    // NEW: Specials like library (e.g., maxconf as range on confidence)
    private static boolean isSpecialModifier(String modifier) {
        return "maxconf".equalsIgnoreCase(modifier) || "minconf".equalsIgnoreCase(modifier); // add more as needed
    }

    private static SearchComparer createSpecialComparer(String modifier, String value) {
        if ("maxconf".equalsIgnoreCase(modifier)) {
            return new NumberRangeComparer("[0," + value + "]");
        } else if ("minconf".equalsIgnoreCase(modifier)) {
            return new NumberRangeComparer("[" + value + ",5]");
        }
        return null;
    }
}