/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.util.all_commands.cli.cmd;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = BasicOutputHelperMixins.List.CMD_NAME)
public final class AllCommandsListCommand extends AbstractBasicOutputCommand {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Getter @Mixin private BasicOutputHelperMixins.List outputHelper;
    @Spec private CommandSpec spec;
    @Option(names = "--include-hidden") private boolean includeHidden;
    @Option(names = "--include-parents") private boolean includeParents;
    
    @Override
    protected JsonNode getJsonNode() {
        ResultHolder result = new ResultHolder();
        addCommands(result, spec.root().subcommands());
        return result.result;
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
    
    private final void addCommands(ResultHolder result, Map<String, CommandLine> subcommands) {
        if ( subcommands!=null && !subcommands.isEmpty() ) {
            for (Map.Entry<String, CommandLine> entry : subcommands.entrySet()) {
                CommandSpec spec = entry.getValue().getCommandSpec();
                if (spec.usageMessage().hidden() && !includeHidden) { continue; }
                var subsubcommands = spec.subcommands();
                if ( includeParents || subsubcommands==null || subsubcommands.isEmpty() ) {
                    result.add(createNode(spec));
                }
                addCommands(result, spec.subcommands());
            }
        }
    }

    private static final ObjectNode createNode(CommandSpec spec) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("command", spec.qualifiedName(" "));
        result.put("hidden", spec.usageMessage().hidden());
        result.put("usageHeader", String.join("\n", spec.usageMessage().header()));
        result.set("aliases", Stream.of(spec.aliases()).map(TextNode::new).collect(JsonHelper.arrayNodeCollector()));
        result.put("aliasesString", Stream.of(spec.aliases()).collect(Collectors.joining(", ")));
        result.set("options", spec.optionsMap().keySet().stream().map(TextNode::new).collect(JsonHelper.arrayNodeCollector()));
        result.put("optionsString", spec.optionsMap().keySet().stream().collect(Collectors.joining(", ")));
        return result;
    }
    
    private static final class ResultHolder {
        private ArrayNode result = objectMapper.createArrayNode();
        private Set<String> processedCommands = new HashSet<>();
        
        public void add(ObjectNode record) {
            String command =  record.get("command").asText();
            if ( !processedCommands.contains(command) ) {
                result.add(record);
                processedCommands.add(command);
            }
        }
    }
    
    
}
