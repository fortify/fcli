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
package com.fortify.cli.common.action.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.ParseException;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.SampleYamlSnippets;
import com.fortify.cli.common.crypto.helper.SignatureHelper.PublicKeyDescriptor;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureDescriptor;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureStatus;
import com.fortify.cli.common.json.JsonNodeDeepCopyWalker;
import com.fortify.cli.common.spel.SpelHelper;
import com.fortify.cli.common.util.JavaHelper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;

/**
 * This class describes an action deserialized from an action YAML file, 
 * containing elements describing things like:
 * <ul> 
 *  <li>Action metadata like name and description</li> 
 *  <li>Action configuration options</li>
 *  <li>Action CLI options</li>
 *  <li>Steps to be executed, like executing REST requests and writing output</li>
 *  <li>Formatters</li>
 * </ul> 
 *
 * @author Ruud Senden
 */
@Reflectable @NoArgsConstructor
@Data
@JsonClassDescription("Fortify CLI action definition")
@JsonTypeName("action")
public class Action implements IActionElement {
    @JsonPropertyDescription("""
        Required string unless `yaml-language-server` comment with schema location is provided: Defines the fcli \
        action YAML schema version used by this action. When a user tries to run this action, fcli will check \
        whether the current fcli version is compatible with the given action schema version.   
        """)
    @SampleYamlSnippets({"$schema: !!!fcli-action-schema-url!!!",
        "# yaml-language-server: $schema=!!!fcli-action-schema-url!!!"})
    @JsonProperty(value = "$schema", required=false) public String schema;
    
    @JsonPropertyDescription("""
        Required string: Author of this action. This is a free-format string, allowing action users to see who \
        provided this action.   
        """)
    @SampleYamlSnippets({"author: MyCompany", "author: John Doe"})
    @JsonProperty(value = "author", required = true) private String author;
    
    @JsonPropertyDescription("""
        Required object: Action usage help, providing action usage instructions for users of this action. For \
        example, this information may be included in action documentation, or can be viewed by users through \
        the 'fcli * action help' command.
        """)
    @SampleYamlSnippets(copyFrom = ActionUsage.class)
    @JsonProperty(value = "usage", required = true) private ActionUsage usage;
    
    @JsonPropertyDescription("""
        Optional object: Action configuration properties. This includes configuration properties for setting \
        default values to be used by some action steps, or how action output should be processed.
        """)
    @SampleYamlSnippets(copyFrom = ActionConfig.class)
    @JsonProperty(value = "config", required = false) private ActionConfig config= new ActionConfig();
    
    @JsonPropertyDescription("""
        Optional map: CLI options accepted by this action. Map keys define the identifier for an option, which \
        can be used in later instructions through the ${cli.optionIdentifier} SpEL template expression. Map values \
        define option definitions like option names that can be specified on the command line, option description, ...
        """)
    @SampleYamlSnippets(copyFrom = ActionCliOption.class)
    @JsonProperty(value = "cli.options", required = false) private Map<String, ActionCliOption> cliOptions = Collections.emptyMap();
    
    @JsonPropertyDescription("""
        Required list: Steps to be executed when this action is being run. Each list item should consist of a \
        single instruction to be executed, optionally together with the 'if:' instruction to allow for conditional \
        execution. Note that the YAML schema allows for multiple instructions to be present within a single list \
        item, but this will result in an error.
        """)
    @SampleYamlSnippets(copyFrom = ActionStep.class)
    @JsonProperty(value = "steps", required = true) private ArrayList<ActionStep> steps;
    
    @JsonPropertyDescription("""
        Optional map: Formatters that can be referenced in action steps to format data. Map keys define formatter \
        name, map values define how the data should be formatted. Each formatter can be defined as either a single \
        string or a structured YAML object. Every string value in the formatter will be processed as a Spring \
        Expression Language expression, allowing access to current action variables and SpEL functions. For example, \
        if action variable 'name' is currently set to 'John Doe', a formatter node like 'hello: Hello ${name}' \
        will set the property 'hello' to 'Hello John Doe'.
        """)
    @SampleYamlSnippets("""
        formatters:
        plainText: |
            This is formatted plain text, with variable ${varName}.
        structured:
            prop1: Some text
            prop2: Hello ${name}!
            prop3:
            nestedProp1: xyz
            nestedProp2: ${varName}    
        """)
    @JsonProperty(value = "formatters", required = false) private Map<String, JsonNode> formatters = Collections.emptyMap();
    
