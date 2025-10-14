/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.output.writer.output.standard;

import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.IObjectNodeProducer.IObjectNodeConsumer;
import com.fortify.cli.common.output.cli.cmd.IRecordCollectionSupport;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.writer.CommandSpecMessageResolver;
import com.fortify.cli.common.output.writer.IMessageResolver;
import com.fortify.cli.common.output.writer.ISingularSupplier;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.OutputRecordWriterFactory;
import com.fortify.cli.common.output.writer.output.OutputRecordWriterFactory.OutputRecordWriterFactoryBuilder;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.output.writer.record.RecordWriterStyle;
import com.fortify.cli.common.output.writer.record.util.AppendOnCloseWriterWrapper;
import com.fortify.cli.common.output.writer.record.util.NonClosingWriterWrapper;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.common.variable.EncryptVariable;
import com.fortify.cli.common.variable.FcliVariableHelper;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Model.CommandSpec;

public class StandardOutputWriter implements IOutputWriter {
    private final StandardOutputConfig outputConfig;
    private final RecordWriterFactory recordWriterFactory;
    private final CommandSpec commandSpec;
    private final IOutputOptions outputOptions;
    private final IMessageResolver messageResolver;
    private final IRecordWriter recordCollector; // instance-level collector, may be null
    private final boolean suppressOutput;

    public StandardOutputWriter(CommandSpec commandSpec, IOutputOptions outputOptions, StandardOutputConfig defaultOutputConfig) {
        this.commandSpec = commandSpec.commandLine() == null ? commandSpec : commandSpec.commandLine().getCommandSpec();
        this.outputOptions = outputOptions;
        this.outputConfig = getOutputConfigOrDefault(commandSpec, defaultOutputConfig);
        this.recordWriterFactory = getRecordWriterFactoryOrDefault(outputConfig, outputOptions);
        this.messageResolver = new CommandSpecMessageResolver(this.commandSpec);
        Object cmd = this.commandSpec.userObject();
        if (cmd instanceof IRecordCollectionSupport) {
            var rcs = (IRecordCollectionSupport) cmd;
            this.recordCollector = rcs.getRecordConsumer() == null ? null : new IRecordWriter() {
                @Override
                public void append(ObjectNode r) {
                    rcs.getRecordConsumer().accept(r);
                }
                @Override
                public void close() {
                }
            };
            this.suppressOutput = rcs.isStdoutSuppressedForRecordCollection();
        } else {
            this.recordCollector = null;
            this.suppressOutput = false;
        }
    }

    // Instance determines whether to collect records based on command user object
    // Instance-level record collector configured in constructor if command supports
    // it

    @Override
    public void write(IObjectNodeProducer recordProducer) {
        if (recordProducer == null) {
            return;
        }
        try (IRecordWriter rw = new OutputAndVariableRecordWriter()) {
            recordProducer.forEach(recordConsumer(rw));
        }
    }

    private IObjectNodeConsumer recordConsumer(IRecordWriter rw) {
        return r -> {
            rw.append(r);
            return com.fortify.cli.common.util.Break.FALSE;
        };
    }

    private static final StandardOutputConfig getOutputConfigOrDefault(CommandSpec commandSpec, StandardOutputConfig defaultOutputConfig) {
        Object cmd = commandSpec.userObject();
        return cmd instanceof IOutputConfigSupplier ? ((IOutputConfigSupplier) cmd).getOutputConfig() : defaultOutputConfig;
    }

    private static final RecordWriterFactory getRecordWriterFactoryOrDefault(StandardOutputConfig outputConfig,
            IOutputOptions outputOptions) {
        var result = outputOptions == null || outputOptions.getOutputFormatConfig() == null
                ? outputConfig.defaultFormat()
                : outputOptions.getOutputFormatConfig().getRecordWriterFactory();
        return result == null ? RecordWriterFactory.table : result;
    }

    protected boolean isSingularOutput() {
        Object cmd = commandSpec.userObject();
        return cmd instanceof ISingularSupplier && ((ISingularSupplier) cmd).isSingular();
    }

    private final class OutputAndVariableRecordWriter implements IRecordWriter {
        private final IRecordWriter outputRecordWriter = createOutputRecordWriter();
        private final IRecordWriter rc = recordCollector;
        private final VariableRecordWriter variableRecordWriter = new VariableRecordWriter();

