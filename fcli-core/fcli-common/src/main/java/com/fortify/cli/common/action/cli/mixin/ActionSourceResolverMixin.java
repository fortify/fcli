/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.action.cli.mixin;

import java.util.List;

import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.util.StringUtils;

import lombok.Getter;
import picocli.CommandLine.Option;

public class ActionSourceResolverMixin {
    public static abstract class AbstractActionSourceResolverMixin  {
        public abstract String getSource();

        public List<ActionSource> getActionSources(String type) {
            var source = getSource();
            return StringUtils.isBlank(source)
                    ? ActionSource.defaultActionSources(type)
                    : ActionSource.externalActionSources(source);
        }
    }
    
    public static class OptionalOption extends AbstractActionSourceResolverMixin {
        @Option(names={"--from-zip", "-z"}, required = false, descriptionKey = "fcli.action.resolver.from-zip")
        @Getter private String source;
    }
}
