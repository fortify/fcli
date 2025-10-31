/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.common.log;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Non-annotation equivalent of {@link MaskValue}.
 *
 * @author Ruud Senden
 */
@RequiredArgsConstructor @Data @Accessors(fluent=true)
public class MaskValueDescriptor {
    private final LogSensitivityLevel sensitivity;
    private final String description;
    private final String pattern;
    
    public MaskValueDescriptor(LogSensitivityLevel sensitivity, String description) {
        this(sensitivity, description, null);
    }
}
