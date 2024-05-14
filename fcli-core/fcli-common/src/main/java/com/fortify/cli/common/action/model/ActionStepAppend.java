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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes an operation to add a given value to the array or object
 * identified by the name property. If the target array or object doesn't exist yet, 
 * it will be created. Whether this operation operates on objects or arrays depends 
 * on the presence of the 'property' property; if 'property' is present, we assume
 * 'name' references an object, if 'property' is not present, we assume 'name' references
 * an array. 
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public final class ActionStepAppend extends AbstractActionStepUpdateProperty {
    @JsonPropertyDescription("Optional SpEL template expression: Property name to be added or updated in the data object specified by 'name'. If specified, 'name' is considered to be an object, otherwise 'name' is considered to be an array.")
    @JsonProperty(required = false) private TemplateExpression property;
}