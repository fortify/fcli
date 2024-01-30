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
package com.fortify.cli.ssc.report.cli.mixin;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.ssc.report.helper.SSCReportDescriptor;
import com.fortify.cli.ssc.report.helper.SSCReportHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCReportResolverMixin {
    private static abstract class AbstractSSCReportResolverMixin {
        protected abstract String getReportNameOrId();
        public SSCReportDescriptor getReportDescriptor(UnirestInstance unirest) {
            return SSCReportHelper.getRequiredReportDescriptor(unirest, getReportNameOrId());
        }
    }
    
    public static abstract class AbstractSSCMultiReportResolverMixin {
        public abstract String[] getReportNamesOrIds();

        public SSCReportDescriptor[] getReportDescriptors(UnirestInstance unirest){
            return Stream.of(getReportNamesOrIds()).map(id->SSCReportHelper.getRequiredReportDescriptor(unirest, id)).toArray(SSCReportDescriptor[]::new);
        }
        
        public Collection<JsonNode> getReportDescriptorJsonNodes(UnirestInstance unirest){
            return Stream.of(getReportDescriptors(unirest)).map(SSCReportDescriptor::asJsonNode).collect(Collectors.toList());
        }
        
        public String[] getReportIds(UnirestInstance unirest) {
            return Stream.of(getReportDescriptors(unirest)).map(SSCReportDescriptor::getId).toArray(String[]::new);
        }
    }
    
    public static class PositionalParameterSingle extends AbstractSSCReportResolverMixin {
        @EnvSuffix("REPORT") @Parameters(index = "0", arity = "1", descriptionKey = "reportNameOrId")
        @Getter private String reportNameOrId;
    }
    public static class RequiredOption extends AbstractSSCReportResolverMixin {
        @Option(names="--report", required=true, descriptionKey = "reportNameOrId")
        @Getter private String reportNameOrId;
    }
    public static class PositionalParameterMulti extends AbstractSSCMultiReportResolverMixin {
        @EnvSuffix("REPORTS") @Parameters(index = "0", arity = "1..", paramLabel = "report names or id's", descriptionKey = "fcli.ssc.report.resolver.name-or-ids")
        @Getter private String[] reportNamesOrIds;
    }
    
}
