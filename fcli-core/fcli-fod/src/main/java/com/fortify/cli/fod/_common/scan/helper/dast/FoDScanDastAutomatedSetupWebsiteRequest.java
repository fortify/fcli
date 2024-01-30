/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 *******************************************************************************/

package com.fortify.cli.fod._common.scan.helper.dast;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.fod._common.util.FoDEnums;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;

@EqualsAndHashCode(callSuper = true)
@Reflectable @NoArgsConstructor @AllArgsConstructor
@Data @SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FoDScanDastAutomatedSetupWebsiteRequest extends FoDScanDastAutomatedSetupBaseRequest {

    @Reflectable @NoArgsConstructor @AllArgsConstructor
    @Getter @ToString
    public static class Exclusion {
        String value;
    }

    @Reflectable @NoArgsConstructor @AllArgsConstructor
    @Getter @ToString
    public static class LoginMacroFileCreationType {
        String primaryUsername;
        String primaryPassword;
        String secondaryUsername;
        String secondaryPassword;
    }

    @Builder.Default
    private Boolean enableRedundantPageDetection = false;
    @Builder.Default
    public String policy = "Standard"; // ['Standard', 'Api', 'CriticalsAndHighs', 'PassiveScan']
    private String dynamicSiteUrl;
    @Builder.Default
    private Integer loginMacroFileId = 0;
    @Builder.Default
    private Boolean requiresSiteAuthentication = false;
    private ArrayList<Exclusion> exclusionsList;
    @Builder.Default
    private Boolean restrictToDirectoryAndSubdirectories = false;
    @Builder.Default
    private Boolean requestLoginMacroFileCreation = false;
    private LoginMacroFileCreationType loginMacroFileCreationDetails;
}
