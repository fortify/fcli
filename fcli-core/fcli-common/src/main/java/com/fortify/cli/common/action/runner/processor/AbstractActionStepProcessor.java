/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.common.action.runner.processor;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.AbstractActionElementForEachRecord;
import com.fortify.cli.common.action.model.ActionStep;
import com.fortify.cli.common.action.model.IActionStepIfSupplier;
import com.fortify.cli.common.action.model.IMapKeyAware;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.spel.wrapper.TemplateExpressionKeySerializer;
import com.fortify.cli.common.util.StringHelper;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @Data @Reflectable
public abstract class AbstractActionStepProcessor implements IActionStepProcessor {
    protected final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private static final ObjectMapper yamlObjectMapper = createYamlObjectMapper();
    
    protected static final String asString(Object o) {
        if ( o==null ) {
            return "<null>";
        } else if ( o instanceof TextNode ) {
            return ((TextNode)o).asText();
        } else if ( o instanceof JsonNode ) {
            return ((JsonNode)o).toPrettyString();
        } else {
            return o.toString();
        }
    }
    
    // TODO This is currently mostly used for writing progress messages, to avoid exceptions if single-line
    //      progress writer is configured. Maybe we should make this optional, providing a reusable approach
    //      for only converting to single line if !progressWriter.isMultiLineSupported()?
    protected static final String asSingleLineString(Object o) {
        return asString(o).replaceAll("\\s+", " ");
    }
    
    protected final void processSteps(List<ActionStep> steps) {
        new ActionStepProcessorSteps(getCtx(), getVars(), steps).process();
    }
    
    protected boolean processForEachStepNode(AbstractActionElementForEachRecord forEachRecord, JsonNode node) {
        if ( forEachRecord==null ) { return false; }
        var breakIf = forEachRecord.getBreakIf();
        getVars().set(forEachRecord.getVarName(), node);
        if ( breakIf!=null && getVars().eval(breakIf, Boolean.class) ) {
            return false;
        }
        if ( _if(forEachRecord) ) {
            processSteps(forEachRecord.get_do());
        }
        return true;
    }
    
    protected final boolean _if(Object o) {
        if (getCtx().isExitRequested() ) {
            LOG.debug("SKIPPED due to exit requested:\n"+getEntryAsString(o));
            return false; 
        }
        if ( o==null ) { return true; }
        if (o instanceof Map.Entry<?,?>) {
            var e = (Map.Entry<?,?>)o;
            return _if(e.getKey()) && _if(e.getValue());
        }
        if (o instanceof IActionStepIfSupplier ) {
            var _if = ((IActionStepIfSupplier) o).get_if();
            if ( _if!=null ) {
                var result = getVars().eval(_if, Boolean.class);
                if ( !result ) { LOG.debug("SKIPPED due to 'if' evaluating to false:\n"+getEntryAsString(o)); }
                return result;
            }
        }
        return true;
    }
    
    protected final String getEntryAsString(Object value) {
        if ( value==null ) { return null; }
        try {
            return StringHelper.indent(String.format("%s%s:\n%s", value.getClass().getSimpleName(),
                    value instanceof IMapKeyAware<?> ? String.format(" (%s)", ((IMapKeyAware<?>) value).getKey()) : "",
                    yamlObjectMapper.writeValueAsString(value)), "    ");
        } catch ( Exception e ) {
            return StringHelper.indent(String.format("(Fallback to unformatted: %s: %s)\n%s", e.getClass().getSimpleName(), e.getMessage(),
                    value.toString()), "  ");
        }
    }
    
    private static final ObjectMapper createYamlObjectMapper() {
        return TemplateExpressionKeySerializer.registerOn(new ObjectMapper(
                new YAMLFactory()
                    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                    .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
                    .disable(YAMLGenerator.Feature.SPLIT_LINES)));
    }
    
    public abstract ActionRunnerContext getCtx();
    public abstract ActionRunnerVars getVars();
}
