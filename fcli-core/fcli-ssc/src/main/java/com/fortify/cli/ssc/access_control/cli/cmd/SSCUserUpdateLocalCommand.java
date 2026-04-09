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
package com.fortify.cli.ssc.access_control.cli.cmd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.access_control.cli.mixin.SSCUserResolverMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "update-local-user") @CommandGroup("user")
public class SSCUserUpdateLocalCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;

    @Mixin private SSCUserResolverMixin.PositionalParameterSingle userResolver;

    @Option(names = {"--firstname"})
    private String firstName;
    @Option(names = {"--lastname"})
    private String lastName;
    @Option(names = {"--email"})
    private String email;
    @Option(names = {"--password"})
    private String password;
    @Option(names = {"--password-never-expires", "--pne"})
    private Boolean pwNeverExpires;
    @Option(names = {"--require-password-change", "--rpc"})
    private Boolean requirePwChange;
    @Option(names = {"--suspend"})
    private Boolean suspend;
    @Option(names = {"--roles"}, split = ",")
    private ArrayList<String> roles;
    @Option(names = {"--add-roles"}, split = ",")
    private ArrayList<String> addRoles;
    @Option(names = {"--rm-roles"}, split = ",")
    private ArrayList<String> rmRoles;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        String userId = resolveUserId(unirest);
        ObjectNode userData = getLocalUser(unirest, userId);
        setUserAttributes(userData);
        setUserRoles(userData);
        return unirest.put(SSCUrls.LOCAL_USER(userId))
                .body(userData)
                .asObject(JsonNode.class).getBody();
    }

    private String resolveUserId(UnirestInstance unirest) {
        ArrayNode authEntities = (ArrayNode) userResolver.getAuthEntityJsonNode(unirest);
        if (authEntities.size() != 1) {
            throw new FcliSimpleException(
                "User specification matched %d users; please provide a unique user id", authEntities.size());
        }
        return authEntities.get(0).get("id").asText();
    }

    private ObjectNode getLocalUser(UnirestInstance unirest, String userId) {
        return (ObjectNode) unirest.get(SSCUrls.LOCAL_USER(userId))
                .asObject(JsonNode.class).getBody().get("data");
    }

    private void setUserAttributes(ObjectNode userData) {
        if (firstName != null) { userData.put("firstName", firstName); }
        if (lastName != null) { userData.put("lastName", lastName); }
        if (email != null) { userData.put("email", email); }
        if (password != null) { userData.put("clearPassword", password); }
        if (pwNeverExpires != null) { userData.put("passwordNeverExpires", pwNeverExpires); }
        if (requirePwChange != null) { userData.put("requirePasswordChange", requirePwChange); }
        if (suspend != null) { userData.put("suspended", suspend); }
    }

    private void setUserRoles(ObjectNode userData) {
        if (roles != null) {
            ArrayNode rolesArray = userData.putArray("roles");
            for (String roleId : roles) {
                rolesArray.addObject().put("id", roleId);
            }
        }
        if (addRoles != null) {
            addRolesToUser(userData);
        }
        if (rmRoles != null) {
            removeRolesFromUser(userData);
        }
    }

    private void addRolesToUser(ObjectNode userData) {
        ArrayNode existingRoles = getOrCreateRolesArray(userData);
        Set<String> existingRoleIds = collectRoleIds(existingRoles);
        for (String roleId : addRoles) {
            if (!existingRoleIds.contains(roleId)) {
                existingRoles.addObject().put("id", roleId);
            }
        }
    }

    private void removeRolesFromUser(ObjectNode userData) {
        ArrayNode existingRoles = (ArrayNode) userData.get("roles");
        if (existingRoles == null) { return; }
        Set<String> idsToRemove = new HashSet<>(rmRoles);
        ArrayNode filteredRoles = userData.putArray("roles");
        for (JsonNode role : existingRoles) {
            if (!idsToRemove.contains(role.get("id").asText())) {
                filteredRoles.add(role);
            }
        }
    }

    private ArrayNode getOrCreateRolesArray(ObjectNode userData) {
        ArrayNode existing = (ArrayNode) userData.get("roles");
        return existing != null ? existing : userData.putArray("roles");
    }

    private Set<String> collectRoleIds(ArrayNode rolesArray) {
        Set<String> ids = new HashSet<>();
        for (JsonNode role : rolesArray) {
            ids.add(role.get("id").asText());
        }
        return ids;
    }

    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
