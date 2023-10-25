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

import java.time.LocalDateTime;

import com.fortify.cli.common.json.JsonNodeHolder;

/**
 * <p>Describe a commit. Implementations must at least provide the properties 
 * defined by this interface, and may optionally define additional properties 
 * as needed by the source-specific results generator.</p>
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
public interface INcdReportCommitDescriptor {
    String getId();
    LocalDateTime getDate();
    String getMessage();
}
