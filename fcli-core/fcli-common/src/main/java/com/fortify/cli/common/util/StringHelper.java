/**
 * 
 */
package com.fortify.cli.common.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 
 */
public class StringHelper {
	public static final String indent(String str, String indentStr) {
		if (str == null) {
			return null;
		}
		return Stream.of(str.split("\n")).collect(Collectors.joining("\n" + indentStr, indentStr, ""));
	}
}
