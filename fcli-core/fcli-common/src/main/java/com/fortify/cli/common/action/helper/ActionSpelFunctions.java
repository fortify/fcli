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
package com.fortify.cli.common.action.helper;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JSONDateTimeConverter;
import com.fortify.cli.common.util.StringUtils;

import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
public class ActionSpelFunctions {
    private static final String CODE_START = "\n===== CODE START =====\n";
    private static final String CODE_END   = "\n===== CODE END =====\n";
    private static final Pattern CODE_PATTERN = Pattern.compile(String.format("%s(.*?)%s", CODE_START, CODE_END), Pattern.DOTALL);
    private static final Pattern uriPartsPattern = Pattern.compile("^(?<serverUrl>(?:(?<protocol>[A-Za-z]+):)?(\\/{0,3})(?<host>[0-9.\\-A-Za-z]+)(?::(?<port>\\d+))?)(?<path>\\/(?<relativePath>[^?#]*))?(?:\\?(?<query>[^#]*))?(?:#(?<fragment>.*))?$");
    
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
            throw new IllegalStateException(msg);
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
        document.select("span.code").forEach(ActionSpelFunctions::replaceCode);
        document.select("code").forEach(ActionSpelFunctions::replaceCode);
        document.select("pre").forEach(ActionSpelFunctions::replaceCode);
        
        var s = Jsoup.clean(document.html().replaceAll("\\\\n", "\n"), "", Safelist.none(), new Document.OutputSettings().prettyPrint(false));
        var sb = new StringBuilder();
        Matcher m = CODE_PATTERN.matcher(s);
        while(m.find()){
            String code = m.group(1);
            m.appendReplacement(sb, Parser.unescapeEntities(code, false));
        }
        m.appendTail(sb);
        return sb.toString();
    }
    
    private static final void replaceCode(Element e) {
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
    public static final String formatDateTime(String pattern, String dateString) {
        return formatDateTimeWithZoneId(pattern, dateString, ZoneId.systemDefault());
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
    
    public static final String copyright() {
        return String.format("Copyright (c) %s Open Text", Year.now().getValue());
    }

}
