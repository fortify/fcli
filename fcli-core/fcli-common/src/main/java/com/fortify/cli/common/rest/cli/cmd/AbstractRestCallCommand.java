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
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IBaseRequestSupplier;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.common.util.JavaHelper;
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
 * implement the various abstract methods.
 * 
 * @author Ruud Senden
 */
public abstract class AbstractRestCallCommand extends AbstractOutputCommand implements IBaseRequestSupplier, IUnirestInstanceSupplier, IInputTransformer, IRecordTransformer, INextPageUrlProducerSupplier {
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
        return prepareRequest(getUnirestInstance());
    }
    
    @Override
    public final UnirestInstance getUnirestInstance() {
        return getUnirestInstanceSupplier().getUnirestInstance();
    }
    
    protected abstract IUnirestInstanceSupplier getUnirestInstanceSupplier();
    protected abstract IProductHelper getProductHelper();

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
    public final INextPageUrlProducer getNextPageUrlProducer() {
        INextPageUrlProducer result = null;
        if ( !noPaging ) {
            result = _getNextPageUrlProducer();
        }
        return result;
    }

    private final JsonNode _transformRecord(JsonNode input) {
        return applyOnProductHelper(IRecordTransformer.class, t->t.transformRecord(input), input);
    }
    private final JsonNode _transformInput(JsonNode input) {
        return applyOnProductHelper(IInputTransformer.class, t->t.transformInput(input), input);
    }
    private final INextPageUrlProducer _getNextPageUrlProducer() {
        return applyOnProductHelper(INextPageUrlProducerSupplier.class, s->s.getNextPageUrlProducer(), null);
    }

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
    
    private final <T,R> R applyOnProductHelper(Class<T> type, Function<T,R> f, R defaultValue) {
        return JavaHelper.as(getProductHelper(), type).map(f).orElse(defaultValue);
    }
    
}
