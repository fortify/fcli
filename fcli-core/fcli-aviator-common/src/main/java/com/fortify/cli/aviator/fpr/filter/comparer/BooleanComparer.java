/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.aviator.fpr.filter.comparer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A SearchComparer that holds a list of other comparers.
 * It correctly handles mixed positive (OR) and negative (AND) conditions for a single attribute.
 */
public class BooleanComparer implements SearchComparer {
    private final List<SearchComparer> orComparers = new ArrayList<>();
    private final List<SearchComparer> andComparers = new ArrayList<>();

    public void addComparer(SearchComparer comparer) {
        if (comparer == null) {
            return;
        }
        if (comparer instanceof IsNotSearchComparer) {
            andComparers.add(comparer);
        } else {
            orComparers.add(comparer);
        }
    }

    @Override
    public boolean matches(Object attributeValue) {
        // THIS IS THE FIX: A completely empty comparer must never match.
        if (andComparers.isEmpty() && orComparers.isEmpty()) {
            return false;
        }

        // All negative ("AND") conditions must pass.
        for (SearchComparer sc : andComparers) {
            if (!sc.matches(attributeValue)) {
                return false;
            }
        }

        // If there are positive ("OR") conditions, at least one must pass.
        for (SearchComparer sc : orComparers) {
            if (sc.matches(attributeValue)) {
                return true;
            }
        }

        boolean result = orComparers.isEmpty();
        return result;
    }

    public List<SearchComparer> getOrComparers() {
        return orComparers;
    }

    public List<SearchComparer> getAndComparers() {
        return andComparers;
    }

    public String getSearchTerm() {
        String orStr = orComparers.stream().map(SearchComparer::getSearchTerm).collect(Collectors.joining(" OR "));
        String andStr = andComparers.stream().map(SearchComparer::getSearchTerm).collect(Collectors.joining(" AND "));
        return "( " + orStr + " ) ( " + andStr + " )";
    }
}
