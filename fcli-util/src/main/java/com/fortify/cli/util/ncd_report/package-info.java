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
/**
 * <p>This package and its sub-packages provide functionality for generating NCD 
 * (Number of Contributing Developers) reports by collecting commit data from 
 * one or more SCM (Source Code Management) systems. This file provides a global
 * overview of the sub-packages and architecture.</p>
 * 
 * <p>Sub-packages:</p>
 * <ul>
 *  <li>cli: Picocli classes providing the 'fcli util ncd-report' commands.</li>
 *  <li>collector: Responsible for processing and collecting repository, branch, 
 *      commit and author data generated from the various sources, and outputting
 *      the collected data to the report.</li>
 *  <li>config: Java representation of the report configuration file, together with
 *      some other configuration-related functionality.</li>
 *  <li>descriptor: Interfaces describing author, branch, commit and repository
 *      data, to be implemented by the various generator implementations, together
 *      with some generic descriptor implementations that hold other descriptors
 *      and supporting data.</li>
 *  <li>generator: Responsible for generating repository, branch, commit and 
 *      author data from various sources. There are some common interfaces and
 *      abstract classes to be implemented by the various source implementation. 
 *      For each SCM system, there is a dedicated sub-package that contains both 
 *      the generator implementation and Jackson-based implementations for the 
 *      various descriptor interfaces (see above).</li>
 *  <li>writer: Responsible for writing the various report files.</li>
 * </ul>
 * 
 * <p>Architecture / Flow:</p>
 * <ol>
 *  <li>NcdReportGenerateCommand:
 *   <ol>
 *    <li>Deserializes the configuration file into the configuration classes defined 
 *        in the config package.</li>
 *    <li>Creates a new NcdReportResultsCollector instance for collecting and outputting
 *        data generated by the various sources.</li>
 *    <li>For each of the source configurations, requests the source-specific generator,
 *        passing in the NcdReportResultsCollector instance created above, and invoking
 *        that generator through its run() method.</li>
 *   </ol></li>
 *  <li>The run() method of every source-specific generator:
 *   <ol>
 *    <li>Loads the repository descriptors from the source, based on the source 
 *        configuration.</li>
 *    <li>Invokes resultsCollector().repositoryProcessor().processRepository(...)
 *        for every discovered repository, passing in a source-specific repository
 *        selector that defines which repositories should be included/excluded from
 *        the report, the repository descriptor, and a source-specific 
 *        INcdReportBranchCommitGenerator instance (which is usually a lambda expression
 *        that invokes a method in the same generator class).</li> 
 *   </ol></li>
 *   <li>The resultsCollector().repositoryProcessor().processRepository(...) method:
 *    <ol>
 *     <li>Decides whether the repository described by the given descriptor
 *         should be included in the report or not, based on the given repository
 *         selector.</li>
 *     <li>For every repository to be processed, calls the given source-specific
 *         INcdReportBranchCommitGenerator to generate commit and author data for
 *         every branch in that repository, passing in a new 
 *         NcdReportRepositoryBranchCommitCollector instance for collecting the
 *         generated data.</li>
 *     <li>Invokes NcdReportRepositoryCollector.reportRepository(...) for every
 *         repository to collect and output repository data.</li>
 *    </ol></li>
 *   <li>The source-specific INcdReportBranchCommitGenerator:
 *    <ol>
 *     <li>Loads the branch descriptors for the given repository.</li>
 *     <li>For every branch, loads all commits not older than 90 days.</li>
 *     <li>For every commit, calls the given NcdReportRepositoryBranchCommitCollector,
 *         passing in the repository, branch, commit and author descriptors.
 *     <li>If no recent commits are found in any of the branches, looks
 *         for the most recent commit in any of the branches that is older
 *         than 90 day, and passes the corresponding descriptors for this
 *         commit to the given NcdReportRepositoryBranchCommitCollector.</li>
 *    </ol></li>
 *   <li>The NcdReportRepositoryBranchCommitCollector:
 *    <ol>
 *     <li>Collects and outputs the branch, commit and author data for a
 *         single repository.</li>
 *     <li>Reports every author to a provided NcdReportAuthorCollector instance.</li>
 *    </ol></li>
 *   <li>The NcdReportAuthorCollector class is responsible for collecting,
 *       de-duplicating (using NcdReportAuthorDeduplicator) and outputting
 *       (contributing) author data.</li>
 * </ol>
 * 
 */
package com.fortify.cli.util.ncd_report;