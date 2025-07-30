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
package com.fortify.cli.util.all_commands.cli.cmd;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.util.all_commands.cli.mixin.AllCommandsCommandSelectorMixin;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

@Command(name = "mcp-server")
public class AllCommandsMCPServerCommand extends AbstractRunnableCommand {
    private static final Logger log = LoggerFactory.getLogger(AllCommandsMCPServerCommand.class);
    @Mixin private AllCommandsCommandSelectorMixin selectorMixin;
    
    public Integer call() throws Exception {
        initialize();
        var specs = selectorMixin.getSelectedCommands().getSpecs().stream()
                .map(AllCommandsMCPServerCommand::getToolSpec)
                .toList();;
        McpSchema.ServerCapabilities serverCapabilities = McpSchema.ServerCapabilities.builder()
                .resources(false, false)
                .prompts(false)
                .tools(true)
                .build();

        McpSyncServer server = McpServer.sync(new StdioServerTransportProvider())
                .serverInfo("fcli", FcliBuildProperties.INSTANCE.getFcliVersion())
                .requestTimeout(Duration.ofSeconds(120))
                .instructions("Fcli MCP Server")
                .capabilities(serverCapabilities)
                .build();

        log.info("Fcli MCP server running on stdio%n");
        
        specs.forEach(server::addTool);
        
        while(true) {
            Thread.sleep(5000L);
        }
    }
    
    @SneakyThrows
    private static final SyncToolSpecification getToolSpec(CommandSpec spec) {
        var name = spec.qualifiedName("_");
        var schema = buildSchema(spec);
        var description = buildDescription(spec);
        
        McpSchema.Tool tool = new McpSchema.Tool(name, description, schema);

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            var args = arguments==null ? "" : arguments.entrySet().stream().map(AllCommandsMCPServerCommand::getArg).collect(Collectors.joining(" ")); 
            var result = FcliCommandExecutorFactory.builder()
                .cmd(spec.qualifiedName(" ")+" "+args)
                .stdoutOutputType(OutputType.collect)
                .stderrOutputType(OutputType.collect)
                .build().create().execute();
            return new McpSchema.CallToolResult(result.getErr()+"\n"+result.getOut(), result.getExitCode()==0);
        });
    }

    private static final String getArg(Entry<String, Object> e) {
        var name = e.getKey();
        if ( name.startsWith("param-") ) {
            return e.getValue().toString();
        } else {
            return String.format("\"--%s=%s\"", e.getKey(), e.getValue());
        }
    }

    private static final String buildDescription(CommandSpec spec) throws IOException {
        var help = spec.commandLine().getHelp();
        return String.format("%s - %s\n%s", spec.qualifiedName(" "), help.header(), help.description());
    }

    
    private static final JsonSchema buildSchema(CommandSpec spec) {
        var properties = new LinkedHashMap<String, Object>();
        var required = new ArrayList<String>();
        spec.options().stream()
            .filter(AllCommandsMCPServerCommand::includeOption)
            .forEach(o->addProperty(o, properties,required));
        spec.positionalParameters().forEach(p->addParameter(p, properties,required));
        return new JsonSchema("object", properties, required, null, null, null);
    }
    
    private static final void addParameter(PositionalParamSpec p, LinkedHashMap<String, Object> properties, ArrayList<String> required) {
        var argName = "param-"+((Field)p.userObject()).getName();
        properties.put(argName, JsonHelper.getObjectMapper().createObjectNode()
                .put("type", "string")
                .put("description", getDescription(p)));
        if ( p.required() ) { required.add(argName); }
    }

    private static String getDescription(ArgSpec as) {
        return "TODO";
    }

    private static final boolean includeOption(OptionSpec o) {
        // TODO Replace with @McpIgnore annotation on given options?
        var excludedOptionNames = new HashSet<String>(List.of("--query", "--log-file", "--log-level", "--debug", "--env-prefix", "--debug", "--to-file"));
        var name = o.longestName();
        return !excludedOptionNames.contains(name);
    }

    private static final void addProperty(OptionSpec o, LinkedHashMap<String, Object> properties, ArrayList<String> required) {
        properties.put(o.longestName(), JsonHelper.getObjectMapper().createObjectNode()
                .put("type", "string")
                .put("description", getDescription(o)));
        if ( o.required() ) {
            required.add(o.longestName());
        }
    }
}
