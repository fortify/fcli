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
package com.fortify.cli.aviator.fpr.model;

import java.util.ArrayList;
import java.util.List;

import com.fortify.cli.aviator.fpr.jaxb.Function;
import com.fortify.cli.aviator.fpr.jaxb.SourceLocationType;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents an ExternalEntries Entry from FVDL, containing URL, fields, function, and location.
 * Used to store external data associated with a vulnerability.
 */
@Getter
@Setter
public class Entry {
    private String url; // From <URL>
    private List<Field> fields = new ArrayList<>(); // From <Fields><Field>
    private Function function; // From <Function>
    private SourceLocationType location; // From <SourceLocation>

    /**
     * Inner class for Field within ExternalEntries Entry.
     * Holds name, value, type, and vulnTag attributes.
     */
    @Getter
    @Setter
    public static class Field {
        private String name; // From <Name>
        private String value; // From <Value>
        private String type; // From @type
        private String vulnTag; // From @vulnTag
    }
}