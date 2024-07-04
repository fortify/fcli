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
package com.fortify.cli.ssc.artifact.cli.cmd;

import java.io.File;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.common.cli.cmd.import_debricked.DebrickedHelper;
import com.fortify.cli.common.cli.cmd.import_debricked.DebrickedLoginOptions;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "import-debricked")
public class SSCArtifactImportDebrickedCommand extends AbstractSSCArtifactUploadCommand {
    @Mixin @Getter private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private DebrickedLoginOptions debrickedLoginOptions; 
    
    @Option(names = {"-e", "--engine-type"}, required = true, defaultValue = "DEBRICKED")
    @Getter private String engineType;
    
    @Option(names = {"-f", "--save-sbom-as"}, required = false)
    private String fileName;
    
    @Option(names = {"-r", "--repository"}, required = true)
    private String repository;
    
    @Option(names = {"-b", "--branch"}, required = true)
    private String branch;
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override @SneakyThrows
    protected File getFile() {
    	File sbomFile = null;
    	if ( StringUtils.isNotBlank(fileName) ) {
    		sbomFile = new File(fileName);
    	} else {
    		sbomFile = File.createTempFile("debricked", ".json");
    		sbomFile.deleteOnExit();
    	}
    	return sbomFile;
    }
    
    @Override
    protected void preUpload(UnirestInstance unirest, IProgressWriterI18n progressWriter, File file) {
    	DebrickedHelper debrickedHelper = new DebrickedHelper(debrickedLoginOptions, repository, branch);
		progressWriter.writeProgress("Status: Generating & downloading SBOM");
    	try ( var debrickedUnirest = GenericUnirestFactory.createUnirestInstance() ) {
    	    debrickedHelper.downloadSbom(debrickedUnirest, file);
    	}
    	progressWriter.writeProgress("Status: Uploading SBOM to SSC");
    }
    
    @Override
    protected void postUpload(UnirestInstance unirest, IProgressWriterI18n progressWriter, File file) {
    	if ( StringUtils.isBlank(fileName) ) {
    		file.delete();
    	}
    	progressWriter.writeProgress("Status: SBOM uploaded to SSC");
    	progressWriter.clearProgress();
    }

}
