package com.fortify.cli.ssc.common.pojos.uploadResponse;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "UploadResponse")
public class UploadResponse{
    @JacksonXmlProperty(isAttribute = true)
    public String entityId;

    @JacksonXmlProperty(localName = "code")
    public Code code;

    @JacksonXmlProperty(localName = "msg")
    public Msg msg;
}
