package com.fortify.cli.ssc.domain.plugin.parser.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import io.micronaut.core.annotation.ReflectiveAccess;

@ReflectiveAccess
public class SSCPluginXmlIssueParser {
    @JacksonXmlElementWrapper(localName = "engine-type")
    public String engine_type;
    @JacksonXmlElementWrapper(localName = "supported-engine-versions")
    public String supported_engine_versions;
    @JacksonXmlElementWrapper(localName = "view-template")
    public SSCPluginXmlViewTemplate view_template;
    public String xsi;
    public String text;
}
