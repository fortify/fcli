package com.fortify.cli.ftest._common.spec;

import static java.lang.annotation.ElementType.FIELD
import static java.lang.annotation.RetentionPolicy.RUNTIME

import java.lang.annotation.Retention
import java.lang.annotation.Target

import com.fortify.cli.ftest._common.util.WorkDirHelper

@Target([FIELD])
@Retention(RUNTIME)
@interface Global {
    Class<? extends IGlobalValueSupplier> value()
    
    public static interface IGlobalValueSupplier extends Closeable, AutoCloseable {
        public Object getValue(WorkDirHelper workDirHelper);
    }
}