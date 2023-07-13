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
package com.fortify.cli.common.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * This {@link Set} implementation stores an index for every
 * inserted entry; the index for an entry can be retrieved 
 * using the {@link #getIndex(Object)} method. Indexes are
 * consecutive unless items are removed from this set.
 * 
 * @author rsenden
 *
 * @param <T>
 */
public class IndexAwareSet<T> implements Set<T> {
    private int currentIndex = 0;
    // We use a LinkedHashMap to have entryStream() return entries in original order
    private Map<T, Integer> map = new LinkedHashMap<>();
    
    public IndexAwareSet() {}
    
    public IndexAwareSet(Collection<T> collection) {
        addAll(collection);
    }
    
    /**
     * Get the index for the given entry, or -1 if the
     * entry cannot be found.
     * @param arg0
     * @return
     */
    public int getIndex(T arg0) {
        return Optional.ofNullable(map.get(arg0)).orElse(-1);
    }

    @Override
    public boolean add(T arg0) {
        if ( map.containsKey(arg0) ) { return false; }
        else {
            return map.put(arg0, ++currentIndex)==null;
        }
    }
    
    public int addAndGetIndex(T arg0) {
        return map.computeIfAbsent(arg0, x->++currentIndex);
    }

    @Override
    public boolean addAll(Collection<? extends T> arg0) {
        return arg0.stream().map(this::add).reduce(Boolean.FALSE, Boolean::logicalOr);
    }

    @Override
    public void clear() {
        map.clear();
        this.currentIndex = 0;
    }

    @Override
    public boolean contains(Object arg0) {
        return map.containsKey(arg0);
    }

    @Override
    public boolean containsAll(Collection<?> arg0) {
        return map.keySet().containsAll(arg0);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public boolean remove(Object arg0) {
        return map.remove(arg0)!=null;
    }

    @Override
    public boolean removeAll(Collection<?> arg0) {
        return map.keySet().removeAll(arg0);
    }

    @Override
    public boolean retainAll(Collection<?> arg0) {
        return map.keySet().retainAll(arg0);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    @Override
    public <A> A[] toArray(A[] arg0) {
        return map.keySet().toArray(arg0);
    }
    
    public Stream<Map.Entry<Integer,T>> entryStream() {
        return map.entrySet().stream()
                .map(e->Map.entry(e.getValue(), e.getKey()));
    }
}
