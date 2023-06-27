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
package com.fortify.cli.sc_sast.entity.scan.helper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ReflectiveAccess
@Data @EqualsAndHashCode(callSuper=true) @JsonIgnoreProperties(ignoreUnknown = true)
public class SCSastControllerScanJobDescriptor extends JsonNodeHolder {
    private String jobToken;
    @JsonProperty("state") private String scanState;
    private boolean hasFiles;
    private String sscUploadState;
    private String scaProgress;
    private String sscArtifactState;
    private int endpointVersion;
}
