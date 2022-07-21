package com.fortify.cli.ssc.common.pojos.uploadResponse;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "UploadResponse")
public class UploadResponse{
    @JacksonXmlProperty(isAttribute = true)
    String entityId;

    @JacksonXmlProperty(localName = "code")
    Code code;

    @JacksonXmlProperty(localName = "msg")
    Msg msg;
}
