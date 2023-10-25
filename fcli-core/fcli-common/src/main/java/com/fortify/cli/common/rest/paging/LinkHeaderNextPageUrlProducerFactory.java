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
package com.fortify.cli.common.rest.paging;

import java.util.Optional;
import java.util.regex.Pattern;

public class LinkHeaderNextPageUrlProducerFactory {
    private static final Pattern linkHeaderPattern = Pattern.compile("<([^>]*)>; *rel=\"([^\"]*)\"");
    
    public static final INextPageUrlProducer nextPageUrlProducer(String headerName, String relName) {
        return (req,resp) -> {
            String linkHeader = resp.getHeaders().getFirst(headerName);
            Optional<String> nextLink = linkHeaderPattern.matcher(linkHeader).results()
                .filter(r1->relName.equals(r1.group(2)))
                .findFirst()
                .map(r2->r2.group(1));
            return nextLink.orElse(null);
        };
    }
}
