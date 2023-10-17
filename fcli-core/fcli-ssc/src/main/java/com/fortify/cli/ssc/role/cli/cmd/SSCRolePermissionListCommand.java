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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc._common.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.role.cli.mixin.SSCRoleResolverMixin;
import com.fortify.cli.ssc.role.helper.SSCRolePermissionHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = SSCOutputHelperMixins.ListPermissions.CMD_NAME) @CommandGroup("permission")
public class SSCRolePermissionListCommand extends AbstractSSCBaseRequestOutputCommand implements IRecordTransformer {
    @Getter @Mixin private SSCOutputHelperMixins.ListPermissions outputHelper; 
    @Mixin private SSCRoleResolverMixin.OptionalOption roleResolverMixin;
    
    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        if ( StringUtils.isBlank(roleResolverMixin.getRoleNameOrId()) ) {
            return unirest.get(SSCUrls.PERMISSIONS);
        } else {
            return unirest.get(SSCUrls.ROLE_PERMISSIONS(roleResolverMixin.getRoleId(unirest)));
        }
    }
    
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return SSCRolePermissionHelper.flattenArrayProperty(record, "dependsOnPermission", "id");
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
