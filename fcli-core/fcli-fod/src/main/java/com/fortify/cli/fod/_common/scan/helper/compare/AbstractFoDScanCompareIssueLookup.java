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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionIssue;

import kong.unirest.UnirestException;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for looking up optional per-issue data used as attribution evidence. Results (including
 * unavailable data) are cached per issue, and the number of lookups is limited to bound the number of
 * API calls. Lookup failures result in the issue being attributed without the looked up data.
 */
@Slf4j
abstract class AbstractFoDScanCompareIssueLookup<T> {
    protected final UnirestInstance unirest;
    protected final String releaseId;
    private final int maxLookups;
    private final String dataDescription;
    private final Map<String, T> cache = new HashMap<>();
    @Getter private int lookupCount;
    @Getter private boolean limitReached;

    protected AbstractFoDScanCompareIssueLookup(UnirestInstance unirest, String releaseId, int maxLookups, String dataDescription) {
        this.unirest = unirest;
        this.releaseId = releaseId;
        this.maxLookups = maxLookups;
        this.dataDescription = dataDescription;
    }

    public final T lookup(FoDAttributionIssue issue) {
        var vulnId = issue.getText("vulnId");
        if ( StringUtils.isBlank(vulnId) ) { return null; }
        if ( cache.containsKey(vulnId) ) { return cache.get(vulnId); }
        if ( lookupCount >= maxLookups ) {
            limitReached = true;
            return null;
        }
        lookupCount++;
        var result = loadSafely(vulnId);
        cache.put(vulnId, result);
        return result;
    }

    /** @return Data for the given issue, or null if not available */
    protected abstract T load(String vulnId);

    private T loadSafely(String vulnId) {
        try {
            return load(vulnId);
        } catch ( UnexpectedHttpResponseException e ) {
            log.debug("No {} available for issue {} (HTTP status {})", dataDescription, vulnId, e.getStatus());
            return null;
        } catch ( UnirestException e ) {
            log.debug("Error loading {} for issue {}", dataDescription, vulnId, e);
            return null;
        }
    }
}
