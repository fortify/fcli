package com.fortify.cli.common.output.helper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fortify.cli.common.output.cli.mixin.spi.IOutputHelper;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.query.OutputQuery;
import com.fortify.cli.common.output.writer.output.query.OutputQueryOperator;
import com.fortify.cli.common.output.writer.output.query.OutputWriterWithQueryFactory;

public class OutputQueryHelper {
    private final List<OutputQuery> outputQueries;
    
    public OutputQueryHelper(IOutputHelper outputHelper) {
        this(outputHelper.getOutputWriterFactory());
    }
    
    public OutputQueryHelper(IOutputWriterFactory outputWriterFactory) {
        if ( outputWriterFactory instanceof OutputWriterWithQueryFactory ) {
            this.outputQueries = ((OutputWriterWithQueryFactory)outputWriterFactory).getOutputQueries();
        } else {
            this.outputQueries = null;
        }
    }
    
    public List<OutputQuery> getOutputQueries() {
        return outputQueries==null ? Collections.emptyList() : Collections.unmodifiableList(outputQueries);
    }
    
    public List<OutputQuery> getOutputQueries(String propertyPath) {
        return getOutputQueries().stream()
            .filter(q->propertyPath.equals(q.getPropertyPath()))
            .collect(Collectors.toList());
    }
    
    public String getQueryValue(String propertyPath, OutputQueryOperator operator) {
        List<String> result = getOutputQueries(propertyPath).stream()
            .filter(q->operator.equals(q.getOperator()))
            .map(OutputQuery::getValueToMatch)
            .collect(Collectors.toList());
        if ( result.size()>1 ) {
            throw new IllegalArgumentException("Multiple comparison values specified for property "+propertyPath+" and operator "+operator.getOperator());
        }
        return result.isEmpty() ? null : result.get(0);
    }
}
