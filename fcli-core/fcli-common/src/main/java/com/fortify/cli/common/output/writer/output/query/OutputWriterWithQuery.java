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
package com.fortify.cli.common.output.writer.output.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.OutputWriterWithQueryFactoryMixin;
import com.fortify.cli.common.output.writer.output.standard.IOutputOptions;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputWriter;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.json.record.IRecordProducer;
import com.fortify.cli.common.json.record.IRecordConsumer;
import com.fortify.cli.common.spel.query.IQueryExpressionSupplier;
import com.fortify.cli.common.spel.query.QueryExpression;

import picocli.CommandLine.Model.CommandSpec;

/**
 * TODO Refactor this class once all commands have been refactored to use {@link OutputHelperMixins};
 *      all picocli annotatations should be removed, as they will be passed by {@link OutputWriterWithQueryFactoryMixin}
 *      through our constructor. As this class by then will no longer be a mixin, it should be renamed
 *      to OutputWriterWithQuery, and moved to the writer.output.query package.
 * @author rsenden
 *
 */
public class OutputWriterWithQuery implements IOutputWriter {
    private final StandardOutputWriter delegate;
    private final IQueryExpressionSupplier queryExpressionSupplier;
    public OutputWriterWithQuery(CommandSpec mixee, IOutputOptions outputOptions, IQueryExpressionSupplier queryExpressionSupplier, StandardOutputConfig defaultOutputConfig) {
        this.delegate = new StandardOutputWriter(mixee, outputOptions, defaultOutputConfig);
        this.queryExpressionSupplier = queryExpressionSupplier;
    }
    @Override
    public void write(IRecordProducer recordProducer) {
        QueryExpression qe = queryExpressionSupplier.getQueryExpression();
        if ( qe==null ) {
            delegate.write(recordProducer);
        } else {
            delegate.write(consumer -> recordProducer.forEach(filteringConsumer(qe, consumer)));
        }
    }
    private IRecordConsumer filteringConsumer(QueryExpression qe, IRecordConsumer consumer) {
        return record -> {
            JsonNode node = record; // ObjectNode extends JsonNode
            return qe.matches(node) ? consumer.accept(record) : com.fortify.cli.common.util.Break.FALSE;
        };
    }
}
