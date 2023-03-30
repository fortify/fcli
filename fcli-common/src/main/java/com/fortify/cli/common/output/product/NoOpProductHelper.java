package com.fortify.cli.common.output.product;

public class NoOpProductHelper implements IProductHelper {
    private static final NoOpProductHelper INSTANCE = new NoOpProductHelper();
    private NoOpProductHelper() {}
    public static IProductHelper instance() {
        return INSTANCE;
    }
}