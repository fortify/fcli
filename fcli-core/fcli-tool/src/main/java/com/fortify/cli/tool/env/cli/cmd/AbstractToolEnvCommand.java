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
package com.fortify.cli.tool.env.cli.cmd;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstallationsResolver;
import com.fortify.cli.tool._common.helper.ToolInstallationsResolver.ToolInstallationRecord;

import picocli.CommandLine.Option;

/**
 * Common base class for {@code fcli tool env <type>} commands. It resolves the
 * requested tool installations and provides helper utilities for evaluating
 * template expressions against installation metadata.
 */
public abstract class AbstractToolEnvCommand extends AbstractRunnableCommand {
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final TemplateParserContext TEMPLATE_CONTEXT = new TemplateParserContext("{", "}");
    private static final Map<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();

    @Option(names = "--tools", split = ",", descriptionKey = "fcli.tool.env.tools")
    private List<String> toolSelectors;

    @Override
    public final Integer call() {
        List<ToolEnvContext> contexts = resolveContexts();
        if (contexts.isEmpty()) {
            throw new FcliSimpleException("No matching tool installations detected");
        }
        process(contexts);
        return 0;
    }

    protected abstract void process(List<ToolEnvContext> contexts);

    protected final EnvTemplate compileTemplate(String template) {
        if (template == null) {
            return null;
        }
        return new EnvTemplate(template, parseExpression(template));
    }

    protected final String renderTemplate(String template, ToolEnvContext context) {
        if (StringUtils.isBlank(template)) {
            return "";
        }
        return renderExpression(parseExpression(template), template, context.model());
    }

    protected final String renderTemplate(EnvTemplate template, ToolEnvContext context) {
        if (template == null) {
            return "";
        }
        return renderExpression(template.expression(), template.source(), context.model());
    }

    private Expression parseExpression(String template) {
        return EXPRESSION_CACHE.computeIfAbsent(template, key -> {
            try {
                return PARSER.parseExpression(decodeEscapes(key), TEMPLATE_CONTEXT);
            } catch (Exception e) {
                throw new FcliSimpleException(String.format("Unable to parse template expression: %s", key), e);
            }
        });
    }

    private String renderExpression(Expression expression, String source, ObjectNode model) {
        try {
            return Optional.ofNullable(JsonHelper.evaluateSpelExpression(model, expression, String.class)).orElse("");
        } catch (RuntimeException e) {
            throw new FcliSimpleException(String.format("Error evaluating template expression: %s", source), e);
        }
    }

    private List<ToolEnvContext> resolveContexts() {
        if (toolSelectors == null || toolSelectors.isEmpty()) {
            return resolveDefaultContexts();
        }
        List<ToolEnvContext> contexts = new ArrayList<>();
        for (String selector : toolSelectors) {
            contexts.addAll(resolveSelector(selector));
        }
        return contexts;
    }

    private List<ToolEnvContext> resolveDefaultContexts() {
        return Arrays.stream(Tool.values())
                .map(ToolInstallationsResolver::resolve)
                .map(installations -> installations.defaultInstallation()
                        .map(record -> toContext(installations.tool(), record)))
                .flatMap(Optional::stream)
                .toList();
    }

    private List<ToolEnvContext> resolveSelector(String selector) {
        String normalized = StringUtils.defaultString(selector).trim();
        if (normalized.isEmpty()) {
            throw new FcliSimpleException("Tool selector may not be blank");
        }
        ToolSelector parsed = parseSelector(normalized);
        var installations = ToolInstallationsResolver.resolve(parsed.tool());
        List<ToolEnvContext> contexts = selectContexts(parsed, installations);
        if (contexts.isEmpty()) {
            throw new FcliSimpleException(String.format(
                    "No tool installation found for %s", formatSelector(parsed)));
        }
        return contexts;
    }

    private List<ToolEnvContext> selectContexts(ToolSelector selector, ToolInstallationsResolver.ToolInstallations installations) {
        if (StringUtils.isBlank(selector.versionSpec())) {
            return installations.defaultInstallation()
                    .map(record -> List.of(toContext(selector.tool(), record)))
                    .orElse(List.of());
        }
        String versionSpec = selector.versionSpec();
        if ("default".equalsIgnoreCase(versionSpec) || "auto".equalsIgnoreCase(versionSpec)) {
            return installations.defaultInstallation()
                .map(record -> List.of(toContext(selector.tool(), record)))
                .orElse(List.of());
        }
        List<ToolEnvContext> contexts;
        if ("*".equals(versionSpec) || "all".equalsIgnoreCase(versionSpec)) {
            contexts = installations.installedStream()
                    .map(record -> toContext(selector.tool(), record))
                    .toList();
        } else {
            contexts = installations.installedStream()
                    .filter(record -> matchesVersion(record, versionSpec))
                    .map(record -> toContext(selector.tool(), record))
                    .toList();
        }
        return contexts;
    }

    private boolean matchesVersion(ToolInstallationRecord record, String versionSpec) {
        String target = StringUtils.trimToEmpty(versionSpec);
        if (target.isEmpty()) {
            return false;
        }
        var versionDescriptor = record.versionDescriptor();
        String version = versionDescriptor.getVersion();
        if (equalsIgnoreCase(version, target) || Arrays.stream(versionDescriptor.getAliases())
                .anyMatch(alias -> equalsIgnoreCase(alias, target))) {
            return true;
        }
        String normalizedTarget = removeVersionPrefix(target);
        String normalizedVersion = removeVersionPrefix(version);
        return matchesVersionParts(normalizedVersion, normalizedTarget);
    }

