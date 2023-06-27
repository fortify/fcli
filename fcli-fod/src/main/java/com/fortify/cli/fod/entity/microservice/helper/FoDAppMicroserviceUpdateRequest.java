/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/

package com.fortify.cli.fod.entity.microservice.helper;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

// TODO Consider using @Builder
// TODO Given that there's only one field, consider using @AllArgsConstructor instead of setters
@ReflectiveAccess
@Getter
@ToString
public class FoDAppMicroserviceUpdateRequest {
    private String microserviceName;

    public FoDAppMicroserviceUpdateRequest setMicroserviceName(String name) {
        this.microserviceName = name;
        return this;
    }

}
