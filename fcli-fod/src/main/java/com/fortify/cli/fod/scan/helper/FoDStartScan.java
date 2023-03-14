package com.fortify.cli.fod.scan.helper;

import java.io.File;

import com.fortify.cli.fod.rest.helper.FoDFileTransferBase;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

// TODO See comments in superclass
// TODO The name of this class is somewhat misleading; in essence it just uploads a file
//      so a better name would be something like FoDUploadScanPackage
public class FoDStartScan extends FoDFileTransferBase {
    public FoDStartScan(UnirestInstance unirest, String relId, HttpRequest<?> endpoint, File uploadFile) {
        super(unirest, endpoint, uploadFile);
    }
}
