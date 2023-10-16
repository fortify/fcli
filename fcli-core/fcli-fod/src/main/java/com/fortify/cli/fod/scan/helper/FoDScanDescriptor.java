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

package com.fortify.cli.fod.scan.helper;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public class FoDScanDescriptor extends JsonNodeHolder {
    private Integer scanId;
    private String scanType;
    private String applicationName;
    private String releaseName;
    private Integer applicationId;
    private Integer releaseId;
    private String microserviceName;
    private String status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyy-MM-dd'T'hh:mm:ss")
    private Date startedDateTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyy-MM-dd'T'hh:mm:ss")
    private Date completedDateTime;
}
