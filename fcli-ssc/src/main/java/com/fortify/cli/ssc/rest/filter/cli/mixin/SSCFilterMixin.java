package com.fortify.cli.ssc.rest.filter.cli.mixin;

import java.util.stream.Collectors;

import com.fortify.cli.common.output.cli.mixin.filter.OptionAnnotationHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.GetRequest;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@ReflectiveAccess
public class SSCFilterMixin {
    private OptionAnnotationHelper optionAnnotationHelper;

    @Option(names={"--dummy"}, hidden = true) private String dummy;
    
    @Spec(Spec.Target.MIXEE)
    public void setMixee(CommandSpec mixee) {
        this.optionAnnotationHelper = new OptionAnnotationHelper(mixee);
    }
    
    public GetRequest addFilterParams(GetRequest getRequest) {
        String qParamValue = optionAnnotationHelper.optionsWithAnnotationStream(SSCFilterQParam.class)
            .filter(this::hasOptionValue)
            .map(this::getQParamValue)
            .collect(Collectors.joining("+and+"));
        return getRequest.queryString("q", qParamValue);
    }
    
    private final <T> T getOptionValue(OptionSpec optionSpec) {
        try {
            return optionSpec.getValue();
        } catch (CommandLine.PicocliException e){
            // Picocli may throw an exception when calling getValue() on an option contained in a non-initialized arggroup.
            return null;
        }
    }
    
    private String getQParamValue(OptionSpec optionspec) {
        String targetName = OptionAnnotationHelper.getOptionTargetName(optionspec, SSCFilterQParam.class);
        Object value = getOptionValue(optionspec);
        String format = value instanceof String ? "%s:\"%s\"" : "%s:%s";
        return String.format(format, targetName, value);
    }
    
    private final boolean hasOptionValue(OptionSpec optionSpec) {
        return getOptionValue(optionSpec)!=null;
    }
}
