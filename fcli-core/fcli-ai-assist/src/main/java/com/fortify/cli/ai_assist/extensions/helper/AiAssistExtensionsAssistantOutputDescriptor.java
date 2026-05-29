/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.ai_assist.extensions.helper;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Output record for the list-assistants command.
 */
@Reflectable @NoArgsConstructor @AllArgsConstructor @Builder @Data
public class AiAssistExtensionsAssistantOutputDescriptor {
    private String id;
    private String name;
    private String[] contentTypes;
    private String contentTypesString;
    private String detected;
    private boolean installed;
    private String installedVersion;
}
