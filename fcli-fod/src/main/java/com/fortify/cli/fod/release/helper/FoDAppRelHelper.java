/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/

package com.fortify.cli.fod.release.helper;

import java.util.List;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod.release.cli.mixin.FoDAppAndRelNameDescriptor;
import com.fortify.cli.fod.release.cli.mixin.FoDAppMicroserviceAndRelNameDescriptor;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanFormatOptions;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDAppRelHelper {
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDAppRelDescriptor getRequiredAppRel(UnirestInstance unirest, String appRelNameOrId, String delimiter, String... fields) {
        FoDAppRelDescriptor descriptor = getOptionalAppRel(unirest, appRelNameOrId, delimiter, fields);
        if (descriptor == null) {
            throw new ValidationException("No application release found for application release name or id: " + appRelNameOrId);
        }
        return descriptor;
    }

    public static final FoDAppRelDescriptor getRequiredAppMicroserviceRel(UnirestInstance unirest, String appMicroserviceRelNameOrId, String delimiter, String... fields) {
        FoDAppRelDescriptor descriptor = getOptionalAppMicroserviceRel(unirest, appMicroserviceRelNameOrId, delimiter, fields);
        if (descriptor == null) {
            throw new ValidationException("No application microservice release found for application microservice release name or id: " + appMicroserviceRelNameOrId);
        }
        return descriptor;
    }

    public static final FoDAppRelDescriptor getOptionalAppRel(UnirestInstance unirest, String appRelNameOrId, String delimiter, String... fields) {
        try {
            int relId = Integer.parseInt(appRelNameOrId);
            return getOptionalAppRelFromId(unirest, relId, fields);
        } catch (NumberFormatException nfe) {
            return getOptionalAppRelFromAppAndRelName(unirest, FoDAppAndRelNameDescriptor.fromCombinedAppAndRelName(appRelNameOrId, delimiter), fields);
        }
    }

    public static final FoDAppRelDescriptor getOptionalAppMicroserviceRel(UnirestInstance unirest, String appMicroserviceRelNameOrId, String delimiter, String... fields) {
        try {
            int relId = Integer.parseInt(appMicroserviceRelNameOrId);
            return getOptionalAppRelFromId(unirest, relId, fields);
        } catch (NumberFormatException nfe) {
            return getOptionalAppMicroserviceRelFromAppAndRelName(unirest, FoDAppMicroserviceAndRelNameDescriptor.fromCombinedAppMicroserviceAndRelName(appMicroserviceRelNameOrId, delimiter), fields);
        }
    }

    public static final FoDAppRelDescriptor getOptionalAppRelFromId(UnirestInstance unirest, int relId, String... fields) {
        GetRequest request = unirest.get(FoDUrls.RELEASES)
                .queryString("filters", String.format("releaseId:%d", relId));
        return getOptionalDescriptor(request);
    }

    public static final FoDAppRelDescriptor getOptionalAppRelFromAppAndRelName(UnirestInstance unirest, FoDAppAndRelNameDescriptor appAndRelNameDescriptor, String... fields) {
        GetRequest request = unirest.get(FoDUrls.RELEASES)
                .queryString("filters", String.format("applicationName:%s+releaseName:%s", appAndRelNameDescriptor.getAppName(), appAndRelNameDescriptor.getRelName()));
        return getOptionalDescriptor(request);
    }

    public static final FoDAppRelDescriptor getOptionalAppMicroserviceRelFromAppAndRelName(UnirestInstance unirest, FoDAppMicroserviceAndRelNameDescriptor appMicroserviceAndRelNameDescriptor, String... fields) {
        GetRequest request = unirest.get(FoDUrls.RELEASES)
                .queryString("filters", String.format("applicationName:%s+microserviceName:%s+releaseName:%s", appMicroserviceAndRelNameDescriptor.getAppName(), appMicroserviceAndRelNameDescriptor.getMicroserviceName(), appMicroserviceAndRelNameDescriptor.getRelName()));
        return getOptionalDescriptor(request);
    }

    public static final FoDAppRelDescriptor getAppRelDescriptor(UnirestInstance unirest, String appAndRelNameOrId, String delimiter, boolean failIfNotFound) {
        String[] appAndRelName = appAndRelNameOrId.split(delimiter);
        GetRequest request = unirest.get(FoDUrls.RELEASES);
        boolean isAppId = false; int appId = 0;
        boolean isRelId = false; int relId = 0;
        if (appAndRelName.length == 1) {
            // only release Id specified
            try {
                relId = Integer.parseInt(appAndRelName[0]); isRelId = true;
                request = request.queryString("filters", String.format("releaseId:%d", relId));
            } catch (NumberFormatException nfe) {
                throw new ValidationException("If application name is not specified then the release must be an id: " + relId);
            }
        } else {
            // application and release name/id specified
            try {
                appId = Integer.parseInt(appAndRelName[0]); isAppId = true;
            } catch (NumberFormatException nfe) {
                isAppId = false;
            }
            try {
                relId = Integer.parseInt(appAndRelName[1]); isRelId = true;
            } catch (NumberFormatException nfe) {
                isRelId = false;
            }
            if (isAppId) {
                if (isRelId) {
                    request = request.queryString("filters", String.format("applicationId:%d+releaseId:%d", appId, relId));
                } else {
                    request = request.queryString("filters", String.format("applicationId:%d+releaseName:%s", appId, appAndRelName[1]));
                }
            } else {
                if (isRelId) {
                    request = request.queryString("filters", String.format("applicationName:%s+releaseId:%d", appAndRelName[0], relId));
                } else {
                    request = request.queryString("filters", String.format("applicationName:%s+releaseName:%s", appAndRelName[0], appAndRelName[1]));
                }
            }
        }
        JsonNode rel = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && rel.size() == 0) {
            throw new ValidationException("No application release found for names or ids: " + appAndRelNameOrId);
        } else if (rel.size() > 1) {
            throw new ValidationException("Multiple application releases found for names or ids: " + appAndRelNameOrId);
        }
        return rel.size() == 0 ? null : getDescriptor(rel.get(0));
    }

    public static final FoDAppRelDescriptor getAppRelDescriptorById(UnirestInstance unirest, String relId, boolean failIfNotFound) {
        GetRequest request = unirest.get(FoDUrls.RELEASE).routeParam("relId", relId);
        JsonNode rel = request.asObject(ObjectNode.class).getBody();
        if (failIfNotFound && rel.get("releaseName").asText().isEmpty()) {
            throw new ValidationException("No application release found for id: " + relId);
        }
        return getDescriptor(rel);
    }

    public static final FoDAppRelAssessmentTypeDescriptor[] getAppRelAssessmentTypes(UnirestInstance unirestInstance,
                                                                                     String relId, FoDScanFormatOptions.FoDScanType scanType, boolean failIfNotFound) {
        GetRequest request = unirestInstance.get(FoDUrls.RELEASE + "/assessment-types")
                .routeParam("relId", relId)
                .queryString("scanType", scanType.name());
        JsonNode assessmentTypes = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && assessmentTypes.size() == 0) {
            throw new ValidationException("No assessment types found for release id: " + relId);
        }
        return JsonHelper.treeToValue(assessmentTypes, FoDAppRelAssessmentTypeDescriptor[].class);
    }

    public static final FoDAppRelAssessmentTypeDescriptor getAppRelAssessmentType(UnirestInstance unirestInstance,
                                                                                  String relId, FoDScanFormatOptions.FoDScanType scanType,
                                                                                  boolean isPlus, boolean failIfNotFound) {
        String filterString = "name:" + scanType.toString() +
                (isPlus ? "+" : "") + " Assessment";
        System.out.println(filterString);
        GetRequest request = unirestInstance.get(FoDUrls.RELEASE + "/assessment-types")
                .routeParam("relId", relId)
                .queryString("scanType", scanType.name())
                .queryString("filters", filterString);
        JsonNode assessmentTypes = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && assessmentTypes.size() == 0) {
            throw new ValidationException("No assessment types found for release id: " + relId);
        }
        return JsonHelper.treeToValue(assessmentTypes, FoDAppRelAssessmentTypeDescriptor.class);
    }

    public static final FoDAppRelDescriptor createAppRel(UnirestInstance unirest, FoDAppRelCreateRequest relCreateRequest) {
        ObjectNode body = objectMapper.valueToTree(relCreateRequest);
        JsonNode response = unirest.post(FoDUrls.RELEASES)
                .body(body).asObject(JsonNode.class).getBody();
        FoDAppRelDescriptor descriptor = JsonHelper.treeToValue(response, FoDAppRelDescriptor.class);
        return getAppRelDescriptorById(unirest, String.valueOf(descriptor.getReleaseId()), true);
    }

    public static final FoDAppRelDescriptor updateAppRel(UnirestInstance unirest, Integer relId,
                                                   FoDAppRelUpdateRequest appUpdateRequest) {
        ObjectNode body = objectMapper.valueToTree(appUpdateRequest);
        unirest.put(FoDUrls.RELEASE)
                .routeParam("relId", String.valueOf(relId))
                .body(body).asObject(JsonNode.class).getBody();
        return getAppRelDescriptorById(unirest, String.valueOf(relId), true);
    }

    public static boolean missing(List<?> list) {
        return list == null || list.isEmpty();
    }

    //

    private static final FoDAppRelDescriptor getOptionalDescriptor(GetRequest request) {
        JsonNode releases = request.asObject(ObjectNode.class).getBody().get("items");
        if (releases.size() > 1) {
            throw new ValidationException("Multiple application releases found");
        }
        return releases.size() == 0 ? null : getDescriptor(releases.get(0));
    }

    private static final FoDAppRelDescriptor getDescriptor(JsonNode node) {
        return  JsonHelper.treeToValue(node, FoDAppRelDescriptor.class);
    }


}
