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
package com.fortify.cli.common.cli.util;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.query.QueryExpression;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Model.ArgGroupSpec;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

/**
 * Wrapper around a picocli {@link CommandSpec} that produces reusable
 * JSON nodes and provides matching and factory helpers.
 */
@RequiredArgsConstructor
public final class CommandSpecDescriptor {
    @Getter private final CommandSpec spec;

    // Lazily initialized JSON nodes
    @Getter(lazy=true) private final ObjectNode commandSpecNode = createCommandSpecNode();
    @Getter(lazy=true) private final ObjectNode commandArgsNode = createCommandArgsNode(spec);

    public boolean matches(QueryExpression qe) {
        return qe == null || qe.matches(getCommandSpecNode());
    }

    public boolean matches(Predicate<CommandSpecDescriptor> predicate) {
        return predicate == null || predicate.test(this);
    }

    public static CommandSpecDescriptor from(CommandSpec spec) {
        return new CommandSpecDescriptor(spec);
    }

    public static CommandSpecDescriptor of(String qualifiedCommandName) {
        var cs = FcliCommandSpecHelper.getCommandSpec(qualifiedCommandName);
        return cs == null ? null : from(cs);
    }

    public static Stream<CommandSpecDescriptor> rootDescriptorStream() {
        return FcliCommandSpecHelper.rootCommandTreeStream().map(CommandSpecDescriptor::from);
    }

    private ObjectNode createCommandSpecNode() {
        var spec = this.spec;
        var hiddenParent = FcliCommandSpecHelper.hasHiddenParent(spec);
        var hiddenSelf = FcliCommandSpecHelper.isHiddenSelf(spec);
        var hidden = FcliCommandSpecHelper.isHiddenSelfOrParent(spec);
        var mcpIgnored = FcliCommandSpecHelper.isMcpIgnored(spec);

        String qualifiedName = spec.qualifiedName(" ");
        String[] nameComponents = qualifiedName.split(" ");
        boolean isRunnable = FcliCommandSpecHelper.isRunnable(spec);
        String module = nameComponents.length > 1 ? nameComponents[1] : "";
        String entity = nameComponents.length > 2 ? nameComponents[2] : "";
        String action = nameComponents.length > 3 ? nameComponents[3] : "";
        // For leaf commands at depth 3 (fcli <module> <action>), the module skips
        // the entity level, so what landed in 'entity' is actually the action verb.
        if (nameComponents.length == 3 && isRunnable) {
            action = entity;
            entity = "";
        }
        String type = switch (nameComponents.length) {
            case 1 -> "root";
            case 2 -> "module";
            default -> isRunnable ? "command" : "entity";
        };

        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("command", spec.qualifiedName(" "));
        result.put("type", type);
        result.put("module", module);
        result.put("entity", entity);
        result.put("action", action);
        result.put("hidden", hidden);
        result.put("hiddenParent", hiddenParent);
        result.put("hiddenSelf", hiddenSelf);
        result.put("mcpIgnored", mcpIgnored);
        result.put("runnable", isRunnable);
        result.put("usageHeader", normalizeNewlines(String.join("\n", spec.usageMessage().header())));
        result.put("usageDescription", normalizeNewlines(String.join("\n", spec.usageMessage().description())));
        result.set("aliases", Stream.of(spec.aliases()).map(TextNode::new).collect(JsonHelper.arrayNodeCollector()));
        result.put("aliasesString", Stream.of(spec.aliases()).collect(Collectors.joining(", ")));
        var fullAliases = computeFullAliases(spec);
        result.set("fullAliases", fullAliases.stream().map(TextNode::new).collect(JsonHelper.arrayNodeCollector()));
        result.put("fullAliasesString", String.join(", ", fullAliases));

        result.set("options", spec.optionsMap().keySet().stream().map(TextNode::new).collect(JsonHelper.arrayNodeCollector()));
        result.put("optionsString", spec.optionsMap().keySet().stream().collect(Collectors.joining(", ")));
        result.set("metadata", createMetadataNode(spec));

        return result;
    }

