package com.fortify.cli.ssc.domain.plugin.parser.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.micronaut.core.annotation.ReflectiveAccess;

import java.util.List;

@ReflectiveAccess
@JacksonXmlRootElement(localName = "plugin")
public class SSCPluginXmlPlugin {
    @JacksonXmlElementWrapper(localName = "plugin-info")
    public SSCPluginXmlPluginInfo pluginInfo;
    @JacksonXmlElementWrapper(localName = "issue-parser")
    public SSCPluginXmlIssueParser issue_parser;
    public String xmlns;
    public String xsi;
    public String type;
    public String id;
    @JacksonXmlElementWrapper(localName = "api-version")
    public String api_version;
    public String text;

    public static class Images {
        public List<SSCPluginXmlImage> image;
    }

    public static class Localization {
        public List<SSCPluginXmlLanguage> language;
    }
}
