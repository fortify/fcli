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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.action.model.FcliActionValidationException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.IConfigurableSpelEvaluator;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Getter;
import lombok.ToString;

/**
 * This class manages action variables that can be stored, formatted, and retrieved during action execution.
 * @author Ruud Senden
 */
@ToString
public final class ActionRunnerVars {
    private static final Logger LOG = LoggerFactory.getLogger(ActionRunnerVars.class);
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    private static final ObjectWriter debugObjectWriter = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false).writerWithDefaultPrettyPrinter();
    private static final String GLOBAL_VAR_NAME = "global";
    private static final String CLI_OPTIONS_VAR_NAME = "cli";
    private static final String[] PROTECTED_VAR_NAMES = {GLOBAL_VAR_NAME, CLI_OPTIONS_VAR_NAME};
    private static final ObjectNode globalValues = objectMapper.createObjectNode(); 
    @Getter private final ObjectNode values;
    private final IConfigurableSpelEvaluator spelEvaluator;
    private final ActionRunnerVars parent;
    
    /**
     * Construct a new instance of this class with the given SpEL evaluator and action parameters.
     * Only a single instance per action run is supposed to be created through this constructor,
     * acting as the top-level instance. Children of this top-level instance can be created 
     * through the {@link #createChild()} method.
     */
    public ActionRunnerVars(IConfigurableSpelEvaluator spelEvaluator, ObjectNode cliOptions) {
        this.spelEvaluator = spelEvaluator;
        this.values = objectMapper.createObjectNode();
        this.values.set(GLOBAL_VAR_NAME, globalValues);
        this.values.set(CLI_OPTIONS_VAR_NAME, cliOptions);
        this.parent = null;
    }
    
    /**
     * Constructor solely used by {@link #createChild()}
     */
    private ActionRunnerVars(ActionRunnerVars parent) {
        this.spelEvaluator = parent.spelEvaluator;
        this.values = JsonHelper.shallowCopy(parent.values);
        this.parent = parent;
    }
    
    /**
     * Create a child of the current {@link ActionRunnerVars} instance
     */
    public final ActionRunnerVars createChild() {
        return new ActionRunnerVars(this);
    }
    
    /**
     * Evaluate the given SpEL expression on current variables,
     * and convert the result to the given return type.
     */
    public final <T> T eval(Expression expression, Class<T> returnType) {
        return expression==null ? null : spelEvaluator.evaluate(expression, values, returnType);
    }
    
    /**
     * Evaluate the given SpEL expression on current variables,
     * and convert the result to the given return type.
     */
    public final <T> T eval(String expression, Class<T> returnType) {
        return expression==null ? null : spelEvaluator.evaluate(expression, values, returnType);
    }
    
    public final <T> Map<String, T> eval(Map<String, TemplateExpression> expressions, Class<T> valueType) {
        Map<String, T> result = new LinkedHashMap<>();
        if ( expressions!=null ) {
            expressions.entrySet().forEach(e->result.put(e.getKey(), eval(e.getValue(), valueType)));
        }
        return result;
    }
    
    public final JsonNode get(String name) {
        return values.get(name);
    }
    
    /**
     * Similar to {@link #set(String, JsonNode)}, but accepting a plain
     * String value that will be converted to a {@link TextNode}.
     */
    public final void set(String name, String value) {
        set(name, new TextNode(value));
    }
    
    /**
     * Set a variable on both this instance and any parent instances. If
     * the variable name starts with 'global.', it will be set as a global
     * variable, otherwise as a normal variable on both this instance and
     * all parent instances.
     */
    public final void set(String name, JsonNode value) {
        BiConsumer<String, JsonNode> setter = this::_setLocalAndParents;
        Function<String, JsonNode> getter = values::get;
        if ( name.startsWith("global.") ) {
            name = name.replaceAll("^global\\.", "");
            setter = globalValues::set;
            getter = globalValues::get;
        }
        var finalName = name; // Needed for lambda below
        logDebug(()->String.format("Set %s: %s", finalName, toDebugString(value)));
        _set(finalName, value, getter, setter);
    }
    
    /**
     * Set a variable on this instance only
     */
    public final void setLocal(String name, JsonNode value) {
        logDebug(()->String.format("Set Local %s: %s", name, toDebugString(value)));
        _set(name, value, values::get, values::set);
    }
    
    /**
     * Unset a variable on both this instance and any parent instances;
     */
    public final void rm(String name) {
        Consumer<String> unsetter = this::_unset;
        if ( name.startsWith("global.") ) {
            name = name.replaceAll("^global\\.", "");
            unsetter = globalValues::remove;
        }
        rejectProtectedVarNames(name);
        var finalName = name; // Needed for lambda below
        logDebug(()->String.format("Unset %s", finalName));
        unsetter.accept(finalName);
    }
    
    /**
     * Set the given variable 
     */
    private final void _set(String name, JsonNode value, Function<String, JsonNode> getter, BiConsumer<String, JsonNode> setter) {
        String processedVarName;
        JsonNode processedValue;
        if ( name.endsWith("..") ) {
            processedVarName = name.substring(0, name.length()-2); 
            processedValue = getOrCreateArray(getter, processedVarName)
                    .add(value);
        } else {
            processedVarName = StringUtils.substringBefore(name, ".");
            var propName = StringUtils.substringAfter(name, ".");
            if ( StringUtils.isNotBlank(propName) ) {
                processedValue = getOrCreateObject(getter, processedVarName)
                        .set(propName, value);
            } else {
                processedValue = value;
            }
        }
        rejectProtectedVarNames(processedVarName);
        setter.accept(processedVarName, processedValue);
    }
    
    private final ArrayNode getOrCreateArray(Function<String, JsonNode> getter, String name) {
        var array = getOrCreate(getter, objectMapper::createArrayNode, name);
        if ( !array.isArray() ) {
            throw new FcliActionStepException("Variable "+name+" is not an array; cannot append value");
        }
        return (ArrayNode)array;
    }

    private final ObjectNode getOrCreateObject(Function<String, JsonNode> getter, String name) {
        var obj = getOrCreate(getter, objectMapper::createObjectNode, name);
        if ( !obj.isObject() ) {
            throw new FcliActionStepException("Variable "+name+" is not a set of properties; can't set a property on this variable");
        }
        return (ObjectNode)obj;
    }
    
    private final JsonNode getOrCreate(Function<String, JsonNode> getter, Supplier<JsonNode> creator, String name) {
        var v = getter.apply(name);
        if ( v==null || v.isNull() ) {
            v = creator.get();
        }
        return v;
    }

    /**
     * Set a variable on both this instance and any parent instances.
     */
    private void _setLocalAndParents(String name, JsonNode value) {
        values.set(name, value);
        if ( parent!=null ) { parent._setLocalAndParents(name, value); }
    }

    /**
     * Unset a variable on both this instance and any parent instances.
     */
    private void _unset(String name) {
        values.remove(name);
        if ( parent!=null ) { parent._unset(name); }
    }
    
    /**
     * If the given name equals 'parameters', this method throws an exception.
     * If we ever implement some action security analysis, we want to consider 
     * user-provided parameter values as 'safe' values in potentially unsafe 
     * actions. As such, actions are not allowed to update the parameters object.
     */
    private static final void rejectProtectedVarNames(String name) {
        for ( var protectedName : PROTECTED_VAR_NAMES ) {
            if ( protectedName.equals(name) ) {
                throw new FcliActionValidationException("Action steps are not allowed to modify the "+protectedName+" variable");
            }
        }
    }
    
    private static final void logDebug(Supplier<String> messageSupplier) {
        if ( LOG.isDebugEnabled() ) {
            LOG.debug(messageSupplier.get());
        }
    }
    
    private static final String toDebugString(JsonNode value) {
        try {
            return value==null ? null : debugObjectWriter.writeValueAsString(value);
        } catch ( Exception e ) {
            return "<ERROR FORMATTING VALUE>";
        }
    }
}