    private static ObjectNode createCommandArgsNode(CommandSpec spec) {
        var mapper = JsonHelper.getObjectMapper();
        ObjectNode commandArgs = mapper.createObjectNode();

        Map<OptionSpec, Boolean> requiredByOption = new HashMap<>();
        for (OptionSpec option : spec.options()) {
            requiredByOption.put(option, FcliCommandSpecHelper.isEffectivelyRequired(option));
        }

        List<ArgGroupSpec> exclusiveGroups = new ArrayList<>();
        collectExclusiveGroups(spec.argGroups(), exclusiveGroups);

        ArrayNode parameters = mapper.createArrayNode();
        for (PositionalParamSpec param : spec.positionalParameters()) {
            parameters.add(createParameterNode(param));
        }
        commandArgs.set("parameters", parameters);

        Map<String, List<OptionSpec>> optionsByHeading = new LinkedHashMap<>();
        Map<OptionSpec, ArgGroupSpec> optionToGroup = buildOptionToGroupMap(spec);

        for (OptionSpec option : spec.options()) {
            if (option.hidden()) {
                continue;
            }
            String heading = getOptionGroupHeading(option, optionToGroup);
            optionsByHeading.computeIfAbsent(heading, h -> new ArrayList<>()).add(option);
        }

        Map<String, List<String>> exclusiveWithById = buildExclusiveWithMap(exclusiveGroups);
        Set<OptionSpec> optionsInSubGroups = new LinkedHashSet<>();
        Map<String, List<ObjectNode>> exclusiveSubGroupsByGroupId = buildExclusiveSubGroups(exclusiveGroups, optionToGroup, exclusiveWithById, requiredByOption, optionsInSubGroups);

        ArrayNode optionGroups = mapper.createArrayNode();

        java.util.function.Consumer<String> addGroupByHeading = heading -> {
            List<OptionSpec> opts = optionsByHeading.get(heading);
            if (opts == null || opts.isEmpty()) {
                return;
            }
            ObjectNode groupNode = mapper.createObjectNode();
            String groupId = toGroupId(heading);
            groupNode.put("title", heading);
            groupNode.put("id", groupId);

            ArrayNode optionsArray = mapper.createArrayNode();
            for (OptionSpec opt : opts) {
                if (!optionsInSubGroups.contains(opt)) {
                    optionsArray.add(createOptionNode(opt, requiredByOption.getOrDefault(opt, false)));
                }
            }
            groupNode.set("options", optionsArray);

            ArrayNode subGroupsArray = mapper.createArrayNode();
            List<ObjectNode> subGroups = exclusiveSubGroupsByGroupId.get(groupId);
            if (subGroups != null) {
                subGroups.forEach(subGroupsArray::add);
            }
            groupNode.set("subGroups", subGroupsArray);

            optionGroups.add(groupNode);
        };

        addGroupByHeading.accept("Command Options");

        List<String> allHeadings = new ArrayList<>(optionsByHeading.keySet());
        for (String heading : allHeadings) {
            if ("Command Options".equals(heading) || "Output options".equals(heading) || "Generic fcli options".equals(heading)) {
                continue;
            }
            addGroupByHeading.accept(heading);
        }

        addGroupByHeading.accept("Output options");
        addGroupByHeading.accept("Generic fcli options");

        commandArgs.set("optionGroups", optionGroups);
        return commandArgs;
    }

