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
package com.fortify.cli.fod.entity.user.helper;

import com.fasterxml.jackson.databind.JsonNode;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@ReflectiveAccess
@Data
@Builder
@ToString
public class FoDUserCreateRequest {
    private String userName;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Integer roleId;
    @Builder.Default
    private Boolean passwordNeverExpires = false;
    @Builder.Default
    private Boolean isSuspended = false;
    private JsonNode userGroupIds;
    private JsonNode applicationIds;
}
