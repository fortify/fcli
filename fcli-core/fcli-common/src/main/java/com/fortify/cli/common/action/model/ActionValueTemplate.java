/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.common.action.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.expression.ParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper.AbstractJsonNodeWalker;
import com.fortify.cli.common.spring.expression.SpelHelper;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes an output, which can be either a top-level output
 * or partial output.
 */
@Reflectable @NoArgsConstructor
@Data
public final class ActionValueTemplate implements IActionElement {
    /** Required name for this output */
    private String name;
    /** Output contents in JSON format, where each text node is assumed to be a template expression */
    private JsonNode contents;
    /** Cached mapping from text node property path to corresponding TemplateExpression instance */  
    private final Map<String, TemplateExpression> valueExpressions = new LinkedHashMap<>();
    
    /**
     * This method checks whether required name and contents are not blank or null, then
     * walks the given contents to parse each text node as a {@link TemplateExpression},
     * caching the resulting {@link TemplateExpression} instance in the {@link #valueExpressions}
     * map, throwing an exception if the text node cannot be parsed as a {@link TemplateExpression}.
     */
    public final void postLoad(Action action) {
        Action.checkNotBlank("(partial) output name", name, this);
        Action.checkNotNull("(partial) output contents", contents, this);
        new ContentsWalker().walk(contents);
    }
    
    private final class ContentsWalker extends AbstractJsonNodeWalker<Void, Void> {
        @Override
        protected Void getResult() { return null; }
        @Override
        protected void walkValue(Void state, String path, JsonNode parent, ValueNode node) {
            if ( node instanceof TextNode ) {
                var expr = node.asText();
                try {
                    valueExpressions.put(path, SpelHelper.parseTemplateExpression(expr));
                } catch (ParseException e) {
                    throw new ActionValidationException(String.format("Error parsing template expression '%s'", expr), ActionValueTemplate.this, e);
                }
            }
            super.walkValue(state, path, parent, node);
        }
    }
}