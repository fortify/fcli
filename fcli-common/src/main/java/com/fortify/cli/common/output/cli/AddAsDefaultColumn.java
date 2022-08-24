package com.fortify.cli.common.output.cli;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.fortify.cli.common.cli.option.DefaultOptionTargetNameProvider;
import com.fortify.cli.common.cli.option.OptionTargetName;
import com.fortify.cli.common.cli.option.OptionTargetNameProvider;

/**
 * This annotation can be used to add a default output column
 * for the current (filtering) field. By default, this annotation 
 * will use the name of the instance field on which this annotation 
 * is declared as the JSON property name from which to retrieve column
 * values. The {@link OptionTargetName} annotation can be used to 
 * specify an alternative JSON property name.
 * @author rsenden
 *
 */
@Retention(RUNTIME)
@Target(FIELD)
@OptionTargetNameProvider(DefaultOptionTargetNameProvider.class)
public @interface AddAsDefaultColumn {}
