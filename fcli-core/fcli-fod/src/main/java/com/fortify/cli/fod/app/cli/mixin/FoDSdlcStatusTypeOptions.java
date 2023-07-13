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
package com.fortify.cli.fod.app.cli.mixin;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import picocli.CommandLine.Option;

//TODO Enum case? See comments in FoDAppTypeOptions
public class FoDSdlcStatusTypeOptions {
    public enum FoDSdlcStatusType {Development, QA, Production}

    public static final class FoDSdlcStatusTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public FoDSdlcStatusTypeIterable() {
            super(Stream.of(FoDSdlcStatusType.values()).map(FoDSdlcStatusType::name).collect(Collectors.toList()));
        }
    }

    public static abstract class AbstractFoDSdlcStatusType {
        public abstract FoDSdlcStatusType getSdlcStatusType();
    }

    public static class RequiredOption extends AbstractFoDSdlcStatusType {
        @Option(names = {"--status", "--sdlc-status"}, required = true,
                completionCandidates = FoDSdlcStatusTypeIterable.class, descriptionKey = "fcli.fod.release.sdlc-status")
        @Getter private FoDSdlcStatusType sdlcStatusType;
    }

    public static class OptionalOption extends AbstractFoDSdlcStatusType {
        @Option(names = {"--status", "--sdlc-status"}, required = false,
                completionCandidates = FoDSdlcStatusTypeIterable.class, descriptionKey = "fcli.fod.release.sdlc-status")
        @Getter private FoDSdlcStatusType sdlcStatusType;
    }

}
