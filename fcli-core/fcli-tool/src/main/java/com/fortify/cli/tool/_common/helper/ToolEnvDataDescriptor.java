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
package com.fortify.cli.tool._common.helper;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Descriptor containing tool environment data for SpEL evaluation.
 * Used by {@link com.fortify.cli.tool._common.cli.cmd.AbstractToolEnvCommand}
 * to provide context for custom format expressions.
 *
 * @author Ruud Senden
 */
@Reflectable
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ToolEnvDataDescriptor {
    private String toolName;
    private String installDir;
    private String binDir;
    private String globalBinDir;
    private String cmd;
}
