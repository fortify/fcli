package com.fortify.cli.common.crypto.helper;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.crypto.helper.impl.KPGenerator;
import com.fortify.cli.common.crypto.helper.impl.PublicKeyTrustStore;
import com.fortify.cli.common.crypto.helper.impl.SignedTextReader;
import com.fortify.cli.common.crypto.helper.impl.Signer;
import com.fortify.cli.common.crypto.helper.impl.TextSigner;
import com.fortify.cli.common.crypto.helper.impl.Verifier;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;


public class SignatureHelper {
    public static final String FORTIFY_PUBLIC_KEY = 
              "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArij9U9yJVNc53oEMFWYp"
            + "NrXUG1UoRZseDh/p34q1uywD70RGKKWZvXIcUAZZwbZtCu4i0UzsrKRJeUwqanbc"
            + "woJvYanp6lc3DccXUN1w1Y0WOHOaBxiiK3B1TtEIH1cK/X+ZzazPG5nX7TSGh8Tp"
            + "/uxQzUFli2mDVLqaP62/fB9uJ2joX9Gtw8sZfuPGNMRoc8IdhjagbFkhFT7WCZnk"
            + "FH/4Co007lmXLAe12lQQqR/pOTeHJv1sfda1xaHtj4/Tcrq04Kx0ZmGAd5D9lA92"
            + "8pdBbzoe/mI5/Sk+nIY3AHkLXB9YAaKJf//Wb1yiP1/hchtVkfXyIaGM+cVyn7AN"
            + "VQIDAQAB";
    private static final Verifier FORTIFY_SIGNATURE_VERIFIER = verifier(FORTIFY_PUBLIC_KEY);
    
    public static final Verifier fortifySignatureVerifier() {
       return FORTIFY_SIGNATURE_VERIFIER; 
    }
    
    public static final Verifier verifier(byte[] publicKey) {
        return new Verifier(publicKey);
    }
    
    public static final Verifier verifier(String pemOrBase64Key) {
        return new Verifier(pemOrBase64Key);
    }
    
    @SneakyThrows
    public static final Verifier verifier(Path pemOrBase64KeyPath) {
        return new Verifier(Files.readString(pemOrBase64KeyPath));
    }
    
    public static final Signer signer(String pemOrBase64Key, char[] passPhrase) {
        return new Signer(pemOrBase64Key, passPhrase);
    }
    
    public static final Signer signer(String pemOrBase64Key, String passPhrase) {
        return new Signer(pemOrBase64Key, StringUtils.isBlank(passPhrase) ? null : passPhrase.toCharArray());
    }
    
    @SneakyThrows
    public static final Signer signer(Path pemOrBase64KeyPath, char[] passPhrase) {
        return new Signer(Files.readString(pemOrBase64KeyPath), passPhrase);
    }
    
    @SneakyThrows
    public static final Signer signer(Path pemOrBase64KeyPath, String passPhrase) {
        return signer(pemOrBase64KeyPath, StringUtils.isBlank(passPhrase) ? null : passPhrase.toCharArray());
    }
    
    @SneakyThrows
    public static final TextSigner textSigner(Path pemOrBase64KeyPath, String passPhrase) {
        return textSigner(pemOrBase64KeyPath, StringUtils.isBlank(passPhrase) ? null : passPhrase.toCharArray());
    }
    
    public static final TextSigner textSigner(String pemOrBase64Key, char[] passPhrase) {
        return new TextSigner(pemOrBase64Key, passPhrase);
    }
    
    public static final TextSigner textSigner(String pemOrBase64Key, String passPhrase) {
        return new TextSigner(pemOrBase64Key, StringUtils.isBlank(passPhrase) ? null : passPhrase.toCharArray());
    }
    
    @SneakyThrows
    public static final TextSigner textSigner(Path pemOrBase64KeyPath, char[] passPhrase) {
        return new TextSigner(Files.readString(pemOrBase64KeyPath), passPhrase);
    }
    
    public static final SignedTextReader signedTextReader() {
        return SignedTextReader.INSTANCE;
    }
    
    @SneakyThrows
    public static final KPGenerator keyPairGenerator(char[] passPhrase) {
        return new KPGenerator(passPhrase);
    }
    
    @SneakyThrows
    public static final KPGenerator keyPairGenerator(String passPhrase) {
        return keyPairGenerator(StringUtils.isBlank(passPhrase) ? null : passPhrase.toCharArray());
    }
    
    public static final PublicKeyTrustStore publicKeyTrustStore() {
        return PublicKeyTrustStore.INSTANCE;
    }
    
    @FunctionalInterface
    public static interface InvalidSignatureHandler {
        public static final InvalidSignatureHandler IGNORE   = null;
        public static final InvalidSignatureHandler EVALUATE = d->{};
        
        void onInvalidSignature(SignedTextDescriptor descriptor);
    }
    
    public static enum SignatureStatus {
        VALID, MISMATCH, NO_PUBLIC_KEY, UNSIGNED, NOT_VERIFIED;
        
        public void throwIfNotValid(boolean throwIfNotValid) {
            if ( throwIfNotValid && this!=VALID ) {
                throw new IllegalStateException("Signature is not valid: "+this);
            }
        }
    }
    
    @Data
    public static final class SignatureValidator {
        private final InvalidSignatureHandler invalidSignatureHandler;
        private final String[] extraPublicKeys;
        
        public SignatureValidator(InvalidSignatureHandler invalidSignatureHandler, String... extraPublicKeys) {
            this.invalidSignatureHandler = invalidSignatureHandler;
            this.extraPublicKeys = extraPublicKeys;
        }
    }
    
    @Reflectable @NoArgsConstructor
    @Data @AllArgsConstructor @Builder
    public static final class SignatureDescriptor {
        /** Actual signature */
        private String signature;
        /** Public key fingerprint */
        private String publicKeyFingerprint;
        /** Signature metadata */
        private SignatureMetadata metadata;
    }
    
    @Reflectable @NoArgsConstructor
    @Data @AllArgsConstructor @Builder
    public static final class SignatureMetadata {
        /** Fcli version */
        private String fcliVersion;
        /** Free-format string describing who signed this file */
        private String signer;
        /** Additional free-format information about the signed file,
         *  for example information about where the public key can
         *  be found, information about the signed file, ... */
        private ObjectNode extraInfo;
    }
    
    @Reflectable @NoArgsConstructor
    @Data @AllArgsConstructor @Builder
    public static final class SignedTextDescriptor {
        /** Original text that was parsed, including signature if present */
        private String original;
        /** Text that was signed */
        private String payload;
        /** Signature descriptor */
        private SignatureDescriptor signatureDescriptor; 
        /** Signature status */
        private SignatureStatus signatureStatus;
        /** Public key descriptor */
        private PublicKeyDescriptor publicKeyDescriptor;
    }
    
    @Reflectable @NoArgsConstructor
    @Data @EqualsAndHashCode(callSuper = false) @AllArgsConstructor @Builder(toBuilder = true)
    public static final class PublicKeyDescriptor extends JsonNodeHolder {
        /** Name for this public key */
        private String name;
        /** Fingerprint for this public key */
        private String fingerprint;
        /** Actual public key */
        private String publicKey;
        /** Source where public key was loaded from */
        @JsonIgnore private PublicKeySource source;
    }
    
    @Reflectable
    public static enum PublicKeySource {
        TRUSTSTORE, EXTERNAL, INTERNAL
    }
}
