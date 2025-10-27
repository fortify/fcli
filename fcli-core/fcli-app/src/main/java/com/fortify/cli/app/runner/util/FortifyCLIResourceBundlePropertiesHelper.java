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
package com.fortify.cli.app.runner.util;

import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fortify.cli.common.action.helper.ActionSchemaVersionHelper;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.fod._common.scan.helper.FoDScanStatus;
import com.fortify.cli.sc_dast.scan.helper.SCDastScanStatus;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobArtifactState;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobState;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactStatus;

/**
 *
 * @author Ruud Senden
 */
public class FortifyCLIResourceBundlePropertiesHelper {
    protected static final Properties getResourceBundleProperties() {
        var props = new Properties();
        initializeFoDProperties(props);
        initializeSCDastProperties(props);
        initializeSCSastProperties(props);
        initializeSSCProperties(props);
        initializeVersionRelatedProperties(props);
        return props;
    }

    private static final void initializeFoDProperties(Properties props) {
        props.setProperty("fcli.fod.scan.states", getValueNamesString(FoDScanStatus.values()));
        props.setProperty("fcli.fod.scan.states.complete", getValueNamesString(FoDScanStatus.getDefaultCompleteStates()));
    }
    
    private static final void initializeSCDastProperties(Properties props) {
        props.setProperty("fcli.sc-dast.scan.states", getValueNamesString(SCDastScanStatus.values()));
        props.setProperty("fcli.sc-dast.scan.states.complete", getValueNamesString(SCDastScanStatus.getDefaultCompleteStates()));
    }
    
    private static final void initializeSCSastProperties(Properties props) {
        props.setProperty("fcli.sc-sast.scan.jobStates", getValueNamesString(SCSastScanJobState.values()));
        props.setProperty("fcli.sc-sast.scan.jobStates.complete", getValueNamesString(SCSastScanJobState.getDefaultCompleteStates()));
        props.setProperty("fcli.sc-sast.scan.jobArtifactStates", getValueNamesString(SCSastScanJobArtifactState.values()));
        props.setProperty("fcli.sc-sast.scan.jobArtifactStates.complete", getValueNamesString(SCSastScanJobArtifactState.getDefaultCompleteStates()));
    }
    
    private static final void initializeSSCProperties(Properties props) {
        props.setProperty("fcli.ssc.artifact.states", getValueNamesString(SSCArtifactStatus.values()));
        props.setProperty("fcli.ssc.artifact.states.complete", getValueNamesString(SSCArtifactStatus.getDefaultCompleteStates()));
    }
    
    private static final void initializeVersionRelatedProperties(Properties props) {
        props.setProperty("fcli.action.supportedSchemaVersions", ActionSchemaVersionHelper.getSupportedSchemaVersionsString());
        props.setProperty("fcli.docBaseUrl", FcliBuildProperties.INSTANCE.getFcliDocBaseUrl());
    }
    
    private static final String getValueNamesString(Enum<?>[] values) {
        return getValuesString(values, Enum::name);
    }
    
    private static final String getValuesString(Enum<?>[] values, Function<Enum<?>, String> f) {
        return Stream.of(values).map(f).collect(Collectors.joining(", "));
    }
    
    public static final void main(String[] args) {
        getResourceBundleProperties().forEach((p,v)->System.out.println(p+":"+v));
    }
}
