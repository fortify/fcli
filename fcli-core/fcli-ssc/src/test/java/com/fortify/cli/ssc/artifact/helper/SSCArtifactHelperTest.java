/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.ssc.artifact.helper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;

class SSCArtifactHelperTest {
    @Test
    void testIsAviatorArtifactRequiresAviatorPrefix() {
        assertTrue(SSCArtifactHelper.isAviatorArtifact(artifact("1", "aviator_app_version.fpr")));
        assertFalse(SSCArtifactHelper.isAviatorArtifact(artifact("2", "regular.fpr")));
    }

    @Test
    void testRequireAviatorArtifactReturnsAviatorArtifact() {
        SSCArtifactDescriptor artifact = artifact("1", "aviator_app_version.fpr");

        assertSame(artifact, SSCArtifactHelper.requireAviatorArtifact(artifact));
    }

    @Test
    void testRequireAviatorArtifactRejectsNonAviatorArtifact() {
        assertThrows(FcliSimpleException.class,
                () -> SSCArtifactHelper.requireAviatorArtifact(artifact("2", "regular.fpr")));
    }

    private static SSCArtifactDescriptor artifact(String id, String originalFileName) {
        SSCArtifactDescriptor descriptor = new SSCArtifactDescriptor();
        descriptor.setId(id);
        descriptor.setJsonNode(JsonHelper.getObjectMapper().createObjectNode()
                .put("id", id)
                .put("originalFileName", originalFileName));
        return descriptor;
    }
}