    private static boolean matchesVersionParts(String version, String spec) {
        String baseVersion = StringUtils.substringBefore(version, "-");
        String[] versionParts = baseVersion.split("\\.");
        String[] specParts = spec.split("\\.");
        for (int i = 0; i < specParts.length; i++) {
            String part = specParts[i];
            if ("*".equals(part)) {
                continue;
            }
            String versionPart = i < versionParts.length ? versionParts[i] : "";
            if (!equalsIgnoreCase(versionPart, part)) {
                return false;
            }
        }
        return true;
    }

    private static boolean equalsIgnoreCase(String first, String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }

    private static String removeVersionPrefix(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        return first == 'v' || first == 'V' ? value.substring(1) : value;
    }

    private ToolEnvContext toContext(Tool tool, ToolInstallationRecord record) {
        ToolInstallationDescriptor descriptor = record.installationDescriptor();
        if (descriptor == null) {
            throw new FcliSimpleException(String.format(
                    "Tool %s version %s is not installed", tool.getToolName(),
                    record.versionDescriptor().getVersion()));
        }
        ObjectNode model = JsonHelper.getObjectMapper().createObjectNode();
        model.put("toolName", tool.getToolName());
        model.put("installDir", descriptor.getInstallDir());
        model.put("binDir", descriptor.getBinDir());
        model.put("globalBinDir", descriptor.getGlobalBinDir());
        model.put("cmd", computeCommand(tool, descriptor));
        model.put("version", record.versionDescriptor().getVersion());
        model.put("defaultEnvPrefix", tool.getDefaultEnvPrefix());
        model.put("isDefault", record.isDefault());
        ArrayNode aliases = model.putArray("aliases");
        Arrays.stream(record.versionDescriptor().getAliases()).forEach(aliases::add);
        return new ToolEnvContext(tool, record, descriptor, model);
    }

    private static String computeCommand(Tool tool, ToolInstallationDescriptor descriptor) {
        String binDir = descriptor.getBinDir();
        String binaryName = tool.getDefaultBinaryName();
        if (StringUtils.isBlank(binDir) || StringUtils.isBlank(binaryName)) {
            return null;
        }
        Path candidate = Path.of(binDir, binaryName).normalize();
        if (Files.exists(candidate)) {
            return candidate.toString();
        }
        return null;
    }

    private ToolSelector parseSelector(String selector) {
        String[] parts = selector.split(":", 2);
        String toolName = parts[0].trim();
        if (toolName.isEmpty()) {
            throw new FcliSimpleException("Tool name in selector may not be blank");
        }
        Tool tool = resolveTool(toolName);
        String versionSpec = parts.length > 1 ? StringUtils.trimToNull(parts[1]) : null;
        return new ToolSelector(tool, versionSpec);
    }

    private Tool resolveTool(String toolName) {
        Tool exact = Tool.getByToolNameOrAlias(toolName);
        if (exact != null) {
            return exact;
        }
        return Arrays.stream(Tool.values())
                .filter(tool -> tool.getToolName().equalsIgnoreCase(toolName))
                .findFirst()
                .orElseThrow(() -> new FcliSimpleException(String.format("Unknown tool: %s", toolName)));
    }

    protected static String decodeEscapes(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\t", "\t")
                .replace("\\b", "\b")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\f", "\f");
    }

    private static String formatSelector(ToolSelector selector) {
        if (selector.versionSpec() == null) {
            return selector.tool().getToolName();
        }
        return selector.tool().getToolName() + ":" + selector.versionSpec();
    }

    protected record EnvTemplate(String source, Expression expression) {}

    protected record ToolEnvContext(
            Tool tool,
            ToolInstallationRecord record,
            ToolInstallationDescriptor descriptor,
            ObjectNode model) {
        public String installDir() {
            return descriptor.getInstallDir();
        }
        public String binDir() {
            return descriptor.getBinDir();
        }
        public String globalBinDir() {
            return descriptor.getGlobalBinDir();
        }
        public String version() {
            return record.versionDescriptor().getVersion();
        }
        public String envPrefix() {
            return tool.getDefaultEnvPrefix();
        }
        public boolean isDefault() {
            return record.isDefault();
        }
        public String cmd() {
            var cmdNode = model.get("cmd");
            return cmdNode == null || cmdNode.isNull() ? null : cmdNode.asText();
        }
    }

    private record ToolSelector(Tool tool, String versionSpec) {}

    protected static void addIfNotBlank(List<String> target, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.add(value);
        }
    }

    protected void writeLines(List<String> lines, File destination, String description) {
        writeLines(lines, destination, description, true);
    }

    protected void writeLines(List<String> lines, File destination, String description, boolean append) {
        if (lines.isEmpty()) {
            return;
        }
        if (destination == null) {
            lines.forEach(System.out::println);
            return;
        }
        writeLinesToPath(lines, destination.toPath(), description, append);
    }

    protected void writeLinesToPath(List<String> lines, Path destination, String description) {
        writeLinesToPath(lines, destination, description, true);
    }

    protected void writeLinesToPath(List<String> lines, Path destination, String description, boolean append) {
        if (lines.isEmpty()) {
            return;
        }
        try {
            ensureParentExists(destination);
            Files.writeString(destination,
                    joinWithTrailingNewline(lines),
                    StandardCharsets.UTF_8,
                    fileOptions(append));
        } catch (IOException e) {
            throw new FcliTechnicalException(String.format("Error writing %s to %s", description, destination), e);
        }
    }

    private static void ensureParentExists(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static StandardOpenOption[] fileOptions(boolean append) {
        return append
                ? new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND }
                : new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING };
    }

    private static String joinWithTrailingNewline(List<String> lines) {
        String joined = String.join(System.lineSeparator(), lines);
        return joined.endsWith(System.lineSeparator())
                ? joined
                : joined + System.lineSeparator();
    }
}
