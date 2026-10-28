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
package com.fortify.cli.common.http.ssl.trust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class CertificateDistrustPolicyTest {
    @Test
    void bundledPolicyContainsAllReviewedIncidentKeys() throws Exception {
        String text;
        try (var input = getClass().getClassLoader().getResourceAsStream(CertificateDistrustPolicy.RESOURCE)) {
            assertNotNull(input);
            text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        var expected = Set.of(
                "ebed6475c2c2d80fcb199f3fe8dd6e6b3ce60b77eb15d4e42c3dec0c6f5b5105",
                "43bffa19d6ef651451f6841e019931e96f6f2eaeff28ab4b6f1ce4576de2717b",
                "e0254c7fcdc265e2f0f8a428dabb75ab4ae039b4872d62f5ee57f0877d5a8a41",
                "49dd353dc2b919a3770645117d0e60c16115e7d1bc259eefe734b00ea434fe5f",
                "63ecefee431d49523bc1136c4e72d0206a477086caf606cd5cf025686c109cf8",
                "5c0660a2b164910d8ff589c18e14552e9e0ec237dcefe2cb736d41a4811f81a0",
                "ec4ffa1ad0ac88081314b7aaffef6a91f97cd57291bb5ba686c5415f5eb4f6b6",
                "92c063d67e8f14db71d288e11266ffdaf69cbf115f7bab14f3bc2f68d25e0e30",
                "c619e4780ed8e0abeda24ad732567de4383a7ea941338f6d1a7d1b49ac792fe9",
                "0a6c2b2ca5b364207954171987ac9d2178561569d8205457e77ca90c32ede282",
                "8d417db2dd8bf5e3084d1e3f196d583849d81bdd4c00c70b9d39369e96b8c782",
                "bd153ed7b0434f6886b17bce8bbe84ed340c7132d702a8f4fa318f756ecbd6f3",
                "376a1a7082a593dccc20d561d119e9ab8d30f11cc321d0a37fa41f0df284e01c",
                "71f8adeec2b58adbdf6205c98dde928a4254b03695193d7f9a63a9a2588095d0");
        var actual = text.lines().filter(line -> !line.isBlank() && !line.startsWith("#")).map(line -> {
            try {
                var fields = line.split("\\|");
                var key = KeyFactory.getInstance(fields[0])
                        .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(fields[1])));
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getEncoded()));
            } catch (GeneralSecurityException e) {
                throw new AssertionError(e);
            }
        }).collect(Collectors.toSet());
        assertEquals(expected, actual);
        assertNotNull(CertificateDistrustPolicy.load(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void rejectsMissingEmptyAndMalformedPolicies() {
        assertThrows(CertificateException.class, () -> CertificateDistrustPolicy.load(null));
        for (var policy : new String[] {"", "# only a comment", "bad entry", "EC|invalid|CN=test|reason"}) {
            assertThrows(GeneralSecurityException.class, () -> load(policy));
        }
    }

    static CertificateDistrustPolicy load(String text) throws Exception {
        return CertificateDistrustPolicy.load(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }
}
