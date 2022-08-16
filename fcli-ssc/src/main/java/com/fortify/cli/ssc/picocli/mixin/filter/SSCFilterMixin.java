package com.fortify.cli.ssc.picocli.mixin.filter;

import java.util.stream.Collectors;

import com.fortify.cli.common.picocli.option.OptionAnnotationHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
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
		return String.format("%s:\"%s\"", OptionAnnotationHelper.getOptionTargetName(optionspec, SSCFilterQParam.class), getOptionValue(optionspec));
	}
	
	private final boolean hasOptionValue(OptionSpec optionSpec) {
		return StringUtils.isNotEmpty(getOptionValue(optionSpec));
	}
}
