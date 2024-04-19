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

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes default values for various action properties.
 */
@Reflectable @NoArgsConstructor
@Data
public final class ActionDefaultValues implements IActionElement {
    /** Default value for {@link ActionStepRequest#from} */
    String requestTarget;
    
    @Override
    public void postLoad(Action action) {}
}