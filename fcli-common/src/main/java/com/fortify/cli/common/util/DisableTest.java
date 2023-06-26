package com.fortify.cli.common.util;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

/**
 * This annotation allows for disabling particular tests on individual
 * commands and options, allowing tests defined in FortifyCLITest to
 * pass. This annotation should only be used if there is a valid reason
 * for disabling a test. For example, if a multi-value option has a valid
 * (plural) name that is not properly handled by FortifyCLITest, then this
 * annotation can be used to disable that test for that particular option.
 * @author rsenden
 *
 */
@Qualifier
@Retention(RUNTIME)
@Target({TYPE, FIELD})
@Inherited
public @interface DisableTest {
    TestType[] value();
    public enum TestType {
        // Check commands have standard options like --help
        CMD_STD_OPTS,
        
        // Check command name and aliases are in kebab-case
        CMD_NAME,
        
        // Check for proper command usage header
        CMD_USAGE_HEADER,
        
        // Check default table options present in resource bundle
        CMD_DEFAULT_TABLE_OPTIONS_PRESENT,
        
        // Check maximum command depth
        CMD_DEPTH,
        
        // Check option name format
        OPT_NAME_FORMAT,
        
        // Check option name order (short option name(s) first)
        // Currently not implemented
        //OPT_NAME_ORDER,
        
        // Check maximum short option name count
        OPT_SHORT_NAME_COUNT,
        
        // Check short option names are in allowed format 
        OPT_SHORT_NAME,
        
        // Check maximum long option name count
        OPT_LONG_NAME_COUNT,
        
        // Check long option names are in allowed format
        OPT_LONG_NAME,
        
        // Check multi-value option names are in plural form
        MULTI_OPT_PLURAL_NAME,
        
        // Check multi-value options have a split=... attribute 
        MULTI_OPT_SPLIT,
        
        // Check that no variable arity is used for options
        OPT_ARITY_VARIABLE,
        
        // Check that arity for boolean option is either 0 or 1
        OPT_ARITY_BOOL,
        
        // Check that arity for interactive option is 0..1
        OPT_ARITY_INTERACTIVE,
        
        // Check that arity is not specified for any other options
        OPT_ARITY_PRESENT,
        
        // Check option doesn't have an empty description
        OPT_EMPTY_DESCRIPTION,
        
        // Check that mixins don't have @Spec(MIXEE)
        INJECT_MIXEE 
    } 
}
