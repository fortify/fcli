package com.fortify.cli.ssc.common.pojos.uploadResponse;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public class Msg {
    @JacksonXmlText(value = true)
    String value;
}
