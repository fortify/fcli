package com.fortify.cli.ssc.entity.appversion.cli.mixin;

import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.ssc.entity.appversion.helper.SSCAppVersionEmbedderSupplier;
import com.fortify.cli.ssc.output.cli.mixin.AbstractSSCBulkEmbedMixin;

import lombok.Getter;
import picocli.CommandLine.Option;

public class SSCAppVersionBulkEmbedMixin extends AbstractSSCBulkEmbedMixin {
    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names = "--embed", required = false, split = "," )
    @Getter private SSCAppVersionEmbedderSupplier[] embedSuppliers;
}
