/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.fod.entity.app.cli.mixin;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import picocli.CommandLine.Option;

//TODO Change description keys to be more like picocli convention
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
                completionCandidates = FoDSdlcStatusTypeIterable.class, descriptionKey = "SdlcStatusMixin")
        @Getter private FoDSdlcStatusType sdlcStatusType;
    }

    public static class OptionalOption extends AbstractFoDSdlcStatusType {
        @Option(names = {"--status", "--sdlc-status"}, required = false,
                completionCandidates = FoDSdlcStatusTypeIterable.class, descriptionKey = "SdlcStatusMixin")
        @Getter private FoDSdlcStatusType sdlcStatusType;
    }

}
