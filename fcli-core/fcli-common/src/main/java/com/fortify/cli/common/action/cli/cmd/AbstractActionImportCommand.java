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
package com.fortify.cli.common.action.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

// TODO Re-implement import functionality 
public abstract class AbstractActionImportCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @ArgGroup(exclusive = true, multiplicity = "1") private ImportArgGroup argGroup = new ImportArgGroup();
    private static final class ImportArgGroup {
        @ArgGroup(exclusive=false) private ZipArgGroup zipArgGroup = new ZipArgGroup();
        @ArgGroup(exclusive=false) private FileArgGroup fileArgGroup = new FileArgGroup();
    }
    private static final class ZipArgGroup {
        @Option(names = {"--zip", "-z"}, required = true, descriptionKey="fcli.action.import.zip") private String zip;
    }
    private static final class FileArgGroup {
        @Option(names = {"--file", "-f"}, required = true, descriptionKey="fcli.action.import.file") private String file;
        @Option(names = {"--name", "-n"}, required = false, descriptionKey="fcli.action.import.name") private String name;
    }
    
    @Override
    public final JsonNode getJsonNode() {
        var zip = argGroup.zipArgGroup.zip;
        /* TODO
        if ( StringUtils.isNotBlank(zip) ) {
            return ActionHelper.importZip(getType(), zip);
        } else {
            return ActionHelper.importSingle(getType(), argGroup.fileArgGroup.name, argGroup.fileArgGroup.file);
        }
        */
        return null;
    }    
    @Override
    public final boolean isSingular() {
        return false;
    }
    protected abstract String getType();
    
    
}
