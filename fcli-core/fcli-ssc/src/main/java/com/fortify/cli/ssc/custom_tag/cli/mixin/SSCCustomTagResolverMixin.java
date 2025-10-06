package com.fortify.cli.ssc.custom_tag.cli.mixin;

import org.apache.commons.lang3.StringUtils;
import com.fortify.cli.ssc.custom_tag.helper.SSCCustomTagDescriptor;
import com.fortify.cli.ssc.custom_tag.helper.SSCCustomTagHelper;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import com.fortify.cli.common.cli.util.EnvSuffix;

public class SSCCustomTagResolverMixin {
    private static abstract class AbstractSSCCustomTagResolverMixin {
        public abstract String getCustomTagNameOrGuid();
        public SSCCustomTagDescriptor getCustomTagDescriptor(UnirestInstance unirest) {
            String customTagNameOrGuid = getCustomTagNameOrGuid();
            return StringUtils.isBlank(customTagNameOrGuid)
                ? null
                : new SSCCustomTagHelper(unirest).getDescriptorByCustomTagSpec(customTagNameOrGuid, true);
        }
    }
    public static class OptionalOption extends AbstractSSCCustomTagResolverMixin {
        @Option(names = {"--custom-tag"}, required = false, descriptionKey = "custom-tag.resolver.nameOrGuid")
        @Getter private String customTagNameOrGuid;
    }
    public static class PositionalParameterSingle extends AbstractSSCCustomTagResolverMixin {
        @EnvSuffix("CUSTOM_TAG")
        @Parameters(index = "0", arity = "1", descriptionKey = "custom-tag.resolver.nameOrGuid")
        @Getter private String customTagNameOrGuid;
    }
}