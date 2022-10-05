package com.fortify.cli.fod.rest.query;

import com.fortify.cli.common.output.cli.mixin.query.OutputQuery;
import com.fortify.cli.common.output.cli.mixin.query.OutputQueryOperator;
import kong.unirest.HttpRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class FoDOutputQueryFiltersParamGenerator {
    private final Map<String, String> filtersNamesByPropertyPaths = new HashMap<>();
    private final Map<String, Function<String,String>> valueGeneratorsByPropertyPaths = new HashMap<>();
    
    public FoDOutputQueryFiltersParamGenerator add(String propertyPath, String filtersName, Function<String,String> valueGenerator) {
        filtersNamesByPropertyPaths.put(propertyPath, filtersName);
        valueGeneratorsByPropertyPaths.put(propertyPath, valueGenerator);
        return this;
    }
    
    public FoDOutputQueryFiltersParamGenerator add(String propertyPath, Function<String,String> valueGenerator) {
        filtersNamesByPropertyPaths.put(propertyPath, propertyPath);
        valueGeneratorsByPropertyPaths.put(propertyPath, valueGenerator);
        return this;
    }
    
    public HttpRequest<?> addFiltersParam(HttpRequest<?> request, List<OutputQuery> queries) {
        String filtersParamValue = getFiltersParamValue(queries);
        if ( filtersParamValue!=null && !filtersParamValue.isBlank() ) {
            request = request.queryString("filters", filtersParamValue);
        }
        return request;
    }
    
    public String getFiltersParamValue(List<OutputQuery> queries) {
        return queries==null
                ? null 
                : queries.stream()
                    .map(this::getFiltersParamValueForQuery)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("+and+"));
    }
    
    protected String getFiltersParamValueForQuery(OutputQuery query) {
        if ( query.getOperator()==OutputQueryOperator.EQUALS ) {
            String propertyPath = query.getPropertyPath();
            String valueToMatch = query.getValueToMatch();
            String filtersName = filtersNamesByPropertyPaths.get(propertyPath);
            if ( filtersName!=null ) {
                Function<String, String> valueGenerator = valueGeneratorsByPropertyPaths.get(propertyPath);
                return String.format("%s:%s", filtersName, valueGenerator.apply(valueToMatch));
            }
        }
        return null;
    }
}
