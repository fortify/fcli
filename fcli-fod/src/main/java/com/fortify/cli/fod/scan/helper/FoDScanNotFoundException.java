package com.fortify.cli.fod.scan.helper;

public class FoDScanNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public FoDScanNotFoundException(String message) {
        super(message);
    }
}
