/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.entity.appversion_attribute.cli.mixin;

import java.util.Map;

import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAppVersionAttributeUpdateMixin {
    private static final String PARAM_LABEL = "[CATEGORY:]ATTR=VALUE[,VALUE...]";
    public static abstract class AbstractSSCAppVersionAttributeUpdateMixin {
        public abstract Map<String,String> getAttributes();

    }
    
    public static class OptionalAttrOption extends AbstractSSCAppVersionAttributeUpdateMixin {
        @Option(names = {"--attrs", "--attributes"}, required = false, split = ",", paramLabel = PARAM_LABEL)
        @Getter private Map<String,String> attributes;
    }
    
    public static class RequiredPositionalParameter extends AbstractSSCAppVersionAttributeUpdateMixin {
        @Parameters(index = "0..*", arity = "1..*", paramLabel = PARAM_LABEL)
        @Getter private Map<String,String> attributes;
    }
    
    
}
