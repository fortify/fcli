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

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

//TODO Consider using @Builder instead of manual setters
@ReflectiveAccess
@Getter
@ToString
public class FoDAppRelUpdateRequest {
    private String releaseName;
    private String releaseDescription;
    private String sdlcStatusType;
    private Integer ownerId;
    private Integer microserviceId;

    public FoDAppRelUpdateRequest setReleaseName(String name) {
        this.releaseName = name;
        return this;
    }

    public FoDAppRelUpdateRequest setReleaseDescription(String description) {
        this.releaseDescription = (description == null ? "" : description);
        return this;
    }

    public FoDAppRelUpdateRequest setSdlcStatusType(String statusType) {
        this.sdlcStatusType = statusType;
        return this;
    }

    public FoDAppRelUpdateRequest setOwnerId(Integer id) {
        this.ownerId = id;
        return this;
    }

    public FoDAppRelUpdateRequest setMicroserviceId(Integer id) {
        this.microserviceId = id;
        return this;
    }

}
