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
package com.fortify.cli.ssc.user.cli.cmd;

import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.user.helper.SSCAuthEntitySpecPredicate;
import com.fortify.cli.ssc.user.helper.SSCAuthEntitySpecPredicate.MatchMode;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = SSCOutputHelperMixins.Delete.CMD_NAME)
public class SSCAuthEntityDeleteCommand extends AbstractSSCOutputCommand implements IUnirestJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.Delete outputHelper;
    @Parameters(index = "0..*", arity = "1..*")
    private String[] authEntitySpecs;
    @Option(names="--allow-multi-match", defaultValue = "false")
    private boolean allowMultiMatch;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        ArrayNode allAuthEntities = (ArrayNode)unirest.get(SSCUrls.AUTH_ENTITIES)
                .queryString("limit", "-1")
                .asObject(JsonNode.class).getBody().get("data");
        ArrayNode authEntitiesToDelete = JsonHelper.stream(allAuthEntities)
                .filter(new SSCAuthEntitySpecPredicate(authEntitySpecs, MatchMode.INCLUDE, allowMultiMatch))
                .collect(JsonHelper.arrayNodeCollector());
        if ( authEntitiesToDelete.size()==0 ) {
            throw new IllegalArgumentException("No matching users found for deletion");
        }
        String authEntityIdsToDelete = JsonHelper.stream(authEntitiesToDelete).map(this::getAuthEntityId).collect(Collectors.joining(","));
        unirest.delete(SSCUrls.AUTH_ENTITIES).queryString("ids", authEntityIdsToDelete).asEmpty().getBody();
        return authEntitiesToDelete;
    }
    
    @Override
    public String getActionCommandResult() {
        return "DELETED";
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }

    private String getAuthEntityId(JsonNode authEntityNode) {
        return authEntityNode.get("id").asText();
    }
}
