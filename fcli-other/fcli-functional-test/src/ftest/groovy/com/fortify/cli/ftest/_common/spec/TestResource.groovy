package com.fortify.cli.ftest._common.spec;

import static java.lang.annotation.ElementType.FIELD
import static java.lang.annotation.RetentionPolicy.RUNTIME

import java.lang.annotation.Retention
import java.lang.annotation.Target

import org.spockframework.runtime.extension.ExtensionAnnotation

import com.fortify.cli.ftest._common.extension.TestResourceExtension

@Target([FIELD])
@Retention(RUNTIME)
@interface TestResource {
    String value()
}