package com.fortify.cli.common.util.printer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.SneakyThrows;

public class PrintXmlHelper {

    @SneakyThrows
    public static final void printXml(JsonNode input, Boolean pretty) {
        XmlMapper xmlMapper = new XmlMapper();
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode root = objectMapper.createObjectNode();
        root.set("item",input);

        if (pretty){
            xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        String xmlString = xmlMapper.writeValueAsString(root).replace("ObjectNode", "content");

        System.out.println(xmlString);
    }

    @SneakyThrows
    public static final void printXml(JsonNode input) {printXml(input, false);}
}
