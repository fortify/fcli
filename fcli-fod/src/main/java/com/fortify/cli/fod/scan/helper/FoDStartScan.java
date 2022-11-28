package com.fortify.cli.fod.scan.helper;

import com.fortify.cli.fod.rest.helper.FoDFileTransferBase;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

import java.io.File;

public class FoDStartScan extends FoDFileTransferBase {
    public FoDStartScan(UnirestInstance unirest, String relId, HttpRequest endpoint, File uploadFile) {
        super(unirest, endpoint, uploadFile);
    }
}
