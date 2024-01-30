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

package com.fortify.cli.fod._common.scan.helper.dast;

import com.formkiq.graalvm.annotations.Reflectable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;

@EqualsAndHashCode(callSuper = true)
@Reflectable @NoArgsConstructor @AllArgsConstructor
@Data @SuperBuilder
public class FoDScanDastAutomatedSetupPostmanRequest extends FoDScanDastAutomatedSetupBaseRequest {
  public ArrayList<Integer> collectionFileIds;
}
