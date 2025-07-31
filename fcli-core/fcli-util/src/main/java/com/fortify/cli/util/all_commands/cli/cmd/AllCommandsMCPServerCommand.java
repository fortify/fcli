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
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.common.util.PicocliSpecHelper;
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
    private static final Logger LOG = LoggerFactory.getLogger(AllCommandsMCPServerCommand.class);
    @Mixin private AllCommandsCommandSelectorMixin selectorMixin;
    
    public Integer call() throws Exception {
        initialize();
        // For some reason, if we calculate this after building the server, clients like
        // Eclipse don't show the full list of tools.
        var toolSpecs = selectorMixin.getSelectedCommands().getSpecs().stream()
                .map(AllCommandsMCPServerCommand::getToolSpec)
                .filter(Objects::nonNull)
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
        
        toolSpecs.forEach(server::addTool);

        LOG.info("Fcli MCP server running on stdio%n");
        
        //server.addResource(getResourceSpec(selectorMixin.getSelectedCommands().getSpecs()));
        
        while(true) {
            Thread.sleep(5000L);
        }
    }

    /* Sample for adding fcli commands as resource; not sure whether this is useful
    @SneakyThrows
    private static final SyncResourceSpecification getResourceSpec(List<CommandSpec> specs) {
        McpSchema.Resource resource = new McpSchema.Resource("fcli://all-commands", "List all fcli commands", "List all fcli commands", "application/json", null);
        return new SyncResourceSpecification(resource, (exchange, request)->{
            var contents = new ArrayList<ResourceContents>();
            specs.forEach(spec->contents.add(
                    new TextResourceContents("fcli://all-commands/"+spec.qualifiedName("+"), "application/json", asJsonString(spec))));
            return new ReadResourceResult(contents);
        });
    }
    @SneakyThrows
    private static final String asJsonString(CommandSpec spec) {
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("command", spec.qualifiedName(" "))
                .put("description", buildDescription(spec))
                .toString();
    }
    */

    @SneakyThrows
    private static final SyncToolSpecification getToolSpec(CommandSpec spec) {
        if ( !PicocliSpecHelper.isRunnable(spec) || PicocliSpecHelper.isHiddenSelfOrParent(spec) ) {
            return null;
        }
        var name = spec.qualifiedName("_");
        var schema = buildSchema(spec);
        var description = buildDescription(spec);
        
        McpSchema.Tool tool = new McpSchema.Tool(name, description, schema);

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            var args = arguments==null ? "" : arguments.entrySet().stream().map(AllCommandsMCPServerCommand::getArg).collect(Collectors.joining(" "));
            var fullCmd = spec.qualifiedName(" ")+" "+args;
            // TODO Should we force JSON output by adding --output=json if supported by current command? CoPilot often seems to add this option by itself, 
            //      but seemed to perform worse if we remove --output from supported args and hard-code --output=json 
            LOG.debug("Executing: "+fullCmd);
            try {
                var result = FcliCommandExecutorFactory.builder()
                    .cmd(spec.qualifiedName(" ")+" "+args)
                    .stdoutOutputType(OutputType.collect)
                    .stderrOutputType(OutputType.collect)
                    .onFail(r->{}) // Continue on non-zero exit code, assuming stdout/stderr shows more info about the error, which in turn can be
                                   //  used by the LLM to provide suggestions on how to fix.
                    .build().create().execute();
                LOG.debug("Stdout: "+result.getOut());
                LOG.debug("Stderr: "+result.getErr());
                // TODO What's the best way to return output? Just a simple string containing stdout+stderr like we have now, separate outputs as commented out
                //      below, structured output, ...
                //var resultContents = new ArrayList<Content>();
                //resultContents.add(new TextContent(result.getOut()+"\n"+result.getErr()));
                //resultContents.add(new EmbeddedResource(List.of(Role.USER), null, new TextResourceContents("fcli://stdout", "text/plain", result.getOut())));
                //resultContents.add(new EmbeddedResource(List.of(Role.USER), null, new TextResourceContents("fcli://stderr", "text/plain", result.getErr())));
                return new McpSchema.CallToolResult(result.getOut()+"\n"+result.getErr(), result.getExitCode()!=0);
            } catch ( Exception e ) {
                LOG.error("Exception while running fcli command", e);
                return new McpSchema.CallToolResult(e.toString(), true);
            }
        });
    }

    private static final String getArg(Entry<String, Object> e) {
        var name = e.getKey();
        var value = String.format("%s", e.getValue());
        if ( !name.startsWith("-") ) {
            return value;
        } else {
            return String.format("\"%s=%s\"", name, value);
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
        var argName = ((Field)p.userObject()).getName(); // Note that argName may not start with dashes, to distinguish from options. Field names cannot contain dashes, so we're fine here
        properties.put(argName, createProperty(p));
        if ( p.required() ) { required.add(argName); }
    }

    private static final ObjectNode createProperty(ArgSpec as) {
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("type", getPropertyType(as))
                .put("description", getDescription(as));
    }

    private static final String getPropertyType(ArgSpec as) {
        // TODO Although technically we can just always accept strings, using proper types based on the type of as.userObject(),
        //      for example booleans, might result in a better user experience. However, we do then need to convert back to string
        //      when generating the fcli command to be executed, which in most cases can likely be just toString() or similar, but
        //      arrays/collections may need to be converted to comma-separated string.
        return "string";
    }

    private static String getDescription(ArgSpec as) {
        String[] descElts = as.description(); 
        return descElts==null || descElts.length<1 ? "" : String.join(" ", descElts);
    }

    private static final boolean includeOption(OptionSpec o) {
        // TODO Replace with @McpIgnore annotation on given options?
        // TODO --query/--q-param are excluded due to LLM often passing incorrect values; any way we can still provide filtering options?
        //      Potentially, command implementation could use annotations to describe LLM-supported filtering options, together with info 
        //      on how to map these to -q / --q-param options? For example, annotation would allow for adding a --app option to the MCP
        //      tool declaration, while mapping this option to something like '-q app.name=%s' when executing fcli?
        var excludedOptionNames = new HashSet<String>(List.of("--query", "--q-param", "--log-file", "--log-level", "--debug", "--env-prefix", "--debug", "--to-file"));
        var name = o.longestName();
        return !excludedOptionNames.contains(name);
    }

    private static final void addProperty(OptionSpec o, LinkedHashMap<String, Object> properties, ArrayList<String> required) {
        properties.put(o.longestName(), createProperty(o));
        if ( o.required() ) {
            required.add(o.longestName());
        }
    }
}
