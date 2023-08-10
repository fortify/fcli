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

package com.fortify.cli.fod.release.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.rest.FoDUrls;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDReleaseHelper {
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDReleaseDescriptor getReleaseDescriptor(UnirestInstance unirest, String qualifiedReleaseNameOrId, String delimiter, boolean failIfNotFound, String... fields) {
        try {
            int relId = Integer.parseInt(qualifiedReleaseNameOrId);
            return getReleaseDescriptorFromId(unirest, relId, failIfNotFound, fields);
        } catch (NumberFormatException nfe) {
            return getReleaseDescriptorFromQualifiedName(unirest, FoDQualifiedReleaseNameDescriptor.fromQualifiedReleaseName(qualifiedReleaseNameOrId, delimiter), failIfNotFound, fields);
        }
    }

    public static final FoDReleaseDescriptor getReleaseDescriptorFromId(UnirestInstance unirest, int relId, boolean failIfNotFound, String... fields) {
        GetRequest request = addFieldsParam(unirest.get(FoDUrls.RELEASES)
                .queryString("filters", String.format("releaseId:%d", relId)), fields);
        return getDescriptor(request, String.valueOf(relId), failIfNotFound);
    }

    public static final FoDReleaseDescriptor getReleaseDescriptorFromQualifiedName(UnirestInstance unirest, FoDQualifiedReleaseNameDescriptor releaseNameDescriptor, boolean failIfNotFound, String... fields) {
        var filters = String.format("applicationName:%s+releaseName:%s", releaseNameDescriptor.getAppName(), releaseNameDescriptor.getReleaseName());
        if ( StringUtils.isNotBlank(releaseNameDescriptor.getMicroserviceName()) ) {
            filters += String.format("+microserviceName:%s", releaseNameDescriptor.getMicroserviceName());
        }
        GetRequest request = addFieldsParam(unirest.get(FoDUrls.RELEASES)
                .queryString("filters", filters), fields);
        return getDescriptor(request, releaseNameDescriptor.getQualifiedName(), failIfNotFound);
    }

    public static final FoDReleaseDescriptor createRelease(UnirestInstance unirest, FoDReleaseCreateRequest relCreateRequest) {
        ObjectNode body = objectMapper.valueToTree(relCreateRequest);
        var releaseId = unirest.post(FoDUrls.RELEASES)
                .body(body).asObject(JsonNode.class).getBody().get("releaseId").asInt();
        return getReleaseDescriptorFromId(unirest, releaseId, true);
    }

    public static final FoDReleaseDescriptor updateRelease(UnirestInstance unirest, String relId,
                                                   FoDReleaseUpdateRequest appUpdateRequest) {
        ObjectNode body = objectMapper.valueToTree(appUpdateRequest);
        // TODO Check whether put request doesn't already return release data
        unirest.put(FoDUrls.RELEASE)
                .routeParam("relId", relId)
                .body(body).asObject(JsonNode.class).getBody();
        return getReleaseDescriptorFromId(unirest, Integer.parseInt(relId), true);
    }
    
    private static final GetRequest addFieldsParam(GetRequest req, String... fields) {
        if ( fields!=null && fields.length>0 ) {
            req = req.queryString("fields", String.join(",", fields));
        }
        return req;
    }

    private static final FoDReleaseDescriptor getDescriptor(GetRequest request, String releaseNameOrId, boolean failIfNotFound) {
        JsonNode releases = request.asObject(ObjectNode.class).getBody().get("items");
        switch ( releases.size() ) {
        case 0:
            return nullOrNotFoundException(releaseNameOrId, failIfNotFound);
        case 1:
            return JsonHelper.treeToValue(releases.get(0), FoDReleaseDescriptor.class);
        default:
            // FoD usually doesn't allow duplicate release names, but we handle this
            // by throwing an exception, just in case.
            throw multipleFoundException(releaseNameOrId);
        }
    }

    private static final FoDReleaseDescriptor nullOrNotFoundException(String releaseNameOrId, boolean failIfNotFound) {
        if ( failIfNotFound ) {
            throw new IllegalArgumentException(String.format("Cannot find release %s", releaseNameOrId));
        }
        return null;
    }
    
    private static final RuntimeException multipleFoundException(String releaseNameOrId) {
        return new IllegalStateException(String.format("Multiple releases found with name or id %s", releaseNameOrId));
    }
}
