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
package com.fortify.cli.license.ncd_report.collector;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fortify.cli.common.exception.FcliBugException;
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
 * <p>The {@link NcdReportContext} uses this class for collecting and 
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
    private final Map<INcdReportCommitDescriptor, Boolean> commitDormantByCommitDescriptor = new LinkedHashMap<>();
    private final Set<NcdReportProcessedAuthorDescriptor> authorDescriptors = new LinkedHashSet<>();
    private boolean repositoryDormant = true;

    @Override
    public void reportBranchCommit(NcdReportBranchCommitDescriptor branchCommitDescriptor) {
        if ( branchCommitDescriptor.getRepositoryDescriptor()!=repositoryDescriptor ) {
            throw new FcliBugException(String.format("Non-matching repository descriptor; please submit an fcli bug\n\trepositoryDescriptor: %s\n\tbranchCommitDescriptor.repositoryDescriptor: %s", repositoryDescriptor, branchCommitDescriptor.getRepositoryDescriptor()));
        }
        var authorDescriptor = authorCollector.reportAuthor(branchCommitDescriptor.getAuthorDescriptor());
        branchCommitDescriptors.put(branchCommitDescriptor, authorDescriptor);
        commitDescriptors.put(branchCommitDescriptor.getCommitDescriptor(), authorDescriptor);
        commitDormantByCommitDescriptor.putIfAbsent(branchCommitDescriptor.getCommitDescriptor(), branchCommitDescriptor.isDormant());
        authorDescriptors.add(authorDescriptor);
        authorCollector.reportAuthorDormant(authorDescriptor, branchCommitDescriptor.isDormant());
        if ( !branchCommitDescriptor.isDormant() ) {
            repositoryDormant = false;
        }
    }
    
    void writeResults(NcdReportResultsWriters writers) {
        branchCommitDescriptors.forEach((commitDescriptor, authorDescriptor)->writers.commitsByBranchWriter().writeBranchCommit(commitDescriptor, authorDescriptor));
        commitDescriptors.forEach((commitDescriptor, authorDescriptor)->writers.commitsByRepositoryWriter()
                .writeRepositoryCommit(repositoryDescriptor, commitDescriptor, authorDescriptor, isCommitDormant(commitDescriptor)));

        var dormantByAuthor = branchCommitDescriptors.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getValue,
                        e -> e.getKey().isDormant(),
                        (current, incoming) -> current && incoming,
                        LinkedHashMap::new));
        authorDescriptors.forEach(authorDescriptor -> writers.authorsByRepositoryWriter()
                .writeRepositoryAuthor(repositoryDescriptor, authorDescriptor, dormantByAuthor.getOrDefault(authorDescriptor, false)));
    }
    
    int getTotalCommitCount() {
        return branchCommitDescriptors.size();
    }

    int getTotalContributorCount() {
        return authorDescriptors.size();
    }
    
    boolean isEmpty() {
        return branchCommitDescriptors.isEmpty();
    }

    boolean isDormant() {
        return repositoryDormant;
    }

    private boolean isCommitDormant(INcdReportCommitDescriptor commitDescriptor) {
        return commitDormantByCommitDescriptor.getOrDefault(commitDescriptor, false);
    }
}