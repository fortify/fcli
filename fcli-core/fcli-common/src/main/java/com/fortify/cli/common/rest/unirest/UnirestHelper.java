/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.rest.unirest;

import java.io.File;
import java.nio.file.StandardCopyOption;

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;

/**
 * This class provides utility methods related to Unirest
 */
public class UnirestHelper {
    public static final File download(String fcliModule, String url, File dest) {
        GenericUnirestFactory.getUnirestInstance(fcliModule, u->ProxyHelper.configureProxy(u, fcliModule, url))
                .get(url).asFile(dest.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING).getBody();
        return dest;
    }
}
