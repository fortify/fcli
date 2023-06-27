/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.rest.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IBaseRequestSupplier;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;

import io.micronaut.core.util.StringUtils;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public abstract class AbstractRestCallCommand extends AbstractOutputCommand implements IBaseRequestSupplier, IProductHelperSupplier, IInputTransformer, IRecordTransformer {
    @Parameters(index = "0", arity = "1..1", descriptionKey = "api.uri") String uri;
    
    @Option(names = {"--request", "-X"}, required = false, defaultValue = "GET")
    @DisableTest(TestType.OPT_SHORT_NAME)
    @Getter private String httpMethod;
    
    @Option(names = {"--data", "-d"}, required = false)
    @Getter private String data; // TODO Add ability to read data from file
    
    @Option(names="--no-transform", negatable = true, defaultValue = "false") 
    private boolean noTransform;
    
    // TODO Add options for content-type, arbitrary headers, ...?
    
    @Override
    public HttpRequest<?> getBaseRequest() {
        if ( getProductHelper() instanceof IUnirestInstanceSupplier ) {
            UnirestInstance unirest = ((IUnirestInstanceSupplier)getProductHelper()).getUnirestInstance();
            return prepareRequest(unirest);
        }
        throw new RuntimeException("Class doesn't implement IUnirestInstanceSupplier: "+getProductHelper().getClass().getName());
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
    
    @Override
    public JsonNode transformInput(JsonNode input) {
        if ( !noTransform && getProductHelper() instanceof IInputTransformer ) {
            input = ((IInputTransformer)getProductHelper()).transformInput(input);
        }
        return input;
    }
    
    @Override
    public JsonNode transformRecord(JsonNode input) {
        if ( !noTransform && getProductHelper() instanceof IRecordTransformer ) {
            input = ((IRecordTransformer)getProductHelper()).transformRecord(input);
        }
        return input;
    }
    
    protected final HttpRequest<?> prepareRequest(UnirestInstance unirest) {
        if ( StringUtils.isEmpty(uri) ) {
            throw new IllegalArgumentException("Uri must be specified");
        }
        var request = unirest.request(httpMethod, uri);
        // TODO Add Content-Type & accept headers
        return data==null ? request : request.body(data);
    }
    
}
