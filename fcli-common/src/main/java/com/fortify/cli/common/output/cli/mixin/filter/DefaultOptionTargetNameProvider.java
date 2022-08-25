package com.fortify.cli.common.output.cli.mixin.filter;

import java.lang.reflect.Field;

import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Option;

/**
 * This {@link IOptionTargetNameProvider} implementation returns the value
 * of the {@link OptionTargetName} annotation if available, and otherwise
 * simply returns the name of the field on which the given {@link Option}
 * annotation has been declared. 
 * @author rsenden
 *
 */
public class DefaultOptionTargetNameProvider implements IOptionTargetNameProvider {
    @Override
    public String getOptionTargetName(OptionSpec optionSpec) {
        return getOptionTargetName((Field)optionSpec.userObject());
    }
    
    public String getOptionTargetName(Field field) {
        OptionTargetName annotation = field.getAnnotation(OptionTargetName.class);
        if ( annotation!=null && annotation.value()!=null) {
            return annotation.value();
        }
        return field.getName();
    }
}
