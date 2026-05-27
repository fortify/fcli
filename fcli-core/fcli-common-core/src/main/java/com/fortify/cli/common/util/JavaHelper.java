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

import java.util.Optional;
import java.util.function.Supplier;

/**
 * This class contains various Java utility methods that don't fit
 * in any of the other Helper classes.
 * 
 * @author rsenden
 *
 */
public final class JavaHelper {
    private JavaHelper() {}
    
    /**
     * Cast the given object to the given type if possible, or
     * return {@link Optional#empty()} if the given object is 
     * null or cannot be cast to the given type.
     */
    public static final <T> Optional<T> as(Object obj, Class<T> type) {
        return is(obj, type) ? Optional.ofNullable(obj).map(type::cast) : Optional.empty();
    }
    
    /**
     * Return the given object of not null, otherwise create a new 
     * object using the given supplier.
     */
    public static final <T> T getOrCreate(T obj, Supplier<T> supplier) {
        return Optional.ofNullable(obj).orElseGet(supplier);
    }
    
    /**
     * This method returns true if the given object is not null and
     * assignable from the given type, false otherwise.
     */
    public static final <T> boolean is(Object obj, Class<T> type) {
        return obj!=null && type.isAssignableFrom(obj.getClass());
    }
    
    /**
     * Returns a stable identity string for the given object, consisting of
     * the simple class name and the object's identity hash code in hexadecimal
     * format, separated by '@' (e.g., "ClassName@1a2b3c4d"). This format is
     * similar to the default Object.toString() output and is useful for creating
     * unique cache keys or stable identifiers that bypass any overridden
     * toString() implementation.
     * 
     * @param obj The object to create an identity string for
     * @return Identity string in format "SimpleClassName@hexIdentityHash"
     */
    public static final String identity(Object obj) {
        return obj.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(obj));
    }
}
