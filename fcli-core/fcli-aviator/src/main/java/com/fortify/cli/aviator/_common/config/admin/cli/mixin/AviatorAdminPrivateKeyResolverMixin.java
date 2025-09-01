package com.fortify.cli.aviator._common.config.admin.cli.mixin;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.cli.mixin.CommonOptionMixins.AbstractTextResolverMixin;
import com.fortify.cli.common.exception.FcliSimpleException;

import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Mixin for resolving an Aviator admin private key from various sources (file, string, environment variable).
 */
public class AviatorAdminPrivateKeyResolverMixin extends AbstractTextResolverMixin {
    @Option(names = {"--private-key", "-p"}, descriptionKey = "fcli.aviator.admin-config.create.private-key", paramLabel = "source", required = true, order = 3)
    @Getter private String privateKeySource;

    @Override
    public String getTextSource() {
        return privateKeySource;
    }

    /**
     * Returns the resolved private key content as a string.
     * This method calls the underlying resolution logic from AbstractTextResolverMixin.
     * @return The resolved Aviator admin private key content string, or null if not resolved.
     */
    public String getPrivateKeyContents() {
        String resolvedKey = super.getText();
        if (StringUtils.isBlank(resolvedKey)) {
            throw new FcliSimpleException("Resolved private key value for --private-key option is blank or empty.");
        }
        return resolvedKey;
    }
}