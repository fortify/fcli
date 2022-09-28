/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.ssc.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.ssc.app.cli.mixin.SSCAppResolverMixin;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.util.SSCOutputConfigHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = "delete")
public class SSCAppDeleteCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
    @Mixin private OutputMixin outputMixin;
    @Mixin private SSCAppResolverMixin.PositionalParameter appResolver;
    @Option(names="--delete-versions") private boolean deleteVersions;
    
    @SneakyThrows
    protected Void run(UnirestInstance unirest) {
        if (!deleteVersions) { throw new IllegalArgumentException("To confirm deleting all versions for this application, the --delete-versions option is required"); }
        JsonNode versions = getAppVersions(unirest);
        versions.forEach(v->deleteAppVersion(unirest, (ObjectNode)v));
        outputMixin.write(versions);
        return null;
    }

    private JsonNode getAppVersions(UnirestInstance unirest) {
        return unirest.get(SSCUrls.PROJECT_VERSIONS_LIST(appResolver.getAppId(unirest)))
                .queryString("limit", "-1")
                .queryString("fields", "id,name,project,createdBy")
                .asObject(JsonNode.class).getBody().get("data");
    }
    
    private void deleteAppVersion(UnirestInstance unirest, ObjectNode version) {
        unirest.delete(SSCUrls.PROJECT_VERSION(version.get("id").asText())).asObject(JsonNode.class).getBody();
        // TODO Should we check the response, or assume SSC will return a proper HTTP response code 
        //      (resulting in an exception being thrown by the statement above)
        version.put("action", "DELETED");
    }
    
    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SSCOutputConfigHelper.table();
        //.defaultColumns("id#project.name#name#createdBy#action");
    }
}
