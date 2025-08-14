/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.action.runner;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;
import org.springframework.integration.json.JsonPropertyAccessor.JsonNodeWrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.schema.ActionSchemaDescriptorFactory;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.FortifyTraceNodeHelper;
import com.fortify.cli.common.json.JSONDateTimeConverter;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.fn.descriptor.SpelFunctionDescriptorsFactory;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.common.util.IssueSourceFileResolver;
import com.fortify.cli.common.util.StringHelper;

import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
public class ActionSpelFunctions {
    //private static final Logger LOG = LoggerFactory.getLogger(ActionSpelFunctions.class);
    private static final String CODE_START = "\n===== CODE START =====\n";
    private static final String CODE_END   = "\n===== CODE END =====\n";
    private static final Pattern CODE_PATTERN = Pattern.compile(String.format("%s(.*?)%s", CODE_START, CODE_END), Pattern.DOTALL);
    private static final Pattern uriPartsPattern = Pattern.compile("^(?<serverUrl>(?:(?<protocol>[A-Za-z]+):)?(\\/{0,3})(?<host>[0-9.\\-A-Za-z]+)(?::(?<port>\\d+))?)(?<path>\\/(?<relativePath>[^?#]*))?(?:\\?(?<query>[^#]*))?(?:#(?<fragment>.*))?$");
    private static final Map<String,Set<String>> builtinActionNamesByModule = new HashMap<>();
    
	@SpelFunction(desc="Resolves the given path against the current working directory.",
	        returns="The absolute, normalized path")
	public static final String resolveAgainstCurrentWorkDir(
			@SpelFunctionParam(name="path", desc="the path to resolve against the current working directory") String path)
	{
		return Path.of(".").resolve(path).toAbsolutePath().normalize().toString();
	}
    
	@SpelFunction(returns="String consisting of the joined elements, separated by the given delimiter")
	public static final String join(
			@SpelFunctionParam(name="delimiter", desc="the delimiter to be used between each element") String delimiter,
			@SpelFunctionParam(name="input", desc="the elements to join", type = "array") Object source)
	{
		switch (delimiter) {
		case "\\n":
		    delimiter = "\n";
			break;
		case "\\t":
		    delimiter = "\t";
			break;
		}
		Stream<?> stream = null;
		if (source instanceof Collection) {
			stream = ((Collection<?>) source).stream();
		} else if (source instanceof ArrayNode) {
			stream = JsonHelper.stream((ArrayNode) source);
		}
		return stream == null ? "" : stream.map(ActionSpelFunctionsHelper::toString).collect(Collectors.joining(delimiter));
	}
	
	@SpelFunction(returns="String consisting of the joined elements separated by the given delimiter _if all elements are non-null_; otherwise `null`")
    public static final String joinOrNull(
            @SpelFunctionParam(name="delimiter", desc="the delimiter to be used between each element") String delimiter,
            @SpelFunctionParam(name="input", desc="the elements to join") String... parts) 
	{
        if (parts == null || Arrays.asList(parts).stream().anyMatch(Objects::isNull)) {return null;}
        return String.join(delimiter, parts);
    }
    
	@SpelFunction(desc = "Returns a literal regex pattern string for the given input string, escaping any characters that have a special meaning in regular expressions.",
	        returns="The regex-quoted string")
	public static final String regexQuote(
			@SpelFunctionParam(name="input", desc="the string to be quoted") String s)
	{
		return Pattern.quote(s);
	}
    
	@SpelFunction(desc = """
	        Replaces all occurrences in the input string based on regex patterns and replacement
	        values provided in the mapping object.
	        """,
	        returns="The input string with all replacements applied")
	public static final String replaceAllFromRegExMap(
			@SpelFunctionParam(name="input", desc="the input string on which to apply replacements") String s,
			@SpelFunctionParam(name="replacements", desc="map containing regex patterns as keys and replacement strings as values", type = "map<string,string>") Object mappingObject)
	{
		var mappingNode = mappingObject instanceof ObjectNode ? (ObjectNode) mappingObject
				: mappingObject instanceof JsonNodeWrapper ? ((JsonNodeWrapper<?>) mappingObject).getRealNode()
						: JsonHelper.getObjectMapper().valueToTree(mappingObject);
		if (!mappingNode.isObject()) {
			throw new FcliTechnicalException("replaceAllFromRegExMap must be called with Map or ObjectNode, actual type: "
					+ mappingObject.getClass().getSimpleName());
		}
		var fields = ((ObjectNode) mappingNode).fields();
		while (fields.hasNext()) {
			var field = fields.next();
			s = s.replaceAll(field.getKey(), field.getValue().asText());
		}
		return s;
	}
    
