package com.fortify.cli.ssc.domain.plugin.parser.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import io.micronaut.core.annotation.ReflectiveAccess;

@ReflectiveAccess
public class PluginInfo {
    public String name;
    public String version;
    @JacksonXmlElementWrapper(localName = "data-version")
    public int data_version;

    public Vendor vendor;
    public String description;
    public Resources resources;
}
