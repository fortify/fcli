/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.action.runner;

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.fcli;
import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.internal;
import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.workflow;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctions;
import com.fortify.cli.common.util.FcliBuildProperties;

import lombok.RequiredArgsConstructor;

@Reflectable @RequiredArgsConstructor
@SpelFunctionPrefix("action.")
public final class ActionRunnerContextSpelFunctions {
    private final ActionRunnerContext ctx;
    private final static String RUN_ID = UUID.randomUUID().toString();
    
    @SpelFunction(cat = workflow, desc = "This function returns the current fcli run id, which uniquely represents the current fcli invocation. Different invocations of the fcli executable are guaranteed to have a different, unique run id. Within a single fcli executable invocation, the run id remains the same, even across run.fcli instructions and any other internal fcli command invocations.", 
            returns = "Current fcli run id in UUID format")
    public final String runID() {
        return RUN_ID;
    }
    
    @SpelFunction(cat=workflow, returns="String listing non-blank command-line options copied from the given `cli.options` group")
    public final String copyParametersFromGroup(
        @SpelFunctionParam(name="group", desc="the `cli.options` group name from which to copy CLI options") String group) 
    {
        StringBuilder result = new StringBuilder();
        for (var e : ctx.getConfig().getAction().getCliOptions().entrySet()) {
            var name = e.getKey();
            var p = e.getValue();
            if (group == null || group.equals(p.getGroup())) {
                var val = ctx.getParameterValues().get(name);
                if (val != null && StringUtils.isNotBlank(val.asText())) {
                    result
                        .append("\"--")
                        .append(name)
                        .append("=")
                        .append(val.asText())
                        .append("\" ");
                }
            }
        }
        return result.toString();
    }

    @SpelFunction(cat=fcli, desc = "Formats the input value using the specified formatter as declared through the `formatters` YAML instruction",
                returns="The formatted value") 
    public final JsonNode fmt(
        @SpelFunctionParam(name="formatterName", desc="the name of the formatter to apply") String formatterName,
        @SpelFunctionParam(name="input", desc="the input to be formatted") JsonNode input)
    {
        return ActionRunnerHelper.fmt(ctx, formatterName, input);
    }
    
    @SpelFunction(cat=internal, desc="""
            Create a document renderer for processing documentation references and $eval{...} expressions.
            Accepts optional vars parameter to provide variables for $eval{...} resolution.
            This is primarily for internal fcli use in documentation generation actions.
            """, returns="DocRenderer builder instance")
    public final DocRenderer docRenderer() {
        return new DocRenderer(ctx);
    }
    
    /**
     * Record holding CI context information for document rendering.
     */
    @Reflectable
    public static record CiContext(String ciSystem, String version, ObjectNode outputs) {}
    
    /**
     * Fluent builder for rendering documentation with automatic reference processing.
     * Handles fcliCmd:command:, actionRef:product:action[#anchor], ciOutputRef:product:output,
     * and $eval{...} references, converting them to either plain text or AsciiDoc format.
     * 
     * <p>Reference syntax:</p>
     * <ul>
     *   <li><code>fcliCmd:command:</code> - References to fcli commands (e.g., "fcliCmd:fcli fod session login:")</li>
     *   <li><code>actionRef:product:action[#anchor]</code> - References to fcli actions
     *       <ul>
     *         <li>product: generic, fod, ssc, or _ (current/generic context)</li>
     *         <li>anchor: optional, auto-generated if omitted</li>
     *         <li>Examples: "actionRef:_:ci", "actionRef:fod:setup-release#_setup_release"</li>
     *       </ul>
     *   </li>
     *   <li><code>ciOutputRef:product:output</code> - References to CI integration output documents
     *       <ul>
     *         <li>product: fod, ssc, or _ (generic/product-agnostic)</li>
     *         <li>Examples: "ciOutputRef:_:setup", "ciOutputRef:fod:ciDocs"</li>
     *       </ul>
     *   </li>
     *   <li><code>$eval{expression}</code> - Evaluate SpEL expression at render time
     *       <ul>
     *         <li>Evaluated using current action context (all variables available)</li>
     *         <li>Examples: "$eval{fortifyProductName}", "$eval{doc.vars.migrationGuide}"</li>
     *       </ul>
     *   </li>
     * </ul>
     * 
     * <p>Usage examples:</p>
     * <pre>
     * // Plain text rendering
     * #action.docRenderer().text().render(description)
     * 
     * // AsciiDoc rendering with automatic action links
     * #action.docRenderer().asciidoc().currentProduct("fod").render(description)
     * 
     * // With CI context for resolving ciOutputRef
     * #action.docRenderer().asciidoc().ciContext(ciSystem, version, outputs).render(description)
     * 
     * // Custom manpage base URL
     * #action.docRenderer().asciidoc().manpageBaseUrl("/docs/cli").render(description)
     * </pre>
     */
    @Reflectable
    @SpelFunctions
    @RequiredArgsConstructor
    public static final class DocRenderer {
        private final ActionRunnerContext ctx;
        private boolean isAsciiDoc = false;
        private String manpageBaseUrl = "../manpage";
        private String currentProduct = null;
        private CiContext ciContext = null;
        private static final int MAX_EVAL_DEPTH = 10;
        
