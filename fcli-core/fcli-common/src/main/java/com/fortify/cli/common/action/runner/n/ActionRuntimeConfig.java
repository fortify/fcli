/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.action.runner.n;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.action.model.ActionParameter;
import com.fortify.cli.common.action.runner.ActionRunner.IActionRequestHelper;
import com.fortify.cli.common.action.runner.ActionSpelFunctions;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spring.expression.IConfigurableSpelEvaluator;
import com.fortify.cli.common.spring.expression.SpelEvaluator;
import com.fortify.cli.common.spring.expression.SpelHelper;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Model.OptionSpec;

@Builder
public class ActionRuntimeConfig {
    /** Type-based OptionSpec configurers */
    @Singular private final Map<String, BiConsumer<OptionSpec.Builder, ActionParameter>> optionSpecTypeConfigurers;
    /** Names request helpers */
    @Singular private final Map<String, IActionRequestHelper> requestHelpers;
    /** SpEL context configurers */
    private final Consumer<SimpleEvaluationContext> spelContextConfigurer;
    /** Action run command as string */
    @Getter private final String actionRunCommand;
    
    public final BiConsumer<OptionSpec.Builder, ActionParameter> getOptionSpecTypeConfigurer(String type) {
        return optionSpecTypeConfigurers.get(type);
    }
    
    public final IActionRequestHelper getRequestHelper(String name) {
        return requestHelpers.get(name);
    }
    
    public final IConfigurableSpelEvaluator createSpelEvaluator() {
        var result = SpelEvaluator.JSON_GENERIC.copy().configure(c->SpelHelper.registerFunctions(c, ActionSpelFunctions.class));
        if ( spelContextConfigurer!=null ) { result.configure(spelContextConfigurer); }
        return result;
    }
    
    public static class ActionRuntimeConfigBuilder {
        public ActionRuntimeConfigBuilder() {
            addDefaultOptionSpecTypeConfigurers();
        }

        private final void addDefaultOptionSpecTypeConfigurers() {
            optionSpecTypeConfigurer("string",  (b,p)->b.arity("1").type(String.class));
            optionSpecTypeConfigurer("boolean", (b,p)->b.arity("0..1").type(Boolean.class).defaultValue("false"));
            optionSpecTypeConfigurer("int",     (b,p)->b.arity("1").type(Integer.class));
            optionSpecTypeConfigurer("long",    (b,p)->b.arity("1").type(Long.class));
            optionSpecTypeConfigurer("double",  (b,p)->b.arity("1").type(Double.class));
            optionSpecTypeConfigurer("float",   (b,p)->b.arity("1").type(Float.class));
            optionSpecTypeConfigurer("array",   (b,p)->b.arity("1").type(ArrayNode.class).converters(new ArrayNodeConverter()));
        }
        
        private static final class ArrayNodeConverter implements ITypeConverter<ArrayNode> {
            @Override
            public ArrayNode convert(String value) throws Exception {
                return StringUtils.isBlank(value)
                    ? JsonHelper.toArrayNode(new String[] {}) 
                    : JsonHelper.toArrayNode(value.split(","));
            }
        }
    }
}
