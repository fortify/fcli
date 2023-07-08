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

// TODO Consider using @Builder instead of manual setters
@Reflectable @NoArgsConstructor
@Getter @ToString
public class FoDAppRelCreateRequest {
    private Integer applicationId;
    private String releaseName;
    private String releaseDescription;
    private boolean copyState = false;
    private Integer copyStateReleaseId;
    private String sdlcStatusType;
    private Integer microserviceId;

    public FoDAppRelCreateRequest setApplicationId(Integer id) {
        this.applicationId = id;
        return this;
    }

    public FoDAppRelCreateRequest setReleaseName(String name) {
        this.releaseName = name;
        return this;
    }

    public FoDAppRelCreateRequest setReleaseDescription(String description) {
        this.releaseDescription = (description == null ? "" : description);
        return this;
    }

    public FoDAppRelCreateRequest setCopyState(Boolean copyState) {
        this.copyState = (copyState != null ? copyState : false);
        return this;
    }

    public FoDAppRelCreateRequest setCopyStateReleaseId(Integer id) {
        this.copyStateReleaseId = id;
        return this;
    }

    public FoDAppRelCreateRequest setSdlcStatusType(String statusType) {
        this.sdlcStatusType = statusType;
        return this;
    }

    public FoDAppRelCreateRequest setMicroserviceId(Integer id) {
        this.microserviceId = id;
        return this;
    }

}
