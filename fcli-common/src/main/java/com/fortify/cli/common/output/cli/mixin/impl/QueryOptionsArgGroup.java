package com.fortify.cli.common.output.cli.mixin.impl;

import java.util.Collections;
import java.util.List;

import com.fortify.cli.common.output.query.IOutputQueriesSupplier;
import com.fortify.cli.common.output.query.OutputQuery;
import com.fortify.cli.common.output.query.OutputQueryConverter;

import lombok.Getter;
import picocli.CommandLine;

public final class QueryOptionsArgGroup implements IOutputQueriesSupplier {
    @CommandLine.Option(names = {"-q", "--query"}, order=1, converter = OutputQueryConverter.class, paramLabel = "<prop><op><value>")
    @Getter private List<OutputQuery> outputQueries = Collections.emptyList();
}