    // --- Helper methods copied/adapted from the original mixin ---
    private final static Map<String, List<String>> buildExclusiveWithMap(List<ArgGroupSpec> exclusiveGroups) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (ArgGroupSpec exclusiveGroup : exclusiveGroups) {
            List<ArgGroupSpec> children = exclusiveGroup.subgroups();
            if (children.size() < 2) {
                continue;
            }
            List<String> childIds = children.stream().map(child -> toGroupId(computeGroupTitle(child))).collect(Collectors.toList());
            for (int i = 0; i < children.size(); i++) {
                String thisId = childIds.get(i);
                List<String> siblings = new ArrayList<>();
                for (int j = 0; j < children.size(); j++) {
                    if (j != i) {
                        siblings.add(childIds.get(j));
                    }
                }
                result.put(thisId, siblings);
            }
        }
        return result;
    }

    private final static Map<String, List<ObjectNode>> buildExclusiveSubGroups(
            List<ArgGroupSpec> exclusiveGroups,
            Map<OptionSpec, ArgGroupSpec> optionToGroup,
            Map<String, List<String>> exclusiveWithById,
            Map<OptionSpec, Boolean> requiredByOption,
            Set<OptionSpec> optionsInSubGroups) {
        var mapper = JsonHelper.getObjectMapper();
        Map<String, List<ObjectNode>> result = new LinkedHashMap<>();

        for (ArgGroupSpec exclusiveGroup : exclusiveGroups) {
            for (ArgGroupSpec child : exclusiveGroup.subgroups()) {
                List<OptionSpec> childOptions = collectAllOptions(child).stream().filter(o -> !o.hidden()).distinct().collect(Collectors.toList());
                if (childOptions.isEmpty()) {
                    continue;
                }

                OptionSpec firstOpt = childOptions.get(0);
                String parentHeading = getOptionGroupHeading(firstOpt, optionToGroup);
                String parentGroupId = toGroupId(parentHeading);

                String title = computeGroupTitle(child);
                String groupId = toGroupId(title);

                ObjectNode groupNode = mapper.createObjectNode();
                groupNode.put("id", groupId);
                groupNode.put("title", title);

                ArrayNode optionsArray = mapper.createArrayNode();
                for (OptionSpec opt : childOptions) {
                    optionsArray.add(createOptionNode(opt, requiredByOption.getOrDefault(opt, false)));
                    optionsInSubGroups.add(opt);
                }
                groupNode.set("options", optionsArray);

                List<String> siblings = exclusiveWithById.get(groupId);
                if (siblings != null && !siblings.isEmpty()) {
                    ArrayNode exclusiveWithArray = mapper.createArrayNode();
                    siblings.forEach(exclusiveWithArray::add);
                    groupNode.set("exclusiveWith", exclusiveWithArray);
                }

                result.computeIfAbsent(parentGroupId, k -> new ArrayList<>()).add(groupNode);
            }
        }
        return result;
    }

    private final static List<OptionSpec> collectAllOptions(ArgGroupSpec group) {
        List<OptionSpec> result = new ArrayList<>(group.options());
        for (ArgGroupSpec sub : group.subgroups()) {
            result.addAll(collectAllOptions(sub));
        }
        return result;
    }

    private final static Map<OptionSpec, ArgGroupSpec> buildOptionToGroupMap(CommandSpec spec) {
        Map<OptionSpec, ArgGroupSpec> map = new HashMap<>();
        collectOptionToGroup(spec.argGroups(), map);
        return map;
    }

    private final static void collectOptionToGroup(Collection<ArgGroupSpec> groups, Map<OptionSpec, ArgGroupSpec> map) {
        for (ArgGroupSpec group : groups) {
            for (OptionSpec opt : group.options()) {
                map.put(opt, group);
            }
            collectOptionToGroup(group.subgroups(), map);
        }
    }

    private final static String getOptionGroupHeading(OptionSpec option, Map<OptionSpec, ArgGroupSpec> optionToGroup) {
        ArgGroupSpec group = optionToGroup.get(option);
        String heading = null;
        if (group != null) {
            if (group.heading() != null && !group.heading().isBlank()) {
                heading = group.heading().replace("%n", "").trim();
            } else if (group.headingKey() != null && !group.headingKey().isBlank()) {
                heading = group.headingKey().trim();
            }
        }
        if (heading == null) {
            heading = "Command Options";
        }
        int idx = heading.indexOf(" (");
        if (idx > 0) {
            heading = heading.substring(0, idx).trim();
        }
        return heading;
    }

    private final static ObjectNode createOptionNode(OptionSpec option, boolean required) {
        ObjectNode node = JsonHelper.getObjectMapper().createObjectNode();
        String title = computeTitleFromOption(option);
        node.put("title", title);
        node.set("names", JsonHelper.getObjectMapper().createArrayNode()
                .addAll(Arrays.stream(option.names()).map(TextNode::new).collect(Collectors.toList())));
        node.put("primaryName", getPrimaryName(option));

        String valueFormat = option.paramLabel();
        if (valueFormat == null) {
            valueFormat = "";
        }
        node.put("valueFormat", valueFormat);

        node.put("description", normalizeNewlines(
                option.description().length > 0 ? option.description()[0] : ""));
        node.put("required", required);
        boolean secret = isSecretOption(option);
        ArrayNode allowedValues = getAllowedValues(option, option.type(), option.type() != null && option.type().isEnum());
        String datatype = getDatatype(option, allowedValues.size() > 0);
        node.put("datatype", datatype);
        node.put("secret", secret);
        node.put("multiselect", isMultiSelect(resolveType(option), option.arity(), option.splitRegex()));
        node.set("allowedValues", allowedValues);
        return node;
    }

    private final static ObjectNode createParameterNode(PositionalParamSpec param) {
        ObjectNode node = JsonHelper.getObjectMapper().createObjectNode();
        String title = computeTitleFromLabel(param.paramLabel());
        node.put("title", title);
        node.put("valueFormat", param.paramLabel());
        node.put("description", normalizeNewlines(
                param.description().length > 0 ? param.description()[0] : ""));
        node.put("required", param.required());
        ArrayNode allowedValues = getAllowedValues(param, param.type(), param.type() != null && param.type().isEnum());
        node.put("datatype", getDatatype(param, allowedValues.size() > 0));
        node.put("multiselect", isMultiSelect(resolveType(param), param.arity(), param.splitRegex()));
        node.set("allowedValues", allowedValues);
        return node;
    }

    private final static void collectExclusiveGroups(
            Collection<ArgGroupSpec> groups,
            List<ArgGroupSpec> out) {
        for (ArgGroupSpec g : groups) {
            if (g.exclusive()) {
                out.add(g);
            }
            collectExclusiveGroups(g.subgroups(), out);
        }
    }

    private final static String computeGroupTitle(ArgGroupSpec group) {
        String heading = group.heading();
        if (heading != null && !heading.isBlank()) {
            return heading.replace("%n", "").trim();
        }
        OptionSpec firstOption = !group.options().isEmpty()
                ? group.options().get(0)
                : group.subgroups().stream()
                        .flatMap(g -> g.options().stream())
                        .findFirst()
                        .orElse(null);
        if (firstOption != null) {
            return computeTitleFromOption(firstOption);
        }
        return "Arguments";
    }

    private final static String getPrimaryName(OptionSpec option) {
        String[] names = option.names();
        if (names == null || names.length == 0) {
            return null;
        }
        return Arrays.stream(names)
                .filter(n -> n.startsWith("--"))
                .findFirst()
                .orElse(names[0]);
    }

    private static String getDatatype(OptionSpec option, boolean hasAllowedValues) {
        return getDatatype(option, resolveType(option), option.arity(), option.splitRegex(), hasAllowedValues, option.paramLabel());
    }

    private static String getDatatype(PositionalParamSpec param, boolean hasAllowedValues) {
        return getDatatype(param, resolveType(param), param.arity(), param.splitRegex(), hasAllowedValues, param.paramLabel());
    }

    private final static String getDatatype(
            ArgSpec argSpec,
            Class<?> type,
            picocli.CommandLine.Range arity,
            String splitRegex,
            boolean hasAllowedValues,
            String paramLabel) {
        if (arity != null && arity.max() == 0) {
            return "boolean";
        }
        if (type == null) {
            return "string";
        }
        if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            if (componentType == char.class || componentType == Character.class) {
                return "string";
            }
        }
        if (type == Path.class || File.class.isAssignableFrom(type)) {
            return "file";
        }
        boolean isListType = Collection.class.isAssignableFrom(type)
                || type.isArray()
                || (splitRegex != null && !splitRegex.isBlank())
                || (arity != null && arity.max() > 1);
        if (isListType && hasAllowedValues) {
            return "array";
        }
        return "string";
    }

    private static Class<?> resolveType(ArgSpec argSpec) {
        Class<?> type = argSpec.type();
        if (type == null || type == String.class || type == Object.class) {
            Class<?> reflectedType = getReflectedType(argSpec.userObject());
            if (reflectedType != null) {
                return reflectedType;
            }
        }
        return type;
    }

    private static Class<?> getReflectedType(Object userObject) {
        if (userObject instanceof Field field) {
            return field.getType();
        }
        if (userObject instanceof Method method) {
            return method.getReturnType();
        }
        if (userObject instanceof Parameter parameter) {
            return parameter.getType();
        }
        if (userObject instanceof AccessibleObject accessibleObject) {
            if (accessibleObject instanceof Field field) {
                return field.getType();
            }
            if (accessibleObject instanceof Method method) {
                return method.getReturnType();
            }
        }
        return null;
    }

    private final static boolean isMultiSelect(Class<?> type, picocli.CommandLine.Range arity, String splitRegex) {
        if (arity != null && arity.max() == 0) {
            return false;
        }
        if (type != null && type.isArray()) {
            Class<?> componentType = type.getComponentType();
            if (componentType == char.class || componentType == Character.class) {
                return false;
            }
        }
        if (type != null && (type.isArray() || Collection.class.isAssignableFrom(type))) {
            return true;
        }
        if (splitRegex != null && !splitRegex.isBlank()) {
            return true;
        }
        if (arity != null && arity.max() > 1) {
            return true;
        }
        return false;
    }

    private final static ArrayNode getAllowedValues(OptionSpec option, Class<?> type, boolean isEnumType) {
        ArrayNode result = JsonHelper.getObjectMapper().createObjectNode().arrayNode();
        if (isEnumType && type != null) {
            Object[] constants = type.getEnumConstants();
            if (constants != null) {
                for (Object constant : constants) {
                    result.add(constant.toString());
                }
            }
        } else {
            Iterable<?> candidates = option.completionCandidates();
            if (candidates != null) {
                for (Object candidate : candidates) {
                    result.add(String.valueOf(candidate));
                }
            }
        }
        return result;
    }

    private final static ArrayNode getAllowedValues(PositionalParamSpec param, Class<?> type, boolean isEnumType) {
        ArrayNode result = JsonHelper.getObjectMapper().createObjectNode().arrayNode();
        if (isEnumType && type != null) {
            Object[] constants = type.getEnumConstants();
            if (constants != null) {
                for (Object constant : constants) {
                    result.add(constant.toString());
                }
            }
        } else {
            Iterable<?> candidates = param.completionCandidates();
            if (candidates != null) {
                for (Object candidate : candidates) {
                    result.add(String.valueOf(candidate));
                }
            }
        }
        return result;
    }

    private final static String computeTitleFromOption(OptionSpec option) {
        String primaryName = getPrimaryName(option);
        if (primaryName == null) {
            return "";
        }
        String withoutDashes = primaryName.replaceFirst("^-+", "");
        return computeTitleFromLabel(withoutDashes);
    }

    private final static String computeTitleFromLabel(String label) {
        if (label == null) {
            return "";
        }
        String sanitized = label.replace("<", "").replace(">", "").replace(":", " ");
        if (sanitized.isBlank()) {
            return "";
        }
        String[] parts = sanitized.split("[-_\\s]+");
        return Arrays.stream(parts).filter(p -> !p.isBlank()).map(p -> p.substring(0, 1).toUpperCase() + p.substring(1)).collect(Collectors.joining(" "));
    }

    private final static boolean isSecretOption(OptionSpec option) {
        return FcliCommandSpecHelper.isSensitive(option);
    }

    private static String toGroupId(String title) {
        if (title == null || title.isBlank()) {
            return "unknown";
        }
        return title.toLowerCase().replaceAll("[^a-z0-9]+", "");
    }

    private final static String normalizeNewlines(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\n%n", "\n\n").replace("%n", "\n");
    }

    private static List<String> computeFullAliases(CommandSpec leafSpec) {
        List<CommandSpec> hierarchy = new ArrayList<>();
        for (CommandSpec current = leafSpec; current != null; current = current.parent()) {
            hierarchy.add(0, current);
        }
        List<List<String>> hierarchyNames = new ArrayList<>();
        boolean hasAnyAlias = false;
        for (CommandSpec cs : hierarchy) {
            List<String> names = new ArrayList<>();
            names.add(cs.name());
            for (String a : cs.aliases()) {
                if (!a.equals(cs.name())) {
                    names.add(a);
                    hasAnyAlias = true;
                }
            }
            hierarchyNames.add(names);
        }
        if (!hasAnyAlias) {
            return List.of();
        }
        Set<String> combinations = new LinkedHashSet<>();
        buildCombinations(hierarchyNames, 0, new ArrayList<>(), combinations);
        String canonical = hierarchy.stream().map(CommandSpec::name).collect(Collectors.joining(" "));
        if (combinations.remove(canonical)) {
            List<String> ordered = new ArrayList<>();
            ordered.add(canonical);
            ordered.addAll(combinations);
            return ordered;
        }
        return new ArrayList<>(combinations);
    }

    private static void buildCombinations(List<List<String>> hierarchyNames, int index, List<String> current, Set<String> out) {
        if (index == hierarchyNames.size()) {
            out.add(String.join(" ", current));
            return;
        }
        for (String name : hierarchyNames.get(index)) {
            current.add(name);
            buildCombinations(hierarchyNames, index + 1, current, out);
            current.remove(current.size() - 1);
        }
    }

    private static ObjectNode createMetadataNode(CommandSpec spec) {
        var mapper = JsonHelper.getObjectMapper();
        var result = mapper.createObjectNode();
        for (var ann : FcliCommandSpecHelper.getMetadataAnnotations(spec)) {
            var key = deriveMetadataKey(ann.annotationType());
            var methods = Stream.of(ann.annotationType().getDeclaredMethods()).toList();
            if (methods.size() == 1 && methods.get(0).getName().equals("value")) {
                result.set(key, annotationElementToJsonNode(methods.get(0), ann));
            } else {
                var node = mapper.createObjectNode();
                for (var method : methods) {
                    node.set(method.getName(), annotationElementToJsonNode(method, ann));
                }
                result.set(key, node);
            }
        }
        return result;
    }

    private static String deriveMetadataKey(Class<? extends Annotation> annotationType) {
        var name = annotationType.getSimpleName();
        if (name.startsWith("Fcli")) { name = name.substring(4); }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static JsonNode annotationElementToJsonNode(Method method, Annotation annotation) {
        try {
            return toJsonNode(method.invoke(annotation));
        } catch (ReflectiveOperationException e) {
            throw new FcliBugException("Error reading annotation element: " + method.getName(), e);
        }
    }

    private static JsonNode toJsonNode(Object value) {
        if (value instanceof Enum<?> e) {
            return TextNode.valueOf(e.toString());
        } else if (value instanceof String s) {
            return TextNode.valueOf(s);
        } else if (value instanceof Boolean b) {
            return BooleanNode.valueOf(b);
        } else if (value instanceof Integer i) {
            return IntNode.valueOf(i);
        } else if (value != null && value.getClass().isArray()) {
            var arr = JsonHelper.getObjectMapper().createArrayNode();
            for (int i = 0; i < Array.getLength(value); i++) {
                arr.add(toJsonNode(Array.get(value, i)));
            }
            return arr;
        } else {
            return TextNode.valueOf(String.valueOf(value));
        }
    }
}
