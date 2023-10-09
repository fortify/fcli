/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.fod.oss_scan.cli.cmd;

import java.util.function.BiFunction;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.fod._common.rest.FoDUrls;

import com.fortify.cli.fod.scan.cli.cmd.AbstractFoDScanImportCommand;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Import.CMD_NAME)
public class FoDOssScanImportCommand extends AbstractFoDScanImportCommand {
    @Getter @Mixin private OutputHelperMixins.Import outputHelper;

    @Option(names="--type", required = true, defaultValue = "CycloneDX")
    private FoDScanImportOpenSourceType type;

    @Override
    protected HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId) {
        return type.getBaseRequest(unirest, releaseId);
    }

    @Override
    protected String getImportScanType() {
        return "OpenSource";
    }

    @RequiredArgsConstructor
    public static enum FoDScanImportOpenSourceType {
        CycloneDX(FoDScanImportOpenSourceType::getBaseRequestCycloneDX);

        private final BiFunction<UnirestInstance, String, HttpRequest<?>> f;

        public HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId) {
            return f.apply(unirest, releaseId);
        }

        private static final HttpRequest<?> getBaseRequestCycloneDX(UnirestInstance unirest, String releaseId) {
            return unirest.put(FoDUrls.RELEASE_IMPORT_CYCLONEDX_SBOM).routeParam("relId", releaseId);
        }
    }
}
