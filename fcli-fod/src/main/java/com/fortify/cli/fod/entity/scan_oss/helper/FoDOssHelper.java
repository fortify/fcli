/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/

package com.fortify.cli.fod.entity.scan_oss.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;

import lombok.Getter;

public class FoDOssHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // TODO Any plans to actually rename any fields? We should document
    //      a convention for methods like these; do we want commands to
    //      consistently call a *Helper.renameFields/transformRecord method,
    //      even if it doesn't do anything, reducing the risk that commands
    //      forget to invoke this method when it actually does something?
    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

}
