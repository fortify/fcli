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
package com.fortify.cli.fod.entity.app.helper;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ReflectiveAccess
@Getter
@Builder
@ToString
public class FoDAppUpdateRequest {
    private String applicationName;
    private String applicationDescription;
    private String businessCriticalityType;
    private String emailList;
    private JsonNode attributes;
    private List<String> addMicroservices;
    private List<String> deleteMicroservices;
    private Map<String,String> renameMicroservices;
}
