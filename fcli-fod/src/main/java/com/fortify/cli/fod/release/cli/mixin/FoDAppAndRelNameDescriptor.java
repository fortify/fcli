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

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;

import javax.validation.ValidationException;

@Data @ReflectiveAccess
public final class FoDAppAndRelNameDescriptor {
    private final String appName, relName;
    
    public static final FoDAppAndRelNameDescriptor fromCombinedAppAndRelName(String appAndRelName, String delimiter) {
        String[] appAndRelNameArray = appAndRelName.split(delimiter);
        if (appAndRelNameArray.length != 2) {
            throw new ValidationException("Application and release name must be specified in the format <application name>"+delimiter+"<release name>");
        }
        return new FoDAppAndRelNameDescriptor(appAndRelNameArray[0], appAndRelNameArray[1]);
    }
}