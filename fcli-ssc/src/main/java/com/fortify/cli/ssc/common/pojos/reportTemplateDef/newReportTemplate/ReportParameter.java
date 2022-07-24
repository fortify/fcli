package com.fortify.cli.ssc.common.pojos.reportTemplateDef.newReportTemplate;

import com.fortify.cli.ssc.common.pojos.reportTemplateDef.newReportTemplate.enums.ReportParameterType;

public class ReportParameter {
    public int index;
    public String name;
    public String description;
    public String identifier;
    public ReportParameterType type;
    public ReportParameterOption[] reportParameterOptions;
    public int paramOrder;
}
