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

package com.fortify.cli.fod.user_group.cli.mixin;

import com.fortify.cli.fod.user_group.helper.FoDUserGroupDescriptor;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

//TODO Change description keys to be more like picocli convention
public class FoDUserGroupResolverMixin {
    @ReflectiveAccess
    public static abstract class AbstractFoDUserGroupResolverMixin {
        public abstract String getUserGroupNameOrId();

        public FoDUserGroupDescriptor getUserGroupDescriptor(UnirestInstance unirest, String... fields){
            return FoDUserGroupHelper.getUserGroupDescriptor(unirest, getUserGroupNameOrId(), true);
        }

        public String getGroupId(UnirestInstance unirest) {
            return getUserGroupDescriptor(unirest, "groupId").getId().toString();
        }
    }

    @ReflectiveAccess
    public static class RequiredOption extends AbstractFoDUserGroupResolverMixin {
        @Option(names = {"--group"}, required = true, descriptionKey = "GroupMixin")
        @Getter private String userGroupNameOrId;
    }

    @ReflectiveAccess
    public static class OptionalOption extends AbstractFoDUserGroupResolverMixin {
        @Option(names = {"--group"}, required = false, descriptionKey = "GroupMixin")
        @Getter private String userGroupNameOrId;
    }

    @ReflectiveAccess
    public static class PositionalParameter extends AbstractFoDUserGroupResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "GroupMixin")
        @Getter private String userGroupNameOrId;
    }
}