    @JsonIgnore ActionMetadata metadata;
    /** Maps/Collections listing action elements. 
     *  These get filled by the {@link #visit(Action, Object)} method. */ 
    @ToString.Exclude @JsonIgnore private final List<IActionElement> allActionElements = new ArrayList<>();
    
    public List<IActionElement> getAllActionElements() {
        return Collections.unmodifiableList(allActionElements);
    }
    
    /**
     * This method is invoked by ActionHelper after deserializing an instance 
     * of this class from a YAML file. It performs the following tasks:
     * <ul>
     *  <li>Set the given attributes on this action</li>
     *  <li>Initialize the collections above</li>
     *  <li>Validate required action elements are present</li>
     *  <li>Invoke the {@link IActionElement#postLoad(Action)} method
     *      for every {@link IActionElement} collected during collection
     *      initialization</li>
     * </ul>
     * We need to initialize collections before invoking {@link IActionElement#postLoad(Action)}
     * methods, as these methods may utilize the collections.
     * @param signatureStatus 
     */
    public final void postLoad(ActionMetadata metadata) {
        this.metadata = metadata;
        initializeAllActionElements();
        allActionElements.forEach(elt->elt.postLoad(this));
    }
    
    /**
     * {@link IActionElement#postLoad(Action)} implementation
     * for this root action element, checking required elements
     * are present.
     */
    public final void postLoad(Action action) {
        checkNotNull("action usage", usage, this);
        checkNotNull("action steps", steps, this);
        convertFormatters();
    }

    private void convertFormatters() {
        var convertedFormatters = new LinkedHashMap<String, JsonNode>();
        for ( var e : formatters.entrySet() ) {
            var name = e.getKey();
            var formatter = e.getValue();
            convertedFormatters.put(name, new FormatterContentsWalker().walk(formatter));
        }
        this.formatters = convertedFormatters;
    }
    
    /**
     * Utility method for throwing an {@link FcliActionValidationException}
     * if the given boolean value is true.
     * @param isFailure
     * @param entity
     * @param msgSupplier
     */
    static final void throwIf(boolean isFailure, Object entity, Supplier<String> msgSupplier) {
        if ( isFailure ) {
            throw new FcliActionValidationException(msgSupplier.get(), entity);
        }
    }
    
    /**
     * Utility method for checking whether the given value is not blank, throwing an
     * exception otherwise.
     * @param property Descriptive name of the YAML property being checked
     * @param value Value to be checked for not being blank
     * @param entity The object containing the property to be checked
     */
    static final void checkNotBlank(String property, String value, Object entity) {
        throwIf(StringUtils.isBlank(value), entity, ()->String.format("Action %s property must be specified", property));
    }
    
    /**
     * Utility method for checking whether the given value is not null, throwing an
     * exception otherwise.
     * @param property Descriptive name of the YAML property being checked
     * @param value Value to be checked for not being null
     * @param entity The object containing the property to be checked
     */
    static final void checkNotNull(String property, Object value, Object entity) {
        throwIf(value==null, entity, ()->String.format("Action %s property must be specified", property));
    }
    
    /**
     * Initialize the {@link #allActionElements} collection using the reflective 
     * visit methods. We use reflection as manually navigating the action element 
     * tree proved to be too error-prone, often forgetting to handle newly added 
     * action element types.
     */
    @SuppressWarnings("unchecked")
    private void initializeAllActionElements() {
        visit(this, this, elt->allActionElements.add(elt),
                (k,v)->JavaHelper.as(v, IMapKeyAware.class).ifPresent(e->e.setKey(k)));
    }
    
