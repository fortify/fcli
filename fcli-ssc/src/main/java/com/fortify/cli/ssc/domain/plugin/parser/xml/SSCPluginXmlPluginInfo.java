package com.fortify.cli.ssc.domain.plugin.parser.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import io.micronaut.core.annotation.ReflectiveAccess;

@ReflectiveAccess
public class SSCPluginXmlPluginInfo {
    public String name;
    public String version;
    @JacksonXmlElementWrapper(localName = "data-version")
    public int data_version;

    public SSCPluginXmlVendor vendor;
    public String description;
    public SSCPluginXmlResources resources;
}
