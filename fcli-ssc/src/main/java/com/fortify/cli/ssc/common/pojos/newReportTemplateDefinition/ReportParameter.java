package com.fortify.cli.ssc.common.pojos.newReportTemplateDefinition;

import com.fortify.cli.ssc.common.pojos.newReportTemplateDefinition.enums.ReportParameterType;

public class ReportParameter {
    int index;
    String name;
    String description;
    String identifier;
    ReportParameterType type;
    ReportParameterOption[] reportParameterOptions;
    int paramOrder;
}
