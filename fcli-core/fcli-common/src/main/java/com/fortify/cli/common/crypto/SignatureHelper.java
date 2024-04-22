package com.fortify.cli.common.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.util.StringUtils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;


public class SignatureHelper {
    private static final Logger LOG = LoggerFactory.getLogger(SignatureHelper.class);
    private static final KeyFactory KEY_FACTORY = createKeyFactory();
    private static final String FORTIFY_PUBLIC_KEY = 
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
        return new Verifier(getKey(pemOrBase64Key));
    }
    
    public static final Signer signer(String pemOrBase64Key, char[] passPhrase) {
        return new Signer(getKey(pemOrBase64Key), passPhrase);
    }
    
    public static final Signer signer(String pemOrBase64Key, String passPhrase) {
        return new Signer(getKey(pemOrBase64Key), StringUtils.isBlank(passPhrase) ? null : passPhrase.toCharArray());
    }
    
    @SneakyThrows
    public static final Signer signer(Path pemOrBase64KeyPath, char[] passPhrase) {
        return new Signer(getKey(Files.readString(pemOrBase64KeyPath)), passPhrase);
    }
    
    @SneakyThrows
    public static final Signer signer(Path pemOrBase64KeyPath, String passPhrase) {
        return signer(pemOrBase64KeyPath, StringUtils.isBlank(passPhrase) ? null : passPhrase.toCharArray());
    }
    
    @SneakyThrows
    private static final KeyFactory createKeyFactory() {
        return KeyFactory.getInstance("RSA");
    }
    
    @SneakyThrows
    public static final KPGenerator keyPairGenerator(String passPhrase) {
        return new KPGenerator(StringUtils.isBlank(passPhrase) ? null : passPhrase.toCharArray());
    }
    
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Verifier {
        private final byte[] publicKey;
        
        public final void verify(File file, String expectedSignature, boolean throwOnFailure) {
            verify(new FileSignatureUpdater(file), expectedSignature, throwOnFailure);
        }
        
        public final void verify(byte[] data, String expectedSignature, boolean throwOnFailure) {
            verify(new DataSignatureUpdater(data), expectedSignature, throwOnFailure);
        }
        
        public final void verify(String data, Charset charset, String expectedSignature, boolean throwOnFailure) {
            verify(new DataSignatureUpdater(data, charset), expectedSignature, throwOnFailure);
        }
        
        private final void verify(ISignatureUpdater updater, String expectedSignature, boolean throwOnFailure) {
            try {
                Signature signature = createSignature();
                updater.updateSignature(signature);
                verifySignature(signature, expectedSignature);
            } catch (Exception e) {
                handleSignatureException(e, throwOnFailure);
            }
        }
        
        @SneakyThrows
        private final void verifySignature(Signature signature, String expectedSignature) {
            if(!signature.verify(Base64.getDecoder().decode(expectedSignature))) {
                String msg = "Signature mismatch"
                        +"\n Expected: "+expectedSignature
                        +"\n Actual:   "+signature.hashCode();
                throw new SignatureMismatchException(msg);
            }
        }
        
        @SneakyThrows
        private final Signature createSignature() {
            PublicKey pub = publicKey();
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(pub);
            return signature;
        }

        @SneakyThrows
        private PublicKey publicKey() {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey);
            PublicKey pub = KEY_FACTORY.generatePublic(spec);
            return pub;
        }

        @SneakyThrows
        public String publicKeyFingerPrint() {
            var encodedKey = publicKey().getEncoded();
            byte[] hash = MessageDigest.getInstance("SHA256").digest(encodedKey);
            String hexKey = String.format("%064X", new BigInteger(1, hash));
            return hexKey;
        }
    }
    
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Signer {
        private final byte[] privateKey;
        private final char[] passPhrase;
        
        @SneakyThrows
        public final PublicKey publicKey() {
            var privateKey = (RSAPrivateCrtKey)createKey();
            var keySpec = new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());
            return KEY_FACTORY.generatePublic(keySpec);
        }
        
        @SneakyThrows
        public final String publicKeyFingerprint() {
            var encodedKey = publicKey().getEncoded();
            byte[] hash = MessageDigest.getInstance("SHA256").digest(encodedKey);
            String hexKey = String.format("%064X", new BigInteger(1, hash));
            return hexKey;
        }
        
        public final byte[] signAsBytes(File file, boolean throwOnFailure) {
            return signAsBytes(new FileSignatureUpdater(file), throwOnFailure);
        }
        
        public final byte[] signAsBytes(byte[] data, boolean throwOnFailure) {
            return signAsBytes(new DataSignatureUpdater(data), throwOnFailure);
        }
        
        public final byte[] signAsBytes(String data, Charset charset, boolean throwOnFailure) {
            return signAsBytes(new DataSignatureUpdater(data, charset), throwOnFailure);
        }
        
        public final String sign(File file, boolean throwOnFailure) {
            return sign(new FileSignatureUpdater(file), throwOnFailure);
        }
        
        public final String sign(byte[] data, boolean throwOnFailure) {
            return sign(new DataSignatureUpdater(data), throwOnFailure);
        }
        
        public final String sign(String data, Charset charset, boolean throwOnFailure) {
            return sign(new DataSignatureUpdater(data, charset), throwOnFailure);
        }
        
        private final String sign(ISignatureUpdater updater, boolean throwOnFailure) {
            var signature = signAsBytes(updater, throwOnFailure);
            return signature==null ? null : Base64.getEncoder().encodeToString(signature);
        }
        
        private final byte[] signAsBytes(ISignatureUpdater updater, boolean throwOnFailure) {
            try {
                Signature signature = createSignature();
                updater.updateSignature(signature);
                return signature.sign();
            } catch (Exception e) {
                handleSignatureException(e, throwOnFailure);
                return null;
            }
        }
        
        @SneakyThrows
        private final Signature createSignature() {
            var key = createKey();
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            return signature;
        }
        
        /** This works for AES-encrypted keys on Java 21 */
        @SneakyThrows
        private PrivateKey createKey() {
            var pkcs8KeySpec = passPhrase==null 
                    ? new PKCS8EncodedKeySpec(privateKey)
                    : decryptEncodedKeySpec(privateKey);
            return KEY_FACTORY.generatePrivate(pkcs8KeySpec);
        }
         
        @SneakyThrows
        private PKCS8EncodedKeySpec decryptEncodedKeySpec(byte[] bytes) {
            EncryptedPrivateKeyInfo encryptPKInfo = new EncryptedPrivateKeyInfo(bytes);
            Cipher cipher = Cipher.getInstance(encryptPKInfo.getAlgName());
            PBEKeySpec pbeKeySpec = new PBEKeySpec(passPhrase);
            SecretKeyFactory secFac = SecretKeyFactory.getInstance(encryptPKInfo.getAlgName());
            Key pbeKey = secFac.generateSecret(pbeKeySpec);
            AlgorithmParameters algParams = encryptPKInfo.getAlgParameters();
            cipher.init(Cipher.DECRYPT_MODE, pbeKey, algParams);
            return encryptPKInfo.getKeySpec(cipher);
        }
    }
    
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class KPGenerator {
        private final char[] passPhrase;
        
        @SneakyThrows
        public final KeyPair generate() {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048); 
            return kpg.generateKeyPair();
        }
        
        @SneakyThrows
        public final void writePem(Path privateKeyPath, Path publicKeyPath) {
            var kp = generate();
            writePem("PRIVATE", kp.getPrivate().getEncoded(), privateKeyPath);
            writePem("PUBLIC", kp.getPublic().getEncoded(), publicKeyPath);
        }
        
        @SneakyThrows
        private final void writePem(String type, byte[] key, Path path) {
            var pemString = asPem(type, key);
            Files.writeString(path, pemString, StandardOpenOption.CREATE_NEW);
        }
        
        private final String asPem(String type, byte[] key) {
            return "-----BEGIN "+type+" KEY-----\n"
                    + Base64.getMimeEncoder().encodeToString(key)
                    + "\n-----END "+type+" KEY-----";
        }
    }
    
    private static final byte[] getKey(String pemOrBase64Key) {
        var base64 = pemOrBase64Key.replaceAll("-----(BEGIN|END) [\\sA-Z]+ KEY-----|\\s", "");
        return Base64.getDecoder().decode(base64);
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
    
    private static interface ISignatureUpdater {
        void updateSignature(Signature signature) throws IOException, SignatureException;
    }
    
    @RequiredArgsConstructor
    private static final class FileSignatureUpdater implements ISignatureUpdater {
        private final File file;
        
        @Override
        public void updateSignature(Signature signature) throws IOException, SignatureException {
            try ( var is = new FileInputStream(file); ) {
                byte[] buffer = new byte[4096];
                int read = 0;
                while ( (read = is.read(buffer)) > 0 ) {
                    signature.update(buffer, 0, read);
                }
            }
        }
    }
    
    @RequiredArgsConstructor
    private static final class DataSignatureUpdater implements ISignatureUpdater {
        private final byte[] data;
        
        private DataSignatureUpdater(String s, Charset charset) {
            this(s.getBytes(charset));
        }
        
        @Override
        public void updateSignature(Signature signature) throws IOException, SignatureException {
            signature.update(data);
        }
    }
    
    public static final class SignatureMismatchException extends IllegalStateException {
        private static final long serialVersionUID = 1L;
        public SignatureMismatchException(String msg) {
            super(msg);
        }
    }
    
    public static void main(String[] args) {
        keyPairGenerator("abc").writePem(Path.of("private.key"), Path.of("public.key"));
    }
}