        @Override
        public void append(ObjectNode record) {
            if (outputRecordWriter != null) {
                outputRecordWriter.append(record);
            }
            if (rc != null) {
                rc.append(record);
            }
            if (variableRecordWriter.isEnabled()) {
                variableRecordWriter.append(record);
            }
        }

        @Override
        public void close() {
            if (outputRecordWriter != null) {
                outputRecordWriter.close();
            }
            if (rc != null) {
                rc.close();
            }
            if (variableRecordWriter.isEnabled()) {
                variableRecordWriter.close();
            }
        }

        private IRecordWriter createOutputRecordWriter() {
            return suppressOutput ? null : createUnsuppressed();
        }

        private IRecordWriter createUnsuppressed() {
            Object cmd = commandSpec.userObject();
            var recordWriterArgs = outputOptions == null || outputOptions.getOutputFormatConfig() == null
                    ? null
                    : outputOptions.getOutputFormatConfig().getRecordWriterArgs();
            return OutputRecordWriterFactory.builder().singular(isSingularOutput()).messageResolver(messageResolver)
                    .addActionColumn(cmd != null && cmd instanceof IActionCommandResultSupplier).recordWriterArgs(recordWriterArgs)
                    .recordWriterFactory(recordWriterFactory)
                    .recordWriterStyle(RecordWriterStyle.apply(outputOptions.getOutputStyleElements())).writerSupplier(this::createWriter)
                    .build().createRecordWriter();
        }

        @SneakyThrows
        private Writer createWriter() {
            var outputFile = outputOptions.getOutputFile();
            return outputFile == null
                    ? new AppendOnCloseWriterWrapper("\n\n", new NonClosingWriterWrapper(new OutputStreamWriter(System.out)))
                    : new FileWriter(outputFile);
        }
    }

    private abstract class AbstractRecordWriterWrapper implements IRecordWriter {
        @Override
        public final void append(ObjectNode record) {
            getWrappedRecordWriter().append(record);
        }
        @Override
        public final void close() {
            getWrappedRecordWriter().close();
        }

        protected final OutputRecordWriterFactoryBuilder createRecordWriterConfigBuilder() {
            Object cmd = commandSpec.userObject();
            return OutputRecordWriterFactory.builder().singular(isSingularOutput()).messageResolver(messageResolver)
                    .addActionColumn(cmd != null && cmd instanceof IActionCommandResultSupplier);
        }

        protected abstract IRecordWriter getWrappedRecordWriter();
    }

    private final class VariableRecordWriter extends AbstractRecordWriterWrapper {
        private final VariableDefinition variableDefinition;
        @Getter
        private final IRecordWriter wrappedRecordWriter;

        public VariableRecordWriter() {
            var cfg = outputOptions.getVariableStoreConfig();
            this.variableDefinition = cfg == null ? null : createVariableDefinition(cfg);
            this.wrappedRecordWriter = cfg == null ? null : createOutputRecordWriterFactory().createRecordWriter();
        }

        public boolean isEnabled() {
            return variableDefinition != null;
        }

        private OutputRecordWriterFactory createOutputRecordWriterFactory() {
            return createRecordWriterConfigBuilder().writerSupplier(() -> createWriter(variableDefinition))
                    .recordWriterArgs(variableDefinition.getRecordWriterArgs()).recordWriterFactory(RecordWriterFactory.json).build();
        }

        private Writer createWriter(VariableDefinition vd) {
            return vd == null
                    ? null
                    : FcliVariableHelper.getVariableContentsWriter(vd.getVariableName(), vd.getDefaultPropertyName(), vd.isSingular(),
                            vd.encrypt);
        }

        private VariableDefinition createVariableDefinition(VariableStoreConfig vsc) {
            String variableName = vsc.getVariableName();
            String recordWriterArgs = vsc.getRecordWriterArgs();
            var ann = FcliCommandSpecHelper.findAnnotation(commandSpec, DefaultVariablePropertyName.class);
            String defaultPropertyName = ann == null ? null : ann.value();
            boolean encrypt = FcliCommandSpecHelper.findAnnotation(commandSpec, EncryptVariable.class) != null;
            return VariableDefinition.builder().variableName(variableName).recordWriterArgs(recordWriterArgs).singular(isSingularOutput())
                    .defaultPropertyName(defaultPropertyName).encrypt(encrypt).build();
        }
    }

    @Data
    @Builder
    private static final class VariableDefinition {
        private final String variableName;
        private final String recordWriterArgs;
        private final String defaultPropertyName;
        private final boolean singular;
        private final boolean encrypt;
    }
}
