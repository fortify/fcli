package com.fortify.cli.aviator.fpr.processor;

import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.jaxb.Description;
import com.fortify.cli.aviator.fpr.model.ReplacementData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processor for FVDL Descriptions section. Caches descriptions by classID and processes
 * description text with replacements and conditionals, using a robust object-oriented parser.
 */
public class DescriptionProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DescriptionProcessor.class);
    private final Map<String, Description> descriptionCache = new ConcurrentHashMap<>();

    /**
     * Processes Descriptions section and caches by classID.
     *
     * @param descriptions List of JAXB Description objects
     */
    public void process(List<Description> descriptions) {
        if (descriptions == null) {
            logger.debug("No Descriptions provided");
            return;
        }
        for (Description desc : descriptions) {
            if (desc.getClassID() != null) {
                descriptionCache.put(desc.getClassID(), desc);
            } else {
                logger.warn("Description missing classID, skipping");
            }
        }
    }

    /**
     * Processes description for a vulnerability, applying replacements and conditionals.
     *
     * @param vuln            Vulnerability object
     * @param classId         Class ID for description lookup
     * @param replacementData Replacement data from AnalysisInfo
     * @return Array of [shortDescription, explanation]
     */
    public String[] processForVuln(Vulnerability vuln, String classId, ReplacementData replacementData) {
        Description desc = descriptionCache.get(classId);
        if (desc == null) {
            logger.debug("No description found for classID: {}", classId);
            return new String[]{"", ""};
        }

        String abstractText = desc.getAbstract() != null ? desc.getAbstract() : "";
        String explanationText = desc.getExplanation() != null ? desc.getExplanation() : "";

        // Use the new parser to process the text
        String shortDesc = FvdlParser.parseAndRender(abstractText, vuln, replacementData);
        String explanation = FvdlParser.parseAndRender(explanationText, vuln, replacementData);

        return new String[]{shortDesc, explanation};
    }

    /**
     * A custom exception to signal that a replacement was required but not found.
     * This is used by ParagraphElement to fall back to AltParagraph.
     */
    private static class MissingReplacementException extends RuntimeException {
    }

    /**
     * Represents any element in the description that can be turned into a string.
     */
    private interface Stringable {
        /**
         * Renders the element into a string, resolving any variables or conditions.
         *
         * @param vuln            The vulnerability context for conditional checks.
         * @param replacementData The data for key-based replacements.
         * @param throwOnFail     If true, throw MissingReplacementException if a key is not found.
         * @return The rendered string content.
         */
        String render(Vulnerability vuln, ReplacementData replacementData, boolean throwOnFail);
    }

    /**
     * A simple element that just holds plain text.
     */
    private static class TextElement implements Stringable {
        private final String text;
        TextElement(String text) { this.text = text; }
        @Override
        public String render(Vulnerability vuln, ReplacementData replacementData, boolean throwOnFail) {
            return text;
        }
    }

    /**
     * A container element that holds a list of other Stringable children.
     * This is the base for all structured tags.
     */
    private static abstract class FancyElement implements Stringable {
        protected final List<Stringable> children = new ArrayList<>();

        @Override
        public String render(Vulnerability vuln, ReplacementData replacementData, boolean throwOnFail) {
            StringBuilder sb = new StringBuilder();
            for (Stringable child : children) {
                sb.append(child.render(vuln, replacementData, throwOnFail));
            }
            return sb.toString();
        }
    }

    /**
     * Handles <Replace key="..." default="..." link="..."/>
     */
    private static class ReplaceElement implements Stringable {
        private final String key;
        private final String defaultValue;
        private final String link;

        ReplaceElement(String attributes) {
            this.key = FvdlParser.parseAttribute("key", attributes);
            this.defaultValue = FvdlParser.parseAttribute("default", attributes);
            this.link = FvdlParser.parseAttribute("link", attributes);
        }

        @Override
        public String render(Vulnerability vuln, ReplacementData replacementData, boolean throwOnFail) {
            if (replacementData != null && key != null) {
                // 1. Check for a rich <Def> replacement first. This is the most common case.
                ReplacementData.Replacement repl = replacementData.getReplacements().get(key);
                if (repl != null) {
                    // Use the replacement's value if it exists, otherwise fall back to the tag's default value.
                    String value = repl.getValue() != null ? repl.getValue() : (defaultValue != null ? defaultValue : "");

                    // If a link is requested in the tag (link="...") AND the replacement has location info, create a descriptive string.
                    // You can change this format to HTML <a> tags if your final output is HTML.
                    if (link != null && repl.hasLocation()) {
                        return String.format("%s (in %s at line %s)", value, repl.getPath(), repl.getLine());
                    }
                    return value;
                }

                // 2. If no <Def> was found, check for a pure <LocationDef> replacement.
                Map<String, String> locRepl = replacementData.getLocationReplacements().get(key);
                if (locRepl != null) {
                    String path = locRepl.getOrDefault("path", "");
                    String line = locRepl.getOrDefault("line", "0");

                    // For a location-only def, the display value is often the path, or a default value from the tag.
                    String displayValue = defaultValue != null ? defaultValue : path;

                    if (link != null) {
                        return String.format("%s (at line %s)", displayValue, line);
                    }
                    return displayValue;
                }
            }

            // 3. Fallback to the default value specified directly in the <Replace default="..."> tag.
            if (defaultValue != null) {
                return defaultValue;
            }

            // 4. If nothing is found, either fail (for <Paragraph> which needs all its data) or return an empty string.
            if (throwOnFail) {
                throw new MissingReplacementException();
            }
            return "";
        }
    }

    /**
     * Handles <Paragraph>...<AltParagraph>...</AltParagraph></Paragraph>
     */
    private static class ParagraphElement extends FancyElement {
        private final List<Stringable> altChildren = new ArrayList<>();

        ParagraphElement(String content) {
            String altParagraphTag = "<AltParagraph>";
            int altStart = content.indexOf(altParagraphTag);
            if (altStart != -1) {
                String mainContent = content.substring(0, altStart);
                this.children.addAll(FvdlParser.parse(mainContent));

                int altEnd = content.indexOf("</AltParagraph>", altStart);
                if (altEnd != -1) {
                    String altContent = content.substring(altStart + altParagraphTag.length(), altEnd);
                    this.altChildren.addAll(FvdlParser.parse(altContent));
                }
            } else {
                this.children.addAll(FvdlParser.parse(content));
            }
        }

        @Override
        public String render(Vulnerability vuln, ReplacementData replacementData, boolean throwOnFail) {
            try {
                // Try to render the main content, forcing it to fail if a replacement is missing.
                return super.render(vuln, replacementData, true);
            } catch (MissingReplacementException e) {
                // If it fails, render the alternative content.
                StringBuilder sb = new StringBuilder();
                for (Stringable child : altChildren) {
                    // Don't throw on fail for alt content, just render what's possible.
                    sb.append(child.render(vuln, replacementData, false));
                }
                return sb.toString();
            }
        }
    }

    /**
     * Handles <IfDef var="...">...</IfDef>
     */
    private static class IfDefElement extends FancyElement {
        private final String var;

        IfDefElement(String attributes, String content) {
            this.var = FvdlParser.parseAttribute("var", attributes);
            this.children.addAll(FvdlParser.parse(content));
        }

        @Override
        public String render(Vulnerability vuln, ReplacementData replacementData, boolean throwOnFail) {
            if (vuln != null && var != null && (vuln.getKnowledge().containsKey(var) || (replacementData != null && replacementData.getReplacements().containsKey(var)))) {
                return super.render(vuln, replacementData, throwOnFail);
            }
            return "";
        }
    }

    /**
     * Handles <IfNotDef var="...">...</IfNotDef>
     */
    private static class IfNotDefElement extends FancyElement {
        private final String var;

        IfNotDefElement(String attributes, String content) {
            this.var = FvdlParser.parseAttribute("var", attributes);
            this.children.addAll(FvdlParser.parse(content));
        }

        @Override
        public String render(Vulnerability vuln, ReplacementData replacementData, boolean throwOnFail) {
            if (vuln != null && var != null && (vuln.getKnowledge().containsKey(var) || (replacementData != null && replacementData.getReplacements().containsKey(var)))) {
                return "";
            }
            return super.render(vuln, replacementData, throwOnFail);
        }
    }

    /**
     * Handles <ConditionalText condition="...">...</ConditionalText>
     */
    private static class ConditionalTextElement extends FancyElement {
        private final String condition;

        ConditionalTextElement(String attributes, String content) {
            this.condition = FvdlParser.parseAttribute("condition", attributes);
            this.children.addAll(FvdlParser.parse(content));
        }

        @Override
        public String render(Vulnerability vuln, ReplacementData replacementData, boolean throwOnFail) {
            if (vuln != null && condition != null && vuln.contains(condition)) {
                return super.render(vuln, replacementData, throwOnFail);
            }
            return "";
        }
    }


    /**
     * The main parser logic that converts a string with FVDL tags into a tree of Stringable objects.
     */
    private static final class FvdlParser {
        private static final Map<String, String> STRUCTURED_TAGS = new HashMap<>();
        static {
            STRUCTURED_TAGS.put("Paragraph>", "</Paragraph>");
            STRUCTURED_TAGS.put("IfDef ", "</IfDef>");
            STRUCTURED_TAGS.put("IfNotDef ", "</IfNotDef>");
            STRUCTURED_TAGS.put("ConditionalText ", "</ConditionalText>");
        }

        // Self-closing tags
        private static final String REPLACE_TAG = "Replace ";
        private static final String REPLACE_END_TAG = "/>";


        public static String parseAndRender(String text, Vulnerability vuln, ReplacementData data) {
            if (text == null || text.isEmpty()) {
                return "";
            }
            // The content is often wrapped in <Content>...</Content>, remove it.
            if (text.startsWith("<Content>")) {
                text = text.substring(9, text.length() - 10);
            }

            List<Stringable> elements = parse(text);
            StringBuilder result = new StringBuilder();
            for (Stringable element : elements) {
                result.append(element.render(vuln, data, false));
            }
            return result.toString().trim();
        }

        public static List<Stringable> parse(String text) {
            List<Stringable> elements = new ArrayList<>();
            int cursor = 0;

            while (cursor < text.length()) {
                int tagStart = text.indexOf('<', cursor);

                if (tagStart == -1) {
                    // No more tags, add remaining text
                    if (cursor < text.length()) {
                        elements.add(new TextElement(text.substring(cursor)));
                    }
                    break;
                }

                // Add text before the tag
                if (tagStart > cursor) {
                    elements.add(new TextElement(text.substring(cursor, tagStart)));
                }

                // Find which tag it is
                String remaining = text.substring(tagStart + 1);
                boolean tagFound = false;

                // Check for self-closing <Replace/> tag
                if (remaining.startsWith(REPLACE_TAG)) {
                    int tagContentEnd = remaining.indexOf(REPLACE_END_TAG);
                    if (tagContentEnd != -1) {
                        String attributes = remaining.substring(REPLACE_TAG.length(), tagContentEnd);
                        elements.add(new ReplaceElement(attributes));
                        cursor = tagStart + 1 + tagContentEnd + REPLACE_END_TAG.length();
                        tagFound = true;
                    }
                }

                // Check for structured tags with content
                if (!tagFound) {
                    for (Map.Entry<String, String> entry : STRUCTURED_TAGS.entrySet()) {
                        String startTag = entry.getKey();
                        String endTag = entry.getValue();

                        if (remaining.startsWith(startTag)) {
                            // Find the content inside the tag
                            int contentStart = tagStart + 1 + startTag.length();
                            int closingTagStart = text.indexOf('>', contentStart -1);

                            // To correctly handle nesting, we need a balanced search, but for simplicity,
                            // we'll find the last corresponding end tag. A proper implementation would count tags.
                            // However, a simple indexOf is often sufficient if nesting of the *same* tag is rare.
                            int contentEnd = text.indexOf(endTag, closingTagStart);

                            if (contentEnd != -1) {
                                String fullAttributes = text.substring(contentStart-1, closingTagStart);
                                String content = text.substring(closingTagStart + 1, contentEnd);

                                elements.add(createStructuredElement(startTag, fullAttributes, content));

                                cursor = contentEnd + endTag.length();
                                tagFound = true;
                                break;
                            }
                        }
                    }
                }

                if (!tagFound) {
                    // Not a recognized tag, treat '<' as literal text
                    elements.add(new TextElement("<"));
                    cursor = tagStart + 1;
                }
            }

            return elements;
        }

        private static Stringable createStructuredElement(String tag, String attributes, String content) {
            switch (tag) {
                case "Paragraph>":
                    return new ParagraphElement(content);
                case "IfDef ":
                    return new IfDefElement(attributes, content);
                case "IfNotDef ":
                    return new IfNotDefElement(attributes, content);
                case "ConditionalText ":
                    return new ConditionalTextElement(attributes, content);
                default:
                    logger.warn("Unknown structured tag type: {}", tag);
                    return new TextElement(""); // Should not happen
            }
        }

        public static String parseAttribute(String attributeName, String content) {
            String searchString = attributeName + "=\"";
            int startIndex = content.indexOf(searchString);
            if (startIndex == -1) {
                return null;
            }
            int valueStart = startIndex + searchString.length();
            int valueEnd = content.indexOf('"', valueStart);
            if (valueEnd == -1) {
                return null;
            }
            return content.substring(valueStart, valueEnd);
        }
    }
}