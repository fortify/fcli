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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.json.JSONDateTimeConverter;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.StringUtils;

import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
public class ActionSpelFunctions {
    //private static final Logger LOG = LoggerFactory.getLogger(ActionSpelFunctions.class);
    private static final String CODE_START = "\n===== CODE START =====\n";
    private static final String CODE_END   = "\n===== CODE END =====\n";
    private static final Pattern CODE_PATTERN = Pattern.compile(String.format("%s(.*?)%s", CODE_START, CODE_END), Pattern.DOTALL);
    private static final Pattern uriPartsPattern = Pattern.compile("^(?<serverUrl>(?:(?<protocol>[A-Za-z]+):)?(\\/{0,3})(?<host>[0-9.\\-A-Za-z]+)(?::(?<port>\\d+))?)(?<path>\\/(?<relativePath>[^?#]*))?(?:\\?(?<query>[^#]*))?(?:#(?<fragment>.*))?$");
    private static final Map<String,Set<String>> builtinActionNamesByModule = new HashMap<>();
    
    public static final String resolveAgainstCurrentWorkDir(String path) {
        return Path.of(".").resolve(path).toAbsolutePath().normalize().toString();
    }
    
    public static final String join(String separator, List<Object> elts) {
        switch (separator) {
        case "\\n": separator="\n"; break;
        case "\\t": separator="\t"; break;
        }
        return elts==null ? "" : elts.stream().map(Object::toString).collect(Collectors.joining(separator));
    }
    
    public static final String numberedList(List<Object> elts) {
        StringBuilder builder = new StringBuilder();
        for ( var i=0; i < elts.size(); i++ ) {
            builder.append(i+1).append(". ").append(elts.get(i)).append('\n');
        }
        return builder.toString();
    }
    
    /**
     * Convenience method to throw an exception if an expression evaluates to false
     * @param throwError true if error should be thrown, false otherwise
     * @param msg Message for exception to be thrown
     * @return true if throwError is false
     * @throws IllegalStateException with the given message if throwError is true
     */
    public static final boolean check(boolean throwError, String msg) {
        if ( throwError ) {
            throw new FcliActionStepException(msg);
        } else {
            return true;
        }
    }
    
    /**
     * Abbreviate the given text to the given maximum width
     * @param text to abbreviate
     * @param maxWidth Maximum width
     * @return Abbreviated text 
     */
    public static final String abbreviate(String text, int maxWidth) {
        return StringUtils.abbreviate(text, maxWidth);
    }
    
    public static final String repeat(String text, int count) {
        if ( count<0 ) { return ""; }
        StringBuilder sb = new StringBuilder();
        for ( int i=0; i<count; i++ ) {
            sb.append(text);
        }
        return sb.toString();
    }
    
    public static final String joinOrNull(String separator, String... parts) {
        if ( parts==null || Arrays.asList(parts).stream().anyMatch(Objects::isNull) ) { return null; }
        return String.join(separator, parts);
    }
    
