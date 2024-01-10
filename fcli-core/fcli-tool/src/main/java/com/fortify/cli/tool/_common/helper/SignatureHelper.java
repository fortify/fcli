package com.fortify.cli.tool._common.helper;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SignatureHelper {
    private static final Logger LOG = LoggerFactory.getLogger(SignatureHelper.class);
    private static String pubKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArij9U9yJVNc53oEMFWYp"
            + "NrXUG1UoRZseDh/p34q1uywD70RGKKWZvXIcUAZZwbZtCu4i0UzsrKRJeUwqanbc"
            + "woJvYanp6lc3DccXUN1w1Y0WOHOaBxiiK3B1TtEIH1cK/X+ZzazPG5nX7TSGh8Tp"
            + "/uxQzUFli2mDVLqaP62/fB9uJ2joX9Gtw8sZfuPGNMRoc8IdhjagbFkhFT7WCZnk"
            + "FH/4Co007lmXLAe12lQQqR/pOTeHJv1sfda1xaHtj4/Tcrq04Kx0ZmGAd5D9lA92"
            + "8pdBbzoe/mI5/Sk+nIY3AHkLXB9YAaKJf//Wb1yiP1/hchtVkfXyIaGM+cVyn7AN"
            + "VQIDAQAB";
    
    public static final void verifyFileSignature(String sigString, File destFile, boolean throwOnFailure) {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decodeBase64(pubKey));
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey pub = kf.generatePublic(spec);
            
            Signature fileSig = Signature.getInstance("SHA256withRSA");
            fileSig.initVerify(pub);
            fileSig.update(Files.readAllBytes(destFile.toPath()));
            if(!fileSig.verify(Base64.decodeBase64(sigString))) {
                String msg = "Signature mismatch"
                        +"\n Expected: "+sigString
                        +"\n Actual:   "+fileSig.hashCode();
                if(throwOnFailure) {
                    throw new IllegalStateException(msg);
                } else {
                    LOG.warn(msg);
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            if(throwOnFailure) {
                throw new RuntimeException(e);
            } else {
                LOG.warn("Signature verification failed", e);
            }
        } 
    }
}
