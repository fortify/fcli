package com.fortify.cli.ssc.common.pojos.newReportTemplateDefinition;

import com.fortify.cli.ssc.common.pojos.newReportTemplateDefinition.enums.ReportRenderingEngineType;
import com.fortify.cli.ssc.common.pojos.newReportTemplateDefinition.enums.ReportType;

public class ReportTemplateDef {
    public String name;
    public String description;
    public ReportType type;
    public ReportRenderingEngineType renderingEngine;
    public String fileName;
    public ReportParameter[] parameters;
    public String templateDocId;
}
