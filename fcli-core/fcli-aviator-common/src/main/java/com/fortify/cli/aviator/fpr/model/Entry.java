package com.fortify.cli.aviator.fpr.model;

import com.fortify.cli.aviator.fpr.jaxb.Function;
import com.fortify.cli.aviator.fpr.jaxb.SourceLocationType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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