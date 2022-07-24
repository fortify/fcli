package com.fortify.cli.ssc.common.pojos.reportTemplateDef.existingReportTemplate;

import java.util.ArrayList;

public class ReportTemplateDef {
    /* ObjectMapper om = new ObjectMapper();
    Root root = om.readValue(myJsonString, Root.class); */
    public int count;
    public Data data;
    public int errorCode;
    public Links links;
    public String message;
    public int responseCode;
    public String stackTrace;
    public int successCount;

    public class Data{
        public boolean crossApp;
        public String description;
        public String fileName;
        public String guid;
        public int id;
        public boolean inUse;
        public String name;
        public int objectVersion;
        public ArrayList<Parameter> parameters;
        public int publishVersion;
        public String renderingEngine;
        public int templateDocId;
        public String type;
        public String typeDefaultText;
    }

    public class Links{
        public String href;
    }

    public class Parameter{
        public String description;
        public int id;
        public String identifier;
        public String name;
        public int paramOrder;
        public int reportDefinitionId;
        public ArrayList<ReportParameterOption> reportParameterOptions;
        public String type;
    }

    public class ReportParameterOption{
        public boolean defaultValue;
        public String description;
        public String displayValue;
        public int id;
        public int order;
        public String reportValue;
    }
}
