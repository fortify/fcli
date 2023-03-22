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
package com.fortify.cli.fod.rest.query;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.fortify.cli.common.output.cli.mixin.spi.unirest.IUnirestOutputHelper;
import com.fortify.cli.common.output.query.QueryExpression;

import kong.unirest.HttpRequest;

// TODO Re-implement server-side filtering
public final class FoDFilterParamGenerator {
    private final Map<String, String> filterNamesByPropertyPaths = new HashMap<>();
    private final Map<String, Function<String,String>> valueGeneratorsByPropertyPaths = new HashMap<>();

    public FoDFilterParamGenerator add(String propertyPath, String filterName, Function<String,String> valueGenerator) {
        filterNamesByPropertyPaths.put(propertyPath, filterName);
        valueGeneratorsByPropertyPaths.put(propertyPath, valueGenerator);
        return this;
    }

    public FoDFilterParamGenerator add(String propertyPath, Function<String,String> valueGenerator) {
        filterNamesByPropertyPaths.put(propertyPath, propertyPath);
        valueGeneratorsByPropertyPaths.put(propertyPath, valueGenerator);
        return this;
    }

    public HttpRequest<?> addFilterParam(IUnirestOutputHelper outputHelper, HttpRequest<?> request) {
        //return addFilterParam(request, new OutputQueryHelper(outputHelper).getOutputQueries());
        return request;
    }

    public HttpRequest<?> addFilterParam(HttpRequest<?> request, QueryExpression query) {
        /*
        String FilterParamValue = getFilterParamValue(queries);
        if ( StringUtils.isNotBlank(FilterParamValue) ) {
            request = request.queryString("filters", FilterParamValue);
        }
        */
        return request;
    }

    public String getFilterParamValue(QueryExpression query) {
        return null;
        /*
        return queries==null
                ? null
                : queries.stream()
                .map(this::getFilterParamValueForQuery)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("+and+"));
         */
    }

    protected String getFilterParamValueForQuery(QueryExpression query) {
        /*
        if ( query.getOperator()==OutputQueryOperator.EQUALS ) {
            String propertyPath = query.getPropertyPath();
            String valueToMatch = query.getValueToMatch();
            String filterName = filterNamesByPropertyPaths.get(propertyPath);
            if ( filterName!=null ) {
                Function<String, String> valueGenerator = valueGeneratorsByPropertyPaths.get(propertyPath);
                return String.format("%s:%s", filterName, valueGenerator.apply(valueToMatch));
            }
        }
        */
        return null;
    }
}