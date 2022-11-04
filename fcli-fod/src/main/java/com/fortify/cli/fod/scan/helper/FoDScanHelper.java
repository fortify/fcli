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

package com.fortify.cli.fod.scan.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanDescriptor;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FoDScanHelper {
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        JsonNode transform = new RenameFieldsTransformer(new String[] {
                "scanId:id", "scanType:type", "analysisStatusType:status", "startedDateTime:started",
                "completedDateTime:completed", "scanMethodTypeName:scanMethod"
        }).transform(record);
        return transform;
    }

    public static final FoDScanDescriptor getScanDescriptor(UnirestInstance unirest, String scanId) {
        GetRequest request = unirest.get(FoDUrls.SCAN + "/summary").routeParam("scanId", scanId);
        return getOptionalDescriptor(request);
    }

    public static final Properties loadProperties() {
        final Properties p = new Properties();
        try (final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("com/fortify/cli/app/fcli-build.properties")) {
            if ( stream!=null ) { p.load(stream); }
        } catch ( IOException ioe ) {
            System.err.println("Error reading fcli-build.properties from classpath");
        }
        return p;
    }

    //

    private static final FoDScanDescriptor getOptionalDescriptor(GetRequest request) {
        JsonNode scan = request.asObject(ObjectNode.class).getBody();
        return scan == null ? null : getDescriptor(scan);
    }

    private static final FoDScanDescriptor getDescriptor(JsonNode node) {
        return  JsonHelper.treeToValue(node, FoDScanDescriptor.class);
    }


}
