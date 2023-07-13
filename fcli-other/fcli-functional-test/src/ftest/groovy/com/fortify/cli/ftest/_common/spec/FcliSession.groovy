package com.fortify.cli.ftest._common.spec;

import static java.lang.annotation.ElementType.METHOD
import static java.lang.annotation.ElementType.TYPE
import static java.lang.annotation.RetentionPolicy.RUNTIME

import java.lang.annotation.Retention
import java.lang.annotation.Target

import org.spockframework.runtime.extension.ExtensionAnnotation

import com.fortify.cli.ftest._common.extension.FcliSessionExtension

@Target([METHOD, TYPE])
@Retention(RUNTIME)
@ExtensionAnnotation(FcliSessionExtension.class)
@interface FcliSession {
    FcliSessionType value()
}