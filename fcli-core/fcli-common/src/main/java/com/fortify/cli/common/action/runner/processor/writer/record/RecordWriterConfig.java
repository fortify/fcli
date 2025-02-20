/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.action.runner.processor.writer.record;

import java.io.Writer;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fortify.cli.common.util.StringUtils;

import lombok.Builder;
import lombok.Getter;

@Builder
public class RecordWriterConfig {
    @Getter private final Supplier<Writer> writerSupplier;
    private final Map<String, String> options;
    
    public String option(String name) {
        return options==null ? null : options.get(name);
    }
    
    public boolean booleanOption(String name) {
        var value = option(name);
        return StringUtils.isNotBlank(value) && Boolean.valueOf(value);
    }
    
    public boolean isSingular() {
        return booleanOption("singular");
    }
    
    public boolean isPretty() {
        return booleanOption("pretty");
    }
    
    public Map<String,String> getHeaders() {
        var headersString = option("headers");
        return headersString==null 
                ? null
                : Arrays.stream(headersString.split("\\s*,\\s*"))
                    .map(kv -> kv.split("="))
                    .collect(Collectors.toMap(kv -> kv[0], kv -> kv.length==1 ? kv[0] : kv[1]));
    }
}
