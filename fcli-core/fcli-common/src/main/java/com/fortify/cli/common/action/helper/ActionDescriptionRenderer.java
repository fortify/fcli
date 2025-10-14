/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.common.action.helper;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.IConfigurableSpelEvaluator;
import com.fortify.cli.common.spel.SpelEvaluator;
import com.fortify.cli.common.spel.SpelHelper;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionDescriptionRenderer {
    private final IConfigurableSpelEvaluator spelEvaluator;
    private final ObjectNode input;
    
    public static final ActionDescriptionRenderer create(ActionDescriptionRendererType type) {
        var spelEvaluator = SpelEvaluator.JSON_GENERIC.copy()
                .configure(ActionDescriptionRenderer::configureContext);
        return new ActionDescriptionRenderer(spelEvaluator, type.getInput());
    }
    
    public final String render(TemplateExpression descriptionExpression) {
        return insertControlCharacters(spelEvaluator.evaluate(descriptionExpression, input, String.class));
    }
    
    private static final String insertControlCharacters(String s) {
        return s.replaceAll("\\\\t", "\t")
                .replaceAll("\\\\b", "\b")
                .replaceAll("\\\\n", "\n")
                .replaceAll("\\\\r", "\r")
                .replaceAll("\\\\f", "\f");
    }
    
    private static final void configureContext(SimpleEvaluationContext ctx) {
        SpelHelper.registerFunctions(ctx, ActionDescriptionRendererSpelFunctions.class);
    }
    
    @RequiredArgsConstructor
    public static enum ActionDescriptionRendererType {
        ASCIIDOC(ActionDescriptionRendererType::supplyForAsciiDoc), 
        PLAINTEXT(ActionDescriptionRendererType::supplyForPlainText);
        
        private static final String isAsciiDocVarName = "isAsciiDoc";
        private static final String isPlainTextVarName = "isPlainText";
        private final Supplier<ObjectNode> inputSupplier;
        
        public final ObjectNode getInput() {
            return inputSupplier.get();
        }
        
        private static final ObjectNode supplyForAsciiDoc() {
            return JsonHelper.getObjectMapper().createObjectNode()
                    .put(isAsciiDocVarName, true)
                    .put(isPlainTextVarName, false);
        }
        
        private static final ObjectNode supplyForPlainText() {
            return JsonHelper.getObjectMapper().createObjectNode()
                    .put(isAsciiDocVarName, false)
                    .put(isPlainTextVarName, true);
        }
    }
    
    @Reflectable
    public static final class ActionDescriptionRendererSpelFunctions {
        public static final String include(String resourcePath) {
            var is = ActionDescriptionRendererSpelFunctions.class.getResourceAsStream(resourcePath);
            if ( is==null ) {
                throw new FcliBugException(String.format("Class path resource %s not found", resourcePath));
            }
            try {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch ( Exception e ) {
                throw new FcliTechnicalException("Unable to load classpath resource "+resourcePath, e);
            }
        }
    }
}
