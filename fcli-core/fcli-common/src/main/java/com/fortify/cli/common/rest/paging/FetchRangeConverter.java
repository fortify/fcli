/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.common.rest.paging;

import com.fortify.cli.common.exception.FcliSimpleException;

import picocli.CommandLine.ITypeConverter;

public class FetchRangeConverter implements ITypeConverter<FetchRange> {
    @Override
    public FetchRange convert(String value) {
        try {
            var parts = value.split("-", 2);
            int start, end;
            if ( parts.length == 1 ) {
                start = 1;
                end = Integer.parseInt(parts[0].trim());
                if ( end < 1 ) throw new FcliSimpleException("--fetch value must be >= 1");
            } else {
                start = Integer.parseInt(parts[0].trim());
                end = Integer.parseInt(parts[1].trim());
                if ( start < 1 ) throw new FcliSimpleException("--fetch start must be >= 1");
                if ( end < start ) throw new FcliSimpleException("--fetch end must be >= start (%d)".formatted(start));
            }
            return new FetchRange(start - 1, end - start + 1);
        } catch ( NumberFormatException e ) {
            throw new FcliSimpleException("Invalid --fetch value '%s'. Expected format: [<start>-]<end>".formatted(value));
        }
    }
}
