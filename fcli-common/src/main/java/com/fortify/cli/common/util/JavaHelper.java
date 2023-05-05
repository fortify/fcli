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
}
