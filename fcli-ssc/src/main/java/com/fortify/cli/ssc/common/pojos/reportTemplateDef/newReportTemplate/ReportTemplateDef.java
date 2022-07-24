package com.fortify.cli.ssc.common.pojos.reportTemplateDef.newReportTemplate;

import com.fortify.cli.ssc.common.pojos.reportTemplateDef.newReportTemplate.enums.ReportRenderingEngineType;
import com.fortify.cli.ssc.common.pojos.reportTemplateDef.newReportTemplate.enums.ReportType;

public class ReportTemplateDef {
    public String name;
    public String description;
    public ReportType type;
    public ReportRenderingEngineType renderingEngine;
    public String fileName;
    public ReportParameter[] parameters;
    public String templateDocId;
}
