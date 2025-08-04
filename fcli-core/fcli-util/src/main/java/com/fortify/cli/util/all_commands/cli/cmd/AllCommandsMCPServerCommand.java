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

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.mcp.MCPIgnore;
import com.fortify.cli.common.output.writer.CommandSpecMessageResolver;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.common.util.PicocliSpecHelper;
import com.fortify.cli.common.util.ReflectionHelper;
import com.fortify.cli.util.all_commands.cli.mixin.AllCommandsCommandSelectorMixin;
import com.networknt.schema.utils.StringUtils;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
        super.initialize(); // Initialize mixins etc
        
        McpServer.sync(new StdioServerTransportProvider())
                .serverInfo("fcli", FcliBuildProperties.INSTANCE.getFcliVersion())
                .requestTimeout(Duration.ofSeconds(120))
                .instructions("Fcli MCP Server")
                .capabilities(getServerCapabilities())
                .tools(createToolSpecs())
                .build();

        LOG.info("Fcli MCP server running on stdio");
        
        // Keep server running forever until killed
        while(true) {
            Thread.sleep(5000L);
        }
    }

    private ServerCapabilities getServerCapabilities() {
        return ServerCapabilities.builder()
                .resources(false, false)
                .prompts(false)
                .tools(true)
                .build();
    }

    private List<SyncToolSpecification> createToolSpecs() {
        return selectorMixin.getSelectedCommands().getSpecs().stream()
                .filter(cs->!ignore(cs))
                .map(cs->createToolSpec(cs))
                .peek(s->LOG.debug("Registering tool: {}", s.tool().name()))
                .toList();
    }
    
    private static final SyncToolSpecification createToolSpec(CommandSpec spec) {
        return new CommandToolSpecHelper(spec).createToolSpec();
    }
    
    private static final boolean ignore(CommandSpec cs) {
        return ReflectionHelper.hasAnnotation(cs, MCPIgnore.class)
                || !PicocliSpecHelper.isRunnable(cs) 
                || PicocliSpecHelper.isHiddenSelfOrParent(cs);
    }
    
    private static final class CommandToolSpecHelper {
        private static final ObjectMapper OM = JsonHelper.getObjectMapper();
        private final CommandSpec commandSpec;
        private final CommandToolSpecArgHelper toolSpecArgHelper;
        
        private CommandToolSpecHelper(CommandSpec commandSpec) {
            this.commandSpec = commandSpec;
            this.toolSpecArgHelper = new CommandToolSpecArgHelper(commandSpec);
        }
        
        @SneakyThrows
        public final SyncToolSpecification createToolSpec() {
            return McpServerFeatures.SyncToolSpecification.builder()
                    .tool(createTool())
                    .callHandler((exchange, request) -> execute(request))
                    .build();
        }
        
        private final Tool createTool() {
            return Tool.builder()
                    .name(commandSpec.qualifiedName("_"))
                    .description(buildToolDescription())
                    .inputSchema(toolSpecArgHelper.getSchema())
                    .build();
        }
        
        private final String buildToolDescription() {
            var help = commandSpec.commandLine().getHelp();
            return String.format("%s - %s\n%s", commandSpec.qualifiedName(" "), help.header(), help.description());
        }

        private final CallToolResult execute(CallToolRequest request) {
            var cmd = commandSpec.qualifiedName(" ");
            var args = request==null || request.arguments()==null ? "" : toolSpecArgHelper.getFcliCmdArgs(request.arguments());
            var fullCmd = String.format("%s %s", cmd, args);
            try {
                return execute(fullCmd);
            } catch ( Exception e ) {
                LOG.error("Exception while running fcli command:\n\t"+fullCmd, e);
                return new CallToolResult(e.toString(), true);
            }
        }

        private final CallToolResult execute(String fullCmd) {
            LOG.debug("Executing: "+fullCmd);
            if ( PicocliSpecHelper.canCollectRecords(commandSpec) ) {
                return executeWithRecordsCollection(fullCmd);
            } else {
                return executePlain(fullCmd);
            }
        }
        
        private CallToolResult executeWithRecordsCollection(String fullCmd) {
            var records = OM.createArrayNode();
            var result = FcliCommandExecutorFactory.builder()
                .cmd(fullCmd)
                .stdoutOutputType(OutputType.suppress)
                .stderrOutputType(OutputType.collect)
                .recordConsumer(records::add)
                .onFail(r->{}) // Continue on non-zero exit code, assuming stdout/stderr shows more info about the error, which in turn can be
                               //  used by the LLM to provide suggestions on how to fix.
                .build().create().execute();
            return new CallToolResult(records.toPrettyString(), result.getExitCode()!=0);
        }

        private final CallToolResult executePlain(String fullCmd) {
            var result = FcliCommandExecutorFactory.builder()
                .cmd(fullCmd)
                .stdoutOutputType(OutputType.collect)
                .stderrOutputType(OutputType.collect)
                .onFail(r->{}) // Continue on non-zero exit code, assuming stdout/stderr shows more info about the error, which in turn can be
                               //  used by the LLM to provide suggestions on how to fix.
                .build().create().execute();
            return new CallToolResult(OM.valueToTree(result).toPrettyString(), result.getExitCode()!=0);
        }
    }
    
    private static final class CommandToolSpecArgHelper {
        private final List<IToolSpecArgHelper> toolSpecArgHelpers;
        @Getter private final JsonSchema schema;
        //@Getter private final boolean pagingSupported;
        
        public CommandToolSpecArgHelper(CommandSpec spec) {
            this.toolSpecArgHelpers = createToolSpecArgHelpers(spec);
            this.schema = createSchema(toolSpecArgHelpers);
        }
        
        public final String getFcliCmdArgs(Map<String, Object> toolArgs) {
            return toolSpecArgHelpers.stream().map(h->h.getFcliCmdArgs(toolArgs)).collect(Collectors.joining(" "));
        }

        private static final List<IToolSpecArgHelper> createToolSpecArgHelpers(CommandSpec spec) {
            var result = new ArrayList<IToolSpecArgHelper>();
            addArgSpecHelpers(result, spec.positionalParameters(), PositionalParamToolSpecArgHelper::new);
            addArgSpecHelpers(result, spec.options(), OptionToolSpecArgHelper::new);
            addQueryToolSpecArgHelper(result, spec);
            return result;
        }

        private static void addQueryToolSpecArgHelper(ArrayList<IToolSpecArgHelper> result, CommandSpec spec) {
            var messageResolver = new CommandSpecMessageResolver(spec);
            if ( spec.optionsMap().containsKey("--query") ) {
                String commonQueryFieldsString = messageResolver.getMessageString("mcp.common-query-fields");
                if ( StringUtils.isNotBlank(commonQueryFieldsString) ) {
                    result.add(new QueryToolSpecArgHelper(Arrays.asList(commonQueryFieldsString.split(","))));
                }
            }
        }

        private static <T extends ArgSpec> void addArgSpecHelpers(List<IToolSpecArgHelper> result, List<T> argSpecs, Function<T, IToolSpecArgHelper> factory) {
            argSpecs.stream()
                .filter(as->!ignore(as))
                .map(factory::apply)
                .forEach(result::add);
        }

        private static final JsonSchema createSchema(List<IToolSpecArgHelper> toolSpecArgHelpers) {
            var result = new JsonSchema("object", new LinkedHashMap<String, Object>(), new ArrayList<String>(), false, null, null);
            toolSpecArgHelpers.forEach(h->h.updateSchema(result));
            return result;
        }
        
        private static final boolean ignore(ArgSpec as) {
            return ReflectionHelper.hasAnnotation(as.userObject(), MCPIgnore.class);
        }
    }
    
    private static interface IToolSpecArgHelper {
        public void updateSchema(JsonSchema schema);
        public String getFcliCmdArgs(Map<String, Object> toolArgs);
    }
    
    
    @RequiredArgsConstructor
    private static final class QueryToolSpecArgHelper implements IToolSpecArgHelper {
        private final List<String> commonQueryFields;
    
        @Override
        public void updateSchema(JsonSchema schema) {
            commonQueryFields.forEach(fieldName->schema.properties().put(getToolArgName(fieldName), 
                    JsonHelper.getObjectMapper().createObjectNode()
                    .put("type", "regex").put("description", getToolArgDescription(fieldName))));
        }
        
        public static final String getToolArgName(String fieldName) {
            return String.format("--match-%s", fieldName);
        }
        
        public static final String getToolArgDescription(String fieldName) {
            return String.format("Return only records for which the %s field matches the given regular expression", fieldName);
        }
        
        @Override
        public String getFcliCmdArgs(Map<String, Object> toolArgs) {
            var queries = new ArrayList<String>();
            for ( var fieldName : commonQueryFields ) {
                var value = toolArgs.get(getToolArgName(fieldName));
                if ( value!=null ) {
                    queries.add(String.format("%s matches '%s'", fieldName, value));
                }
            }
            return queries.isEmpty() ? "" : String.format("\"--query=%s\"", String.join(" && ", queries));
        }
        
    }
    
    private static abstract class AbstractArgSpecToolSpecArgHelper implements IToolSpecArgHelper { 
        protected abstract ArgSpec getArgSpec();
        protected abstract String getName();
        protected abstract String combineFcliCmdArgs(String name, Stream<String> values);
        @Override
        public void updateSchema(JsonSchema schema) {
            var argSpec = getArgSpec();
            schema.properties().put(getName(), createProperty(argSpec));
            if ( isRequired(argSpec) ) {
                schema.required().add(getName());
            }
        }
        
        @Override
        public String getFcliCmdArgs(Map<String, Object> toolArgs) {
            var name = getName();
            var toolArgValue = toolArgs.get(name);
            return toolArgValue==null ? "" : combineFcliCmdArgs(name, streamValueElts(toolArgValue));
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
        
        private static final boolean isRequired(ArgSpec argSpec) {
            return argSpec.required(); // TODO If option is contained in exclusive arggroup, we need to consider it as optional
        }
        private static final ObjectNode createProperty(ArgSpec argSpec) {
            return JsonHelper.getObjectMapper().createObjectNode()
                    .put("description", getDescription(argSpec))
                    .set("type", getPropertyType(argSpec.typeInfo()));
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
    
        private static String getDescription(ArgSpec argSpec) {
            String[] descElts = argSpec.description(); 
            return descElts==null || descElts.length<1 ? "" : String.join(" ", descElts);
        }
    }
    
    @RequiredArgsConstructor
    private static final class PositionalParamToolSpecArgHelper extends AbstractArgSpecToolSpecArgHelper {
        @Getter private final PositionalParamSpec argSpec;
        @Override
        protected String getName() {
           return ((Field)argSpec.userObject()).getName();
        }
        @Override
        protected String combineFcliCmdArgs(String name, Stream<String> values) {
            return values.map(v->"\""+v+"\"").collect(Collectors.joining(" "));
        }
    }
    
    @RequiredArgsConstructor
    private static final class OptionToolSpecArgHelper extends AbstractArgSpecToolSpecArgHelper {
        @Getter private final OptionSpec argSpec;
        @Override
        protected String getName() {
            return argSpec.longestName();
        }
        @Override
        protected String combineFcliCmdArgs(String name, Stream<String> values) {
            return String.format("\"%s=%s\"", name, values.collect(Collectors.joining(",")));
        }
    }
}
