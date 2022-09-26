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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.role.cli.mixin.SSCRoleResolverMixin;
import com.fortify.cli.ssc.util.SSCOutputConfigHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.JsonNode;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.Arrays;

@ReflectiveAccess
@Command(name = "create")
public class SSCRoleCreateCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {

    @Mixin
    private OutputMixin outputMixin;

/*
    # NOTE: There are some permissions that have a dependency on another permission.
    When using the REST API, SSC will not tell you if you have unsatisfied permission dependencies.
 */

    @Getter
    @Option(names = {"-n", "--name"}, required = true)
    private String name;

    @Getter
    @Option(names = {"-d", "--description"})
    private String description;

    @Getter
    @Option(names = "--universal-access", defaultValue = "false")
    private Boolean allApplicationRole;

    @Getter
    @Option(names = {"-p", "--permission-id"})
    private String[] permissionIds;

    @SneakyThrows
    protected Void run(UnirestInstance unirest) {
        ObjectNode newRole = (new ObjectMapper()).createObjectNode();
        newRole.put("name",name);
        newRole.put("description", description);
        newRole.put("allApplicationRole", allApplicationRole);
        newRole.set("permissionIds", JsonHelper.toArrayNode(permissionIds));

        outputMixin.write(
                unirest.post(SSCUrls.ROLES).body(newRole)
        );
        return null;
    }

    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SSCOutputConfigHelper.tableFromData()
                .defaultColumns("id#name#permissionIds");
    }
}
