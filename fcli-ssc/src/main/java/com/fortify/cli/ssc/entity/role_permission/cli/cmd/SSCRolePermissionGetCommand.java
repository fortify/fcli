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
package com.fortify.cli.ssc.entity.role_permission.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.ssc.entity.role_permission.cli.mixin.SSCRolePermissionResolverMixin;
import com.fortify.cli.ssc.entity.role_permission.helper.SSCRolePermissionHelper;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class SSCRolePermissionGetCommand extends AbstractSSCBaseRequestOutputCommand implements IRecordTransformer {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper; 
    @Mixin private SSCRolePermissionResolverMixin.PositionalParameter rolePermissionResolver;

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get(SSCUrls.PERMISSION(rolePermissionResolver.getRolePermissionId(unirest)));
    }
    
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return SSCRolePermissionHelper.flattenArrayProperty(record, "dependsOnPermission", "id");
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
