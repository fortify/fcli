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
package com.fortify.cli.util.all_commands.cli.mixin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.query.QueryExpression;
import com.fortify.cli.common.spel.query.QueryExpressionTypeConverter;
import com.fortify.cli.common.util.PicocliSpecHelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

/**
 *
 * @author Ruud Senden
 */
public class AllCommandsCommandSelectorMixin {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Mixin private CommandHelperMixin commandHelper;
    @Option(names = {"-q", "--query"}, order=1, converter = QueryExpressionTypeConverter.class, paramLabel = "<SpEL expression>")
    @Getter private QueryExpression queryExpression;
    
    public CommandSelectorResult getSelectedCommands() {
        CommandSelectorResult result = new CommandSelectorResult(queryExpression);
        PicocliSpecHelper.commandTreeStream(commandHelper.getCommandSpec().root()).forEach(result::add);
        return result;
    }
    
    @RequiredArgsConstructor
    public static final class CommandSelectorResult {
        @Getter private ArrayNode nodes = objectMapper.createArrayNode();
        @Getter private List<CommandSpec> specs = new ArrayList<>();
        private Set<String> processedCommands = new HashSet<>();
        private final QueryExpression queryExpression;
        
        public void add(CommandSpec spec) {
            var record = createNode(spec);
            String command = record.get("command").asText();
            if ( !processedCommands.contains(command) ) {
                if ( queryExpression==null || queryExpression.matches(record) ) {
                    nodes.add(record);
                    specs.add(spec);
                }
                processedCommands.add(command);
            }
        }
        
        private static final ObjectNode createNode(CommandSpec spec) {
            var hiddenParent = PicocliSpecHelper.hasHiddenParent(spec);
            var hiddenSelf = PicocliSpecHelper.isHiddenSelf(spec);
            var hidden = PicocliSpecHelper.isHiddenSelfOrParent(spec);
            var mcpIgnored = PicocliSpecHelper.isMcpIgnored(spec);
            var nameComponents = spec.qualifiedName(" ").split(" ");
            var module = nameComponents.length>1 ? nameComponents[1] : "";
            var entity = nameComponents.length>2 ? nameComponents[2] : "";
            var action = nameComponents.length>3 ? nameComponents[3] : "";
            ObjectNode result = objectMapper.createObjectNode();
            result.put("command", spec.qualifiedName(" "));
            result.put("module", module);
            result.put("entity", entity);
            result.put("action", action);
            result.put("hidden", hidden);
            result.put("hiddenParent", hiddenParent);
            result.put("hiddenSelf", hiddenSelf);
            result.put("mcpIgnored", mcpIgnored);
            result.put("runnable", PicocliSpecHelper.isRunnable(spec));
            result.put("usageHeader", String.join("\n", spec.usageMessage().header()));
            result.set("aliases", Stream.of(spec.aliases()).map(TextNode::new).collect(JsonHelper.arrayNodeCollector()));
            result.put("aliasesString", Stream.of(spec.aliases()).collect(Collectors.joining(", ")));
            result.set("options", spec.optionsMap().keySet().stream().map(TextNode::new).collect(JsonHelper.arrayNodeCollector()));
            result.put("optionsString", spec.optionsMap().keySet().stream().collect(Collectors.joining(", ")));
            return result;
        }
    }
}
