/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/

package com.fortify.cli.fod.release.helper;

import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ReflectiveAccess
@Data
@EqualsAndHashCode(callSuper = true)
public class FoDAppRelDescriptor extends JsonNodeHolder {
    private Integer releaseId;
    private String releaseName;
    private String releaseDescription;
    private Boolean suspended;
    private String microserviceName;
    private Integer microserviceId;
    private Integer applicationId;
    private String applicationName;
    private Integer rating;
    private Integer critical;
    private Integer high;
    private Integer medium;
    private Integer low;
    private Integer issueCount;
    private Boolean isPassed;
    private String passFailReasonType;
    private String sdlcStatusType;
    private Integer ownerId;
    private Integer currentStaticScanId;
    private Integer currentDynamicScanId;
    private Integer currentMobileScanId;
    private String staticAnalysisStatusType;
    private String dynamicAnalysisStatusType;
    private String mobileAnalysisStatusType;
}
