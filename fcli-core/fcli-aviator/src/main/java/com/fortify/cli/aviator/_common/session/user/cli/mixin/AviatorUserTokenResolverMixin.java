package com.fortify.cli.aviator._common.session.user.cli.mixin;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.cli.mixin.CommonOptionMixins.AbstractTextResolverMixin;
import com.fortify.cli.common.exception.FcliSimpleException;

import picocli.CommandLine.Option;

/**
 * Mixin for resolving an Aviator user token from various sources (direct string, file, URL, environment variable).
 */
public class AviatorUserTokenResolverMixin extends AbstractTextResolverMixin {
    @Option(names = {"--token", "-t"}, descriptionKey = "fcli.aviator.session.login.token", paramLabel = "source", required = true, order = 1)
    private String textSource;

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
        String resolvedToken = super.getText();
        if (StringUtils.isBlank(resolvedToken)) {
            throw new FcliSimpleException("Resolved token value for --token option is blank or empty.");
        }
        return super.getText();
    }
}