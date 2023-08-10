package com.fortify.cli.ftest._common;

import org.spockframework.runtime.extension.ExtensionAnnotation

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtensionAnnotation(StepWiseExceptExtension.class)
public @interface StepWiseExcept {
    String except() default ""
    
}