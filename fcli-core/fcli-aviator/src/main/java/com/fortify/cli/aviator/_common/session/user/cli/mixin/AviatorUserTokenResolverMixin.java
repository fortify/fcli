package com.fortify.cli.aviator._common.session.user.cli.mixin;

import com.fortify.cli.common.cli.mixin.CommonOptionMixins.AbstractTextResolverMixin;

import com.fortify.cli.common.exception.FcliSimpleException;
import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Mixin for resolving an Aviator user token from various sources (direct string, file, URL, environment variable).
 */
public class AviatorUserTokenResolverMixin extends AbstractTextResolverMixin {
    @Option(names = {"--token", "-t"}, descriptionKey = "fcli.aviator.session.login.token", paramLabel = "source", required = true, order = 1)
    @Getter private String textSource;

    @Override
    public String getTextSource() {
        return textSource;
    }

    /**
     * Returns the resolved token text.
     * This method calls the underlying resolution logic from AbstractTextResolverMixin.
     * @return The resolved Aviator user token string, or null if not provided or resolved.
     */
    public String getToken() {
        String source = getTextSource();
        if (source != null && source.toLowerCase().startsWith("url:")) {
            throw new FcliSimpleException("Providing Aviator tokens via URL ('url:' prefix) is not supported");
        }
        return super.getText();
    }
}