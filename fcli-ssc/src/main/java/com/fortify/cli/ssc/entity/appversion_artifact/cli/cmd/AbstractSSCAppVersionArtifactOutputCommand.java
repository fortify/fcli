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
package com.fortify.cli.ssc.entity.appversion_artifact.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.ssc.entity.appversion_artifact.helper.SSCAppVersionArtifactHelper;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;

public abstract class AbstractSSCAppVersionArtifactOutputCommand extends AbstractSSCOutputCommand implements IRecordTransformer {
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return SSCAppVersionArtifactHelper.addScanTypes(record);
    }
}