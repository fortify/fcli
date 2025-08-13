/**
 * 
 */
package com.fortify.cli.common.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fortify.cli.common.spring.expression.fn.descriptor.annotation.SpelFunctionDescription;
import com.fortify.cli.common.spring.expression.fn.descriptor.annotation.SpelFunctionParamDescription;
import com.fortify.cli.common.spring.expression.fn.descriptor.annotation.SpelFunctionReturnDescription;

/**
 * 
 */
public class StringHelper {
	
	@SpelFunctionDescription("Indents each line of the given string by prefixing it with the specified indent String. "
			+ "If the input string is `null`, this method returns `null`.")
	public static final @SpelFunctionReturnDescription("the indented string where every line is prefixed by indent String, "
			+ "or `null` if input String is `null`") String indent(
					@SpelFunctionParamDescription("the string to indent; may be `null`") String str,
					@SpelFunctionParamDescription("the string to use as the indent prefix for each line") String indentStr) {
		if (str == null) {
			return null;
		}
		return Stream.of(str.split("\n")).collect(Collectors.joining("\n" + indentStr, indentStr, ""));
	}
}
