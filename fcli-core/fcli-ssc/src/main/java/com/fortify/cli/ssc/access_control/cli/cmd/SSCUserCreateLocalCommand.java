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
package com.fortify.cli.ssc.access_control.cli.cmd;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.access_control.helper.SSCUserCreateRequest;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "create-local-user") @CommandGroup("user")
public class SSCUserCreateLocalCommand extends AbstractSSCJsonNodeOutputCommand  {
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper; 
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    @Option(names = {"--username"}, required = true)
    private String username;
    @Option(names = {"--password"}, required = true)
    private String password;
    @Option(names = {"--firstname"})
    private String firstName;
    @Option(names = {"--lastname"})
    private String lastName;
    @Option(names = {"--email"})
    private String email;
    @Option(names = {"--password-never-expires", "--pne"})
    private boolean pwNeverExpires;
    @Option(names = {"--require-password-change", "--rpc"})
    private boolean requirePwChange;
    @Option(names = {"--suspend"})
    private boolean suspend;
    @Option(names = {"--roles"}, required = false, split = ",", descriptionKey = "fcli.ssc.access-control.role.resolver.nameOrId")
    private ArrayList<String> roles = new ArrayList<String>();
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCUserCreateRequest userCreateRequest = SSCUserCreateRequest.builder()
                .userName(username)
                .clearPassword(password)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .passwordNeverExpires(pwNeverExpires)
                .requirePasswordChange(requirePwChange)
                .suspended(suspend)
                .build();
        userCreateRequest.addRoles(roles);
        ObjectNode body = objectMapper.valueToTree(userCreateRequest);
        
        return unirest.post(SSCUrls.LOCAL_USERS)
                .body(body).asObject(JsonNode.class).getBody();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    
}
