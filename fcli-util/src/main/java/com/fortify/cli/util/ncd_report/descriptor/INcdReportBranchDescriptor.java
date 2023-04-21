package com.fortify.cli.util.ncd_report.descriptor;

import com.fortify.cli.common.json.JsonNodeHolder;

/**
 * <p>Describe a repository branch. Implementations must at least provide 
 * the properties defined by this interface, and may optionally define 
 * additional properties as needed by the source-specific results
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
public interface INcdReportBranchDescriptor {
    String getName();
}