        @SpelFunction(cat=internal, desc="Configure renderer for plain text output", returns="This renderer for method chaining")
        public DocRenderer text() {
            this.isAsciiDoc = false;
            return this;
        }
        
        @SpelFunction(cat=internal, desc="Configure renderer for AsciiDoc output", returns="This renderer for method chaining")
        public DocRenderer asciidoc() {
            this.isAsciiDoc = true;
            return this;
        }
        
        @SpelFunction(cat=internal, desc="Set the base URL for manpage links (default: '../manpage')", returns="This renderer for method chaining")
        public DocRenderer manpageBaseUrl(@SpelFunctionParam(name="url", desc="Base URL for fcli command manpage links") String url) {
            this.manpageBaseUrl = url;
            return this;
        }
        
        @SpelFunction(cat=internal, desc="Set the current product context (e.g., 'fod', 'ssc') for resolving '_' in references", returns="This renderer for method chaining")
        public DocRenderer currentProduct(@SpelFunctionParam(name="product", desc="Current product identifier") String product) {
            this.currentProduct = product;
            return this;
        }
        
        @SpelFunction(cat=internal, desc="Set CI context (system, version, outputs) for resolving ciOutputRef references", returns="This renderer for method chaining")
        public DocRenderer ciContext(
                @SpelFunctionParam(name="ciSystem", desc="CI system identifier") String ciSystem,
                @SpelFunctionParam(name="version", desc="CI version") String version,
                @SpelFunctionParam(name="outputs", desc="Map of output definitions") ObjectNode outputs) {
            this.ciContext = new CiContext(ciSystem, version, outputs);
            return this;
        }
        
        @SpelFunction(cat=internal, desc="""
                Render text, processing all fcliCmd:command:, actionRef:product:action[#anchor],
                ciOutputRef:product:output, and $eval{...} references.
                - fcliCmd references become command links (AsciiDoc) or backtick-wrapped text (plain text)
                - actionRef references become action links (AsciiDoc) or backtick-wrapped action names (plain text)
                - ciOutputRef references become CI output document links (AsciiDoc) or backtick-wrapped output names (plain text)
                - $eval{...} expressions are evaluated using current action context
                Use '_' for product to refer to current/generic context.
                """, returns="Rendered text with all references processed")
        public String render(@SpelFunctionParam(name="text", desc="Text containing documentation references") String text) {
            if (text == null) return "";
            
            // Process $eval{...} expressions first so that generated content can contain other references
            text = processEvalExpressions(text, 0);
            
            // Process fcliCmd:command: references
            text = processFcliCmdReferences(text);
            
            // Process actionRef:product:action[#anchor] references
            text = processActionReferences(text);
            
            // Process ciOutputRef:product:output references
            text = processOutputReferences(text);
            
            return text;
        }
        
        @SpelFunction(cat=internal, desc="""
                Format text for use in table cells, converting newlines to appropriate line breaks.
                For AsciiDoc: converts newlines to hard line breaks (space + plus + newline).
                For plain text: keeps newlines as-is.
                This method does NOT process other references - call render() first if needed.
                """, returns="Text formatted for table cell display")
        public String tableCell(@SpelFunctionParam(name="text", desc="Text to format for table cell") String text) {
            if (text == null) return "";
            
            if (isAsciiDoc) {
                // For AsciiDoc table cells, convert newlines to hard line breaks
                // AsciiDoc requires " +\n" (space-plus-newline) for hard line breaks
                return text.replaceAll("\n", " +\n");
            } else {
                // For plain text, keep newlines as-is (table writer will handle them)
                return text;
            }
        }
        
        public String processEvalExpressions(String text, int depth) {
            if (depth >= MAX_EVAL_DEPTH) {
                throw new FcliSimpleException("Maximum evaluation depth (" + MAX_EVAL_DEPTH + ") exceeded for $eval{...} expressions. Check for circular references.");
            }
            
            // Pattern to match $eval{...} with simple brace matching
            Pattern pattern = Pattern.compile("\\$eval\\{([^}]+)\\}");
            Matcher matcher = pattern.matcher(text);
            
            if (!matcher.find()) {
                return text;  // No more expressions to evaluate
            }
            
            StringBuffer result = new StringBuffer();
            matcher.reset();
            
            while (matcher.find()) {
                String expression = matcher.group(1);
                try {
                    String evaluated = ctx.getVars().eval(expression, String.class);
                    if (evaluated == null) {
                        evaluated = "";
                    }
                    matcher.appendReplacement(result, Matcher.quoteReplacement(evaluated));
                } catch (Exception e) {
                    throw new FcliSimpleException(
                        String.format("Failed to evaluate $eval{%s}: %s", expression, e.getMessage()), 
                        e
                    );
                }
            }
            matcher.appendTail(result);
            
            // Recursively process in case evaluated content contains more $eval{...}
            return processEvalExpressions(result.toString(), depth + 1);
        }
        
