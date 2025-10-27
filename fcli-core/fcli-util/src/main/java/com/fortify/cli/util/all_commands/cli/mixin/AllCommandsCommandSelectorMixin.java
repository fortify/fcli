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
package com.fortify.cli.util.all_commands.cli.mixin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.StreamingObjectNodeProducer;
import com.fortify.cli.common.spel.query.QueryExpression;
import com.fortify.cli.common.spel.query.QueryExpressionTypeConverter;

import lombok.Data;
import lombok.Getter;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

/**
 *
 * @author Ruud Senden
 */
public class AllCommandsCommandSelectorMixin {
    @Option(names = {"-q", "--query"}, order=1, converter = QueryExpressionTypeConverter.class, paramLabel = "<SpEL expression>")
    @Getter private QueryExpression queryExpression;

    public final IObjectNodeProducer getObjectNodeProducer() {
        return StreamingObjectNodeProducer.builder()
                .streamSupplier(this::createObjectNodeStream)
                .build();
    }

    public final Stream<ObjectNode> createObjectNodeStream() {
        return createStream().map(n->n.getNode());
    }
    
    public final Stream<CommandSpec> createCommandSpecStream() {
        return createStream().map(n->n.getSpec());
    }
    
    private final Stream<CommandSpecAndNode> createStream() {
        return FcliCommandSpecHelper.rootCommandTreeStream()
            .map(CommandSpecAndNode::new)
            .filter(n->n.matches(queryExpression))
            .distinct();
    }
    
    @Data
    private static final class CommandSpecAndNode  {
        private final CommandSpec spec;
        private final ObjectNode node;
        
        private CommandSpecAndNode(CommandSpec spec) {
            this.spec = spec;
            this.node = createNode(spec);
        }

        public boolean matches(QueryExpression queryExpression) {
            return queryExpression==null || queryExpression.matches(node);
        }
    }

    private static final ObjectNode createNode(CommandSpec spec) {
        var hiddenParent = FcliCommandSpecHelper.hasHiddenParent(spec);
        var hiddenSelf = FcliCommandSpecHelper.isHiddenSelf(spec);
        var hidden = FcliCommandSpecHelper.isHiddenSelfOrParent(spec);
        var mcpIgnored = FcliCommandSpecHelper.isMcpIgnored(spec);
        var nameComponents = spec.qualifiedName(" ").split(" ");
        var module = nameComponents.length>1 ? nameComponents[1] : "";
        var entity = nameComponents.length>2 ? nameComponents[2] : "";
        var action = nameComponents.length>3 ? nameComponents[3] : "";
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("command", spec.qualifiedName(" "));
        result.put("module", module);
        result.put("entity", entity);
        result.put("action", action);
        result.put("hidden", hidden);
        result.put("hiddenParent", hiddenParent);
        result.put("hiddenSelf", hiddenSelf);
        result.put("mcpIgnored", mcpIgnored);
        result.put("runnable", FcliCommandSpecHelper.isRunnable(spec));
        result.put("usageHeader", String.join("\n", spec.usageMessage().header()));
        result.set("aliases", Stream.of(spec.aliases()).map(TextNode::new).collect(JsonHelper.arrayNodeCollector()));
        result.put("aliasesString", Stream.of(spec.aliases()).collect(Collectors.joining(", ")));
        var fullAliases = computeFullAliases(spec);
        result.set("fullAliases", fullAliases.stream().map(TextNode::new).collect(JsonHelper.arrayNodeCollector()));
        result.put("fullAliasesString", String.join(", ", fullAliases));
        result.set("options", spec.optionsMap().keySet().stream().map(TextNode::new).collect(JsonHelper.arrayNodeCollector()));
        result.put("optionsString", spec.optionsMap().keySet().stream().collect(Collectors.joining(", ")));
        return result;
    }

    /**
     * Compute all possible full command aliases for the given {@link CommandSpec} by
     * generating the cartesian product of primary names + aliases for every command
     * in the hierarchy (root to leaf). The canonical command name (concatenation of
     * primary names) is INCLUDED as the first element if there is at least one alias
     * somewhere in the hierarchy; if there are no aliases anywhere, an empty list is
     * returned.
     * 
     * Example: For hierarchy fcli -> ssc -> appversion (alias: av) -> list (alias: ls),
     * this method returns (order preserved): ["fcli ssc appversion list", "fcli ssc appversion ls",
     * "fcli ssc av list", "fcli ssc av ls"].
     */
    private static final List<String> computeFullAliases(CommandSpec leafSpec) {
        // Build ordered list of specs from root to leaf
        List<CommandSpec> hierarchy = new ArrayList<>();
        for (CommandSpec current = leafSpec; current != null; current = current.parent()) {
            hierarchy.add(0, current);
        }
        // Collect possible names (primary + aliases) for each spec in hierarchy
        List<List<String>> hierarchyNames = new ArrayList<>();
        boolean hasAnyAlias = false;
        for (CommandSpec cs : hierarchy) {
            List<String> names = new ArrayList<>();
            names.add(cs.name());
            for (String a : cs.aliases()) {
                if (!a.equals(cs.name())) { // avoid duplicate of primary name
                    names.add(a);
                    hasAnyAlias = true;
                }
            }
            hierarchyNames.add(names);
        }
        if (!hasAnyAlias) { // No aliases anywhere => no full alias combinations
            return List.of();
        }
        // Cartesian product
        Set<String> combinations = new LinkedHashSet<>();
        buildCombinations(hierarchyNames, 0, new ArrayList<>(), combinations);
        // Ensure canonical (all primary names) appears first if present
        String canonical = hierarchy.stream().map(CommandSpec::name).collect(Collectors.joining(" "));
        if (combinations.remove(canonical)) {
            // Re-insert at beginning by creating new list
            List<String> ordered = new ArrayList<>();
            ordered.add(canonical);
            ordered.addAll(combinations);
            return ordered;
        }
        return new ArrayList<>(combinations);
    }

    private static final void buildCombinations(List<List<String>> hierarchyNames, int index, List<String> current, Set<String> out) {
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
}
