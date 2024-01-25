/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.util;

import java.lang.module.ModuleDescriptor.Version;
import java.util.Optional;
import java.util.regex.Pattern;

public final class SemVerHelper {
    private static final Pattern semverPattern = Pattern.compile("([1-9]\\d*)\\.(\\d+)\\.(\\d+)(?:-(.*))?");
    /**
     * Loosely compare two semantic versions, returning -1 if first semver is lower than
     * the second, 0 if they are the same, or 1 if first semver is higher than the 
     * second. Null, blank, or non-semver values are always considered lower than
     * true semvers.
     * 
     * @param semver1
     * @param semver2
     * @return
     */
    public static final int compare(String semver1, String semver2) {
        var semver1Matcher = semverPattern.matcher(semver1==null?"":semver1);
        var semver2Matcher = semverPattern.matcher(semver2==null?"":semver2);
        if ( (semver1==null && semver2==null) || semver1.equals(semver2) ) {
            return 0;
        } else if ( !semver1Matcher.matches() && !semver2Matcher.matches() ) {
            return semver1.compareTo(semver2);
        } else if ( semver1Matcher.matches() && !semver2Matcher.matches() ) {
            return 1;
        } else if ( !semver1Matcher.matches() && semver2Matcher.matches() ) {
            return -1;
        } else {
            var version1 = Version.parse(semver1);
            var version2 = Version.parse(semver2);
            return version1.compareTo(version2);
        }
    }
    
    public static final Optional<String> getMajor(String semver) {
        var matcher = semverPattern.matcher(semver);
        return !matcher.matches() ? Optional.empty() : Optional.of(matcher.group(1));
    }
    
    public static final Optional<String> getMajorMinor(String semver) {
        var matcher = semverPattern.matcher(semver);
        return !matcher.matches() 
                ? Optional.empty() 
                : Optional.of(String.format("%s.%s", matcher.group(1), matcher.group(2)));
    }
}
