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
package com.fortify.cli.ssc.appversion_groupset.cli.mixin;

import com.fortify.cli.ssc.appversion_groupset.helper.*;
import kong.unirest.*;
import lombok.*;
import picocli.CommandLine.*;

public class SSCAppVersionGroupSetResolverMixin {
    private static abstract class AbstractSSCGroupSetResolverMixin {
        public abstract String getGroupSetDisplayNameOrId();
        
        public SSCAppVersionGroupSetDescriptor getGroupSetDescriptor(UnirestInstance unirest, String appVersionId) {
            return new SSCAppVersionGroupSetHelper(unirest, appVersionId).getDescriptorByDisplayNameOrId(getGroupSetDisplayNameOrId(), true);
        }
    }
    
    public static class GroupByOption extends AbstractSSCGroupSetResolverMixin {
        @Option(names="--by", defaultValue = "FOLDER", descriptionKey = "fcli.ssc.appversion-group-set.resolver.displayNameOrId")
        @Getter public String groupSetDisplayNameOrId;
    }
    
    public static class PositionalParameterSingle extends AbstractSSCGroupSetResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.appversion-group-set.resolver.displayNameOrId")
        @Getter private String groupSetDisplayNameOrId;
    }
}
