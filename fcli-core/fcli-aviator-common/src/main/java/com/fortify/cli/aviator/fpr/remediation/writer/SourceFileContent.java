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
package com.fortify.cli.aviator.fpr.remediation.writer;

import java.nio.charset.Charset;


public final class SourceFileContent {
    private final String content;
    private final Charset charset;
    private final String encodingSource;

    public SourceFileContent(String content, Charset charset, String encodingSource) {
        this.content = content;
        this.charset = charset;
        this.encodingSource = encodingSource;
    }

    public String content() {
        return content;
    }

    public Charset charset() {
        return charset;
    }

    public String encodingSource() {
        return encodingSource;
    }
}
