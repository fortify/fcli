/*******************************************************************************
 * Copyright 2021, 2025 Open Text.
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

 package com.fortify.cli.fod.issue.helper;

 import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.fod.access_control.helper.FoDUserHelper;

import kong.unirest.UnirestInstance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
 
 @Reflectable @NoArgsConstructor @AllArgsConstructor
 @Getter
 @ToString
 @Builder
 @JsonInclude(JsonInclude.Include.NON_NULL)
 public class FoDBulkIssueUpdateRequest {
     private Integer userId;
     private String developerStatus;
     private String auditorStatus;
     private String severity;
     private String comment;
     private ArrayList<String> vulnerabilityIds;
 
     @JsonIgnore
     public final FoDBulkIssueUpdateRequest validate(Consumer<List<String>> validationMessageConsumer) {
         var messages = new ArrayList<String>();
         validateRequired(messages, vulnerabilityIds, "Vulnerability Ids not specified");
         if ( !messages.isEmpty() ) {
             validationMessageConsumer.accept(messages);
         }
         return this;
     }
 
     @JsonIgnore
     public final FoDBulkIssueUpdateRequest validate() {
         return validate(messages->{throw new FcliSimpleException("Unable to update issues:\n\t"+String.join("\n\t", messages)); });
     }
     
     @JsonIgnore
     private final void validateRequired(List<String> messages, Object obj, String message) {
         if ( obj==null || (obj instanceof String && StringUtils.isBlank((String)obj)) ) {
             messages.add(message);
         }
     }
 
     public static class FoDBulkIssueUpdateRequestBuilder {
         public FoDBulkIssueUpdateRequestBuilder user(UnirestInstance unirest, String user) {
             int userId = 0;
             if (user == null) return userId(null);
             try {
                 userId = Integer.parseInt(user);
             } catch (NumberFormatException nfe) {
                 userId = FoDUserHelper.getUserDescriptor(unirest, user, true).getUserId();
             }
             return userId(userId);
         }
     }
 }