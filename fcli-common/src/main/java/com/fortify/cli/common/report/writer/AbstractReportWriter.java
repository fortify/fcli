package com.fortify.cli.common.report.writer;

import java.io.BufferedWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.IMessageResolver;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

/**
 * Abstract base class for writing reports that consist of at least
 * a summary file, and potentially additional files with more details.
 * 
 * @author rsenden
 *
 */
@Accessors(fluent = true)
public abstract class AbstractReportWriter implements IReportWriter {
    @Getter private final Path  absoluteOutputPath;
    @Getter private final ObjectNode summary;
    private final IMessageResolver messageResolver;
    private final IRecordWriter summaryWriter;
    private Map<String, BufferedWriter> bufferedWriters = new ConcurrentHashMap<>();
    private Map<String, IRecordWriter> recordWriters = new ConcurrentHashMap<>();

    public AbstractReportWriter(String outputPathName, IMessageResolver messageResolver) {
        this.absoluteOutputPath = Path.of(outputPathName).toAbsolutePath();
        this.messageResolver = messageResolver;
        this.summaryWriter = recordWriter(OutputFormat.yaml, "summary.txt", true, null);
        this.summary = JsonHelper.getObjectMapper().createObjectNode();
    }
    
    protected abstract Path entryPath(String fileName);
    protected abstract BufferedWriter newBufferedWriter(String fileName);
    protected abstract void closeReport();
    
    @Override
    public BufferedWriter bufferedWriter(String fileName) {
        return bufferedWriters.computeIfAbsent(fileName, this::newBufferedWriter);
    }
    
    @Override
    public IRecordWriter recordWriter(OutputFormat format, String fileName, boolean singular, String options) {
        return recordWriters.computeIfAbsent(fileName, f->newRecordWriter(format, fileName, singular, options));
    }

    @Override @SneakyThrows
    public void copyTextFile(Path source, String targetFileName) {
        try ( var fileWriter = bufferedWriter(targetFileName) ) {
            Files.newBufferedReader(source)
                .lines()
                .forEach(line->writeLine(fileWriter, line));
        }
    }
    
    @Override
    public void close() {
        writeSummary();
        recordWriters.values().forEach(IRecordWriter::close);
        bufferedWriters.values().forEach(this::close);
        writeChecksum();
        closeReport();
    }
    
    private IRecordWriter newRecordWriter(OutputFormat format, String fileName, boolean singular, String options) {
        var config = RecordWriterConfig.builder()
                .addActionColumn(false)
                .messageResolver(messageResolver)
                .options(options)
                .outputFormat(format)
                .pretty(true)
                .singular(singular)
                .writer(bufferedWriter(fileName))
                .build();
        return format.getRecordWriterFactory().createRecordWriter(config);
    }
    
    @SneakyThrows
    private void writeLine(BufferedWriter writer, String line) {
        writer.write(line); writer.newLine();
    }
    
    @SneakyThrows
    private void writeSummary() {
        summaryWriter.writeRecord(summary);
    }
    
    @SneakyThrows
    private void writeChecksum() {
        try (var checksumsWriter = newBufferedWriter("checksums.sha256")) {
            bufferedWriters.keySet().forEach(entry->writeChecksum(checksumsWriter, entry));
        }
    }

    @SneakyThrows
    private void writeChecksum(BufferedWriter writer, String entry) {
        Path path = entryPath(entry);
        var binaryIndicator = ""; //isBinary(path) ? "*" : "";
        byte[] hash = MessageDigest.getInstance("SHA256").digest(Files.readAllBytes(path));
        String checksum = String.format("%064X", new BigInteger(1, hash));
        writer.append(String.format("%s %s%s\n", checksum, binaryIndicator, entry));
    }
    
    @SneakyThrows
    private boolean isBinary(Path path) {
        String contentType = Files.probeContentType(path);
        return contentType!=null && contentType.startsWith("text/");
    }

    @SneakyThrows
    private void close(BufferedWriter writer) {
        writer.close();
    }
}
