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
package com.fortify.cli.sc_sast.scan.helper;

import java.io.File;

import lombok.Builder;
import lombok.Data;

/**
 * This class describes the payload to be scanned, which can either be a
 * package file or MBS file. The descriptor can be loaded through the 
 * {@link SCSastScanPayloadHelper#loadDescriptor()} method.
 */
@Builder @Data
public class SCSastScanPayloadDescriptor {
    private final File payloadFile;
    private final String productVersion;
    private final String buildId;
    private final boolean dotNetRequired;
    private final String dotNetVersion;
    private final SCSastScanJobType jobType;
    @Builder.Default private final SCSastOperatingSystem requiredOs = SCSastOperatingSystem.ANY;
}
