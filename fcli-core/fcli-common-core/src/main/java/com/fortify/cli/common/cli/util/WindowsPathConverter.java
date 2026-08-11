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
package com.fortify.cli.common.cli.util;

import java.nio.file.Path;

import com.fortify.cli.common.util.WindowsPathValidator;

import picocli.CommandLine.ITypeConverter;

public final class WindowsPathConverter implements ITypeConverter<Path> {
    @Override
    public Path convert(String value) {
        return WindowsPathValidator.toPath("The supplied path", value);
    }
}