    /**
     * Visit the given action element. If the action element implements 
     * the {@link IActionElement} interface (i.e., it's not a simple value 
     * like String or TemplateExpression), it is passed to the given consumer
     * and subsequently we recurse into all action element fields.
     * If the given action element is a collection, we recurse into each
     * collection element.
     */
    private final void visit(Action action, Object actionElement, Consumer<IActionElement> consumer, BiConsumer<Object, Object> mapEntryConsumer) {
        if ( actionElement!=null ) {
            if ( actionElement instanceof IActionElement ) {
                var instance = (IActionElement)actionElement;
                consumer.accept(instance);
                visitFields(action, instance.getClass(), instance, consumer, mapEntryConsumer);
            } else if ( actionElement instanceof Collection ) {
                ((Collection<?>)actionElement).stream()
                    .forEach(o->visit(action, o, consumer, mapEntryConsumer));
            } else if ( actionElement instanceof Map ) {
                ((Map<?,?>)actionElement).entrySet().stream()
                    .forEach(e->visitMapEntry(action, e.getKey(), e.getValue(), consumer, mapEntryConsumer));
            }
        }
    }

    /**
     * Visit the given action element that's contained in a Map. This will check whether
     * the action element implements any of the I[type]KeyAware interface to inform the
     * action element about their associated map key, then call {@link #visit(Action, Object, Consumer)}
     * to further process the action element.
     * Note that this is somewhat of a code smell, as we're only supposed to be visiting
     * action elements, not perform any operation on them.
     */
    private void visitMapEntry(Action action, Object key, Object value, Consumer<IActionElement> consumer, BiConsumer<Object, Object> mapEntryConsumer) {
        mapEntryConsumer.accept(key,  value);
        visit(action, value, consumer, mapEntryConsumer);
    }

    /**
     * Visit all fields of the given class, with field values being
     * retrieved from the given action element. 
     */
    private void visitFields(Action action, Class<?> clazz, Object actionElement, Consumer<IActionElement> consumer, BiConsumer<Object,Object> mapEntryConsumer) {
        if ( clazz!=null && IActionElement.class.isAssignableFrom(clazz) ) {
            // Visit fields provided by any superclasses of the given class.
            visitFields(action, clazz.getSuperclass(), actionElement, consumer, mapEntryConsumer);
            // Iterate over all declared fields, and invoke the
            // postLoad(action, actionElement) for each field value.
            Stream.of(clazz.getDeclaredFields())
                .peek(f->f.setAccessible(true))
                .filter(f->f.getAnnotation(JsonIgnore.class)==null)
                .map(f->getFieldValue(actionElement, f))
                .forEach(o->visit(action, o, consumer, mapEntryConsumer));
        }
    }

    /**
     * Get the value for the given field from the given action element.
     * Only reason to have this method is to handle any exceptions
     * through {@link SneakyThrows}, to allow getting field values in
     * lambda expressions like above.
     */
    @SneakyThrows
    private Object getFieldValue(Object actionElement, Field field) {
        return field.get(actionElement);
    }
    
    @Reflectable @Data @Builder(toBuilder = true) @AllArgsConstructor
    public static final class ActionMetadata {
        private final String name;
        private final boolean custom;
        private final SignatureDescriptor signatureDescriptor;
        private final PublicKeyDescriptor publicKeyDescriptor;
        @Builder.Default private final SignatureStatus signatureStatus = SignatureStatus.NOT_VERIFIED;
        
        public static final ActionMetadata create(boolean custom) {
            return builder().custom(custom).build();
        }
    }
    
    private final class FormatterContentsWalker extends JsonNodeDeepCopyWalker {
        @Override
        protected JsonNode copyValue(JsonNode state, String path, JsonNode parent, ValueNode node) {
            if ( node instanceof TextNode ) {
                var expr = node.asText();
                try {
                    return new POJONode(SpelHelper.parseTemplateExpression(expr));
                } catch (ParseException e) {
                    throw new FcliActionValidationException(String.format("Error parsing template expression '%s'", expr), this, e);
                }
            } else {
                return super.copyValue(state, path, parent, node);
            }
        }
    }
}
