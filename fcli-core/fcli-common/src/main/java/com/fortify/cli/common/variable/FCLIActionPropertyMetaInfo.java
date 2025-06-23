/**
 * 
 */
package com.fortify.cli.common.variable;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This is the metadata annotation created for holding the content of FCLI Action fields
 */
@Retention(RUNTIME)
@Target(ElementType.FIELD)
public @interface FCLIActionPropertyMetaInfo {
	String fieldName();
	String fieldDesc();
}
