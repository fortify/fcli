package com.fortify.cli.ssc.common.pojos.newReportTemplateDefinition;

import com.fortify.cli.ssc.common.pojos.newReportTemplateDefinition.enums.ReportRenderingEngineType;
import com.fortify.cli.ssc.common.pojos.newReportTemplateDefinition.enums.ReportType;

public class ReportTemplateDef {
    String name;
    String description;
    ReportType type;
    ReportRenderingEngineType renderingEngine;
    String fileName;
    ReportParameter[] parameters;
    String templateDocId;
}
