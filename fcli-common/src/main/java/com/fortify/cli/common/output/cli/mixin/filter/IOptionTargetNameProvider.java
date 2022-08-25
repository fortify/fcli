package com.fortify.cli.common.output.cli.mixin.filter;

import java.lang.reflect.Field;

import picocli.CommandLine.Model.OptionSpec;

/**
 * This interface provides a {@link #getOptionTargetName(Field)} method 
 * that allows implementing classes to return some target name for the
 * given {@link OptionSpec}
 * 
 * @author rsenden
 *
 */
public interface IOptionTargetNameProvider {
	String getOptionTargetName(OptionSpec optionSpec);
}
