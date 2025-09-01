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
package com.fortify.cli.common.action.runner;

import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.fortify.cli.common.action.model.TemplateExpressionWithFormatter;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeDeepCopyWalker;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.RequiredArgsConstructor;

/**
 *
 * @author Ruud Senden
 */
public final class ActionRunnerHelper {
    public static final JsonNode fmt(ActionRunnerContext ctx, String formatterName, JsonNode input) {
        var format = ctx.getConfig().getAction().getFormatters().get(formatterName);
        return new JsonNodeSpelEvaluatorWalker(ctx, input).walk(format);
    }
     
    public static final String getFormatterName(ActionRunnerVars vars, TemplateExpressionWithFormatter templateExpressionWithFormatter) {
        var formatterExpression = templateExpressionWithFormatter==null ? null : templateExpressionWithFormatter.getFmt();
        return formatterExpression==null ? null : vars.eval(formatterExpression, String.class);
    }
     
    public static final Object getValueAsObject(ActionRunnerVars vars, TemplateExpressionWithFormatter templateExpressionWithFormatter) {
        var valueExpression = templateExpressionWithFormatter==null ? null : templateExpressionWithFormatter.getValue();
        return valueExpression==null ? null : vars.eval(valueExpression, Object.class);
    }
     
    public static final JsonNode getValueAsJsonNode(ActionRunnerVars vars, TemplateExpressionWithFormatter templateExpressionWithFormatter) {
        var rawValue = getValueAsObject(vars, templateExpressionWithFormatter);
        if ( rawValue==null ) { return null; }
        if ( rawValue instanceof JsonNode ) { return (JsonNode)rawValue; }
        return JsonHelper.getObjectMapper().valueToTree(rawValue);
    }
    
    public static final Object formatValueAsObject(ActionRunnerContext ctx, ActionRunnerVars vars, TemplateExpressionWithFormatter templateExpressionWithFormatter) {
        return formatValue(ctx, vars, templateExpressionWithFormatter, ActionRunnerHelper::getValueAsObject);
    }
    
    public static final JsonNode formatValueAsJsonNode(ActionRunnerContext ctx, ActionRunnerVars vars, TemplateExpressionWithFormatter templateExpressionWithFormatter) {
        return formatValue(ctx, vars, templateExpressionWithFormatter, ActionRunnerHelper::getValueAsJsonNode);
    }
    
    @SuppressWarnings("unchecked") // BiFunction parameter must return either Object or JsonNode
    public static final <T> T formatValue(ActionRunnerContext ctx, ActionRunnerVars vars, TemplateExpressionWithFormatter templateExpressionWithFormatter, BiFunction<ActionRunnerVars, TemplateExpressionWithFormatter, T> nonFormattedValueProvider) {
        var formatterName = getFormatterName(vars, templateExpressionWithFormatter);
        if ( StringUtils.isBlank(formatterName) ) {
            return nonFormattedValueProvider.apply(vars, templateExpressionWithFormatter);
        } else {
            var jsonValue = getValueAsJsonNode(vars, templateExpressionWithFormatter);
            return (T)ActionRunnerHelper.fmt(ctx, formatterName, jsonValue!=null ? jsonValue : vars.getValues());
        }
    }
    
    @RequiredArgsConstructor
    private static final class JsonNodeSpelEvaluatorWalker extends JsonNodeDeepCopyWalker {
        private final ActionRunnerContext ctx;
        private final JsonNode input;
        @Override
        protected JsonNode copyValue(JsonNode state, String path, JsonNode parent, ValueNode node) {
            if ( node instanceof POJONode ) {
                var pojoValue = ((POJONode)node).getPojo();
                if ( pojoValue instanceof TemplateExpression ) {
                    var rawResult = ctx.getSpelEvaluator().evaluate((TemplateExpression)pojoValue, input, Object.class);
                    if ( rawResult instanceof CharSequence ) {
                        rawResult = new TextNode(((String)rawResult).replace("\\n", "\n"));
                    }
                    return JsonHelper.getObjectMapper().valueToTree(rawResult);
                }
            }
            return super.copyValue(state, path, parent, node);
        }
    }

}
