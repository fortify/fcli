/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.rest.cli.cmd;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IBaseRequestSupplier;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.common.util.StringUtils;

import kong.unirest.HttpRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Abstract base class for 'fcli <product> rest call' commands. Concrete implementations must 
 * implement the various abstract methods. As dictated by the {@link IProductHelperSupplier},
 * implementations must implement the {@link IProductHelperSupplier#getProductHelper()} method,
 * but please note that the provided product helper may not implement any of the 
 * {@link IInputTransformer}, {@link IRecordTransformer}, {@link INextPageUrlProducerSupplier}
 * or {@link INextPageUrlProducer} as this would break the --no-transform and --no-paging options.
 * Instead, subclasses should implement the corresponding _* methods defined in this class,
 * to enable/disable paging and transformations on demand.
 * 
 * @author Ruud Senden
 */
public abstract class AbstractRestCallCommand extends AbstractOutputCommand implements IBaseRequestSupplier, IProductHelperSupplier, IInputTransformer, IRecordTransformer, INextPageUrlProducerSupplier {
    @EnvSuffix("URI") @Parameters(index = "0", arity = "1..1", descriptionKey = "api.uri") String uri;
    
    @Option(names = {"--request", "-X"}, required = false, defaultValue = "GET")
    @DisableTest(TestType.OPT_SHORT_NAME)
    @Getter private String httpMethod;
    
    @Option(names = {"--data", "-d"}, required = false)
    @Getter private String data;
    
    @Option(names="--no-paging", negatable = false, defaultValue = "false") 
    private boolean noPaging;
    
    @ArgGroup(exclusive = true) private TransformArgGroup transform = new TransformArgGroup();
    private static class TransformArgGroup {
        @Option(names="--no-transform", negatable = false, defaultValue = "false") 
        private boolean noTransform;
    
        @Option(names={"-t", "--transform"}, paramLabel = "<expr>") 
        private String transformExpression;
    }
    
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
    public final JsonNode transformInput(JsonNode input) {
        if ( StringUtils.isNotBlank(transform.transformExpression) ) {
            input = JsonHelper.evaluateSpelExpression(input, transform.transformExpression, JsonNode.class);
        } else if ( !transform.noTransform ) {
            input = _transformInput(input);
        }
        return input;
    }

    @Override
    public final JsonNode transformRecord(JsonNode input) {
        if ( !transform.noTransform ) {
            input = _transformRecord(input);
        }
        return input;
    }

    @Override
    public final INextPageUrlProducer getNextPageUrlProducer(HttpRequest<?> originalRequest) {
        INextPageUrlProducer result = null;
        if ( !noPaging ) {
            result = _getNextPageUrlProducer(originalRequest);
        }
        return result;
    }

    protected abstract JsonNode _transformRecord(JsonNode input);
    protected abstract JsonNode _transformInput(JsonNode input);
    protected abstract INextPageUrlProducer _getNextPageUrlProducer(HttpRequest<?> originalRequest);

    @SneakyThrows
    protected final HttpRequest<?> prepareRequest(UnirestInstance unirest) {
        if ( StringUtils.isBlank(uri) ) {
            throw new IllegalArgumentException("Uri must be specified");
        }
        HttpRequest<?> request = unirest.request(httpMethod, uri);
        if ( StringUtils.isNotBlank(data) ) {
            if ( "GET".equals(httpMethod) || !(request instanceof HttpRequestWithBody) ) {
                throw new IllegalArgumentException("Request body not supported for "+httpMethod+" requests");
            } else if ( data.startsWith("@") ) {
                var path = Path.of(data.replaceAll("^@+", ""));
                request = ((HttpRequestWithBody)request).body(Files.readString(path));
            } else {
                request = ((HttpRequestWithBody)request).body(data);
            }
        }
        return request;
    }
    
}
