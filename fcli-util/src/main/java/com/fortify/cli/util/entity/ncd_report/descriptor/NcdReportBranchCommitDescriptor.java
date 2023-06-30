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
package com.fortify.cli.util.entity.ncd_report.descriptor;

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