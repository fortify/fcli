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
package com.fortify.cli.ssc.report.cli.cmd;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCDelimiterMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.attribute.helper.SSCAttributeDefinitionHelper;
import com.fortify.cli.ssc.report.cli.mixin.SSCReportTemplateResolverMixin;
import com.fortify.cli.ssc.report.helper.SSCReportFormat;
import com.fortify.cli.ssc.report.helper.SSCReportHelper;
import com.fortify.cli.ssc.report.helper.SSCReportTemplateDescriptor;
import com.fortify.cli.ssc.report.helper.SSCReportTemplateDescriptor.SSCReportTemplateParameter;
import com.fortify.cli.ssc.report.helper.SSCReportTemplateDescriptor.SSCReportTemplateParameterOption;

import kong.unirest.UnirestInstance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class SSCReportCreateCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper; 
    @Mixin private SSCReportTemplateResolverMixin.RequiredOption templateResolver;
    @Option(names={"--name","-n"}, required = true) private String reportName;
    @Option(names={"--notes"}, required = false, defaultValue = "") private String notes;
    @Option(names={"--format","-f"}, required=true, defaultValue="pdf") private SSCReportFormat format;
    @Option(names={"--parameters","-p"}, required=false, split = ",", paramLabel = "PARAM=VALUE", descriptionKey = "fcli.ssc.report.create.parameters") 
    private Map<String, String> parameters;
    @Mixin private SSCDelimiterMixin delimiterMixin; // For parsing app:version
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        var reportId = unirest.post(SSCUrls.REPORTS)
            .body(createReportCreateRequest(unirest))
            .asObject(JsonNode.class)
            .getBody().get("data").get("id").asText();
        return SSCReportHelper.getRequiredReportDescriptor(unirest, reportId).asJsonNode();
    }
    
    @Override
    public String getActionCommandResult() {
        return "CREATED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private SSCReportCreateRequest createReportCreateRequest(UnirestInstance unirest) {
        var templateDescriptor = templateResolver.getReportTemplateDescriptor(unirest);
        AbstractSSCInputReportParameter[] inputParameters = 
                new SSCInputReportParameterBuilder(unirest, templateDescriptor, parameters, delimiterMixin.getDelimiter())
                .buildInputParameters();
        return SSCReportCreateRequest.builder()
                .name(reportName)
                .note(notes)
                .format(format.name().toUpperCase())
                .inputReportParameters(inputParameters)
                .reportDefinitionId(templateDescriptor.getId())
                .type(templateDescriptor.getType().name())
                .build();
    }

    @RequiredArgsConstructor
    private static final class SSCInputReportParameterBuilder {
        private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
        private final UnirestInstance unirest;
        private final SSCReportTemplateDescriptor templateDescriptor;
        private final Map<String, String> parameters;
        private final String delimiter;
        private SSCAttributeDefinitionHelper attrDefinitionHelper;
        public final AbstractSSCInputReportParameter[] buildInputParameters() {
            return Stream.of(templateDescriptor.getParameters())
                    .map(this::createInputParameter)
                    .toArray(AbstractSSCInputReportParameter[]::new);
        }
        
        private final AbstractSSCInputReportParameter createInputParameter(SSCReportTemplateParameter param) {
            AbstractSSCInputReportParameter result;
            switch (param.getType() ) {
            case BOOLEAN: result = createBooleanInputParameter(param); break;
            case DATE: result = createDateInputParameter(param); break;
            case MULTI_PROJECT: result = createMultiProjectInputParameter(param); break;
            case PROJECT_ATTRIBUTE: result = createProjectAttributeInputParameter(param); break;
            case SINGLE_PROJECT: result = createSingleProjectInputParameter(param); break;
            case SINGLE_SELECT_DEFAULT: result = createSingleSelectDefaultInputParameter(param); break;
            case STRING: result = createStringInputParameter(param); break;
            
            default: throw new RuntimeException(String.format("Unknown type %s for report parameter %s", param.getType(), param.getName()));
            }
            addCommonFields(param, result);
            return result;
        }
        
        private void addCommonFields(SSCReportTemplateParameter templateParam, AbstractSSCInputReportParameter inputParam) {
            inputParam.setIdentifier(templateParam.getIdentifier());
            inputParam.setName(templateParam.getName());
            inputParam.setType(templateParam.getType().name());
        }

        private AbstractSSCInputReportParameter createBooleanInputParameter(SSCReportTemplateParameter param) {
            var value = getValue(param, "true"); // Just like SSC UI, we set boolean options to 'true' by default
            if ( "true".equalsIgnoreCase(value) || "1".equals(value) ) {
                return new SSCInputReportParameterSingleBoolean(true);
            } else if ( "false".equalsIgnoreCase(value) || "0".equals(value) ) {
                return new SSCInputReportParameterSingleBoolean(true);
            }
            throw new IllegalArgumentException(String.format("Value for boolean report parameter %s must be one of true|1|false|0", param.getName()));
        }
        
        private AbstractSSCInputReportParameter createDateInputParameter(SSCReportTemplateParameter param) {
            var value = getValue(param, null);
            var matcher = DATE_PATTERN.matcher(value);
            if ( !matcher.matches() ) {
                throw new IllegalArgumentException(String.format("Value for date report parameter %s must be specified as yyyy-MM-dd", param.getName()));
            }
            var reportValue = String.format("%s/%s/%s", matcher.group(2), matcher.group(3), matcher.group(1));
            return new SSCInputReportParameterSingleString(reportValue);
        }

        private AbstractSSCInputReportParameter createMultiProjectInputParameter(SSCReportTemplateParameter param) {
            var value = getValue(param, null);
            var reportValue = Stream.of(value.split("[\\[\\];]"))
                    .filter(StringUtils::isNotBlank)
                .mapToInt(this::getAppVersionId)
                .toArray();
            return new SSCInputReportParameterMultiInt(reportValue);
        }

        private AbstractSSCInputReportParameter createProjectAttributeInputParameter(SSCReportTemplateParameter param) {
            var value = getValue(param, null);
            if ( attrDefinitionHelper==null ) {
                attrDefinitionHelper = new SSCAttributeDefinitionHelper(unirest);
            }
            var attrDefinitionId = Integer.parseInt(attrDefinitionHelper.getAttributeDefinitionDescriptor(value).getId());
            return new SSCInputReportParameterSingleInt(attrDefinitionId);
        }

        private AbstractSSCInputReportParameter createSingleProjectInputParameter(SSCReportTemplateParameter param) {
            var value = getValue(param, null);
            var appVersionId = getAppVersionId(value);
            return new SSCInputReportParameterSingleInt(appVersionId);
        }

        private int getAppVersionId(String value) {
            return SSCAppVersionHelper.getRequiredAppVersion(unirest, value, delimiter, "id").getIntVersionId();
        }

        private AbstractSSCInputReportParameter createSingleSelectDefaultInputParameter(SSCReportTemplateParameter param) {
            var value = getValue(param, "");
            var reportValue = Stream.of(param.getOptions())
                    .filter(o->matchesOptionValue(o,value))
                    .findFirst()
                    .map(SSCReportTemplateParameterOption::getReportValue)
                    .orElse(null);
            return new SSCInputReportParameterSingleString(reportValue);
        }
        
        private final boolean matchesOptionValue(SSCReportTemplateParameterOption option, String value) {
            if ( StringUtils.isBlank(value) ) {
                return option.isDefaultValue();
            } else {
                return value.equals(option.getReportValue()) 
                        || value.equals(option.getIdentifier())
                        || value.equals(option.getDisplayValue());
            }
                    
        }

        private AbstractSSCInputReportParameter createStringInputParameter(SSCReportTemplateParameter param) {
            return new SSCInputReportParameterSingleString(getValue(param, ""));
        }
        
        private final String getValue(SSCReportTemplateParameter param, String defaultValue) {
            String result = null;
            if ( parameters!=null ) {
                result = parameters.get(param.getId()+"");
                if ( result==null ) {
                    result = parameters.get(param.getIdentifier());
                }
                if ( result==null ) {
                    result = parameters.get(param.getName());
                }
            }
            if ( result==null && defaultValue!=null ) {
                result = defaultValue;
            }
            if ( result==null ) {
                throw new IllegalArgumentException("No value specified for required report parameter "+param.getName());
            }
            return result;
        }
        
    }


    @Reflectable // We only serialize, so no no-args constructor needed
    @Data @Builder
    private static final class SSCReportCreateRequest {
        private String name;
        private String note;
        private String format;
        private AbstractSSCInputReportParameter[] inputReportParameters;
        private int reportDefinitionId;
        private String type;
    }
    
    @Reflectable @NoArgsConstructor
    @Data
    private static abstract class AbstractSSCInputReportParameter {
        private String name;
        private String identifier;
        private String type;
    }
    
    @Reflectable @NoArgsConstructor @AllArgsConstructor
    @Data @EqualsAndHashCode(callSuper=true)
    private static final class SSCInputReportParameterSingleInt extends AbstractSSCInputReportParameter {
        private int paramValue;
    }
    
    @Reflectable @NoArgsConstructor @AllArgsConstructor
    @Data @EqualsAndHashCode(callSuper=true)
    private static final class SSCInputReportParameterMultiInt extends AbstractSSCInputReportParameter {
        private int[] paramValue;
    }
    
    @Reflectable @NoArgsConstructor @AllArgsConstructor
    @Data @EqualsAndHashCode(callSuper=true)
    private static final class SSCInputReportParameterSingleBoolean extends AbstractSSCInputReportParameter {
        private boolean paramValue;
    }
    
    @Reflectable @NoArgsConstructor @AllArgsConstructor
    @Data @EqualsAndHashCode(callSuper=true)
    private static final class SSCInputReportParameterSingleString extends AbstractSSCInputReportParameter {
        private String paramValue;
    }
    
}
