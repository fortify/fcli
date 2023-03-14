package com.fortify.cli.fod.scan.helper;

// TODO Although we still need to come to a final conclusion (https://github.com/fortify/fcli/issues/15),
//      most fcli code throws existing exceptions like IllegalArumentException in case an entity is not found.
public class FoDScanNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public FoDScanNotFoundException(String message) {
        super(message);
    }
}
