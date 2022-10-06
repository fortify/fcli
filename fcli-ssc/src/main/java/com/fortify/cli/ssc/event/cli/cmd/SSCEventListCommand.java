/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
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
package com.fortify.cli.ssc.event.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCListCommand;
import com.fortify.cli.ssc.rest.query.SSCOutputQueryQParamGenerator;
import com.fortify.cli.ssc.rest.query.SSCQParamValueGenerators;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Command;

@ReflectiveAccess
@Command(name = "list")
public class SSCEventListCommand extends AbstractSSCListCommand {
    @Override
    protected SSCOutputQueryQParamGenerator getQParamGenerator() {
        return new SSCOutputQueryQParamGenerator()
                .add("userName", SSCQParamValueGenerators::wrapInQuotes)
                .add("eventType", SSCQParamValueGenerators::wrapInQuotes)
                .add("detailedNote", SSCQParamValueGenerators::wrapInQuotes)
                .add("applicationVersionId", "projectVersionId", SSCQParamValueGenerators::plain)
                .add("required", SSCQParamValueGenerators::plain);
    }
    
    @Override
    protected JsonNode transformRecord(JsonNode record) {
        return new RenameFieldsTransformer("projectVersionId", "applicationVersionId").transform(record);
    }
    
    protected GetRequest generateRequest(UnirestInstance unirest) {
        return unirest.get("/api/v1/events?limit=-1");
    }
}
