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
package com.fortify.cli.ssc.access_control.cli.cmd;

import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.access_control.helper.SSCUserSpecPredicate;
import com.fortify.cli.ssc.access_control.helper.SSCUserSpecPredicate.MatchMode;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "delete-user", aliases = "rm-user") @CommandGroup("user")
public class SSCUserDeleteCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @EnvSuffix("USERS") @Parameters(index = "0..*", arity = "1..*", descriptionKey = "fcli.ssc.access-control.delete-users.spec")
    private String[] authEntitySpecs;
    @Option(names="--allow-multi-match", defaultValue = "false")
    private boolean allowMultiMatch;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        ArrayNode allAuthEntities = (ArrayNode)unirest.get(SSCUrls.AUTH_ENTITIES)
                .queryString("limit", "-1")
                .asObject(JsonNode.class).getBody().get("data");
        ArrayNode authEntitiesToDelete = JsonHelper.stream(allAuthEntities)
                .filter(new SSCUserSpecPredicate(authEntitySpecs, MatchMode.INCLUDE, allowMultiMatch))
                .collect(JsonHelper.arrayNodeCollector());
        if ( authEntitiesToDelete.size()==0 ) {
            throw new IllegalArgumentException("No matching users found for deletion");
        }
        String authEntityIdsToDelete = JsonHelper.stream(authEntitiesToDelete).map(this::getAuthEntityId).collect(Collectors.joining(","));
        unirest.delete(SSCUrls.AUTH_ENTITIES).queryString("ids", authEntityIdsToDelete).asObject(JsonNode.class).getBody();
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
