package com.fortify.cli.common.output.cli.mixin.spi.output;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface MinusVariableDefinition {
    String name();
    String options();
}
