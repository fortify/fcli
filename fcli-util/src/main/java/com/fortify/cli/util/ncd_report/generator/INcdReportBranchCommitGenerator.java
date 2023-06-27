/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.util.ncd_report.generator;

import com.fortify.cli.util.ncd_report.collector.INcdReportRepositoryBranchCommitCollector;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportRepositoryDescriptor;

/**
 * <p>Functional interface defining the process for generating and reporting branch, 
 * commit and author data for a given repository descriptor. Source-specific 
 * generators usually use a lambda expression to provide a branch commit generator
 * method.</p>
 * 
 * <p>Implementations should load all branches for the given repository and all 
 * applicable commits for each of these branches, and report the collected data 
 * to the given {@link INcdReportRepositoryBranchCommitCollector} instance.
 *  
 * @author rsenden
 */
@FunctionalInterface
public interface INcdReportBranchCommitGenerator<R extends INcdReportRepositoryDescriptor> {
    void generateBranchCommitData(R repoDescriptor, INcdReportRepositoryBranchCommitCollector commitCollector);
}
