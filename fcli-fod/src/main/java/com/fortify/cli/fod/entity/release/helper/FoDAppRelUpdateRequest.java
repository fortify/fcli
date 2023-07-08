/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod.entity.release.helper;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

//TODO Consider using @Builder instead of manual setters
@Reflectable @NoArgsConstructor
@Getter @ToString
public class FoDAppRelUpdateRequest {
    private String releaseName;
    private String releaseDescription;
    private String sdlcStatusType;
    private Integer ownerId;
    private Integer microserviceId;

    public FoDAppRelUpdateRequest setReleaseName(String name) {
        this.releaseName = name;
        return this;
    }

    public FoDAppRelUpdateRequest setReleaseDescription(String description) {
        this.releaseDescription = (description == null ? "" : description);
        return this;
    }

    public FoDAppRelUpdateRequest setSdlcStatusType(String statusType) {
        this.sdlcStatusType = statusType;
        return this;
    }

    public FoDAppRelUpdateRequest setOwnerId(Integer id) {
        this.ownerId = id;
        return this;
    }

    public FoDAppRelUpdateRequest setMicroserviceId(Integer id) {
        this.microserviceId = id;
        return this;
    }

}
