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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureStatus;
import com.fortify.cli.common.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

/**
 * This class describes an action deserialized from an action YAML file, 
 * containing elements describing things like:
 * <ul> 
 *  <li>Action metadata like name and description</li> 
 *  <li>Action parameters</li>
 *  <li>Steps to be executed, like executing REST requests and writing output</li>
 *  <li>Value templates</li>
 *  <Various other action configuration elements</li>
 * </ul> 
 *
 * @author Ruud Senden
 */
@Reflectable @NoArgsConstructor
@Data
public class Action implements IActionElement {
    /** Action name, set in {@link #postLoad(ActionProperties)} method */
    private String name;
    /** Whether this is a custom action, set in {@link #postLoad(ActionProperties)} method */
    private boolean custom;
    /** Signature status for this action, set in {@link #postLoad(ActionProperties)} method */
    private SignatureStatus signatureStatus;
    /** Action description */
    private ActionUsage usage;
    /** Action parameters */
    private List<ActionParameter> parameters;
    /** Additional requests targets */
    private List<ActionRequestTarget> addRequestTargets;
    /** Default values for certain action properties */
    ActionDefaultValues defaults;
    /** Action steps, evaluated in the order as defined in the YAML file */
    private List<ActionStep> steps;
    /** Value templates */
    private List<ActionValueTemplate> valueTemplates;
    /** Maps/Collections listing action elements. 
     *  These get filled by the {@link #visit(Action, Object)} method. */ 
    @JsonIgnore private final Map<String, ActionValueTemplate> valueTemplatesByName = new HashMap<>();
    @JsonIgnore private final List<IActionElement> allActionElements = new ArrayList<>();
    
    public Map<String, ActionValueTemplate> getValueTemplatesByName() {
        return Collections.unmodifiableMap(valueTemplatesByName);
    }
    
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
    public final void postLoad(ActionProperties properties) {
        this.name = properties.getName();
        this.custom = properties.isCustom();
        initializeCollections();
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
        if ( parameters==null ) {
            parameters = Collections.emptyList();
        }
    }
    
    /**
     * Utility method for throwing an {@link ActionValidationException}
     * if the given boolean value is true.
     * @param isFailure
     * @param entity
     * @param msgSupplier
     */
    static final void throwIf(boolean isFailure, Object entity, Supplier<String> msgSupplier) {
        if ( isFailure ) {
            throw new ActionValidationException(msgSupplier.get(), entity);
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
     * Utility method for checking the attributes of an {@link IActionStepValueSupplier}
     * instance.
     */
    static final void checkActionValueSupplier(Action action, IActionStepValueSupplier supplier) {
        var value = supplier.getValue();
        var valueTemplate = supplier.getValueTemplate();
        throwIf(value!=null && StringUtils.isNotBlank(valueTemplate), supplier, ()->"Either value or valueTemplate must be specified, not both");
        throwIf(value==null && StringUtils.isBlank(valueTemplate), supplier, ()->"Either value or valueTemplate must be specified");
        if ( valueTemplate!=null ) {
            throwIf(!action.getValueTemplates().stream().anyMatch(d->d.getName().equals(valueTemplate)), supplier, 
                    ()->"No value template found with name "+valueTemplate);
        }
    }
    
    /**
     * Initialize the {@link #allActionElements} and {@link #valueTemplatesByName}
     * collections, using the reflective visit methods. We use reflection as
     * manually navigating the action element tree proved to be too error-prone,
     * often forgetting to handle newly added action element types.
     */
    private void initializeCollections() {
        visit(this, this, elt->{
            allActionElements.add(elt);
            if ( elt instanceof ActionValueTemplate ) {
                var actionValueTemplate = (ActionValueTemplate)elt;
                valueTemplatesByName.put(actionValueTemplate.getName(), actionValueTemplate);
            }
        });
    }
    
    /**
     * Visit the given action element. If the action element implements 
     * the {@link IActionElement} interface (i.e., it's not a simple value 
     * like String or TemplateExpression), it is passed to the given consumer
     * and subsequently we recurse into all action element fields.
     * If the given action element is a collection, we recurse into each
     * collection element.
     */
    private final void visit(Action action, Object actionElement, Consumer<IActionElement> consumer) {
        if ( actionElement!=null ) {
            if ( actionElement instanceof IActionElement ) {
                var instance = (IActionElement)actionElement;
                consumer.accept(instance);
                visitFields(action, instance.getClass(), instance, consumer);
            } else if ( actionElement instanceof Collection ) {
                ((Collection<?>)actionElement).stream()
                    .forEach(o->visit(action, o, consumer));
            }
        }
    }

    /**
     * Visit all fields of the given class, with field values being
     * retrieved from the given action element. 
     */
    private void visitFields(Action action, Class<?> clazz, Object actionElement, Consumer<IActionElement> consumer) {
        if ( clazz!=null && IActionElement.class.isAssignableFrom(clazz) ) {
            // Visit fields provided by any superclasses of the given class.
            visitFields(action, clazz.getSuperclass(), actionElement, consumer);
            // Iterate over all declared fields, and invoke the
            // postLoad(action, actionElement) for each field value.
            Stream.of(clazz.getDeclaredFields())
                .peek(f->f.setAccessible(true))
                .filter(f->f.getAnnotation(JsonIgnore.class)==null)
                .map(f->getFieldValue(actionElement, f))
                .forEach(o->visit(action, o, consumer));
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
    
    @Data @Builder(toBuilder = true) @AllArgsConstructor
    public static final class ActionProperties {
        private final String name;
        private final boolean custom;
        @Builder.Default private final SignatureStatus signatureStatus = SignatureStatus.NOT_VERIFIED;
        
        public static final ActionProperties create(boolean custom) {
            return builder().custom(custom).build();
        }
    }
}
