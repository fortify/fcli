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

package com.fortify.cli.fod.rest.cli.mixin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

// TODO Why is this class in 'rest' package?
// TODO Any reason for not using the period syntax from DateTimePeriodHelper from fcli-common?
//      Preferably, all fcli commands should use similar date/time/period formats if not dictated
//      by server.
//TODO Change description keys to be more like picocli convention
public class FoDTimePeriodOptions {
    public enum FoDTimePeriodType {
        Last30, Last60, Last90, Last180, Last360;

        public String getDateTime() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
            Calendar cal = new GregorianCalendar();
            cal.setTime(new Date());
            switch(this) {
                case Last30:
                    cal.add(Calendar.DAY_OF_MONTH, -30);
                    break;
                case Last60:
                    cal.add(Calendar.DAY_OF_MONTH, -60);
                    break;
                case Last90:
                    cal.add(Calendar.DAY_OF_MONTH, -90);
                    break;
                case Last180:
                    cal.add(Calendar.DAY_OF_MONTH, -180);
                    break;
                case Last360:
                    cal.add(Calendar.DAY_OF_MONTH, -360);
                    break;
                default:
                    break;
            }
            return simpleDateFormat.format(cal.getTime());
        }
    }

    @ReflectiveAccess
    public static final class FoDTimePeriodTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public FoDTimePeriodTypeIterable() {
            super(Stream.of(FoDTimePeriodType.values()).map(FoDTimePeriodType::name).collect(Collectors.toList()));
        }
    }
    @ReflectiveAccess
    public static abstract class AbstractFoDTimePeriodType {
        public abstract FoDTimePeriodType getTimePeriodType();
    }

    @ReflectiveAccess
    public static class RequiredOption extends AbstractFoDTimePeriodType {
        @Option(names = {"--period", "--time-period"}, required = true, arity = "1",
                completionCandidates = FoDTimePeriodTypeIterable.class, defaultValue = "Last30",
                descriptionKey = "TimePeriodMixin")
        @Getter private FoDTimePeriodType timePeriodType;
    }

    @ReflectiveAccess
    public static class OptionalOption extends AbstractFoDTimePeriodType {
        @Option(names = {"--period", "--time-period"}, required = false, arity = "1",
                completionCandidates = FoDTimePeriodTypeIterable.class, defaultValue = "Last30",
                descriptionKey = "TimePeriodMixin")
        @Getter private FoDTimePeriodType timePeriodType;
    }

}
