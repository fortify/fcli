/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.common.util;

import java.lang.module.ModuleDescriptor.Version;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;

/**
 * This class represents an optional semantic version, based on the
 * version string passed to the constructor. This version string doesn't
 * need to be a proper semantic version, in which case most methods will 
 * return an empty {@link Optional}, and comparison methods regard a
 * non-proper semantic version as not equal to/less than a true semantic 
 * version.
 * 
 * <p>This class supports both standard 3-part semantic versions (major.minor.patch)
 * and extended versions with 4 or more parts (e.g., major.minor.patch.build). 
 * Extended versions are considered valid and comparable.
 *
 * @author Ruud Senden
 */
@Getter
public final class SemVer implements Comparable<SemVer> {
    private static final Pattern semverPattern = Pattern.compile("([1-9]\\d*)\\.(\\d+)\\.(\\d+)(?:\\.(\\d+(?:\\.\\d+)*))?(?:-(.*))?");
    private final String semver;
    private final boolean properSemver;
    private final Optional<String> major;  
    private final Optional<String> minor;
    private final Optional<String> patch;
    private final Optional<String> additionalParts;
    private final Optional<String> label;
    private final Optional<String> majorMinor;
    private final Optional<String> majorMinorPatch;
    private final Optional<Version> version;
    
    public SemVer(String semver) {
        this.semver = semver==null?"":semver;
        var matcher = semverPattern.matcher(this.semver);
        this.properSemver = matcher.matches();
        this.major = optionalFormat(matcher, "{1}");
        this.minor = optionalFormat(matcher, "{2}");
        this.patch = optionalFormat(matcher, "{3}");
        this.additionalParts = optionalFormat(matcher, "{4}");
        this.label = optionalFormat(matcher, "{5}");
        this.majorMinor = optionalFormat(matcher, "{1}.{2}");
        this.majorMinorPatch = optionalFormat(matcher, "{1}.{2}.{3}");
        this.version = Optional.ofNullable(!matcher.matches() ? null : parseVersion(matcher));
    }
    
    private static Version parseVersion(Matcher matcher) {
        String versionStr = matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3);
        String additional = matcher.group(4);
        String label = matcher.group(5);
        
        if (additional != null) {
            versionStr += "." + additional;
        }
        if (label != null) {
            versionStr += "-" + label;
        }
        
        try {
            return Version.parse(versionStr);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * @return Minimal compatible version, i.e., major.0.0
     */
    public final Optional<String> getMinimalCompatible() {
        return major.map(_major->String.format("%s.0.0", _major));
    }
    
    /**
     * @return Maximal compatible version, i.e., major.minor.*
     */
    public final Optional<String> getMaximalCompatibleString() {
        return majorMinor.map(_majorMinor->String.format("%s.*", _majorMinor));
    }
    
    /**
     * @return String describing minimal and maximal compatible versions
     */
    public final Optional<String> getCompatibleVersionsString() {
        var _minCompat = getMinimalCompatible();
        var _maxCompat = getMaximalCompatibleString();
        return _minCompat.equals(getMajorMinorPatch())
                ? _maxCompat
                : Optional.ofNullable(!properSemver
                    ? null
                    : String.format("%s-%s", _minCompat.get(), _maxCompat.get()));
    }
    
    /**
     * 
     * @param other semver string to compare
     * @return true, unless either this or other isn't a proper semver, or if major
     *         version is different, or if this minor is less than other minor
     */
    public final boolean isCompatibleWith(String other) {
        return isCompatibleWith(new SemVer(other));
    }
    
    /**
     * @param other semver to compare
     * @return true, unless either this or other isn't a proper semver, or if major
     *         version is different, or if this minor is less than other minor 
     */
    public final boolean isCompatibleWith(SemVer other) {
        if ( !this.properSemver || !other.properSemver ) { return false; }
        if ( !this.major.equals(other.major) ) { return false; }
        if ( Integer.parseInt(this.minor.orElseThrow())<Integer.parseInt(other.minor.orElseThrow()) ) { 
            return false; 
        }
        return true;
    }

    public final int compareTo(String other) {
        return compareTo(new SemVer(other));
    }
    
    /**
     * Loosely compare the given version to our version, returning -1 if first semver 
     * is lower than the second, 0 if they are the same, or 1 if first semver is higher 
     * than the second. Null, blank, or non-semver values are always considered lower 
     * than true semvers.
     * 
     * @param other semver to compare this semver against
     * @return -1, 0, or 1 for less than, equal, or greater than
     */
    public final int compareTo(SemVer other) {
        if ( this.semver.equals(other.semver) ) {
            return 0;
        } else if ( !this.properSemver && !other.properSemver ) {
            return this.semver.compareTo(other.semver);
        } else if ( this.properSemver && !other.properSemver ) {
            return 1;
        } else if ( !this.properSemver && other.properSemver ) {
            return -1;
        } else {
            return this.version.orElseThrow().compareTo(other.version.orElseThrow());
        }
    }
    
    /**
     * Return an optional string based on the given matcher and format, where format
     * represents a {@link MessageFormat} format that can reference:
     * <ul>
     * <li>{0} - entire match</li>
     * <li>{1} - major version</li>
     * <li>{2} - minor version</li>
     * <li>{3} - patch version</li>
     * <li>{4} - additional parts (4th, 5th, etc. numeric parts)</li>
     * <li>{5} - version label (text after hyphen)</li>
     * </ul>
     * @param matcher regex matcher
     * @param format MessageFormat pattern
     * @return formatted string or empty
     */
    private static final Optional<String> optionalFormat(Matcher matcher, String format) {
        return Optional.ofNullable(!matcher.matches() 
                ? null
                : new MessageFormat(format).format(new Object[] {
                        matcher.group(0), matcher.group(1), matcher.group(2), 
                        matcher.group(3), matcher.group(4), matcher.group(5)
                }));
    }
}
