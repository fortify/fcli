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

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes an operation to add a given value to the array
 * identified by the name property. If the target array doesn't exist yet, 
 * it will be created.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public final class ActionStepAppendDescriptor extends AbstractActionStepUpdatePropertyDescriptor {
    /** Optional property name to be added to the object */
    private TemplateExpression property;
}