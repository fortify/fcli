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

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes action usage header and description.
 */
@Reflectable @NoArgsConstructor
@Data
public final class ActionUsageDescriptor {
    /** Required usage header */
    private String header;
    /** Required usage description */
    private String description;
    
    public void postLoad(ActionDescriptor action) {
        ActionDescriptor.checkNotBlank("usage header", header, this);
        ActionDescriptor.checkNotBlank("usage description", description, this);
    }
}