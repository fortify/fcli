/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.tool.env.cli.cmd;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.tool.env.helper.ToolEnvContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "expr")
public final class ToolEnvExprCommand extends AbstractToolEnvCommand {
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final TemplateParserContext TEMPLATE_CONTEXT = new TemplateParserContext("{", "}");
    private static final Map<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();

    @DisableTest({TestType.MULTI_OPT_PLURAL_NAME, TestType.MULTI_OPT_SPLIT, TestType.OPT_ARITY_VARIABLE})
    @Option(names = "--expr", required = true, arity = "1..*")
    private List<String> expressions;

    @Option(names = "--join", defaultValue = "\\n")
    private String joiner;

    @Option(names = "--write-mode", defaultValue = "append")
    private WriteMode writeMode;

    @Mixin private CommonOptionMixins.OptionalFile fileMixin;

    @Override
    protected void process(List<ToolEnvContext> contexts) {
        if (expressions == null || expressions.isEmpty()) {
            throw new FcliSimpleException("At least one --expr must be specified");
        }
        String separator = decodeEscapes(joiner);
        List<String> outputs = new ArrayList<>();
        for (String expr : expressions) {
            Expression compiled = parseExpression(expr);
            List<String> values = contexts.stream()
                    .map(context -> renderExpression(compiled, expr, context.model()))
                    .filter(StringUtils::isNotBlank)
                    .toList();
            if (!values.isEmpty()) {
                outputs.add(separator == null ? String.join("", values) : String.join(separator, values));
            }
        }
        if (outputs.isEmpty()) {
            return;
        }
        File target = fileMixin.getFile();
        if (target == null) {
            outputs.forEach(System.out::println);
        } else {
            write(outputs, target.toPath());
        }
    }

    private static Expression parseExpression(String template) {
        return EXPRESSION_CACHE.computeIfAbsent(template, key -> {
            try {
                return PARSER.parseExpression(decodeEscapes(key), TEMPLATE_CONTEXT);
            } catch (Exception e) {
                throw new FcliSimpleException(String.format("Unable to parse template expression: %s", key), e);
            }
        });
    }

    private static String renderExpression(Expression expression, String source, ObjectNode model) {
        try {
            return Optional.ofNullable(JsonHelper.evaluateSpelExpression(model, expression, String.class)).orElse("");
        } catch (RuntimeException e) {
            throw new FcliSimpleException(String.format("Error evaluating template expression: %s", source), e);
        }
    }

    private static String decodeEscapes(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\t", "\t")
                .replace("\\b", "\b")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\f", "\f");
    }

    private void write(List<String> outputs, Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String content = String.join(System.lineSeparator(), outputs) + System.lineSeparator();
            EnumSet<StandardOpenOption> options = EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            if (writeMode == WriteMode.overwrite) {
                options.add(StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                options.add(StandardOpenOption.APPEND);
            }
            Files.writeString(path, content, StandardCharsets.UTF_8, options.toArray(new StandardOpenOption[0]));
        } catch (IOException e) {
            throw new FcliTechnicalException(String.format("Error writing expression output to %s", path), e);
        }
    }

    private enum WriteMode {
        append,
        overwrite
    }
}
