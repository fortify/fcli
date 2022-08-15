package com.fortify.cli.common.picocli.option;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be applied to annotations to specify an {@link IOptionTargetNameProvider} 
 * implementation. This allows individual annotations to specify how to retrieve a corresponding
 * target (property) name for the option on which the annotation has been declared. 
 * @author rsenden
 *
 */
@Retention(RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface OptionTargetNameProvider {
	Class<? extends IOptionTargetNameProvider> value();	
}
