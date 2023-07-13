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
package com.fortify.cli.ssc.role.cli.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class SSCRoleCreateCommand extends AbstractSSCBaseRequestOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;

/*
    # NOTE: There are some permissions that have a dependency on another permission.
    When using the REST API, SSC will not tell you if you have unsatisfied permission dependencies.
 */

    @Getter
    @Parameters(index = "0", descriptionKey = "fcli.ssc.role.name")
    private String name;

    @Getter
    @Option(names = {"-d", "--description"})
    private String description;

    @Getter
    @Option(names = "--universal-access", defaultValue = "false")
    private Boolean allApplicationRole;

    @Getter
    @Option(names = {"-p", "--permission-ids"}, split = ",")
    private String[] permissionIds;

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        ObjectNode newRole = (new ObjectMapper()).createObjectNode();
        newRole.put("name",name);
        newRole.put("description", description);
        newRole.put("allApplicationRole", allApplicationRole);
        newRole.set("permissionIds", JsonHelper.toArrayNode(permissionIds));

        return unirest.post(SSCUrls.ROLES).body(newRole);
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override
    public String getActionCommandResult() {
        return "CREATED";
    }
}
