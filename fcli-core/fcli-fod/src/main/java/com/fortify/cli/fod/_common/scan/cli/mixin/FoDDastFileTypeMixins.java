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

package com.fortify.cli.fod._common.scan.cli.mixin;

import com.fortify.cli.fod._common.util.FoDEnums;

import lombok.Getter;
import picocli.CommandLine.Option;

public class FoDDastFileTypeMixins {
    public static class RequiredOption {
        @Option(names = {"--file-type", "--dast-file-type"}, required = true, descriptionKey = "fcli.fod.scan.dast-file-type")
        @Getter private FoDEnums.DastAutomatedFileTypes dastFileType;
    }
    public static class OptionalOption {
        @Option(names = {"--file-type", "--dast-file-type"}, required = false, descriptionKey = "fcli.fod.scan.dast-file-type")
        @Getter private FoDEnums.DastAutomatedFileTypes dastFileType;
    }
}
