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

package com.fortify.cli.fod.user_group.cli.mixin;

import com.fortify.cli.fod.user_group.helper.FoDUserGroupDescriptor;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDUserGroupResolverMixin {
    public static abstract class AbstractFoDUserGroupResolverMixin {
        public abstract String getUserGroupNameOrId();

        public FoDUserGroupDescriptor getUserGroupDescriptor(UnirestInstance unirest, String... fields){
            return FoDUserGroupHelper.getUserGroupDescriptor(unirest, getUserGroupNameOrId(), true);
        }

        public String getGroupId(UnirestInstance unirest) {
            return getUserGroupDescriptor(unirest, "groupId").getId().toString();
        }
    }

    public static class RequiredOption extends AbstractFoDUserGroupResolverMixin {
        @Option(names = {"--group"}, required = true, descriptionKey = "fcli.fod.group.group-name-or-id")
        @Getter private String userGroupNameOrId;
    }

    public static class OptionalOption extends AbstractFoDUserGroupResolverMixin {
        @Option(names = {"--group"}, required = false, descriptionKey = "fcli.fod.group.group-name-or-id")
        @Getter private String userGroupNameOrId;
    }

    public static class PositionalParameter extends AbstractFoDUserGroupResolverMixin {
        @Parameters(index = "0", descriptionKey = "fcli.fod.group.group-name-or-id")
        @Getter private String userGroupNameOrId;
    }
}
