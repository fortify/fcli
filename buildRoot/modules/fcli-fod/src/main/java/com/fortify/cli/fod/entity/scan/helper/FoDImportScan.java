/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/

package com.fortify.cli.fod.entity.scan.helper;

import java.io.File;

import com.fortify.cli.fod.rest.helper.FoDFileTransferBase;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

// TODO See comments in superclass
public class FoDImportScan extends FoDFileTransferBase {
    public FoDImportScan(UnirestInstance unirest, String relId, HttpRequest<?> endpoint, File uploadFile) {
        super(unirest, endpoint, uploadFile);
        importScanSessionId = getImportScanSessionDescriptor(relId).getImportScanSessionId();
        //System.out.println("Created import scan session id: " + importScanSessionId);
    }
}
