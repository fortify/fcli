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
package com.fortify.cli.common.output.writer.record.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.fortify.cli.common.util.ConsoleHelper;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;
import com.github.freva.asciitable.OverflowBehaviour;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecordWriterTable extends AbstractRecordWriter<RecordWriterTable.TableWriter> {
    @Getter private final RecordWriterConfig config;
    private static final int BATCH_SIZE = 100; // Row batch size for fast-output streaming

    @Override
    protected void append(TableWriter out, ObjectNode formattedRecord) throws IOException { out.append(formattedRecord); }

    @Override
    protected Function<ObjectNode, ObjectNode> createRecordFormatter(ObjectNode objectNode) throws IOException {
        return createStructuredOutputTransformer(true, Function.identity()); // Always flatten
    }

    @Override
    protected void close(TableWriter out) throws IOException { out.close(); }

    @Override
    protected void closeWithNoData(Writer writer) throws IOException { writer.write("No data"); writer.flush(); writer.close(); }

    @Override
    protected TableWriter createOut(Writer writer, ObjectNode formattedRecord) throws IOException {
        if ( formattedRecord==null ) { return null; }
        List<String> headers = formattedRecord.properties().stream().map(e->e.getKey()).toList();
        return new TableWriter(writer, headers);
    }

    @RequiredArgsConstructor
    protected final class TableWriter implements Closeable {
        private final Writer writer;
        private final List<String> headers;
        private final List<String[]> rows = new ArrayList<>();
        private int[] columnWidths; // Computed content widths (excluding padding)
        private long totalRowCount = 0;
        private Character[] firstAndOnlySegmentBorders;
        private Character[] firstOfMultiSegmentBorders;
        private Character[] intermediateSegmentBorders;
        private Character[] lastSegmentBorders;

        public void append(ObjectNode formattedRecord) {
            rows.add(asColumnArray(formattedRecord));
            totalRowCount++;
            if ( config.getStyle().isFastOutput() ) {
                if ( columnWidths==null && rows.size()==BATCH_SIZE ) { // First batch triggers width calculation & output
                    columnWidths = calculateColumnWidths(rows);
                    outputSegment(false);
                    rows.clear();
                } else if ( columnWidths!=null && rows.size()==BATCH_SIZE ) { // Subsequent full batch
                    outputSegment(false);
                    rows.clear();
                }
            }
        }

        private String[] asColumnArray(ObjectNode formattedRecord) {
            return headers.stream().map(h->getColumnValue(formattedRecord, h)).toArray(String[]::new);
        }

        private String getColumnValue(ObjectNode formattedRecord, String property) {
            var node = formattedRecord.get(property);
            if ( node==null || node.isNull() ) { return "N/A"; }
            if ( node.isArray() ) {
                return JsonHelper.stream((ArrayNode)node).map(n->n.asText()).collect(Collectors.joining(","));
            }
            return node.asText();
        }

        @Override
        public void close() throws IOException {
            if ( columnWidths==null && !rows.isEmpty() ) { // no-fast-output or trailing partial batch
                columnWidths = calculateColumnWidths(rows);
            }
            if ( rows.isEmpty() && totalRowCount==0 ) {
                writer.write("No data\n");
            } else if ( !rows.isEmpty() ) {
                outputSegment(true);
            }
            writer.flush();
            writer.close();
        }

        private void outputSegment(boolean finalSegment) {
            Character[] borders = determineSegmentBorders(finalSegment);
            String table = asTable(borders, shouldIncludeHeadersForCurrentSegment());
            if ( table.isEmpty() ) { return; }
            try {
                writer.write(table);
                writer.write('\n');
                writer.flush();
            } catch ( IOException e ) {
                throw new RuntimeException("Error writing table segment", e);
            }
        }

        private String asTable(Character[] borders, boolean includeHeaders) {
            if ( rows.isEmpty() ) { return ""; }
            Column[] columns = headers.stream().map(h -> {
                Column col = new Column().dataAlign(HorizontalAlign.LEFT).headerAlign(HorizontalAlign.LEFT);
                if ( includeHeaders && config.getStyle().withHeaders() ) { col.header(formatHeader(h)); }
                if ( columnWidths!=null ) {
                    int contentWidth = columnWidths[headers.indexOf(h)];
                    int paddedWidth = contentWidth + 2; // Add padding expected by AsciiTable
                    col.minWidth(paddedWidth).maxWidth(paddedWidth, config.getStyle().isWrap() ? OverflowBehaviour.NEWLINE : OverflowBehaviour.ELLIPSIS_RIGHT);
                }
                return col;
            }).toArray(Column[]::new);
            String result = AsciiTable.getTable(borders, columns, rows.toArray(String[][]::new));
            if ( config.getStyle().isMarkdownBorder() ) {
                result = result.replaceAll("(?m)^\\s+$", "").replaceAll("(?m)^\\n", "");
            }
            return result;
        }

        private boolean shouldIncludeHeadersForCurrentSegment() {
            // In no-fast-output mode we output only once at close(), so always include headers.
            if ( !config.getStyle().isFastOutput() ) { return true; }
            // In fast-output mode, only include headers in the first segment (<= first batch size)
            return totalRowCount <= BATCH_SIZE;
        }

        private Character[] determineSegmentBorders(boolean finalSegment) {
            if ( firstAndOnlySegmentBorders==null ) {
                firstAndOnlySegmentBorders = getBorders();
                firstOfMultiSegmentBorders = createFirstMultiSegmentBorders(firstAndOnlySegmentBorders);
                intermediateSegmentBorders = createContinuationBorders(firstAndOnlySegmentBorders);
                lastSegmentBorders = createFinalMultiSegmentBorders(firstAndOnlySegmentBorders);
            }
            // For no-fast-output we always output a single segment, regardless of row count
            if ( !config.getStyle().isFastOutput() ) { return firstAndOnlySegmentBorders; }
            if ( totalRowCount <= BATCH_SIZE ) { return finalSegment ? firstAndOnlySegmentBorders : firstOfMultiSegmentBorders; }
            return finalSegment ? lastSegmentBorders : intermediateSegmentBorders;
        }

        private Character[] createFirstMultiSegmentBorders(Character[] original) {
            Character[] modified = original.clone();
            for ( int src=14, dst=25; src<=17; src++, dst++ ) { modified[dst]=original[src]; }
            return modified;
        }
        private Character[] createContinuationBorders(Character[] original) {
            Character[] modified = original.clone();
            for ( int i=0;i<=3;i++ ) { modified[i]=null; }
            for ( int i=7;i<=10;i++ ) { modified[i]=null; }
            for ( int src=14, dst=25; src<=17; src++, dst++ ) { modified[dst]=original[src]; }
            return modified;
        }
        private Character[] createFinalMultiSegmentBorders(Character[] original) {
            Character[] modified = original.clone();
            for ( int i=0;i<=3;i++ ) { modified[i]=null; }
            for ( int i=7;i<=10;i++ ) { modified[i]=null; }
            return modified;
        }

        private int[] calculateColumnWidths(List<String[]> sampleRows) {
            int cols = headers.size();
            int[] dataWidths = new int[cols];
            for ( var row : sampleRows ) {
                for ( int i=0;i<row.length;i++ ) {
                    int w = maxLineLength(row[i]); if ( w>dataWidths[i] ) { dataWidths[i]=w; }
                }
            }
            int[] headerWidths = headers.stream().mapToInt(h->maxLineLength(formatHeader(h))).toArray();
            int[] minWidths = new int[cols];
            int[] maxWidths = new int[cols];
            for ( int i=0;i<cols;i++ ) {
                int headerWidth = headerWidths[i];
                minWidths[i] = Math.max(6, headerWidth);
                maxWidths[i] = Math.max(minWidths[i], dataWidths[i]);
            }
            Integer terminalWidth = ConsoleHelper.getTerminalWidth();
            if ( terminalWidth==null ) { return maxWidths; }
            int paddingPerCol = 2;
            int columnSeparators = cols-1;
            int verticalBorders = 2;
            Character[] borderChars = getBorders();
            boolean hasOuterBorder = borderChars[0]!=null && borderChars[3]!=null;
            if ( !hasOuterBorder ) { verticalBorders = 0; }
            int overhead = paddingPerCol*cols + columnSeparators + verticalBorders;
            int availableForContent = terminalWidth - overhead;
            int[] finalWidths = Arrays.copyOf(minWidths, cols);
            int remaining = availableForContent - Arrays.stream(finalWidths).sum();
            if ( remaining<0 ) { remaining = 0; }
            while ( remaining>0 ) {
                boolean progress=false;
                for ( int i=0;i<finalWidths.length && remaining>0;i++ ) {
                    if ( finalWidths[i]<maxWidths[i] ) { finalWidths[i]++; remaining--; progress=true; }
                }
                if ( !progress ) { break; }
            }
            return finalWidths;
        }

        private int maxLineLength(String text) {
            if ( text==null ) { return 0; }
            int max=0, start=0, len=text.length();
            for ( int i=0;i<=len;i++ ) {
                if ( i==len || text.charAt(i)=='\n' ) {
                    int lineLen=i-start; if ( lineLen>max ) { max=lineLen; }
                    start=i+1;
                }
            }
            return max;
        }
        private String formatHeader(String header) { return header.startsWith("_.") ? "" : header; }
        private Character[] getBorders() {
            var style = config.getStyle();
            if ( style.isMarkdownBorder() ) { return "    ||||-|||||               ".chars().mapToObj(c->(char)c).toArray(Character[]::new); }
            if ( style.isBorder() ) { return AsciiTable.BASIC_ASCII; }
            return AsciiTable.NO_BORDERS;
        }
    }
}
