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
package com.fortify.cli.common.action.helper.descriptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.StringUtils;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * This class describes an action deserialized from an action YAML file, 
 * containing elements describing:
 * <ul> 
 *  <li>Action metadata like name and description</li> 
 *  <li>Action parameters</li>
 *  <li>Steps to be executed, like executing REST requests and writing output</li>
 *  <li>Data formatters</li>
 * </ul> 
 *
 * @author Ruud Senden
 */
@Reflectable @NoArgsConstructor
@Data
public class ActionDescriptor {
    /** Action name, set in {@link #postLoad(String)} method */
    private String name;
    /** Whether this is a custom action, set in {@link #postLoad(String)} method */
    private boolean custom;
    /** Action description */
    private ActionUsageDescriptor usage;
    /** Action parameters */
    private List<ActionParameterDescriptor> parameters = Collections.emptyList();
    /** Additional requests targets */
    private List<ActionRequestTargetDescriptor> addRequestTargets;
    /** Default values for certain action properties */
    ActionDefaultValuesDescriptor defaults;
    /** Action steps, evaluated in the order as defined in the YAML file */
    private List<ActionStepDescriptor> steps;
    /** Value templates */
    private List<ActionValueTemplateDescriptor> valueTemplates;
    @JsonIgnore @Getter(lazy=true) private final Map<String, ActionValueTemplateDescriptor> valueTemplatesByName = 
            valueTemplates.stream().collect(Collectors.toMap(ActionValueTemplateDescriptor::getName, Function.identity()));
    
    /**
     * This method is invoked by ActionHelper after deserializing
     * an instance of this class from a YAML file. It performs some additional
     * initialization and validation.
     */
    public final void postLoad(String name, boolean isCustom) {
        this.name = name;
        this.custom = isCustom;
        checkNotNull("action usage", usage, this);
        checkNotNull("action steps", steps, this);
        usage.postLoad(this);
        if ( parameters!=null ) { parameters.forEach(d->d.postLoad(this)); }
        if ( addRequestTargets!=null ) { addRequestTargets.forEach(d->d.postLoad(this)); }
        steps.forEach(d->d.postLoad(this));
        if ( valueTemplates!=null ) {
            valueTemplates.forEach(d->d.postLoad(this));
        }
    }
    
    static final void check(boolean isFailure, Object entity, Supplier<String> msgSupplier) {
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
        check(StringUtils.isBlank(value), entity, ()->String.format("Action %s property must be specified", property));
    }
    
    /**
     * Utility method for checking whether the given value is not null, throwing an
     * exception otherwise.
     * @param property Descriptive name of the YAML property being checked
     * @param value Value to be checked for not being null
     * @param entity The object containing the property to be checked
     */
    static final void checkNotNull(String property, Object value, Object entity) {
        check(value==null, entity, ()->String.format("Action %s property must be specified", property));
    }
    
    static final void checkActionValueSupplier(ActionDescriptor action, IActionStepValueSupplier supplier) {
        var value = supplier.getValue();
        var valueTemplate = supplier.getValueTemplate();
        check(value!=null && StringUtils.isNotBlank(valueTemplate), supplier, ()->"Either value or valueTemplate must be specified, not both");
        check(value==null && StringUtils.isBlank(valueTemplate), supplier, ()->"Either value or valueTemplate must be specified");
        if ( valueTemplate!=null ) {
            check(!action.getValueTemplates().stream().anyMatch(d->d.getName().equals(valueTemplate)), supplier, 
                    ()->"No value template found with name "+valueTemplate);
        }
    }
}
