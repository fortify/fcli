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

package com.fortify.cli.fod.release.cli.mixin;

import javax.validation.ValidationException;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;

@Data @ReflectiveAccess
public final class FoDAppMicroserviceAndRelNameDescriptor {
    private final String appName, microserviceName, relName;
    
    public static final FoDAppMicroserviceAndRelNameDescriptor fromCombinedAppMicroserviceAndRelName(String appMicroserviceAndRelName, String delimiter) {
        String[] appMicroserviceAndRelNameArray = appMicroserviceAndRelName.split(delimiter);
        if (appMicroserviceAndRelNameArray.length < 2) {
            throw new ValidationException("Application microservice and release name must be specified in the format <application name>"+delimiter+"<microservice name>"+delimiter+"<release name>");
        }
        if (appMicroserviceAndRelNameArray.length == 3) {
            return new FoDAppMicroserviceAndRelNameDescriptor(appMicroserviceAndRelNameArray[0], appMicroserviceAndRelNameArray[1], appMicroserviceAndRelNameArray[2]);
        } else {
            return new FoDAppMicroserviceAndRelNameDescriptor(appMicroserviceAndRelNameArray[0], "", appMicroserviceAndRelNameArray[1]);

        }
    }
}