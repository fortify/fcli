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

package com.fortify.cli.fod.entity.rest.cli.mixin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static final class FoDTimePeriodTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public FoDTimePeriodTypeIterable() {
            super(Stream.of(FoDTimePeriodType.values()).map(FoDTimePeriodType::name).collect(Collectors.toList()));
        }
    }
    public static abstract class AbstractFoDTimePeriodType {
        public abstract FoDTimePeriodType getTimePeriodType();
    }

    public static class RequiredOption extends AbstractFoDTimePeriodType {
        @Option(names = {"--period", "--time-period"}, required = true,
                completionCandidates = FoDTimePeriodTypeIterable.class, defaultValue = "Last30",
                descriptionKey = "fcli.fod.scan.time-period")
        @Getter private FoDTimePeriodType timePeriodType;
    }

    public static class OptionalOption extends AbstractFoDTimePeriodType {
        @Option(names = {"--period", "--time-period"}, required = false,
                completionCandidates = FoDTimePeriodTypeIterable.class, defaultValue = "Last30",
                descriptionKey = "fcli.fod.scan.time-period")
        @Getter private FoDTimePeriodType timePeriodType;
    }

}
