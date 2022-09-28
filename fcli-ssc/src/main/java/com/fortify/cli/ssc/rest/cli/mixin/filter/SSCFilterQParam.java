package com.fortify.cli.ssc.rest.cli.mixin.filter;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import com.fortify.cli.common.output.cli.mixin.filter.DefaultOptionTargetNameProvider;
import com.fortify.cli.common.output.cli.mixin.filter.OptionTargetName;
import com.fortify.cli.common.output.cli.mixin.filter.OptionTargetNameProvider;
import com.fortify.cli.ssc.rest.cli.mixin.filter.SSCFilterQParam.SSCFilterQParamOptionTargetNameProvider;

/**
 * This annotation allows for filtering the output, based on input property names.
 * By default, this annotation will use the name of the instance field on which this
 * annotation is declared as the JSON property name on which to filter. The 
 * {@link OptionTargetName} annotation can be used to specify an alternative 
 * JSON property name.
 * @author rsenden
 *
 */
@Retention(RUNTIME)
@Target(FIELD)
@OptionTargetNameProvider(SSCFilterQParamOptionTargetNameProvider.class)
public @interface SSCFilterQParam {
    String value() default "";
    
    public static final class SSCFilterQParamOptionTargetNameProvider extends DefaultOptionTargetNameProvider {
        @Override
        public String getOptionTargetName(Field field) {
            SSCFilterQParam annotation = field.getAnnotation(SSCFilterQParam.class);
            return annotation.value().isBlank() 
                    ? super.getOptionTargetName(field)
                    : annotation.value();
        }
    }
}