    /**
     * @param html to be converted to plain text
     * @return Formatted plain-text string for the given HTML contents
     */
    public static final String htmlToText(String html) {
        if( html==null ) { return null; }
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
            text = "\n\n"+CODE_START+StringUtils.indent(text.replaceAll("\t", "    "), "    ")+CODE_END+"\n\n";
        } else {
            text = "`"+text+"`";
        }
        e.replaceWith(new TextNode(text));
    }
    
    public static final String cleanRuleDescription(String description) {
        if( description==null ) { return ""; }
        Document document = _asDocument(description);
        var paragraphs = document.select("Paragraph");
        for ( var p : paragraphs ) {
            var altParagraph = p.select("AltParagraph");
            if ( !altParagraph.isEmpty() ) {
                p.replaceWith(new TextNode(String.join("\n\n",altParagraph.eachText())));
            } else {
                p.remove();
            }
        }
        document.select("IfDef").remove();
        document.select("ConditionalText").remove();
        return _htmlToText(document);
    }
    
    public static final String cleanIssueDescription(String description) {
        if( description==null ) { return ""; }
        Document document = _asDocument(description);
        document.select("AltParagraph").remove();
        return _htmlToText(document);
    }
    
    /**
     * @param html to be converted to plain text
     * @return Single line of plain text for the given HTML contents
     */
    public static final String htmlToSingleLineText(String html) {
        if( html==null ) { return null; }
        return Jsoup.clean(html, "", Safelist.none());
    }
    
    /**
     * Parse the given uriString using the regular expression <code>{@value #uriPartsPattern}</code> and return 
     * the value of the named capture group specified by the <code>part</code> parameter.
     * @param uriString to be parsed
     * @param part to be returned
     * @return Specified part of the given uriString
     */
    public static final String uriPart(String uriString, String part) {
        if ( StringUtils.isBlank(uriString) ) {return null;}
        // We use a regex as WebInspect results may contain URL's that contain invalid characters according to URI class
        Matcher matcher = uriPartsPattern.matcher(uriString);
        return matcher.matches() ? matcher.group(part) : null;
    }
    
    /**
     * Parse the given dateString as a JSON date (see {@link JSONDateTimeConverter}, then format it using the given
     * {@link DateTimeFormatter} pattern.
     * @param pattern used to format the specified date
     * @param dateString JSON string representation of date to be formatted
     * @return Formatted date
     */
    public static final String formatDateTime(String pattern, String... dateStrings) {
        var dateString = dateStrings==null||dateStrings.length==0 
                ? currentDateTime() 
                : dateStrings[0];
        return formatDateTimeWithZoneId(pattern, dateString, ZoneId.systemDefault());
    }
    
    public static final String currentDateTime() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
    }
    
    /**
     * Parse the given dateString in the given time zone id as a JSON date (see {@link JSONDateTimeConverter}, 
     * then format it using the given {@link DateTimeFormatter} pattern.
     * @param pattern used to format the specified date
     * @param dateString JSON string representation of date to be formatted
     * @param defaultZoneId Default time zone id to be used if dateString doesn't provide time zone 
     * @return Formatted date
     */
    public static final String formatDateTimeWithZoneId(String pattern, String dateString, ZoneId defaultZoneId) {
        ZonedDateTime zonedDateTime = new JSONDateTimeConverter(defaultZoneId).parseZonedDateTime(dateString);
        return DateTimeFormatter.ofPattern(pattern).format(zonedDateTime);
    }
    
    /**
     * Parse the given dateString as a JSON date (see {@link JSONDateTimeConverter}, convert it to UTC time,
     * then format it using the given {@link DateTimeFormatter} pattern.
     * @param pattern used to format the specified date
     * @param dateString JSON string representation of date to be formatted
     * @return Formatted date
     */
    public static final String formatDateTimeAsUTC(String pattern, String dateString) {
        return formatDateTimewithZoneIdAsUTC(pattern, dateString, ZoneId.systemDefault());
    }
    
    /**
     * Parse the given dateString as a JSON date (see {@link JSONDateTimeConverter}, convert it to UTC time,
     * then format it using the given {@link DateTimeFormatter} pattern.
     * @param pattern used to format the specified date
     * @param dateString JSON string representation of date to be formatted
     * @param defaultZoneId Default time zone id to be used if dateString doesn't provide time zone
     * @return Formatted date
     */
    public static final String formatDateTimewithZoneIdAsUTC(String pattern, String dateString, ZoneId defaultZoneId) {
        ZonedDateTime zonedDateTime = new JSONDateTimeConverter(defaultZoneId).parseZonedDateTime(dateString);
        LocalDateTime utcDateTime = LocalDateTime.ofInstant(zonedDateTime.toInstant(), ZoneOffset.UTC);
        return DateTimeFormatter.ofPattern(pattern).format(utcDateTime);
    }
    
    public static final <T> Iterable<T> asIterable(Iterator<T> iterator) { 
        return () -> iterator; 
    } 
    
    /**
     * Given an environment variable prefix, module name, and built-in fcli action name, 
     * this method returns the fcli command for running the action, allowing the action 
     * name to overridden, and extra options to be specified, through environment variables
     * that are based on the given environment variable prefix. Some examples:
     * <ul>
     * <li>If envPrefix is 'SETUP', we look for SETUP_ACTION and SETUP_EXTRA_OPTS</li>
     * <li>If envPrefix is 'PACKAGE_ACTION', we look for PACKAGE_ACTION and PACKAGE_ACTION_EXTRA_OPTS</li>
     * </ul> 
     * As can be seen in the second example, if the given envPrefix already ends with _ACTION,
     * we skip the extra _ACTION suffixes, to avoid looking for PACKAGE_ACTION_ACTION. However,
     * we do keep _ACTION for the extra options environment variable, to allow for having both
     * PACKAGE_EXTRA_OPTS (on the 'scancentral package' command), and PACKAGE_ACTION_EXTRA_OPTS
     * (on the 'fcli * action run package' command).
     */
    public static final String actionCmd(String envPrefix, String moduleName, String actionName) {
        return String.format("fcli %s action run \"%s\" %s",
                moduleName,
                // If envPrefix is <cmd>_ACTION, we remove want to avoid <cmd>_ACTION_ACTION,
                // however we'd still use <cmd>_ACTION_EXTRA_OPTS
                _envOrDefault(envPrefix.replaceAll("_ACTION$", ""), "ACTION", actionName),
                extraOpts(envPrefix));
    }
    
    /**
     * If a custom action has been configured through _ACTION env var, this method returns true.
     * If no custom action has been configured, this method checks whether a built-in action
     * exists with the given name.
     */
    public static final boolean hasAction(String envPrefix, String moduleName, String actionName) {
        var envValue = _envOrDefault(envPrefix.replaceAll("_ACTION$", ""), "ACTION", null);
        return StringUtils.isNotBlank(envValue) ? true : _hasBuiltInAction(moduleName, actionName);
    }
    
    private static boolean _hasBuiltInAction(String moduleName, String actionName) {
        return builtinActionNamesByModule
                .computeIfAbsent(moduleName, ActionSpelFunctions::_getBuiltinActionNames)
                .contains(actionName);
    }
    
    private static final Set<String> _getBuiltinActionNames(String moduleName) {
        return ActionLoaderHelper
                    .streamAsNames(ActionSource.defaultActionSources(moduleName), ActionValidationHandler.IGNORE)
                    .collect(Collectors.toSet());
    }

    public static final String fcliCmd(String envPrefix, String cmd) {
        return String.format("%s %s",
                cmd,
                extraOpts(envPrefix));
    }
    
    /**
     * This method takes a string in the format "--opt1=ENV1 -o=ENV2 ...", outputting a string
     * with environment variable names replaced by the corresponding values. Options for which
     * the environment variable value is null or empty will be removed.
     */
    public static final String optsFromEnv(String opts) {
        if ( StringUtils.isBlank(opts) ) { return ""; }
        var output = new ArrayList<String>();
        var elts = opts.split(" ");
        for ( var elt : elts ) {
            var names = elt.split("=");
            var envValue = EnvHelper.env(names[1]);
            if ( StringUtils.isNotBlank(envValue) ) {
                output.add(String.format("\"%s=%s\"", names[0], envValue));
            }
        }
        return String.join(" ", output);
    }

    /**
     * Given an environment variable prefix, this method returns the value of the 
     * envPrefix_EXTRA_OPTS environment variable if defined, or an empty string if not.
     */
    public static final String extraOpts(String envPrefix) {
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
    
    public static final ArrayNode properties(ObjectNode o) {
        var mapper = JsonHelper.getObjectMapper();
        var result = mapper.createArrayNode();
        o.properties().forEach(
                p->result.add(mapper.createObjectNode().put("key", p.getKey()).set("value", p.getValue())));
        return result;
    }
    
    public static final String copyright() {
        return String.format("Copyright (c) %s Open Text", Year.now().getValue());
    }
}
