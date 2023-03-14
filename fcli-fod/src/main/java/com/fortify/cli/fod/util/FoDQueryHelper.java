package com.fortify.cli.fod.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;

public class FoDQueryHelper {
    // TODO This method is only used by FoDPagingHelper, so probable better to move it there 
    public static URI appendUri(String uri, String appendQuery) throws URISyntaxException {
        URI oldUri = new URI(uri);

        String newQuery = oldUri.getQuery();
        if (newQuery == null) {
            newQuery = appendQuery;
        } else {
            newQuery += "&" + appendQuery;
        }

        return new URI(oldUri.getScheme(), oldUri.getAuthority(),
                oldUri.getPath(), newQuery, oldUri.getFragment());
    }

    // This seems a fairly generic helper method, so maybe move to JsonHelper in fcli-common
    public static void stripNulls(JsonNode node) {
        Iterator<JsonNode> it = node.iterator();
        while (it.hasNext()) {
            JsonNode child = it.next();
            if (child.isNull())
                it.remove();
            else
                stripNulls(child);
        }
    }
}
