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

package com.fortify.cli.fod.entity.scan.cli.mixin;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import picocli.CommandLine.Option;

// TODO Can we dynamically get the number of units for each scan type?
//      Potentially these could change in future FoD versions.
// TODO Use @RequiredArgsConstructor instead of manually defining constructor
// TODO USe @Getter to generate getters
// TODO Change description keys to be more like picocli convention
// TODO Maybe move enum its own file?
public class FoDAssessmentTypeOptions {
    public enum FoDAssessmentType {
        Static(1, 4),
        StaticPlus(2, 6),
        DynamicWebsite(2, 6),
        DynamicPlusWebsite(6, 18),
        DynamicAPI(2, 6),
        DynamicPlusAPI(6, 18),
        Mobile(1, 4),
        MobilePlus(6, 18);

        public final int singleUnits;
        public final int subscriptionUnits;

        FoDAssessmentType(int singleUnits, int subscriptionUnits) {
            this.singleUnits = singleUnits;
            this.subscriptionUnits = subscriptionUnits;
        }

        public int getSingleUnits() {
            return this.singleUnits;
        }

        public int getSubscriptionUnits() {
            return this.subscriptionUnits;
        }

        public FoDScanTypeOptions.FoDScanType toScanType() {
            switch (this) {
                case DynamicWebsite:
                case DynamicPlusWebsite:
                case DynamicAPI:
                case DynamicPlusAPI:
                        return FoDScanTypeOptions.FoDScanType.Dynamic;
                case Mobile:
                case MobilePlus:
                        return FoDScanTypeOptions.FoDScanType.Mobile;
                default:
                    return FoDScanTypeOptions.FoDScanType.Static;
            }
        }

    }

    public static final class FoDAssessmentTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;

        public FoDAssessmentTypeIterable() {
            super(Stream.of(FoDAssessmentType.values()).map(FoDAssessmentType::name).collect(Collectors.toList()));
        }
    }

    public static abstract class AbstractFoDAssessmentType {
        public abstract FoDAssessmentType getAssessmentType();
    }

    public static class RequiredOption extends AbstractFoDAssessmentType {
        @Option(names = {"--assessment", "--assessment-type"}, required = true,
                completionCandidates = FoDAssessmentTypeIterable.class, descriptionKey = "fcli.fod.scan.assessment-type")
        @Getter
        private FoDAssessmentType assessmentType;
    }

    public static class OptionalOption extends AbstractFoDAssessmentType {
        @Option(names = {"--assessment", "--assessment-type"}, required = false,
                completionCandidates = FoDAssessmentTypeIterable.class, descriptionKey = "fcli.fod.scan.assessment-type")
        @Getter
        private FoDAssessmentType assessmentType;
    }

}
