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
package com.fortify.cli.ssc.auth_entity.cli.cmd;

import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.common.util.JsonHelper;
import com.fortify.cli.ssc.auth_entity.helper.SSCAuthEntitySpecPredicate;
import com.fortify.cli.ssc.auth_entity.helper.SSCAuthEntitySpecPredicate.MatchMode;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCUnirestRunnerCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@ReflectiveAccess
@Command(name = "delete", aliases = "rm")
public class SSCAuthEntityDeleteCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
    @Parameters(index = "0..*", arity = "1..*")
    private String[] authEntitySpecs;
    @Mixin private OutputMixin outputMixin;
    @Option(names="--allowMultiMatch", defaultValue = "false")
    private boolean allowMultiMatch;
    
    @Override
    protected Void run(UnirestInstance unirest) {
        ArrayNode allAuthEntities = (ArrayNode)unirest.get(SSCUrls.AUTH_ENTITIES)
                .queryString("limit", "-1")
                .asObject(JsonNode.class).getBody().get("data");
        ArrayNode authEntitiesToDelete = JsonHelper.stream(allAuthEntities)
                .filter(new SSCAuthEntitySpecPredicate(authEntitySpecs, MatchMode.INCLUDE, allowMultiMatch))
                .map(this::addAction)
                .collect(JsonHelper.arrayNodeCollector());
        if ( authEntitiesToDelete.size()==0 ) {
            throw new IllegalArgumentException("No matching users found for deletion");
        }
        String authEntityIdsToDelete = JsonHelper.stream(authEntitiesToDelete).map(this::getAuthEntityId).collect(Collectors.joining(","));
        unirest.delete(SSCUrls.AUTH_ENTITIES).queryString("ids", authEntityIdsToDelete).asEmpty().getBody();
        outputMixin.write(authEntitiesToDelete);
        return null;
    }
    
    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return OutputConfig.table()
            .defaultColumns("id#entityName:Name#displayName#type#email#isLdap#action");
    }

    private JsonNode addAction(JsonNode authEntityNode) {
        return ((ObjectNode)authEntityNode).put("action", "DELETED");
    }

    private String getAuthEntityId(JsonNode authEntityNode) {
        return authEntityNode.get("id").asText();
    }
}
