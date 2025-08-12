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
import java.util.Iterator;
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
import com.fortify.cli.common.spring.expression.fn.metadata.SpelFunctionMetadataFactory;
import com.fortify.cli.common.spring.expression.fn.metadata.annotation.SpelFunctionDescription;
import com.fortify.cli.common.spring.expression.fn.metadata.annotation.SpelFunctionParamDescription;
import com.fortify.cli.common.spring.expression.fn.metadata.annotation.SpelFunctionReturnDescription;
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
    
	@SpelFunctionDescription("Resolves the given path against the current working directory.")
	public static final @SpelFunctionReturnDescription("the absolute, normalized path as a string") String resolveAgainstCurrentWorkDir(
			@SpelFunctionParamDescription("the path to resolve against the current working directory") String path) {
		return Path.of(".").resolve(path).toAbsolutePath().normalize().toString();
	}
    
	@SpelFunctionDescription("Joins elements from the given source into a single string separated by the specified separator.")
	public static final @SpelFunctionReturnDescription("a string consisting of the joined elements separated by the given separator") String join(
			@SpelFunctionParamDescription("the string to use as a separator between elements") String separator,
			@SpelFunctionParamDescription("the source object containing elements to join, either a `Collection` or an `ArrayNode`") Object source) {
		switch (separator) {
		case "\\n":
			separator = "\n";
			break;
		case "\\t":
			separator = "\t";
			break;
		}
		Stream<?> stream = null;
		if (source instanceof Collection) {
			stream = ((Collection<?>) source).stream();
		} else if (source instanceof ArrayNode) {
			stream = JsonHelper.stream((ArrayNode) source);
		}
		return stream == null ? "" : stream.map(ActionSpelFunctions::toString).collect(Collectors.joining(separator));
	}

    
    private static final String toString(Object o) {
        if ( o==null ) {
            return "";
        } else if ( o instanceof JsonNode ) {
            return ((JsonNode)o).asText();
        } else {
            return o.toString();
        }
    }
    
	@SpelFunctionDescription("Returns a literal pattern String for the specified string, escaping any regex special characters.")
	public static final @SpelFunctionReturnDescription("the quoted string suitable for use in a regular expression") String regexQuote(
			@SpelFunctionParamDescription("the input string to be quoted") String s) {
		return Pattern.quote(s);
	}
    
	@SpelFunctionDescription("Replaces all occurrences in the input string based on regex patterns and replacement values provided in the mapping object.")
	public static final @SpelFunctionReturnDescription("the resulting string after performing all replacements") String replaceAllFromRegExMap(
			@SpelFunctionParamDescription("the input string on which replacements will be performed") String s,
			@SpelFunctionParamDescription("the mapping object containing regex patterns as keys and replacement strings as values; can be an ObjectNode, JsonNodeWrapper, or any object convertible to a JSON tree") Object mappingObject) {

		var mappingNode = mappingObject instanceof ObjectNode ? (ObjectNode) mappingObject
				: mappingObject instanceof JsonNodeWrapper ? ((JsonNodeWrapper<?>) mappingObject).getRealNode()
						: JsonHelper.getObjectMapper().valueToTree(mappingObject);
		if (!mappingNode.isObject()) {
			throw new FcliTechnicalException("replaceAllFromMap must be called with Map or ObjectNode, actual type: "
					+ mappingObject.getClass().getSimpleName());
		}
		var fields = ((ObjectNode) mappingNode).fields();
		while (fields.hasNext()) {
			var field = fields.next();
			s = s.replaceAll(field.getKey(), field.getValue().asText());
		}
		return s;
	}
    
	@SpelFunctionDescription("Generates a numbered list string from the given list of elements, each prefixed with its position number.")
	public static final @SpelFunctionReturnDescription("a string representing the numbered list of elements, each on a new line") String numberedList(
			@SpelFunctionParamDescription("the list of elements to be numbered and joined") List<Object> elts) {
		StringBuilder builder = new StringBuilder();
		for (var i = 0; i < elts.size(); i++) {
			builder.append(i + 1).append(". ").append(elts.get(i)).append('\n');
		}
		return builder.toString();
	}
    
	/**
	 * Convenience method to throw an exception if an expression evaluates to false
	 * 
	 * @param throwError true if error should be thrown, false otherwise
	 * @param msg        Message for exception to be thrown
	 * @return true if throwError is false
	 * @throws IllegalStateException with the given message if throwError is true
	 */
	@SpelFunctionDescription("Checks the condition and either throws an exception with the specified message or returns `true`.")
	public static final @SpelFunctionReturnDescription("`true` if no exception is thrown") boolean check(
			@SpelFunctionParamDescription("if `true`, an exception will be thrown") boolean throwError,
			@SpelFunctionParamDescription("the message to use for the thrown exception") String msg) {

		if (throwError) {
			throw new FcliActionStepException(msg);
		} else {
			return true;
		}
	}
    
	@SpelFunctionDescription("Repeats the given text a specified number of times.")
	public static final @SpelFunctionReturnDescription("the concatenated string consisting of the text repeated count times") String repeat(
			@SpelFunctionParamDescription("the text to repeat") String text,
			@SpelFunctionParamDescription("the number of times to repeat the text; if negative, returns an empty string") int count) {
		if (count < 0) { return ""; }
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; i++) { sb.append(text);}
		return sb.toString();
	}
    
	@SpelFunctionDescription("Joins the given parts into a single string separated by the specified separator, or returns `null` if any part is `null`.")
	public static final @SpelFunctionReturnDescription("the joined string if all parts are non-null; otherwise `null`") String joinOrNull(
			@SpelFunctionParamDescription("the string to use as a separator between parts") String separator,
			@SpelFunctionParamDescription("the parts to join; must not contain null elements") String... parts) {
		if (parts == null || Arrays.asList(parts).stream().anyMatch(Objects::isNull)) {return null;}
		return String.join(separator, parts);
	}
    
	@SpelFunctionDescription("Converts the given HTML string into plain text.")
	public static final @SpelFunctionReturnDescription("the plain text extracted from the input HTML, or null if the input is `null`") String htmlToText(
			@SpelFunctionParamDescription("the HTML string to convert to plain text") String html) {
		if (html == null) { return null; }
		Document document = _asDocument(html);
		return _htmlToText(document);
	}

    private static final Document _asDocument(String html) {
        Document document = Jsoup.parse(html);
        document.outputSettings(new Document.OutputSettings().prettyPrint(false));//makes html() preserve linebreaks and spacing
        return document;
    }

    private static String _htmlToText(Document document) {
        document.select("li").append("\\n");
        document.select("br").forEach(e->e.replaceWith(new TextNode("\n")));
        document.select("p").prepend("\\n\\n");
        // Replace code blocks, either embedding in backticks if inline (no newline characters)
        // or indenting with 4 spaces and fencing with CODE_START and CODE_END, which will remain
        // in place when cleaning all HTML tags, and removed using pattern matching below.
        document.select("span.code").forEach(ActionSpelFunctions::_replaceCode);
        document.select("code").forEach(ActionSpelFunctions::_replaceCode);
        document.select("pre").forEach(ActionSpelFunctions::_replaceCode);
        
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
    
    private static final void _replaceCode(Element e) {
        var text = e.text();
        if ( text.contains("\n") ) {
			text = StringHelper.indent("\n\n" + CODE_START + text.replaceAll("\t", "    "), "    ") + CODE_END + "\n\n";
        } else {
            text = "`"+text+"`";
        }
        e.replaceWith(new TextNode(text));
    }
    
	@SpelFunctionDescription("Cleans the given rule description by processing specific HTML elements and converting it to plain text.")
	public static final @SpelFunctionReturnDescription("the cleaned and converted plain text from the input description; returns an empty string if input is `null`") String cleanRuleDescription(
			@SpelFunctionParamDescription("the HTML description string to be cleaned") String description) {
		if (description == null) { return ""; }
		Document document = _asDocument(description);
		var paragraphs = document.select("Paragraph");
		for (var p : paragraphs) {
			var altParagraph = p.select("AltParagraph");
			if (!altParagraph.isEmpty()) { p.replaceWith(new TextNode(String.join("\n\n", altParagraph.eachText())));} 
			else { p.remove(); }
		}
		document.select("IfDef").remove();
		document.select("ConditionalText").remove();
		return _htmlToText(document);
	}

	@SpelFunctionDescription("Cleans the given issue description by removing `AltParagraph` elements and converting it to plain text.")
	public static final @SpelFunctionReturnDescription("the cleaned and converted plain text from the input description; returns an empty string if input is `null`") String cleanIssueDescription(
			@SpelFunctionParamDescription("the HTML description string to be cleaned") String description) {
		if (description == null) { return ""; }
		Document document = _asDocument(description);
		document.select("AltParagraph").remove();
		return _htmlToText(document);
	}
	
	@SpelFunctionDescription("Converts the given HTML string into a single-line plain text string by removing all HTML tags.")
	public static final @SpelFunctionReturnDescription("the plain text extracted from the input HTML with all tags removed, or null if the input is `null`") String htmlToSingleLineText(
			@SpelFunctionParamDescription("the HTML string to convert to single-line plain text") String html) {
		if (html == null) { return null; }
		return Jsoup.clean(html, "", Safelist.none());
	}

	@SpelFunctionDescription("Parse the given uriString using the regular expression `#uriPartsPattern` and return the value of the named capture group specified by the `arg1` parameter.")
    public static final @SpelFunctionReturnDescription("Specified part of the given uriString") String uriPart(@SpelFunctionParamDescription("to be parsed") String uriString, @SpelFunctionParamDescription("to be returned") String part) {
        if ( StringUtils.isBlank(uriString) ) {return null;}
        // We use a regex as WebInspect results may contain URL's that contain invalid characters according to URI class
        Matcher matcher = uriPartsPattern.matcher(uriString);
        return matcher.matches() ? matcher.group(part) : null;
    }
    
	@SpelFunctionDescription("Parse the given dateString as a JSON date (see `JSONDateTimeConverter`, then format it using the given `DateTimeFormatter` pattern.")
	public static final @SpelFunctionReturnDescription("Formatted date") String formatDateTime(
			@SpelFunctionParamDescription("used to format the specified date") String pattern,
			@SpelFunctionParamDescription("JSON string representation of date to be formatted") String... dateStrings) {
		var dateString = dateStrings == null || dateStrings.length == 0 ? currentDateTime() : dateStrings[0];
		return formatDateTimeWithZoneId(pattern, dateString, ZoneId.systemDefault());
	}
    
	@SpelFunctionDescription("Returns the current date and time formatted as a string in `yyyy-MM-dd HH:mm:ss` pattern.")
	public static final @SpelFunctionReturnDescription("the current date and time as a formatted string") String currentDateTime() {
		return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
	}
    
	@SpelFunctionDescription("Parse the given dateString in the given time zone id as a JSON date (see `JSONDateTimeConverter`, then format it using the given `DateTimeFormatter` pattern.")
	public static final @SpelFunctionReturnDescription("Formatted date") String formatDateTimeWithZoneId(
			@SpelFunctionParamDescription("used to format the specified date") String pattern,
			@SpelFunctionParamDescription("JSON string representation of date to be formatted") String dateString,
			@SpelFunctionParamDescription("Default time zone id to be used if dateString doesn't provide time zone") ZoneId defaultZoneId) {
		ZonedDateTime zonedDateTime = new JSONDateTimeConverter(defaultZoneId).parseZonedDateTime(dateString);
		return DateTimeFormatter.ofPattern(pattern).format(zonedDateTime);
	}
    
	@SpelFunctionDescription("Parse the given dateString as a JSON date (see {`JSONDateTimeConverter`, convert it to UTC time, then format it using the given `DateTimeFormatter` pattern.")
	public static final @SpelFunctionReturnDescription("Formatted date") String formatDateTimeAsUTC(
			@SpelFunctionParamDescription("used to format the specified date") String pattern,
			@SpelFunctionParamDescription("JSON string representation of date to be formatted") String dateString) {
		return formatDateTimewithZoneIdAsUTC(pattern, dateString, ZoneId.systemDefault());
	}
    
	@SpelFunctionDescription("Parse the given dateString as a JSON date (see `JSONDateTimeConverter`, convert it to UTC time, then format it using the given `DateTimeFormatter` pattern.")
	public static final @SpelFunctionReturnDescription("Formatted date") String formatDateTimewithZoneIdAsUTC(
			@SpelFunctionParamDescription("used to format the specified date") String pattern,
			@SpelFunctionParamDescription("JSON string representation of date to be formatted") String dateString,
			@SpelFunctionParamDescription("Default time zone id to be used if dateString doesn't provide time zone") ZoneId defaultZoneId) {
		ZonedDateTime zonedDateTime = new JSONDateTimeConverter(defaultZoneId).parseZonedDateTime(dateString);
		LocalDateTime utcDateTime = LocalDateTime.ofInstant(zonedDateTime.toInstant(), ZoneOffset.UTC);
		return DateTimeFormatter.ofPattern(pattern).format(utcDateTime);
	}
    
	@SpelFunctionDescription("Converts the given Iterator into an Iterable, allowing it to be used in enhanced for-loops or other Iterable contexts.")
	public static final @SpelFunctionReturnDescription("an Iterable wrapping the provided Iterator") <T> Iterable<T> asIterable(
			@SpelFunctionParamDescription("the Iterator to convert into an Iterable") Iterator<T> iterator) {
		return () -> iterator;
	}
 
	@SpelFunctionDescription("Given an environment variable prefix, module name, and built-in fcli action name, "
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
			+ "(on the `fcli * action run package` command).")
    public static final @SpelFunctionReturnDescription("the formatted command string to run the specified action") String actionCmd(
    		@SpelFunctionParamDescription("the environment variable prefix used for determining action options") String envPrefix, 
    		@SpelFunctionParamDescription("the name of the module to run the action on") String moduleName,
    		@SpelFunctionParamDescription("the name of the action to execute") String actionName) {
        return String.format("fcli %s action run \"%s\" %s",
                moduleName,
                // If envPrefix is <cmd>_ACTION, we remove want to avoid <cmd>_ACTION_ACTION,
                // however we'd still use <cmd>_ACTION_EXTRA_OPTS
                _envOrDefault(envPrefix.replaceAll("_ACTION$", ""), "ACTION", actionName),
                extraOpts(envPrefix));
    }
    
	@SpelFunctionDescription("Constructs the fcli command string using the given environment prefix and command.")
	public static final @SpelFunctionReturnDescription("the formatted command string with extra options appended") String fcliCmd(
			@SpelFunctionParamDescription("the environment variable prefix used to determine extra options") String envPrefix,
			@SpelFunctionParamDescription("the base command to be executed") String cmd) {
		return String.format("%s %s", cmd, extraOpts(envPrefix));
	}
    
	@SpelFunctionDescription("Determines the reason to skip executing an action command based on environment variables and available built-in actions.")
	public static final @SpelFunctionReturnDescription("a message explaining why the action command should be skipped, or null if no skip is needed") String actionCmdSkipNoActionReason(
			@SpelFunctionParamDescription("the environment variable prefix used to check the action environment variable") String envPrefix,
			@SpelFunctionParamDescription("the name of the module to check for built-in actions") String moduleName,
			@SpelFunctionParamDescription("the name of the action to check for availability") String actionName) {
		var actionEnvValue = EnvHelper.env(String.format("%s_ACTION", envPrefix.replaceAll("_ACTION$", "")));
		if (StringUtils.isBlank(actionEnvValue)) {
			if (StringUtils.isBlank(actionName)) {
				return "No built-in action available";
			}
			if (!_hasBuiltInAction(moduleName, actionName)) {
				return String.format("Built-in %s action %s doesn't exist", moduleName, actionName);
			}
		}
		return null;
	}
    
	@SpelFunctionDescription("Determines the reason to skip an action command based on environment variables and a default skip flag.")
	public static final @SpelFunctionReturnDescription("a message explaining why the action is skipped, or null if the action should proceed") String actionCmdSkipFromEnvReason(
			@SpelFunctionParamDescription("the environment variable prefix used to construct related environment variable names") String envPrefix,
			@SpelFunctionParamDescription("flag indicating whether to skip by default when no relevant environment variables are set") boolean skipByDefault) {
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
    
	@SpelFunctionDescription("Determines the reason to skip an fcli command based on environment variables and a default skip flag.")
	public static final @SpelFunctionReturnDescription("a message explaining why the fcli command is skipped, or null if the command should proceed") String fcliCmdSkipFromEnvReason(
			@SpelFunctionParamDescription("the environment variable prefix used to construct related environment variable names") String envPrefix,
			@SpelFunctionParamDescription("flag indicating whether to skip by default when no relevant environment variables are set") boolean skipByDefault) {
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
    
	@SpelFunctionDescription("Returns the specified reason if the skip condition is true; otherwise returns null.")
	public static final @SpelFunctionReturnDescription("the reason string if skip is true; otherwise null") String skipReasonIf(
			@SpelFunctionParamDescription("the condition indicating whether to skip") boolean skip,
			@SpelFunctionParamDescription("the reason to return if skipping") String reason) {
		return skip ? reason : null;
	}
    
	@SpelFunctionDescription("Returns null if the specified environment variable is set and not blank; otherwise returns a message indicating it is not set.")
	public static final @SpelFunctionReturnDescription("null if the environment variable is set and not blank; otherwise a message indicating the variable is not set") String skipBlankEnvReason(
			@SpelFunctionParamDescription("the name of the environment variable to check") String envName) {
		return StringUtils.isNotBlank(EnvHelper.env(envName)) ? null : String.format("%s not set", envName);
	}

	@SpelFunctionDescription("If a custom action has been configured through _ACTION env var, this method returns true. "
			+ "If no custom action has been configured, this method checks whether a built-in action "
			+ "exists with the given name.")
    public static final @SpelFunctionReturnDescription("`true` if an action is specified in the environment or is a built-in action; otherwise `false`") boolean hasAction(
    		@SpelFunctionParamDescription("the environment variable prefix used to check for an action") String envPrefix,
    		@SpelFunctionParamDescription("the name of the module to check for a built-in action") String moduleName,
    		@SpelFunctionParamDescription("the name of the action to check for availability") String actionName) {
        var envValue = _envOrDefault(envPrefix.replaceAll("_ACTION$", ""), "ACTION", null);
        return StringUtils.isNotBlank(envValue) ? true : _hasBuiltInAction(moduleName, actionName);
    }
    
	@SpelFunctionDescription("Returns the action name if it is a built-in action for the specified module; otherwise returns `null`.")
	public static final @SpelFunctionReturnDescription("the action name if available as a built-in action; otherwise `null`") String actionOrNull(
			@SpelFunctionParamDescription("the name of the module to check for the built-in action") String moduleName,
			@SpelFunctionParamDescription("the name of the action to verify") String actionName) {
		return _hasBuiltInAction(moduleName, actionName) ? actionName : null;
	}
    
    private static boolean _hasBuiltInAction(String moduleName, String actionName) {
        if ( StringUtils.isBlank(actionName) ) { return false; }
        return builtinActionNamesByModule
                .computeIfAbsent(moduleName, ActionSpelFunctions::_getBuiltinActionNames)
                .contains(actionName);
    }
    
    private static final Set<String> _getBuiltinActionNames(String moduleName) {
        return ActionLoaderHelper
                    .streamAsNames(ActionSource.defaultActionSources(moduleName), ActionValidationHandler.IGNORE)
                    .collect(Collectors.toSet());
    }
    
    /**
     * This method takes a string in the format "--opt1=ENV1 -o=ENV2 ...", outputting a string
     * with environment variable names replaced by the corresponding values. Options for which
     * the environment variable value is null or empty will be removed.
     */
	@SpelFunctionDescription("Constructs a string of option key-value pairs by looking up environment variable values based on the given opts string.")
	public static final @SpelFunctionReturnDescription("a string of options formatted as `key=value` pairs with values fetched from the environment; empty string if input is blank or no matches found") String optsFromEnv(
			@SpelFunctionParamDescription("the input string containing options in the format `key=ENV_VAR` separated by spaces") String opts) {
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

    @SpelFunctionDescription("Retrieves the value of the environment variable constructed as envPrefix + `_EXTRA_OPTS`, or returns an empty string if not set.")
    public static final @SpelFunctionReturnDescription("the value of the specified EXTRA_OPTS environment variable, or an empty string if not defined") String extraOpts(
        @SpelFunctionParamDescription("the environment variable prefix used to construct the full `EXTRA_OPTS` variable name") String envPrefix) {
        return _envOrDefault(envPrefix, "EXTRA_OPTS", "");
    }

    
    /**
     * Given an environment variable prefix and suffix, this method will return
     * the value of the combined environment variable name, or the given default
     * value if the combined environment variable is not defined. 
     */
    private static final String _envOrDefault(String prefix, String suffix, String defaultValue) {
        var envName = String.format("%s_%s", prefix, suffix).toUpperCase().replace('-', '_');
        var envValue = EnvHelper.env(envName);
        return StringUtils.isNotBlank(envValue) ? envValue : defaultValue; 
    }
    
	@SpelFunctionDescription("Converts the properties of the given ObjectNode into an `ArrayNode` of key-value pair objects.")
	public static final @SpelFunctionReturnDescription("an ArrayNode containing objects with `key` and `value` fields from the input `ObjectNode`") ArrayNode properties(
			@SpelFunctionParamDescription("the ObjectNode whose properties are to be converted") ObjectNode o) {

		var mapper = JsonHelper.getObjectMapper();
		var result = mapper.createArrayNode();
		o.properties()
				.forEach(p -> result.add(mapper.createObjectNode().put("key", p.getKey()).set("value", p.getValue())));
		return result;
	}

	@SpelFunctionDescription("Creates a POJONode wrapping an IssueSourceFileResolver built from the provided configuration map.")
	public static final @SpelFunctionReturnDescription("a POJONode representing the configured IssueSourceFileResolver instance") POJONode issueSourceFileResolver(
			@SpelFunctionParamDescription("the configuration map containing settings such as 'sourceDir' for the resolver") Map<String, String> config) {

		var sourceDir = config.get("sourceDir");
		var builder = IssueSourceFileResolver.builder()
				.sourcePath(StringUtils.isBlank(sourceDir) ? null : Path.of(sourceDir));
		// TODO Update builder based on other config properties
		return new POJONode(builder.build());
	}

	@SpelFunctionDescription("Normalizes the given ArrayNode of trace nodes.")
	public static final @SpelFunctionReturnDescription("the normalized ArrayNode of trace nodes") ArrayNode normalizeTraceNodes(
			@SpelFunctionParamDescription("the ArrayNode containing trace nodes to normalize") ArrayNode traceNodes) {
		return FortifyTraceNodeHelper.normalize(traceNodes);
	}

	@SpelFunctionDescription("Normalizes and merges the given ArrayNode of trace nodes.")
	public static final @SpelFunctionReturnDescription("the normalized and merged ArrayNode of trace nodes") ArrayNode normalizeAndMergeTraceNodes(
			@SpelFunctionParamDescription("the ArrayNode containing trace nodes to normalize and merge") ArrayNode traceNodes) {
		return FortifyTraceNodeHelper.normalizeAndMerge(traceNodes);
	}

	@SpelFunctionDescription("Retrieve the JSON representation of the action schema.")
	public static final @SpelFunctionReturnDescription("a JsonNode representing the action schema descriptor") JsonNode actionSchema() {
		return ActionSchemaDescriptorFactory.getActionSchemaDescriptor().asJson();
	}

	@SpelFunctionDescription("Retrieve a JSON array listing all available spEL functions.")
	public static final @SpelFunctionReturnDescription("a JsonNode representing the SpEL functions descriptor") JsonNode spelFunctionsMetadata() {
		return SpelFunctionMetadataFactory.getSpelFunctionsDescriptor().asJson();
	}

	@SpelFunctionDescription("Converts the FcliBuildProperties singleton instance into a JsonNode representation.")
	public static final @SpelFunctionReturnDescription("a JsonNode representing the FcliBuildProperties instance") JsonNode fcliBuildProperties() {
		return JsonHelper.getObjectMapper().valueToTree(FcliBuildProperties.INSTANCE);
	}

	@SpelFunctionDescription("Returns the formatted copyright string for the current year.")
	public static final @SpelFunctionReturnDescription("a string representing the copyright notice with the current year") String copyright() {
		return String.format("Copyright (c) %s Open Text", Year.now().getValue());
	}
}
