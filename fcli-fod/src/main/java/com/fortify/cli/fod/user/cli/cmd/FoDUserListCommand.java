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
package com.fortify.cli.fod.user.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestBaseRequestSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.query.FoDFilterParamGenerator;
import com.fortify.cli.fod.rest.query.FoDFiltersParamValueGenerators;
import com.fortify.cli.fod.rest.query.IFoDFilterParamGeneratorSupplier;
import com.fortify.cli.fod.user.helper.FoDUserHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.List.CMD_NAME)
public class FoDUserListCommand extends AbstractFoDOutputCommand implements IUnirestBaseRequestSupplier, IRecordTransformer, IFoDFilterParamGeneratorSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.List outputHelper;

    @Getter private FoDFilterParamGenerator filterParamGenerator = new FoDFilterParamGenerator()
            .add("userId","userId", FoDFiltersParamValueGenerators::plain)
            .add("userName","userName", FoDFiltersParamValueGenerators::plain)
            .add("email", "email", FoDFiltersParamValueGenerators::plain)
            .add("roleName", "roleName", FoDFiltersParamValueGenerators::plain);

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get(FoDUrls.USERS);
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDUserHelper.renameFields(record);
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
