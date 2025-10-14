/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.fod._common.scan.cli.cmd;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDEntitlementFrequencyTypeMixins;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public abstract class AbstractFoDScanSetupCommand<T> extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    private static final Log LOG = LogFactory.getLog(AbstractFoDScanSetupCommand.class);
    @Mixin protected FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin protected FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;

    @Option(names = {"--assessment-type"}, required = true)
    protected String assessmentType; // Plain text name as custom assessment types can be created
    @Mixin protected FoDEntitlementFrequencyTypeMixins.RequiredOption entitlementFrequencyTypeMixin;
    @Option(names = {"--entitlement-id"})
    protected Integer entitlementId;
    @Option(names={"--skip-if-exists"})
    protected Boolean skipIfExists = false;

    protected String assessmentTypeName;

    protected abstract String getScanType();
    protected abstract String getSetupType();
    protected abstract T getSetupDescriptor(UnirestInstance unirest, String releaseId);
    protected abstract boolean isExistingSetup(T setupDescriptor);
    protected abstract ObjectNode convertToObjectNode(T setupDescriptor);
    protected JsonNode handleSkipIfExists(boolean skipIfExists, T setupDescriptor, FoDReleaseDescriptor releaseDescriptor) {
        if (skipIfExists && isExistingSetup(setupDescriptor)) {
            ObjectNode skippedNode = convertToObjectNode(setupDescriptor);
            skippedNode.put("setupType", getSetupType())
                    .put("scanType", getScanType())
                    .put("applicationName", releaseDescriptor.getApplicationName())
                    .put("releaseName", releaseDescriptor.getReleaseName())
                    .put("microserviceName", releaseDescriptor.getMicroserviceName())
                    .put("__action__", "SKIPPED_EXISTING");
            return skippedNode;
        }
        return null;
    }
    protected abstract JsonNode setup(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, T currentSetup);

    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        var releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
        var releaseId = releaseDescriptor.getReleaseId();
        T setupDescriptor = getSetupDescriptor(unirest, releaseId);
        var skippedNode = handleSkipIfExists(skipIfExists, setupDescriptor, releaseDescriptor);
        if (skippedNode != null) {
            return skippedNode;
        } else {
            return setup(unirest, releaseDescriptor, setupDescriptor);
        }
    }

    @Override
    public final String getActionCommandResult() {
        return "SETUP";
    }

    @Override
    public final boolean isSingular() {
        return true;
    }

}
