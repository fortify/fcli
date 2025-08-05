/**
 * 
 */
package com.fortify.cli.common.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fortify.cli.common.action.schema.annotations.MethodDescriptor;
import com.fortify.cli.common.action.schema.annotations.ParamDescriptor;
import com.fortify.cli.common.action.schema.annotations.ReturnDescriptor;

/**
 * 
 */
public class StringHelper {
	
	@MethodDescriptor("Indents each line of the given string by prefixing it with the specified indent String. "
			+ "If the input string is `null`, this method returns `null`.")
	public static final @ReturnDescriptor("the indented string where every line is prefixed by indent String, "
			+ "or `null` if input String is `null`") String indent(
					@ParamDescriptor("the string to indent; may be `null`") String str,
					@ParamDescriptor("the string to use as the indent prefix for each line") String indentStr) {
		if (str == null) {
			return null;
		}
		return Stream.of(str.split("\n")).collect(Collectors.joining("\n" + indentStr, indentStr, ""));
	}
}
