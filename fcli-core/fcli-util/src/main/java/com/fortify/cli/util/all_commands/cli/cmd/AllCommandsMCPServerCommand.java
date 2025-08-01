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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.mcp.MCPIgnore;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.common.util.JavaHelper;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.common.util.PicocliSpecHelper;
import com.fortify.cli.util.all_commands.cli.mixin.AllCommandsCommandSelectorMixin;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.ITypeInfo;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

@Command(name = "mcp-server") 
@MCPIgnore // Doesn't make sense to allow mcp-server command to be called from MCP server
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
                .peek(s->LOG.debug("Registering tool: {}", s.tool().name()))
                .toList();;
        McpSchema.ServerCapabilities serverCapabilities = McpSchema.ServerCapabilities.builder()
                .resources(false, false)
                .prompts(false)
                .tools(true)
                .build();

        McpServer.sync(new StdioServerTransportProvider())
                .serverInfo("fcli", FcliBuildProperties.INSTANCE.getFcliVersion())
                .requestTimeout(Duration.ofSeconds(120))
                .instructions("Fcli MCP Server")
                .capabilities(serverCapabilities)
                //.resources(getResourceSpec(selectorMixin.getSelectedCommands().getSpecs()))
                .tools(toolSpecs)
                .build();

        LOG.info("Fcli MCP server running on stdio");
        
        // Keep server running forever until killed
        while(true) {
            Thread.sleep(5000L);
        }
    }

    /* Sample for adding fcli commands as resource; not sure whether this is useful */
    /*
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
        if ( !include(spec) ) { return null; }
        var name = spec.qualifiedName("_");
        var schema = buildSchema(spec);
        var description = buildDescription(spec);
        
        McpSchema.Tool tool = new McpSchema.Tool(name, description, schema);

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            var args = arguments==null ? "" : arguments.entrySet().stream().map(AllCommandsMCPServerCommand::getArg).collect(Collectors.joining(" "));
            var fullCmd = spec.qualifiedName(" ")+" "+args;
            if ( spec.optionsMap().containsKey("--output") && !fullCmd.contains("--output=") ) {
                fullCmd+=" --output=json"; 
            }
            LOG.debug("Executing: "+fullCmd);
            try {
                var result = FcliCommandExecutorFactory.builder()
                    .cmd(fullCmd)
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
    
    private static final boolean include(CommandSpec cs) {
        return includeAnnotatedElement(cs.userObject().getClass()) 
                && PicocliSpecHelper.isRunnable(cs) 
                && !PicocliSpecHelper.isHiddenSelfOrParent(cs);
    }

    private static final String getArg(Entry<String, Object> e) {
        var name = e.getKey();
        var value = e.getValue();
        if ( !name.startsWith("-") ) {
            return streamValueElts(value).map(v->"\""+v+"\"").collect(Collectors.joining(" "));
        } else {
            return String.format("\"%s=%s\"", name, streamValueElts(value).collect(Collectors.joining(",")));
        }
    }

    private static Stream<String> streamValueElts(Object value) {
        Stream<?> os = null;
        if ( value==null ) { 
            os = Stream.empty(); 
        } else if ( value.getClass().isArray() ) { 
            os = Stream.of((Object[])value); 
        } else if ( Collection.class.isAssignableFrom(value.getClass()) ) {
            os = ((Collection<?>)value).stream();
        } else {
            os = Stream.of(value);
        }
        return os.filter(Objects::nonNull).map(Object::toString);
    }

    private static final String buildDescription(CommandSpec spec) throws IOException {
        var help = spec.commandLine().getHelp();
        return String.format("%s - %s\n%s", spec.qualifiedName(" "), help.header(), help.description());
    }

    
    private static final JsonSchema buildSchema(CommandSpec spec) {
        var properties = new LinkedHashMap<String, Object>();
        var required = new ArrayList<String>();
        spec.options().stream()
            .filter(AllCommandsMCPServerCommand::include)
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
                .put("description", getDescription(as))
                .set("type", getPropertyType(as.typeInfo()));
    }

    private static final JsonNode getPropertyType(ITypeInfo typeInfo) {
        // TODO Although technically we can just always accept strings, using proper types based on the type of as.userObject(),
        //      for example booleans, might result in a better user experience. However, we do then need to convert back to string
        //      when generating the fcli command to be executed, which in most cases can likely be just toString() or similar, but
        //      arrays/collections may need to be converted to comma-separated string.
        var type = typeInfo.getType();
        
        // GitHub Copilot Eclipse plugin doesn't seem to like 'array' or 'enum' types, so returning 'string' for those for now.
        // Symptoms include Copilot preferences not listing any MCP tools (even from other MCP servers), and no response to chat messages.
        
        /* 
        if ( typeInfo.isArray() || typeInfo.isCollection() ) {
            return JsonHelper.getObjectMapper().createObjectNode()
                    .put("type", "array");
                    .set("items", 
                        JsonHelper.getObjectMapper().createObjectNode().set("type", getPropertyType(typeInfo.getAuxiliaryTypeInfos().get(0))));
        }
        */
        if ( typeInfo.isArray() || typeInfo.isCollection() ) { return new TextNode("string"); }
        if (typeInfo.isBoolean()) { return new TextNode("boolean"); }
        if (type==Integer.class || type==int.class ) { return new TextNode("integer"); }
        if (type==Number.class || type==float.class || type==double.class) { return new TextNode("number"); }
        //if (typeInfo.isEnum()) { return JsonHelper.getObjectMapper().createObjectNode().set("enum", JsonHelper.toArrayNode(typeInfo.getEnumConstantNames().toArray(String[]::new))); }
        return new TextNode("string");
    }

    private static String getDescription(ArgSpec as) {
        String[] descElts = as.description(); 
        return descElts==null || descElts.length<1 ? "" : String.join(" ", descElts);
    }
    
    private static final boolean include(ArgSpec as) {
        return includeAnnotatedElement(as.userObject());
    }
    
    private static final boolean includeAnnotatedElement(Object o) {
        return JavaHelper.as(o, AnnotatedElement.class)
                .map(e->!e.isAnnotationPresent(MCPIgnore.class))
                .orElse(true);
    }

    private static final void addProperty(OptionSpec o, LinkedHashMap<String, Object> properties, ArrayList<String> required) {
        properties.put(o.longestName(), createProperty(o));
        if ( o.required() ) {
            required.add(o.longestName());
        }
    }
}
