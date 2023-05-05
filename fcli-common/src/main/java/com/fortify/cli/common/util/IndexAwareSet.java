package com.fortify.cli.common.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private Map<T, Integer> map = new HashMap<>();
    
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
}
