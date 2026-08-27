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
package com.fortify.cli.aviator.ssc.cli.cmd;

import com.fortify.cli.common.variable.DefaultVariablePropertyName;

import picocli.CommandLine.Command;

/**
 * Deprecated SAST audit command retained for backward compatibility.
 * Use {@code fcli aviator ssc audit-sast} instead.
 */
@Command(name = "audit")
@DefaultVariablePropertyName("artifactId")
public class AviatorSSCAuditCommand extends AviatorSSCSastAuditCommand {}
