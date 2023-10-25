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
package com.fortify.cli.license.ncd_report.collector;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.fortify.cli.license.ncd_report.descriptor.INcdReportCommitDescriptor;
import com.fortify.cli.license.ncd_report.descriptor.INcdReportRepositoryDescriptor;
import com.fortify.cli.license.ncd_report.descriptor.NcdReportBranchCommitDescriptor;
import com.fortify.cli.license.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor;
import com.fortify.cli.license.ncd_report.generator.INcdReportBranchCommitGenerator;
import com.fortify.cli.license.ncd_report.writer.NcdReportResultsWriters;

import lombok.RequiredArgsConstructor;

/**
 * <p>This class implements the {@link INcdReportRepositoryBranchCommitCollector} 
 * interface, collecting {@link NcdReportBranchCommitDescriptor} instances for a 
 * single repository. Various data from these descriptors is stored in instance 
 * variables for later processing.</p>
 * 
 * <p>The {@link NcdReportResultsCollector} uses this class for collecting and 
 * processing commit data as follows:</p>
 * <ul>
 *  <li>Creating a new instance of this class for every individual repository being processed</li>
 *  <li>Passing this instance to the various {@link INcdReportBranchCommitGenerator} instances
 *      used to generate the commit data</li>
 *  <li>Calling the {@link #writeResults(NcdReportResultsWriters)} method once all commit data 
 *      for the current repository has been generated</li>
 *  <li>Invoke the various getters to retrieve collected data for further processing</li> 
 * </ul>
 * 
 * @author rsenden
 *
 */
@RequiredArgsConstructor
final class NcdReportRepositoryBranchCommitCollector implements INcdReportRepositoryBranchCommitCollector {
    private final NcdReportAuthorCollector authorCollector;
    private final INcdReportRepositoryDescriptor repositoryDescriptor;
    private final Map<NcdReportBranchCommitDescriptor, NcdReportProcessedAuthorDescriptor> branchCommitDescriptors = new LinkedHashMap<>();
    private final Map<INcdReportCommitDescriptor, NcdReportProcessedAuthorDescriptor> commitDescriptors = new LinkedHashMap<>();
    private final Set<NcdReportProcessedAuthorDescriptor> authorDescriptors = new LinkedHashSet<>();
    @Override
    public void reportBranchCommit(NcdReportBranchCommitDescriptor branchCommitDescriptor) {
        if ( branchCommitDescriptor.getRepositoryDescriptor()!=repositoryDescriptor ) {
            throw new IllegalStateException(String.format("Non-matching repository descriptor; please submit an fcli bug\n\trepositoryDescriptor: %s\n\tbranchCommitDescriptor.repositoryDescriptor: %s", repositoryDescriptor, branchCommitDescriptor.getRepositoryDescriptor()));
        }
        var authorDescriptor = authorCollector.reportAuthor(branchCommitDescriptor.getAuthorDescriptor());
        branchCommitDescriptors.put(branchCommitDescriptor, authorDescriptor);
        commitDescriptors.put(branchCommitDescriptor.getCommitDescriptor(), authorDescriptor);
        authorDescriptors.add(authorDescriptor);
    }
    
    void writeResults(NcdReportResultsWriters writers) {
        branchCommitDescriptors.forEach((commitDescriptor, authorDescriptor)->writers.commitsByBranchWriter().writeBranchCommit(commitDescriptor, authorDescriptor));
        commitDescriptors.forEach((commitDescriptor, authorDescriptor)->writers.commitsByRepositoryWriter().writeRepositoryCommit(repositoryDescriptor, commitDescriptor, authorDescriptor));
        authorDescriptors.forEach(authorDescriptor->writers.authorsByRepositoryWriter().writeRepositoryAuthor(repositoryDescriptor, authorDescriptor));
    }
    
    int getTotalCommitCount() {
        return branchCommitDescriptors.size();
    }
    
    boolean isEmpty() {
        return branchCommitDescriptors.isEmpty();
    }
}