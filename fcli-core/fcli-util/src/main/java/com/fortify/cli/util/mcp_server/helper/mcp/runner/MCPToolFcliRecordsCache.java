/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.util.mcp_server.helper.mcp.runner;

import com.fasterxml.jackson.databind.util.LRUMap;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * This class provides functionality for caching the results of fcli invocations.
 * 
 * @author Ruud Senden
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MCPToolFcliRecordsCache {
    public static final MCPToolFcliRecordsCache INSTANCE = new MCPToolFcliRecordsCache();
    private static final long TTL = 7*60*1000; // 7 minutes in milliseconds
    private final LRUMap<String, CacheEntry> cache = new LRUMap<>(0, 3);
    
    public final MCPToolResultRecords getOrCollect(String fullCmd, boolean refresh) {
        synchronized(cache) {
            var cacheEntry = cache.get(fullCmd);
            var result = ( cacheEntry==null || cacheEntry.isExpired() || refresh )
                    ? null
                    : cacheEntry.getFullResult();
            if ( result==null ) {
                result = MCPToolFcliRunnerHelper.collectRecords(fullCmd);
                // Don't cache failed results. For example, if failed due to no session being available,
                // users want to retry after logging in.
                if ( result.getExitCode()==0 ) {
                    cache.put(fullCmd, new CacheEntry(result));
                }
            }
            return result;
        }
    }
    
    @Data @RequiredArgsConstructor
    private static final class CacheEntry {
        private final MCPToolResultRecords fullResult;
        private final long created = System.currentTimeMillis();
        
        public final boolean isExpired() {
            return System.currentTimeMillis() > created + TTL; 
        }
    }
}
