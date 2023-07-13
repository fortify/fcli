package com.fortify.cli.ftest._common.spec;

import static java.lang.annotation.ElementType.TYPE
import static java.lang.annotation.RetentionPolicy.RUNTIME

import java.lang.annotation.Retention
import java.lang.annotation.Target

@Target(TYPE)
@Retention(RUNTIME)
@interface Prefix {
    String value()
}