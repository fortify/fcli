package com.fortify.cli.tool._common.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
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
    
    public static final void verifyFileSignature(File destFile, String expectedSignature, boolean throwOnFailure) {
        try {
            Signature signature = createSignature();
            updateSignature(signature, destFile);
            verifySignature(signature, expectedSignature);
        } catch (Exception e) {
            handleSignatureException(e, throwOnFailure);
        }
    }

    private static Signature createSignature() throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decodeBase64(pubKey));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey pub = kf.generatePublic(spec);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(pub);
        return signature;
    }

    private static void updateSignature(Signature signature, File destFile)
            throws IOException, SignatureException, FileNotFoundException {
        try ( var is = new FileInputStream(destFile); ) {
            byte[] buffer = new byte[4096];
            int read = 0;
            while ( (read = is.read(buffer)) > 0 ) {
                signature.update(buffer, 0, read);
            }
        }
    }
    
    private static void verifySignature(Signature signature, String expectedSignature) 
            throws SignatureException, SignatureMismatchException {
        if(!signature.verify(Base64.decodeBase64(expectedSignature))) {
            String msg = "Signature mismatch"
                    +"\n Expected: "+expectedSignature
                    +"\n Actual:   "+signature.hashCode();
            throw new SignatureMismatchException(msg);
        }
    }
    
    private static void handleSignatureException(Exception e, boolean throwOnFailure) {
        if(!throwOnFailure) {
            LOG.warn("Signature verification failed", e);
        } else {
            if ( e instanceof RuntimeException ) {
                throw (RuntimeException)e;
            } else {
                throw new IllegalStateException("Signature verification failed", e);
            }
        }
    }
    
    public static final class SignatureMismatchException extends IllegalStateException {
        private static final long serialVersionUID = 1L;
        public SignatureMismatchException(String msg) {
            super(msg);
        }
    }
    
    
}
