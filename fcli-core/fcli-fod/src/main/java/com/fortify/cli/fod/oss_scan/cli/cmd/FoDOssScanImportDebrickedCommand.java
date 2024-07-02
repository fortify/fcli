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

import java.io.File;
import java.util.function.BiFunction;

import com.fortify.cli.common.cli.cmd.import_debricked.DebrickedHelper;
import com.fortify.cli.common.cli.cmd.import_debricked.DebrickedLoginOptions;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanImportCommand;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "import-debricked")
public class FoDOssScanImportDebrickedCommand extends AbstractFoDScanImportCommand {
    @Getter @Mixin private OutputHelperMixins.Import outputHelper;
    @Mixin private DebrickedLoginOptions debrickedLoginOptions; 
    
    //@Option(names = {"-f", "--save-sbom-as"}, required = false, defaultValue = "sbom.json")
    //private String fileName;
    
    @Option(names = {"-r", "--repository"}, required = true)
    private String repository;
    
    @Option(names = {"-b", "--branch"}, required = true)
    private String branch;

    @Option(names="--type", required = true, defaultValue = "CycloneDX")
    private FoDScanImportOpenSourceType type;

    @Override
    protected HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId) {
        return type.getBaseRequest(unirest, releaseId);
    }

    @Override
    protected FoDScanType getScanType() {
        return FoDScanType.OpenSource;
    }

    @Override
    protected void preUpload(UnirestInstance unirest, IProgressWriterI18n progressWriter, File file) {
    	DebrickedHelper debrickedHelper = new DebrickedHelper(debrickedLoginOptions, repository, branch);
		progressWriter.writeProgress("Status: Generating & downloading SBOM");
    	try ( var debrickedUnirest = GenericUnirestFactory.createUnirestInstance() ) {
    	    debrickedHelper.downloadSbom(debrickedUnirest, file);
    	}
    	progressWriter.writeProgress("Status: Uploading SBOM to FoD");
    }
    
    @Override
    protected void postUpload(UnirestInstance unirest, IProgressWriterI18n progressWriter, File file) {
    	//if ( StringUtils.isBlank(fileName) ) {
    	//	file.delete();
    	//}
    	progressWriter.writeProgress("Status: SBOM uploaded to FoD");
    	progressWriter.clearProgress();
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
