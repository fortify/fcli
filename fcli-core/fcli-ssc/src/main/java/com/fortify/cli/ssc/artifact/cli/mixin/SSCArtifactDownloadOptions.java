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
package com.fortify.cli.ssc.artifact.cli.mixin;

import com.fortify.cli.common.cli.mixin.CommonOptionMixins;

import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Getter
public class SSCArtifactDownloadOptions {
    @Mixin private CommonOptionMixins.OptionalOutputFile destination;
    @Option(names = "--no-include-sources", negatable = true, descriptionKey = "download.no-include-sources") private boolean includeSources = true;
}
