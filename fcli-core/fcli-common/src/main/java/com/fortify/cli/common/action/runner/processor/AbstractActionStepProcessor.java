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
package com.fortify.cli.common.action.runner.processor;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.AbstractActionStepElementForEachRecord;
import com.fortify.cli.common.action.model.ActionStep;
import com.fortify.cli.common.action.model.IActionStepElement;
import com.fortify.cli.common.action.model.IMapKeyAware;
import com.fortify.cli.common.action.model.MessageWithCause;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
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
    
    protected boolean processForEachStepNode(AbstractActionStepElementForEachRecord forEachRecord, JsonNode node) {
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
        if (o instanceof IActionStepElement ) {
            var _if = ((IActionStepElement) o).get_if();
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
    
    /**
     * Wraps element execution with generic error handling. Sets lastException* variables,
     * executes on.fail/on.success steps, and allows processors to customize via hooks.
     * 
     * @param element The action element being executed
     * @param coreLogic The actual execution logic to run
     */
    protected final void processWithErrorHandling(IActionStepElement element, Runnable coreLogic) {
        String elementName = getElementName(element);
        try {
            coreLogic.run();
            setSuccessVars(element, elementName);
            if (element.getOnSuccess() != null) {
                processSteps(element.getOnSuccess());
            }
        } catch (Exception e) {
            setGenericExceptionVars(e, elementName, getVars()::set);
            setFailureVars(element, elementName, e);
            if (element.getOnFail() != null) {
                processSteps(element.getOnFail());
            } else {
                throw e;
            }
        }
    }
    
    /**
     * Sets generic exception variables available in on.fail blocks.
     * Serializes exception to an ObjectNode with properties: type, message, httpStatus (if applicable), and pojo.
     * Always sets lastException, and also ${name}_exception if element has a name.
     * 
     * @param exception The exception to expose as variables
     * @param elementName The element name/key (may be null)
     */
    protected void setGenericExceptionVars(Throwable exception, String elementName, BiConsumer<String, JsonNode> varSetter) {
        var exceptionNode = buildExceptionNode(exception);
        
        // Always set lastException
        varSetter.accept("lastException", exceptionNode);
        
        // For named elements, also set ${name}_exception
        if (elementName != null) {
            varSetter.accept(elementName + "_exception", exceptionNode);
        }
    }

    private ObjectNode buildExceptionNode(Throwable exception) {
        var exceptionNode = JsonHelper.getObjectMapper().createObjectNode();
        
        // Set common exception properties
        exceptionNode.put("type", exception.getClass().getSimpleName());
        exceptionNode.put("message", exception.getMessage());
        
        // Add httpStatus if this is an HTTP exception
        if (exception instanceof UnexpectedHttpResponseException) {
            var httpException = (UnexpectedHttpResponseException) exception;
            exceptionNode.put("httpStatus", httpException.getStatus());
        }
        
        // Store original exception as POJONode for advanced use cases (e.g., calling methods)
        exceptionNode.set("pojo", new POJONode(exception));
        return exceptionNode;
    }
    
    /**
     * Extracts the element name/key for variable naming.
     * 
     * @param element The action element
     * @return The element name/key, or null if not available
     */
    protected String getElementName(Object element) {
        if (element instanceof IMapKeyAware<?>) {
            Object key = ((IMapKeyAware<?>) element).getKey();
            return key != null ? key.toString() : null;
        }
        return null;
    }
    
    /**
     * Sets processor-specific success variables. Override in subclasses to add custom variables.
     * 
     * @param element The action element that succeeded
     * @param elementName The element name/key (may be null)
     */
    protected void setSuccessVars(IActionStepElement element, String elementName) {
        // Default: no additional variables. Subclasses override to add custom variables.
    }
    
    /**
     * Sets processor-specific failure variables. Override in subclasses to add custom variables.
     * 
     * @param element The action element that failed
     * @param elementName The element name/key (may be null)
     * @param exception The exception that was thrown
     */
    protected void setFailureVars(IActionStepElement element, String elementName, Throwable exception) {
        // Default: no additional variables. Subclasses override to add custom variables.
    }
    
    /**
     * Evaluates a MessageWithCause and returns the evaluated message and optional cause.
     * 
     * @param msgWithCause The MessageWithCause to evaluate
     * @param vars The action variables for expression evaluation
     * @return EvaluatedMessageWithCause record containing message and cause (may be null)
     */
    protected static final EvaluatedMessageWithCause evaluateMessageWithCause(
            MessageWithCause msgWithCause, 
            ActionRunnerVars vars) {
        String message = msgWithCause.getMsg() != null 
            ? asString(vars.eval(msgWithCause.getMsg(), Object.class))
            : null;
        
        // Treat blank strings as null for consistency
        if (StringUtils.isBlank(message)) {
            message = null;
        }
        
        Throwable cause = null;
        if (msgWithCause.getCause() != null) {
            Object causeValue = vars.eval(msgWithCause.getCause(), Object.class);
            cause = extractThrowableFromCause(causeValue);
        }
        
        return new EvaluatedMessageWithCause(message, cause);
    }
    
    /**
     * Extracts a Throwable from a cause value that may be wrapped in POJONode or ObjectNode.
     * This method is private and only called by evaluateMessageWithCause.
     * 
     * @param causeValue The evaluated cause expression value
     * @return The extracted Throwable, or null if not found or not a Throwable
     */
    private static Throwable extractThrowableFromCause(Object causeValue) {
        if (causeValue == null) {
            return null;
        }
        
        // If ObjectNode with 'pojo' property, extract that property
        if (causeValue instanceof ObjectNode) {
            JsonNode pojoNode = ((ObjectNode) causeValue).get("pojo");
            if (pojoNode != null) {
                causeValue = pojoNode;
            }
        }
        
        // If POJONode, extract the wrapped object
        if (causeValue instanceof POJONode) {
            causeValue = ((POJONode) causeValue).getPojo();
        }
        
        // Return causeValue if it's a Throwable, otherwise null
        return causeValue instanceof Throwable ? (Throwable) causeValue : null;
    }
    
    public abstract ActionRunnerContext getCtx();
    public abstract ActionRunnerVars getVars();
}