	@SpelFunction(desc = "Generates a numbered list from the given list of elements.",
	        returns="Numbered list of input elements, each on a new line") 
	public static final String numberedList(
			@SpelFunctionParam(name="input", desc="the list of elements to be numbered and joined") List<Object> elts)
	{
		StringBuilder builder = new StringBuilder();
		for (var i = 0; i < elts.size(); i++) {
			builder.append(i + 1).append(". ").append(elts.get(i)).append('\n');
		}
		return builder.toString();
	}
    
	@SpelFunction(desc = "Throws an error with the given message if the first argument evaluates to true.",
	        returns="`true` if no error is thrown")
	public static final boolean check(
			@SpelFunctionParam(name="throwError", desc="if `true`, an error will be thrown") boolean throwError,
			@SpelFunctionParam(name="msg", desc="the error message") String msg)
	{
		if (throwError) {
			throw new FcliActionStepException(msg);
		} else {
			return true;
		}
	}
    
	@SpelFunction(desc = "Repeats the input text a specified number of times.",
	        returns= "The input text repeated the given number of times") 
	public static final String repeat(
			@SpelFunctionParam(name="input", desc="the text to repeat.") String text,
			@SpelFunctionParam(name="count", desc="the number of times to repeat the text; if <=0, an empty string will be returned") int count)
	{
		if (count <= 0) { return ""; }
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; i++) { sb.append(text);}
		return sb.toString();
	}
    
	@SpelFunction(desc = "Converts the given HTML string into plain text.",
	        returns="The plain text extracted from the input HTML, or `null` if the input is `null`")
	public static final String htmlToText(
			@SpelFunctionParam(name="html", desc="the HTML string to convert to plain text; may be `null`") String html)
	{
		if (html == null) { return null; }
		Document document = ActionSpelFunctionsJsoupHelper.asDocument(html);
		return ActionSpelFunctionsJsoupHelper.documentToPlainText(document);
	}
    
	@SpelFunction(desc = "Cleans the given rule description and returns it as plain text.",
	        returns="The cleaned rule description, or empty string if input is `null`")
	public static final String cleanRuleDescription(
			@SpelFunctionParam(name="ruleDescription", desc="the rule description string to be cleaned; may be `null`") String description)
	{
		if (description == null) { return ""; }
		Document document = ActionSpelFunctionsJsoupHelper.asDocument(description);
		var paragraphs = document.select("Paragraph");
		for (var p : paragraphs) {
			var altParagraph = p.select("AltParagraph");
			if (!altParagraph.isEmpty()) { p.replaceWith(new TextNode(String.join("\n\n", altParagraph.eachText())));} 
			else { p.remove(); }
		}
		document.select("IfDef").remove();
		document.select("ConditionalText").remove();
		return ActionSpelFunctionsJsoupHelper.documentToPlainText(document);
	}

	@SpelFunction(desc = "Cleans the given issue description and returns it as plain text.",
	        returns="The cleaned issue description, or empty string if input is `null`")
	public static final String cleanIssueDescription(
			@SpelFunctionParam(name="issueDescription", desc="the issue description string to be cleaned") String description)
	{
		if (description == null) { return ""; }
		Document document = ActionSpelFunctionsJsoupHelper.asDocument(description);
		document.select("AltParagraph").remove();
		return ActionSpelFunctionsJsoupHelper.documentToPlainText(document);
	}
	
	@SpelFunction(desc = "Converts the given HTML string into a single-line plain text string by removing all HTML tags.",
	        returns="The plain text representation of the given HTML input, or `null` if the input is `null`")
	public static final String htmlToSingleLineText(
			@SpelFunctionParam(name="html", desc="the HTML string to convert to single-line plain text") String html) 
	{
		if (html == null) { return null; }
		return Jsoup.clean(html, "", Safelist.none());
	}

	@SpelFunction(desc = "Retrieves a given part of the given URI",
	        returns="Requested part of the given URI, or `null` if part name is not valid or not present") 
    public static final String uriPart(
            @SpelFunctionParam(name="uri", desc="URI from which to retrieve the requested part") String uriString, 
            @SpelFunctionParam(name="part", desc="URI part to be returned; may be one of serverUrl, protocol, host, port, path, relativePath, query, fragment") String part)
	{
        if ( StringUtils.isBlank(uriString) ) {return null;}
        // We use a regex as WebInspect results may contain URL's that contain invalid characters according to URI class
        Matcher matcher = uriPartsPattern.matcher(uriString);
        return matcher.matches() ? matcher.group(part) : null;
    }
    
	@SpelFunction(desc = """
	        Returns either current or given date/time formatted according to the given formatter pattern. See
	        'Patterns for Formatting and Parsing' section at
	        https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html
	        for details on formatter pattern syntax. If the given date/time doesn't include time zone, system
            default time zone will be assumed.
	        """,
            returns="Formatted date/time")
	public static final String formatDateTime(
			@SpelFunctionParam(name="fmt", desc="formatter pattern used to format given or current date/time") String pattern,
			@SpelFunctionParam(
			  desc = "optional date/time in JSON format to be formatted; if not specified, current date/time will be formatted",
			  name="input", type = "string", optional = true) String... dateStrings)
	{
		var dateString = dateStrings == null || dateStrings.length == 0 ? currentDateTime() : dateStrings[0];
		return formatDateTimeWithZoneId(pattern, dateString, ZoneId.systemDefault());
	}
	
	@SpelFunction(desc = """
	        Returns given date/time formatted according to the given formatter pattern. See 'Patterns for Formatting
	        and Parsing' section at
	        https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html
            for details on formatter pattern syntax. If the given date/time doesn't include time zone, the given
            default time zone id will be used.
	        """,
            returns="Formatted date/time")
    public static final String formatDateTimeWithZoneId(
            @SpelFunctionParam(name="fmt", desc="formatter pattern used to format given date/time") String pattern,
            @SpelFunctionParam(name="input", desc="date/time in JSON format to be formatted") String dateString,
            @SpelFunctionParam(name="tz", desc="default time zone id if given date/time doesn't include time zone") ZoneId defaultZoneId)
	{
        ZonedDateTime zonedDateTime = new JSONDateTimeConverter(defaultZoneId).parseZonedDateTime(dateString);
        return DateTimeFormatter.ofPattern(pattern).format(zonedDateTime);
    }
	
	@SpelFunction(desc = """
            Converts given date/time to UTC time zone and formats the result according to the given formatter pattern. 
            See 'Patterns for Formatting and Parsing' section at
            https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html
            for details on formatter pattern syntax. If the given date/time doesn't include time zone, system
            default time zone will be assumed.
            """,
            returns="Formatted date/time")
    public static final String formatDateTimeAsUTC(
            @SpelFunctionParam(name="fmt", desc="formatter pattern used to format given date/time") String pattern,
            @SpelFunctionParam(name="input", desc="date/time in JSON format to be formatted") String dateString) 
	{
        return formatDateTimewithZoneIdAsUTC(pattern, dateString, ZoneId.systemDefault());
    }
    
	@SpelFunction(desc = """
            Converts given date/time to UTC time zone and formats the result according to the given formatter pattern. 
            See 'Patterns for Formatting and Parsing' section at
            https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html
            for details on formatter pattern syntax. If the given date/time doesn't include time zone, the given
            default time zone id will be used.
            """,
            returns="Formatted date/time")
    public static final String formatDateTimewithZoneIdAsUTC(
            @SpelFunctionParam(name="fmt", desc="formatter pattern used to format given date/time") String pattern,
            @SpelFunctionParam(name="input", desc="date/time in JSON format to be formatted") String dateString,
            @SpelFunctionParam(name="tz", desc="default time zone id if given date/time doesn't include time zone") ZoneId defaultZoneId)
	{
        ZonedDateTime zonedDateTime = new JSONDateTimeConverter(defaultZoneId).parseZonedDateTime(dateString);
        LocalDateTime utcDateTime = LocalDateTime.ofInstant(zonedDateTime.toInstant(), ZoneOffset.UTC);
        return DateTimeFormatter.ofPattern(pattern).format(utcDateTime);
    }
    
	@SpelFunction(returns="The current date/time as `yyyy-MM-dd HH:mm:ss`")
	public static final String currentDateTime() {
		return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
	}
 
	@SpelFunction(desc= "Given an environment variable prefix, module name, and built-in fcli action name, "
			+ "this method returns the fcli command for running the action, allowing the action "
			+ "name to overridden, and extra options to be specified, through environment variables"
			+ "that are based on the given environment variable prefix. Some examples:\r\n"
			+ "- If envPrefix is `SETUP`, we look for SETUP_ACTION and SETUP_EXTRA_OPTS\n"
			+ "- If envPrefix is `PACKAGE_ACTION`, we look for PACKAGE_ACTION and PACKAGE_ACTION_EXTRA_OPTS\n"
			+ "\r\n"
			+ "As can be seen in the second example, if the given envPrefix already ends with _ACTION, "
			+ "we skip the extra _ACTION suffixes, to avoid looking for PACKAGE_ACTION_ACTION. However, "
			+ "we do keep _ACTION for the extra options environment variable, to allow for having both "
			+ "PACKAGE_EXTRA_OPTS (on the `scancentral package` command), and PACKAGE_ACTION_EXTRA_OPTS "
			+ "(on the `fcli * action run package` command).",
            returns="the formatted command string to run the specified action")
    public static final String actionCmd(
    		@SpelFunctionParam(name="envPrefix", desc="the environment variable prefix used for determining action options") String envPrefix, 
    		@SpelFunctionParam(name="moduleName", desc="the name of the module to run the action on") String moduleName,
    		@SpelFunctionParam(name="actionName", desc="the name of the action to execute") String actionName)
	{
        return String.format("fcli %s action run \"%s\" %s",
                moduleName,
                // If envPrefix is <cmd>_ACTION, we remove want to avoid <cmd>_ACTION_ACTION,
                // however we'd still use <cmd>_ACTION_EXTRA_OPTS
                ActionSpelFunctionsHelper.envOrDefault(envPrefix.replaceAll("_ACTION$", ""), "ACTION", actionName),
                extraOpts(envPrefix));
    }
    
	@SpelFunction(desc = "Constructs the fcli command string using the given environment prefix and command.",
	        returns="the formatted command string with extra options appended") 
	public static final String fcliCmd(
			@SpelFunctionParam(name="envPrefix", desc="the environment variable prefix used to determine extra options") String envPrefix,
			@SpelFunctionParam(name="cmd", desc="the base command to be executed") String cmd)
	{
		return String.format("%s %s", cmd, extraOpts(envPrefix));
	}
    
	@SpelFunction(desc = "Determines the reason to skip executing an action command based on environment variables and available built-in actions.",
            returns="a message explaining why the action command should be skipped, or null if no skip is needed") 
	public static final String actionCmdSkipNoActionReason(
			@SpelFunctionParam(name="envPrefix", desc="the environment variable prefix used to check the action environment variable") String envPrefix,
			@SpelFunctionParam(name="moduleName", desc="the name of the module to check for built-in actions") String moduleName,
			@SpelFunctionParam(name="actionName", desc="the name of the action to check for availability") String actionName)
	{
		var actionEnvValue = EnvHelper.env(String.format("%s_ACTION", envPrefix.replaceAll("_ACTION$", "")));
		if (StringUtils.isBlank(actionEnvValue)) {
			if (StringUtils.isBlank(actionName)) {
				return "No built-in action available";
			}
			if (!ActionSpelFunctionsHelper.hasBuiltInAction(moduleName, actionName)) {
				return String.format("Built-in %s action %s doesn't exist", moduleName, actionName);
			}
		}
		return null;
	}
    
	@SpelFunction(desc = "Determines the reason to skip an action command based on environment variables and a default skip flag.",
            returns="a message explaining why the action is skipped, or null if the action should proceed")
	public static final String actionCmdSkipFromEnvReason(
			@SpelFunctionParam(name="envPrefix", desc="the environment variable prefix used to construct related environment variable names") String envPrefix,
			@SpelFunctionParam(name="skipByDefault", desc="flag indicating whether to skip by default when no relevant environment variables are set") boolean skipByDefault)
	{
		var doEnvName = String.format("DO_%s", envPrefix);
		var doEnvValue = EnvHelper.env(doEnvName);
		if (StringUtils.isNotBlank(doEnvValue)) {
			switch (doEnvValue.toLowerCase()) {
			case "true":
				return null;
			case "false":
				return String.format("%s set to 'false'", doEnvName);
			default:
				throw new FcliSimpleException(String
						.format("%s must be either blank, true, or false; current value: %s", doEnvName, doEnvValue));
			}
		}
		var actionEnvValue = EnvHelper.env(String.format("%s_ACTION", envPrefix.replaceAll("_ACTION$", "")));
		var extraOptsEnvValue = EnvHelper.env(String.format("%s_EXTRA_OPTS", envPrefix));
		if (StringUtils.isNotBlank(actionEnvValue) || StringUtils.isNotBlank(extraOptsEnvValue)) {
			return null;
		}
		return skipByDefault ? String.format("Set %s to 'true' to enable this step", doEnvName) : null;
	}
    
	@SpelFunction(desc = "Determines the reason to skip an fcli command based on environment variables and a default skip flag.",
            returns="a message explaining why the fcli command is skipped, or null if the command should proceed") 
	public static final String fcliCmdSkipFromEnvReason(
			@SpelFunctionParam(name="envPrefix", desc="the environment variable prefix used to construct related environment variable names") String envPrefix,
			@SpelFunctionParam(name="skipByDefault", desc="flag indicating whether to skip by default when no relevant environment variables are set") boolean skipByDefault)
	{
		var doEnvName = String.format("DO_%s", envPrefix);
		var doEnvValue = EnvHelper.env(doEnvName);
		if (StringUtils.isNotBlank(doEnvValue)) {
			switch (doEnvValue.toLowerCase()) {
			case "true":
				return null;
			case "false":
				return String.format("%s set to 'false'", doEnvName);
			default:
				throw new FcliSimpleException(String
						.format("%s must be either blank, true, or false; current value: %s", doEnvName, doEnvValue));
			}
		}
		var extraOptsEnvValue = EnvHelper.env(String.format("%s_EXTRA_OPTS", envPrefix));
		if (StringUtils.isNotBlank(extraOptsEnvValue)) {
			return null;
		}
		return skipByDefault ? String.format("Set %s to 'true' to enable this step", doEnvName) : null;
	}
    
	@SpelFunction(desc = "Returns the specified reason if the skip condition is true; otherwise returns null.",
            returns="the reason string if skip is true; otherwise null") 
	public static final String skipReasonIf(
			@SpelFunctionParam(name="skip", desc="the condition indicating whether to skip") boolean skip,
			@SpelFunctionParam(name="reason", desc="the reason to return if skipping") String reason)
	{
		return skip ? reason : null;
	}
    
	@SpelFunction(desc = "Returns null if the specified environment variable is set and not blank; otherwise returns a message indicating it is not set.",
            returns="null if the environment variable is set and not blank; otherwise a message indicating the variable is not set") 
	public static final String skipBlankEnvReason(
			@SpelFunctionParam(name="", desc="the name of the environment variable to check") String envName) 
	{
		return StringUtils.isNotBlank(EnvHelper.env(envName)) ? null : String.format("%s not set", envName);
	}

	@SpelFunction(desc = "If a custom action has been configured through _ACTION env var, this method returns true. "
			+ "If no custom action has been configured, this method checks whether a built-in action "
			+ "exists with the given name.",
            returns="`true` if an action is specified in the environment or is a built-in action; otherwise `false`")
    public static final  boolean hasAction(
    		@SpelFunctionParam(name="envPrefix", desc="the environment variable prefix used to check for an action") String envPrefix,
    		@SpelFunctionParam(name="moduleName", desc="the name of the module to check for a built-in action") String moduleName,
    		@SpelFunctionParam(name="actionName", desc="the name of the action to check for availability") String actionName)
	{
        var envValue = ActionSpelFunctionsHelper.envOrDefault(envPrefix.replaceAll("_ACTION$", ""), "ACTION", null);
        return StringUtils.isNotBlank(envValue) ? true : ActionSpelFunctionsHelper.hasBuiltInAction(moduleName, actionName);
    }
    
	@SpelFunction(desc = "Returns the action name if it is a built-in action for the specified module; otherwise returns `null`.",
            returns="the action name if available as a built-in action; otherwise `null`") 
	public static final String actionOrNull(
			@SpelFunctionParam(name="moduleName", desc="the name of the module to check for the built-in action") String moduleName,
			@SpelFunctionParam(name="actionName", desc="the name of the action to verify") String actionName)
	{
		return ActionSpelFunctionsHelper.hasBuiltInAction(moduleName, actionName) ? actionName : null;
	}
    
    /**
     * This method takes a string in the format "--opt1=ENV1 -o=ENV2 ...", outputting a string
     * with environment variable names replaced by the corresponding values. Options for which
     * the environment variable value is null or empty will be removed.
     */
	@SpelFunction(desc = "Constructs a string of option key-value pairs by looking up environment variable values based on the given opts string.",
            returns="a string of options formatted as `key=value` pairs with values fetched from the environment; empty string if input is blank or no matches found") 
	public static final String optsFromEnv(
			@SpelFunctionParam(name="opts", desc="the input string containing options in the format `key=ENV_VAR` separated by spaces") String opts)
	{
		if (StringUtils.isBlank(opts)) { return ""; }
		var output = new ArrayList<String>();
		var elts = opts.split(" ");
		for (var elt : elts) {
			var names = elt.split("=");
			var envValue = EnvHelper.env(names[1]);
			if (StringUtils.isNotBlank(envValue)) {
				output.add(String.format("\"%s=%s\"", names[0], envValue));
			}
		}
		return String.join(" ", output);
	}

    @SpelFunction(desc = "Retrieves the value of the environment variable constructed as envPrefix + `_EXTRA_OPTS`, or returns an empty string if not set.",
            returns="the value of the specified EXTRA_OPTS environment variable, or an empty string if not defined") 
    public static final String extraOpts(
        @SpelFunctionParam(name="envPrefix", desc="the environment variable prefix used to construct the full `EXTRA_OPTS` variable name") String envPrefix)
    {
        return ActionSpelFunctionsHelper.envOrDefault(envPrefix, "EXTRA_OPTS", "");
    }
    
	@SpelFunction(desc = "Converts the properties of the given ObjectNode into an `ArrayNode` of key-value pair objects.",
            returns="an ArrayNode containing objects with `key` and `value` fields from the input `ObjectNode`")
	public static final ArrayNode properties(
			@SpelFunctionParam(name="input", desc="the ObjectNode whose properties are to be converted") ObjectNode o)
	{
		var mapper = JsonHelper.getObjectMapper();
		var result = mapper.createArrayNode();
		o.properties()
				.forEach(p -> result.add(mapper.createObjectNode().put("key", p.getKey()).set("value", p.getValue())));
		return result;
	}

	@SpelFunction(desc = "Creates a POJONode wrapping an IssueSourceFileResolver built from the provided configuration map.",
            returns="a POJONode representing the configured IssueSourceFileResolver instance") 
	public static final POJONode issueSourceFileResolver(
			@SpelFunctionParam(name="config", desc="the configuration map containing settings such as 'sourceDir' for the resolver") Map<String, String> config) 
	{
		var sourceDir = config.get("sourceDir");
		var builder = IssueSourceFileResolver.builder()
				.sourcePath(StringUtils.isBlank(sourceDir) ? null : Path.of(sourceDir));
		// TODO Update builder based on other config properties
		return new POJONode(builder.build());
	}

	// TODO Add proper description, explaining what 'normalizing' means
	@SpelFunction(returns="normalized array of trace nodes") 
	public static final ArrayNode normalizeTraceNodes(
			@SpelFunctionParam(name="input", desc="the original, non-normalized array of trace nodes") ArrayNode traceNodes)
	{
		return FortifyTraceNodeHelper.normalize(traceNodes);
	}

	// TODO Add proper description, explaining what 'normalizing and merging' means
	@SpelFunction(returns="normalized and merged array of trace nodes") 
	public static final ArrayNode normalizeAndMergeTraceNodes(
			@SpelFunctionParam(name="input", desc="the original, non-normalized array of trace nodes") ArrayNode traceNodes)
	{
		return FortifyTraceNodeHelper.normalizeAndMerge(traceNodes);
	}

	@SpelFunction(returns="an object describing the fcli action YAML schema")
	public static final JsonNode actionSchema() {
		return ActionSchemaDescriptorFactory.getActionSchemaDescriptor().asJson();
	}

	@SpelFunction(returns="an array listing all available spEL functions")
	public static final JsonNode actionSpelFunctions() {
		return SpelFunctionDescriptorsFactory.getActionSpelFunctionsDescriptors().asJson();
	}

	@SpelFunction(returns="fcli build properties like version number and build date")
	public static final JsonNode fcliBuildProperties() {
		return JsonHelper.getObjectMapper().valueToTree(FcliBuildProperties.INSTANCE);
	}

	@SpelFunction(returns="copyright notice with the current year")
	public static final String copyright() {
		return String.format("Copyright (c) %s Open Text", Year.now().getValue());
	}
	
	private static final class ActionSpelFunctionsJsoupHelper {
        private static final void reeplaceCode(Element e) {
            var text = e.text();
            if ( text.contains("\n") ) {
                text = StringHelper.indent("\n\n" + CODE_START + text.replaceAll("\t", "    "), "    ") + CODE_END + "\n\n";
            } else {
                text = "`"+text+"`";
            }
            e.replaceWith(new TextNode(text));
        }

        private static final Document asDocument(String html) {
            Document document = Jsoup.parse(html);
            document.outputSettings(new Document.OutputSettings().prettyPrint(false));//makes html() preserve linebreaks and spacing
            return document;
        }

        private static String documentToPlainText(Document document) {
            document.select("li").append("\\n");
            document.select("br").forEach(e->e.replaceWith(new TextNode("\n")));
            document.select("p").prepend("\\n\\n");
            // Replace code blocks, either embedding in backticks if inline (no newline characters)
            // or indenting with 4 spaces and fencing with CODE_START and CODE_END, which will remain
            // in place when cleaning all HTML tags, and removed using pattern matching below.
            document.select("span.code").forEach(ActionSpelFunctionsJsoupHelper::reeplaceCode);
            document.select("code").forEach(ActionSpelFunctionsJsoupHelper::reeplaceCode);
            document.select("pre").forEach(ActionSpelFunctionsJsoupHelper::reeplaceCode);
            
            // Remove all HTML tags. Note that for now, this keeps escaped characters like &gt;
            // We may want to have separate methods or method parameter to allow for escaped
            // characters to be unescaped.
            var s = Jsoup.clean(document.html().replaceAll("\\\\n", "\n"), "", Safelist.none(), new Document.OutputSettings().prettyPrint(false));
            
            var sb = new StringBuilder();
            // Remove CODE_START and CODE_END fences
            Matcher m = CODE_PATTERN.matcher(s);
            while(m.find()){
                String code = m.group(1);
                // Code may contain regex-related characters like ${..}, which we don't
                // want to interpret as regex groups. So, we append an empty replacement
                // (have Matcher append all text before the code block), then manually 
                // append the code block. See https://stackoverflow.com/a/948381
                m.appendReplacement(sb, "");
                sb.append(Parser.unescapeEntities(code, false));
            }
            m.appendTail(sb);
            return sb.toString();
        }
	    
	}
	
	private static final class ActionSpelFunctionsHelper {
    	private static final String toString(Object o) {
            if ( o==null ) {
                return "";
            } else if ( o instanceof JsonNode ) {
                return ((JsonNode)o).asText();
            } else {
                return o.toString();
            }
        }
    	
    	/**
         * Given an environment variable prefix and suffix, this method will return
         * the value of the combined environment variable name, or the given default
         * value if the combined environment variable is not defined. 
         */
        private static final String envOrDefault(String prefix, String suffix, String defaultValue) {
            var envName = String.format("%s_%s", prefix, suffix).toUpperCase().replace('-', '_');
            var envValue = EnvHelper.env(envName);
            return StringUtils.isNotBlank(envValue) ? envValue : defaultValue; 
        }
        
        private static boolean hasBuiltInAction(String moduleName, String actionName) {
            if ( StringUtils.isBlank(actionName) ) { return false; }
            return builtinActionNamesByModule
                    .computeIfAbsent(moduleName, ActionSpelFunctionsHelper::getBuiltinActionNames)
                    .contains(actionName);
        }
        
        private static final Set<String> getBuiltinActionNames(String moduleName) {
            return ActionLoaderHelper
                        .streamAsNames(ActionSource.defaultActionSources(moduleName), ActionValidationHandler.IGNORE)
                        .collect(Collectors.toSet());
        }
	}
}
