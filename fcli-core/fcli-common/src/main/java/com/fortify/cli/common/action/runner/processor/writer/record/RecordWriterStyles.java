/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.common.action.runner.processor.writer.record;

import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecordWriterStyles {
    private final List<RecordWriterStyle> styles;
    
    public static final RecordWriterStyles apply(RecordWriterStyle... styles) {
        return new RecordWriterStyles(List.of(styles));
    }
    
    public final boolean has(RecordWriterStyle style) {
        return styles.contains(style);
    }
    
    public static enum RecordWriterStyle {
        SHOW_HEADERS, FLATTEN
    }
}
