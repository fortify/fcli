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
package com.fortify.cli.fod._common.scan.helper.compare;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

/**
 * Counts all added items, but retains only the first {@code maxItems} items according to
 * the given sort order, bounding memory usage and output size for large numbers of changes.
 */
final class FoDScanCompareLimitedList<T> {
    private final int maxItems;
    private final Comparator<T> comparator;
    // Reversed order, such that the head is the item to drop first when exceeding maxItems
    private final PriorityQueue<T> retained;
    private long count;

    FoDScanCompareLimitedList(int maxItems, Comparator<T> comparator) {
        this.maxItems = maxItems;
        this.comparator = comparator;
        this.retained = new PriorityQueue<>(comparator.reversed());
    }

    void add(T item) {
        count++;
        if ( maxItems <= 0 ) { return; }
        retained.add(item);
        if ( retained.size() > maxItems ) { retained.poll(); }
    }

    long getCount() {
        return count;
    }

    boolean isTruncated() {
        return count > retained.size();
    }

    /** Add the retained items as '{name}s', together with '{name}Count' and '{name}sTruncated' */
    void addTo(ObjectNode result, String name, Function<T, JsonNode> mapper) {
        var array = JsonHelper.getObjectMapper().createArrayNode();
        retained.stream().sorted(comparator).map(mapper).forEach(array::add);
        result.put(name + "Count", count);
        result.set(name + "s", array);
        result.put(name + "sTruncated", isTruncated());
    }
}
