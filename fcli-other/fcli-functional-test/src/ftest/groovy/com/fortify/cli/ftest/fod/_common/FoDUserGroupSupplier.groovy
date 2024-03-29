/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.ftest.fod._common

import com.fortify.cli.ftest._common.AbstractCloseableEntitySupplier
import com.fortify.cli.ftest._common.Fcli

public class FoDUserGroupSupplier extends AbstractCloseableEntitySupplier<FoDUserGroup> {
    @Override
    protected FoDUserGroup createInstance() {
        new FoDUserGroup().create()
    }

    public static final class FoDUserGroup implements Closeable {
        private final String random = System.currentTimeMillis()
        final String variableName = "fod_usergroup_"+random
        final String variableRef = "::"+variableName+"::"
        final String groupName = "fcli-"+random

        public FoDUserGroup create() {
            Fcli.run("fod ac create-group $groupName "+
                "--store $variableName",
                {it.expectSuccess(true, "Unable to create user-group")})
            return this
        }

        public String get(String propertyPath) {
            Fcli.run("util var contents $variableName -o expr={$propertyPath}",
                {it.expectSuccess(true, "Error getting user group property "+propertyPath)})
                .stdout[0]
        }

        public void close() {
            Fcli.run("fod ac rm-group $groupName",
                {it.expectSuccess(true, "Unable to delete user-group")})
        }
    }
}
