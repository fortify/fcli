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
package com.fortify.cli.common.rest.unirest;

import java.nio.file.Path;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;

/**
 * Describes a REST response body that was saved to a file.
 *
 * @param file absolute, normalized path of the saved file
 * @param size number of bytes written
 * @param contentType value of the response Content-Type header, or null if absent
 * @param status HTTP status code of the response
 */
@Reflectable
public record RestResponseFileRecord(Path file, long size, String contentType, int status) {
    /**
     * @return JSON representation with properties file, size, contentType and status,
     *         as output by commands and stored in action variables
     */
    public ObjectNode asObjectNode() {
        var result = JsonHelper.getObjectMapper().createObjectNode()
                .put("file", file.toString())
                .put("size", size);
        if ( contentType==null ) {
            result.putNull("contentType");
        } else {
            result.put("contentType", contentType);
        }
        return result.put("status", status);
    }
}