        private String processFcliCmdReferences(String text) {
            Pattern pattern = Pattern.compile("fcliCmd:([^:]+):");
            Matcher matcher = pattern.matcher(text);
            StringBuffer result = new StringBuffer();
            
            while (matcher.find()) {
                String command = matcher.group(1);
                String replacement;
                
                if (isAsciiDoc) {
                    var commandDashed = command.replace(" ", "-");
                    var url = manpageBaseUrl.contains("${cmd}") 
                        ? manpageBaseUrl.replace("${cmd}", commandDashed)
                        : manpageBaseUrl + "/" + commandDashed + ".html";
                    replacement = String.format("link:%s[`%s`]", url, command);
                } else {
                    replacement = String.format("`%s`", command);
                }
                
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(result);
            return result.toString();
        }
        
        private String processActionReferences(String text) {
            // Pattern: actionRef:product:action[#anchor]
            // product: generic, fod, ssc, or _ (current/generic)
            // anchor is optional
            Pattern pattern = Pattern.compile("actionRef:(generic|fod|ssc|_)?:([\\w_-]+)(#[\\w_-]+)?");
            Matcher matcher = pattern.matcher(text);
            StringBuffer result = new StringBuffer();
            
            while (matcher.find()) {
                String product = matcher.group(1);
                String actionName = matcher.group(2);
                String anchor = matcher.group(3);
                
                // Resolve product context
                String resolvedProduct;
                if (product == null || product.equals("_")) {
                    // Use current product if set, otherwise default to 'generic'
                    resolvedProduct = (currentProduct != null) ? currentProduct : "generic";
                } else {
                    resolvedProduct = product;
                }
                
                // Auto-generate anchor if not provided
                if (anchor == null) {
                    anchor = "#_" + actionName.replace("-", "_");
                }
                
                String replacement;
                if (isAsciiDoc) {
                    // Use FcliBuildProperties.getFcliDocBaseUrl() to construct absolute URL
                    String baseUrl = FcliBuildProperties.INSTANCE.getFcliDocBaseUrl();
                    String url = baseUrl + "/" + resolvedProduct + "-actions.html" + anchor;
                    replacement = String.format("link:%s[%s]", url, actionName);
                } else {
                    replacement = String.format("`%s`", actionName);
                }
                
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(result);
            return result.toString();
        }
        
        private String processOutputReferences(String text) {
            // Pattern: ciOutputRef:output-name (product-independent) or ciOutputRef:product:output-name
            // For product-specific outputs, product can be: fod, ssc, or _ (current product)
            Pattern pattern = Pattern.compile("ciOutputRef:(?:(fod|ssc|_):)?([\\w_-]+)");
            Matcher matcher = pattern.matcher(text);
            StringBuffer result = new StringBuffer();
            
            while (matcher.find()) {
                String product = matcher.group(1);  // May be null for product-independent outputs
                String outputKey = matcher.group(2);
                
                String replacement;
                if (isAsciiDoc && ciContext != null) {
                    try {
                        var outputs = ciContext.outputs();
                        var outputDef = outputs.get(outputKey);
                        
                        if (outputDef != null && !outputDef.isMissingNode()) {
                            var filePatternNode = outputDef.get("filePattern");
                            if (filePatternNode == null || filePatternNode.isMissingNode()) {
                                replacement = String.format("`%s`", outputKey);
                                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                                continue;
                            }
                            String filePattern = filePatternNode.asText();
                            
                            // Resolve product for _ placeholder
                            String resolvedProduct = "_".equals(product) ? currentProduct : product;
                            
                            // Check if this is a product-specific output
                            var productsNode = outputDef.get("products");
                            boolean isProductSpecific = productsNode != null && !productsNode.isMissingNode() && productsNode.size() > 0;
                            
                            // Replace placeholders in file pattern
                            String fileName = filePattern
                                .replace("{ciSystem}", ciContext.ciSystem())
                                .replace("{version}", ciContext.version());
                            
                            // Only add product to path if output is product-specific and product is specified
                            if (isProductSpecific && resolvedProduct != null && !resolvedProduct.isEmpty()) {
                                fileName = fileName.replace("{product}", resolvedProduct);
                            }
                            
                            // Extract title from doc metadata for display name
                            String displayName = outputKey;
                            var docNode = outputDef.get("doc");
                            if (docNode != null && !docNode.isMissingNode()) {
                                var titleNode = docNode.get("title");
                                if (titleNode != null && !titleNode.isMissingNode()) {
                                    displayName = titleNode.asText();
                                }
                            }
                            
                            // Convert .adoc to .html and create link
                            replacement = String.format("link:%s[%s]", fileName.replace(".adoc", ".html"), displayName);
                        } else {
                            // Fallback if output not found in context
                            replacement = String.format("`%s`", outputKey);
                        }
                    } catch (Exception e) {
                        // Fallback on any error
                        replacement = String.format("`%s`", outputKey);
                    }
                } else {
                    replacement = String.format("`%s`", outputKey);
                }
                
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(result);
            return result.toString();
        }
    }

}