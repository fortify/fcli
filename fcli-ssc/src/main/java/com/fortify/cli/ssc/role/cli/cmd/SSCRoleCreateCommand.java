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
package com.fortify.cli.ssc.role.cli.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestBaseRequestSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = SSCOutputHelperMixins.Create.CMD_NAME)
public class SSCRoleCreateCommand extends AbstractSSCOutputCommand implements IUnirestBaseRequestSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.Create outputHelper;

/*
    # NOTE: There are some permissions that have a dependency on another permission.
    When using the REST API, SSC will not tell you if you have unsatisfied permission dependencies.
 */

    @Getter
    @Parameters(index = "0", paramLabel = "role-name")
    private String name;

    @Getter
    @Option(names = {"-d", "--description"})
    private String description;

    @Getter
    @Option(names = "--universal-access", defaultValue = "false")
    private Boolean allApplicationRole;

    @Getter
    @Option(names = {"-p", "--permission-id"}, split = ",")
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
