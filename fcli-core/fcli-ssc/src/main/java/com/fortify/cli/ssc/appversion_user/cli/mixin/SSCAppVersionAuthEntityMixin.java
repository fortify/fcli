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
package com.fortify.cli.ssc.appversion_user.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAppVersionAuthEntityMixin {
    public static abstract class AbstractSSCAppVersionAuthEntityMixin {
        public abstract String[] getAuthEntitySpecs();
    }
    
    public static class OptionalUserAddOption extends AbstractSSCAppVersionAuthEntityMixin {
        @Option(names = {"--add-users"}, required = false, split = ",", descriptionKey = "fcli.ssc.appversion-auth-entity.add.specs")
        @Getter private String[] authEntitySpecs;
    }
    
    public static class OptionalUserRemoveOption extends AbstractSSCAppVersionAuthEntityMixin {
        @Option(names = {"--rm-users"}, required = false, split = ",", descriptionKey = "fcli.ssc.appversion-auth-entity.rm.specs")
        @Getter private String[] authEntitySpecs;
    }
    
    public static class RequiredPositionalParameter extends AbstractSSCAppVersionAuthEntityMixin {
        @Parameters(index = "0..*", arity = "1..*", descriptionKey = "fcli.ssc.appversion-auth-entity.specs")
        @Getter private String[] authEntitySpecs;
    }
    
    
}
