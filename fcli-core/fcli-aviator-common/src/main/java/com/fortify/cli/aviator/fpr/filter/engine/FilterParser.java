package com.fortify.cli.aviator.fpr.filter.engine;

import com.fortify.cli.aviator.fpr.filter.AttributeMapper;
import com.fortify.cli.aviator.fpr.filter.SearchQuery;
import com.fortify.cli.aviator.fpr.filter.SearchTree;
import com.fortify.cli.aviator.fpr.filter.comparer.ContainsSearchComparer;
import com.fortify.cli.aviator.fpr.filter.comparer.ExactMatchComparer;
import com.fortify.cli.aviator.fpr.filter.comparer.IsNotSearchComparer;
import com.fortify.cli.aviator.fpr.filter.comparer.NumberRangeComparer;
import com.fortify.cli.aviator.fpr.filter.comparer.SearchComparer;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FilterParser {

    private static final Pattern TOKEN_FINDER = Pattern.compile(
            "\\b(AND|OR)\\b|" +
                    "(!?\\s*(?:\\[[^\\]]+\\]|\\w+):(?:\"(?:\\\\\"|[^\"])*\"|[^\\s\"\\(].*?(?=\\s+(?:AND|OR|!|\\[|\\w+:)|$)|\\S+))",
            Pattern.CASE_INSENSITIVE
    );

    public static SearchTree parse(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new SearchTree(null);
        }
        List<Object> tokens = tokenizeAndBuild(query);
        return buildTreeFromTokens(tokens);
    }

    private static List<Object> tokenizeAndBuild(String query) {
        List<Object> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_FINDER.matcher(query);
        Object lastToken = null;

        while (matcher.find()) {
            String operator = matcher.group(1);
            String term = matcher.group(2);

            if (operator != null) {
                lastToken = "AND".equalsIgnoreCase(operator) ? SearchTree.LogicalOperator.AND : SearchTree.LogicalOperator.OR;
                tokens.add(lastToken);
            } else if (term != null) {
                if (lastToken instanceof SearchQuery) {
                    tokens.add(SearchTree.LogicalOperator.AND); // Implicit AND
                }
                lastToken = parseTerm(term.trim());
                tokens.add(lastToken);
            }
        }
        return tokens;
    }

    private static SearchQuery parseTerm(String term) {
        int colonIndex = term.lastIndexOf(':');
        if (colonIndex < 0) throw new IllegalArgumentException("Malformed term (missing ':'): '" + term + "'");

        String modifierPart = term.substring(0, colonIndex).trim();
        String valuePart = term.substring(colonIndex + 1).trim();

        // --- THIS IS THE FINAL, CORRECT LOGIC ---

        // 1. Check for negation on the whole term first.
        // This handles both `!modifier:value` and `modifier:!value`.
        boolean isNegation = modifierPart.startsWith("!") || valuePart.startsWith("!");

        // 2. Clean the modifier and value parts by removing the negation symbol if present.
        if (modifierPart.startsWith("!")) {
            modifierPart = modifierPart.substring(1).trim();
        }
        if (valuePart.startsWith("!")) {
            valuePart = valuePart.substring(1).trim();
        }

        // 3. Clean the modifier of brackets.
        if (modifierPart.startsWith("[") && modifierPart.endsWith("]")) {
            modifierPart = modifierPart.substring(1, modifierPart.length() - 1);
        }

        // 4. Look up the clean attribute name.
        String attributeName = AttributeMapper.getAttributeName(modifierPart.toLowerCase());
        if (attributeName == null) {
            throw new IllegalArgumentException("Unknown modifier: '" + modifierPart + "'");
        }

        // 5. Create the comparer with the CLEAN value part and the SEPARATE negation flag.
        SearchComparer comparer = createComparer(valuePart, isNegation);

        return new SearchQuery(attributeName, comparer);
    }
    private static SearchComparer createComparer(String value, boolean isNegated) {
        SearchComparer baseComparer;
        String trimmedValue = value.trim();

        boolean isRange = (trimmedValue.startsWith("[") || trimmedValue.startsWith("(")) &&
                (trimmedValue.endsWith("]") || trimmedValue.endsWith(")"));

        if (isRange) {
            baseComparer = new NumberRangeComparer(trimmedValue);
        } else if (trimmedValue.startsWith("\"") && trimmedValue.endsWith("\"")) {
            baseComparer = new ExactMatchComparer(trimmedValue.substring(1, trimmedValue.length() - 1));
        } else if ("<none>".equalsIgnoreCase(trimmedValue)) {
            baseComparer = new ExactMatchComparer(trimmedValue);
        } else {
            baseComparer = new ContainsSearchComparer(trimmedValue.replace("\\:", ":"));
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
        // AND has higher precedence than OR.
        if (op1 == SearchTree.LogicalOperator.AND && op2 == SearchTree.LogicalOperator.OR) {
            return true;
        }
        return false;
    }

    private static SearchTree.Node applyOp(SearchTree.LogicalOperator op, SearchTree.Node b, SearchTree.Node a) {
        return new SearchTree.Node(op, a, b);
    }
}