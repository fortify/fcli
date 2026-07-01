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
package com.fortify.cli.license.ncd_report.helper;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.license.ncd_report.writer.NcdReportContributorsCsvSchema;

public final class NcdReportContributorHelper {
    public static ObjectNode createExpressionInput(String name, String email) {
        var normalizedName = StringUtils.defaultIfBlank(name, "");
        var normalizedEmail = StringUtils.defaultIfBlank(email, "");
        var lcName = normalizedName.toLowerCase();
        var lcEmail = normalizedEmail.toLowerCase();
        var lcEmailDomain = StringUtils.substringAfter(lcEmail, "@");
        var lcEmailName = StringUtils.substringBefore(lcEmail, "@");
        var cleanName = lcName.replaceAll("[^a-z]", "");
        var cleanEmailName = lcEmailName.replaceAll("[^a-z0-9]", "");
        if ( !cleanEmailName.matches("[0-9]+") ) {
            cleanEmailName = cleanEmailName.replaceAll("^[0-9]+", "");
        }
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("name", normalizedName)
                .put("email", normalizedEmail)
                .put("lcName", lcName)
                .put("lcEmail", lcEmail)
                .put("lcEmailDomain", lcEmailDomain)
                .put("lcEmailName", lcEmailName)
                .put(NcdReportContributorsCsvSchema.CLEAN_NAME, cleanName)
                .put(NcdReportContributorsCsvSchema.CLEAN_EMAIL_NAME, cleanEmailName);
    }

    public static String computeAuthorId(ObjectNode expressionInput) {
        var name = expressionInput.path("name").asText("");
        var email = expressionInput.path("email").asText("");
        var input = name.toLowerCase() + ":" + email.toLowerCase();
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return String.format("%032x", new BigInteger(1, hash)).substring(0, 16);
        } catch ( NoSuchAlgorithmException e ) {
            throw new FcliBugException("SHA-256 not available", e);
        }
    }

    public static void normalizeContributorRow(Map<String, String> row) {
        var expressionInput = createExpressionInput(
                row.get(NcdReportContributorsCsvSchema.AUTHOR_NAME),
                row.get(NcdReportContributorsCsvSchema.AUTHOR_EMAIL));
        row.compute(NcdReportContributorsCsvSchema.CLEAN_NAME,
                (key, value) -> StringUtils.isBlank(value)
                        ? expressionInput.path(NcdReportContributorsCsvSchema.CLEAN_NAME).asText("")
                        : value);
        row.compute(NcdReportContributorsCsvSchema.CLEAN_EMAIL_NAME,
                (key, value) -> StringUtils.isBlank(value)
                        ? expressionInput.path(NcdReportContributorsCsvSchema.CLEAN_EMAIL_NAME).asText("")
                        : value);
        row.compute(NcdReportContributorsCsvSchema.AUTHOR_ID,
                (key, value) -> StringUtils.isBlank(value) ? computeAuthorId(expressionInput) : value);
    }

    private NcdReportContributorHelper() {}
}