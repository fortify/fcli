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
package com.fortify.cli.ssc.appversion.cli.mixin;

import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.ssc._common.output.cli.mixin.AbstractSSCBulkEmbedMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionEmbedderSupplier;

import lombok.Getter;
import picocli.CommandLine.Option;

public class SSCAppVersionBulkEmbedMixin extends AbstractSSCBulkEmbedMixin {
    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names = "--embed", required = false, split = "," )
    @Getter private SSCAppVersionEmbedderSupplier[] embedSuppliers;
}
