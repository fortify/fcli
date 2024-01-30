/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.report.helper;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper=true)
public class SSCReportTemplateDescriptor extends JsonNodeHolder {
    private int id;
    private String name;
    private String description;
    private SSCReportType type;
    private String fileName;
    private int templateDocId;
    private SSCReportRenderingEngineType renderingEngine;
    private SSCReportTemplateParameter[] parameters;
    
    @JsonIgnore
    public final String getIdString() {
        return ""+id;
    }
    
    @Reflectable @NoArgsConstructor
    @Data 
    public static final class SSCReportTemplateParameter {
        private int id;
        private String name;
        private SSCReportParameterType type;
        private String description;
        private String identifier;
        private int paramOrder;
        @JsonProperty("reportParameterOptions")
        private SSCReportTemplateParameterOption[] options; 
        
        public String getOptionsString() {
            return options==null 
                    ? "" 
                    : Stream.of(options)
                        .map(SSCReportTemplateParameterOption::getDisplayValueWithDefaultValueIndicator)
                        .collect(Collectors.joining("\n"));
        }
        
        public String getTypeString() {
            return type.name().replace("PROJECT", "APPVERSION");
        }
    }
    
    @Reflectable @NoArgsConstructor
    @Data
    public static final class SSCReportTemplateParameterOption {
        private int id;
        private String identifier;
        private String displayValue;
        private String reportValue;
        private boolean defaultValue;
        private String description;
        private int order;
        
        public final String getDisplayValueWithDefaultValueIndicator() {
            return String.format("%s%s", displayValue, defaultValue?"*":"");
        }
    }
}
