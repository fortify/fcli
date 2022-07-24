/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.ssc.common.pojos.report.template.existingReportTemplate;

import java.util.ArrayList;

public class ReportTemplateDef {
    public int count;
    public Data data;
    public int errorCode;
    public String _href;
    public String message;
    public int responseCode;
    public String stackTrace;
    public int successCount;

    public class Data{
        public boolean crossApp;
        public String description;
        public String fileName;
        public String guid;
        public Integer id;
        public boolean inUse;
        public String name;
        public int objectVersion;
        public ArrayList<Parameter> parameters;
        public int publishVersion;
        public String renderingEngine;
        public int templateDocId;
        public String type;
        public String typeDefaultText;
        public String _href;
    }

    public class _href{
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
