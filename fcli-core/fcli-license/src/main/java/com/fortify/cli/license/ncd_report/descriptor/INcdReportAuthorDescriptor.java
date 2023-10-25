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
package com.fortify.cli.license.ncd_report.descriptor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.util.StringUtils;

/**
 * <p>Describe a commit author. Implementations must at least
 * provide the properties defined by this interface, and may optionally 
 * define additional properties as needed by the source-specific results
 * generator.</p>
 * 
 * <p>Note: For correct report output, the equals() and hashCode() methods 
 * should only take explicitly defined fields into account, not the full 
 * original JSON object (if applicable). In particular, if an implementation 
 * extends from {@link JsonNodeHolder}, then it should not call equals()
 * and hashCode() from this superclass. When using Lombok annotations, 
 * this means that you should use &#064;EqualsAndHashCode(callSuper = false).</p>
 *   
 * @author rsenden
 */
public interface INcdReportAuthorDescriptor {
    String getName();
    String getEmail();
    
    public default ObjectNode toExpressionInput() {
        var name = StringUtils.ifBlank(getName(), "");
        var email = StringUtils.ifBlank(getEmail(), "");
        var lcName = name.toLowerCase();
        var lcEmail = email.toLowerCase();
        var lcEmailDomain = StringUtils.substringAfter(lcEmail, "@");
        var lcEmailName = StringUtils.substringBefore(lcEmail, "@");
        var cleanName = lcName.replaceAll("[^a-z]", "");
        // Remove all special characters, then remove leading digits unless remaining string contains only digits
        // TODO Any better way of doing this instead of having to iterate through the string 3 times?
        var cleanEmailName = lcEmailName.replaceAll("[^a-z0-9]", "");
        if ( !cleanEmailName.matches("[0-9]+") ) {
            cleanEmailName = cleanEmailName.replaceAll("^[0-9]+", "");
        }
        return JsonHelper.getObjectMapper().createObjectNode()
            .put("name", name)
            .put("email", email)
            .put("lcName", lcName)
            .put("lcEmail", lcEmail)
            .put("lcEmailDomain", lcEmailDomain)
            .put("lcEmailName", lcEmailName)
            .put("cleanName", cleanName)
            .put("cleanEmailName", cleanEmailName);
    }
}
