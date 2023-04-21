package com.fortify.cli.util.ncd_report.descriptor;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * This class holds {@link INcdReportRepositoryDescriptor},
 * {@link INcdReportBranchDescriptor}, {@link INcdReportCommitDescriptor}
 * and {@link INcdReportAuthorDescriptor} instances to
 * full describe a commit on a particular repository branch.
 *  
 * @author rsenden
 */
@RequiredArgsConstructor @Data
public final class NcdReportBranchCommitDescriptor {
    private final INcdReportRepositoryDescriptor repositoryDescriptor;
    private final INcdReportBranchDescriptor branchDescriptor;
    private final INcdReportCommitDescriptor commitDescriptor;
    private final INcdReportAuthorDescriptor authorDescriptor;
}