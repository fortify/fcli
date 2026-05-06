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
package com.fortify.cli.aviator.fpr.model;


import lombok.Builder;
import lombok.Data;

/**
 * Streaming representation of a Description from FVDL.
 * Replaces JAXB Description for streaming parsing.
 *
 * Structure matches JAXB Description class from Descriptions section:
 * <Description classID="xxx">
 *   <Abstract>...</Abstract>
 *   <Explanation>...</Explanation>
 * </Description>
 */
@Data
@Builder
public class StreamedDescription {
    /**
     * The classID attribute - unique identifier for this description.
     * Used to map descriptions to vulnerabilities.
     */
    private String classID;

    /**
     * Short description text from <Abstract> element.
     * Contains FVDL markup with Replace, Paragraph, IfDef, etc. tags.
     */
    private String abstractText;

    /**
     * Detailed explanation text from <Explanation> element.
     * Contains FVDL markup with Replace, Paragraph, IfDef, etc. tags.
     */
    private String explanation;

    /**
     * Get abstract text (alias for compatibility with JAXB Description).
     */
    public String getAbstract() {
        return abstractText;
    }

    /**
     * Set abstract text (alias for compatibility with JAXB Description).
     */
    public void setAbstract(String abstractText) {
        this.abstractText = abstractText;
    }
}
