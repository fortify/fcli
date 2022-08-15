package com.fortify.cli.common.picocli.option;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

@RequiredArgsConstructor
public class OptionAnnotationHelper {
	private final CommandSpec commandSpec;
	
	public Stream<OptionSpec> optionsWithAnnotationStream(Class<? extends Annotation> annotationClazz) {
		return commandSpec.options().stream().filter(o->hasAnnotation(o, annotationClazz));
	}
	
	public static final Field getOptionField(OptionSpec optionSpec) {
		return (Field)optionSpec.userObject();
	}
	
	public static final <T extends Annotation> T getAnnotation(OptionSpec optionSpec, Class<T> annotationClazz) {
		return getOptionField(optionSpec).getAnnotation(annotationClazz);
	}
	
	public static final boolean hasAnnotation(OptionSpec optionSpec, Class<? extends Annotation> annotationClazz) {
		return getAnnotation(optionSpec, annotationClazz) != null;
	}
	
	/**
	 * Get the target name for the given annotation class, for the given option.
	 * @param optionSpec
	 * @param annotationClazz
	 * @return
	 */
	public static final String getOptionTargetName(OptionSpec optionSpec, Class<? extends Annotation> annotationClazz) {
		Annotation annotation = getAnnotation(optionSpec, annotationClazz);
		if ( annotation!=null ) {
			OptionTargetNameProvider optionTargetNameProvider = annotation.annotationType().getAnnotation(OptionTargetNameProvider.class);
			if ( optionTargetNameProvider!=null ) {
				try {
					return optionTargetNameProvider.value().getDeclaredConstructor().newInstance().getOptionTargetName(optionSpec);
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new RuntimeException("Error getting option target name for option "+optionSpec.longestName());
				}
			}
		}
		return getOptionField(optionSpec).getName();
	}
}
