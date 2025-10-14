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

import com.fortify.cli.common.output.writer.record.RecordWriterFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
// TODO Add null checks in case any input or record transformation returns null?
public class StandardOutputConfig {
    @Getter @Setter private RecordWriterFactory defaultFormat;

    public static final StandardOutputConfig csv() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.csv);
    }

    public static final StandardOutputConfig json() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.json);
    }

    public static final StandardOutputConfig table() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.table);
    }

    public static final StandardOutputConfig xml() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.xml);
    }

    public static final StandardOutputConfig yaml() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.yaml);
    }

    public static final StandardOutputConfig details() {
        return yaml();
    }
}
