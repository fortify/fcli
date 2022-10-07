/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.common.output.writer.record.json_properties;

import java.io.PrintWriter;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class JsonPropertiesRecordWriter implements IRecordWriter {
    @Getter private final RecordWriterConfig config;
    private final TreeSet<String> paths = new TreeSet<>();
    
    private PrintWriter getPrintWriter() {
        return getConfig().getPrintWriter();
    }

    @Override @SneakyThrows
    public void writeRecord(ObjectNode record) {
        Configuration configuration = Configuration.builder()
            .options(Option.AS_PATH_LIST)
            .jsonProvider(new JacksonJsonNodeJsonProvider(JsonHelper.getObjectMapper()))
            .mappingProvider(new JacksonMappingProvider(JsonHelper.getObjectMapper()))
            .build();
        JsonPath.using(configuration).parse(record).read("$..*", ArrayNode.class).forEach(j->paths.add(normalize(j.asText())));;
    }
    
    private String normalize(String s) {
        return s.replaceAll("\\['(.+?)'\\]", ".$1").replaceFirst("\\$\\.", "");
    }

    @Override
    public void finishOutput() {
        getPrintWriter().println(paths.stream().collect(Collectors.joining("\n")));
    }
}
