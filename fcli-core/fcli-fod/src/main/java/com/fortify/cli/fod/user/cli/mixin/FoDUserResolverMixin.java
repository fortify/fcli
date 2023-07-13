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

package com.fortify.cli.fod.user.cli.mixin;

import com.fortify.cli.fod.user.helper.FoDUserDescriptor;
import com.fortify.cli.fod.user.helper.FoDUserHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDUserResolverMixin {
    public static abstract class AbstractFoDUserResolverMixin {
        public abstract String getUserNameOrId();

        public FoDUserDescriptor getUserDescriptor(UnirestInstance unirest, String... fields){
            return FoDUserHelper.getUserDescriptor(unirest, getUserNameOrId(), true);
        }

        public String getUserId(UnirestInstance unirest) {
            return getUserDescriptor(unirest, "userId").getUserId().toString();
        }
    }

    public static class RequiredOption extends AbstractFoDUserResolverMixin {
        @Option(names = {"--user"}, required = true, descriptionKey = "fcli.fod.user.user-name-or-id")
        @Getter private String userNameOrId;
    }

    public static class OptionalOption extends AbstractFoDUserResolverMixin {
        @Option(names = {"--user"}, required = false, descriptionKey = "fcli.fod.user.user-name-or-id")
        @Getter private String userNameOrId;
    }

    public static class PositionalParameter extends AbstractFoDUserResolverMixin {
        @Parameters(index = "0", descriptionKey = "fcli.fod.user.user-name-or-id")
        @Getter private String userNameOrId;
    }
}
