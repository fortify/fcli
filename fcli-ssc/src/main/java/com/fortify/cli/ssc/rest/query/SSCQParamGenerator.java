package com.fortify.cli.ssc.rest.query;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.fortify.cli.common.output.cli.mixin.spi.unirest.IUnirestOutputHelper;
import com.fortify.cli.common.output.query.QueryExpression;

import kong.unirest.HttpRequest;

// TODO Re-implement server-side filtering
public final class SSCQParamGenerator {
    private final Map<String, String> qNamesByPropertyPaths = new HashMap<>();
    private final Map<String, Function<String,String>> valueGeneratorsByPropertyPaths = new HashMap<>();
    
    public SSCQParamGenerator add(String propertyPath, String qName, Function<String,String> valueGenerator) {
        qNamesByPropertyPaths.put(propertyPath, qName);
        valueGeneratorsByPropertyPaths.put(propertyPath, valueGenerator);
        return this;
    }
    
    public SSCQParamGenerator add(String propertyPath, Function<String,String> valueGenerator) {
        qNamesByPropertyPaths.put(propertyPath, propertyPath);
        valueGeneratorsByPropertyPaths.put(propertyPath, valueGenerator);
        return this;
    }
    
    public HttpRequest<?> addQParam(IUnirestOutputHelper unirestOutputHelper, HttpRequest<?> request) {
        //return addQParam(request, new OutputQueryHelper(unirestOutputHelper).getOutputQueries());
        return request;
    }
    
    public HttpRequest<?> addQParam(HttpRequest<?> request, QueryExpression query) {
        /*
        String qParamValue = getQParamValue(queries);
        if ( StringUtils.isNotBlank(qParamValue) ) {
            request = request.queryString("q", qParamValue);
        }
        */
        return request;
    }
    
    public String getQParamValue(QueryExpression query) {
        /*
        return queries==null 
                ? null 
                : queries.stream()
                    .map(this::getQParamValueForQuery)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("+and+"));
        */
        return null;
    }
    
    protected String getQParamValueForQuery(QueryExpression query) {
        /*
        if ( query.getOperator()==OutputQueryOperator.EQUALS ) {
            String propertyPath = query.getPropertyPath();
            String valueToMatch = query.getValueToMatch();
            String qName = qNamesByPropertyPaths.get(propertyPath);
            if ( qName!=null ) {
                Function<String, String> valueGenerator = valueGeneratorsByPropertyPaths.get(propertyPath);
                return String.format("%s:%s", qName, valueGenerator.apply(valueToMatch));
            }
        }
        */
        return null;
    }
}
