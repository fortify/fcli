package com.fortify.cli.common.crypto;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.function.Consumer;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.common.util.StringUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;


public class SignatureHelper {
    // This character is used by TextFileSigner and SignedTextFileReader to 
    // separate original text document and signature YAML document.
    public static final char FILE_SEPARATOR = '\u001C';
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
    
    @SneakyThrows
    public static final Verifier verifier(Path pemOrBase64KeyPath) {
        return new Verifier(getKey(Files.readString(pemOrBase64KeyPath)));
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
    public static final TextSigner textSigner(Path pemOrBase64KeyPath, String passPhrase) {
        return textSigner(pemOrBase64KeyPath, StringUtils.isBlank(passPhrase) ? null : passPhrase.toCharArray());
    }
    
    public static final TextSigner textSigner(String pemOrBase64Key, char[] passPhrase) {
        return new TextSigner(getKey(pemOrBase64Key), passPhrase);
    }
    
    public static final TextSigner textSigner(String pemOrBase64Key, String passPhrase) {
        return new TextSigner(getKey(pemOrBase64Key), StringUtils.isBlank(passPhrase) ? null : passPhrase.toCharArray());
    }
    
    @SneakyThrows
    public static final TextSigner textSigner(Path pemOrBase64KeyPath, char[] passPhrase) {
        return new TextSigner(getKey(Files.readString(pemOrBase64KeyPath)), passPhrase);
    }
    
    public static final SignedTextReader signedTextReader() {
        return new SignedTextReader();
    }
    
    @SneakyThrows
    public static final KPGenerator keyPairGenerator(char[] passPhrase) {
        return new KPGenerator(passPhrase);
    }
    
    @SneakyThrows
    public static final KPGenerator keyPairGenerator(String passPhrase) {
        return keyPairGenerator(StringUtils.isBlank(passPhrase) ? null : passPhrase.toCharArray());
    }
    
    @SneakyThrows
    private static final KeyFactory createKeyFactory() {
        return KeyFactory.getInstance("RSA");
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
        
        public final String sign(Path path, boolean throwOnFailure) {
            return sign(new FileSignatureUpdater(path.toFile()), throwOnFailure);
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
            writePem("PRIVATE KEY", kp.getPrivate().getEncoded(), privateKeyPath);
            writePem("PUBLIC KEY", kp.getPublic().getEncoded(), publicKeyPath);
        }
        
        @SneakyThrows
        private final void writePem(String type, byte[] key, Path path) {
            var pemString = asPem(type, key);
            Files.writeString(path, pemString, StandardOpenOption.CREATE_NEW);
        }
        
        private final String asPem(String type, byte[] key) {
            if ( "PRIVATE KEY".equals(type) && passPhrase!=null ) {
                //throw new RuntimeException("Generating password-protected private key not supported yet");
                return asPem("ENCRYPTED PRIVATE KEY", pemEncrypt(key, passPhrase));
            }
            return "-----BEGIN "+type+"-----\n"
                    + Base64.getMimeEncoder().encodeToString(key)
                    + "\n-----END "+type+"-----";
        }
        
        /* Less preferred algorithm, but both encrypting and decrypting the
         * private key work on Java 17. 
         */
        @SneakyThrows
        private static byte[] pemEncrypt(byte[] key, char[] passPhrase) {
            byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
            String pbealg = "PBEwithSHA1andDESede"; 
            SecretKey secretKey = SecretKeyFactory.getInstance(pbealg) 
                    .generateSecret(new PBEKeySpec(passPhrase));
            Cipher cipher = Cipher.getInstance(pbealg);
            cipher.init (Cipher.ENCRYPT_MODE, secretKey, new PBEParameterSpec(salt,65536));
            byte[] body = cipher.doFinal(key);
            return new EncryptedPrivateKeyInfo(cipher.getParameters(),body).getEncoded();
        }
        
        /* Template-based approach
         * See comments at https://stackoverflow.com/questions/78372763/java-21-generate-encrypted-private-rsa-key-in-pem-format */
        /*
        @SneakyThrows
        private static byte[] pemEncrypt(byte[] key, char[] passPhrase) {
            EncryptedPrivateKeyInfo encryptPKInfo = new EncryptedPrivateKeyInfo(readTemplate());
            Cipher cipher = Cipher.getInstance(encryptPKInfo.getAlgName());
            PBEKeySpec pbeKeySpec = new PBEKeySpec(passPhrase);
            SecretKeyFactory secFac = SecretKeyFactory.getInstance(encryptPKInfo.getAlgName());
            Key pbeKey = secFac.generateSecret(pbeKeySpec);
            // For now using original parameters; 
            // I suppose I should create new parameters instead?
            AlgorithmParameters algParams = encryptPKInfo.getAlgParameters();
            cipher.init(Cipher.ENCRYPT_MODE, pbeKey, algParams);
            byte[] body = cipher.doFinal(key);
            return new EncryptedPrivateKeyInfo(cipher.getParameters(), body).getEncoded();
        }
        
        @SneakyThrows
        private static byte[] readTemplate() {
            try ( var is = KPGenerator.class.getClassLoader().getResourceAsStream("com/fortify/cli/common/crypto/key-template.aes256.pem") ) {
                return getKey(IOUtils.toString(is, StandardCharsets.US_ASCII));
            }
        }
        */
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
    
    public static final class SignedTextReader {
        private SignedTextReader() {}
        
        public final SignedTextDescriptor load(InputStream is, Charset charset, boolean evaluateSignature) {
            return load(FileUtils.readInputStreamAsString(is, charset), evaluateSignature);
        }
        
        public final SignedTextDescriptor load(InputStream is, Charset charset, Consumer<SignedTextDescriptor> onInvalidSingature) {
            return load(FileUtils.readInputStreamAsString(is, charset), onInvalidSingature);
        }
        
        @SneakyThrows
        public final SignedTextDescriptor load(String signedOrUnsignedText, boolean evaluateSignature) {
            var elts = signedOrUnsignedText.split(String.valueOf(FILE_SEPARATOR));
            if ( elts.length>2 ) {
                throw new IllegalStateException("Input may contain only single Unicode File Separator character");
            } else if ( elts.length==1) {
                return buildUnsignedDescriptor(elts[0]);
            } else {
                var signatureDescriptor = new ObjectMapper(new YAMLFactory())
                        .readValue(elts[1], SignatureDescriptor.class);
                return buildSignedDescriptor(elts[0], signatureDescriptor, evaluateSignature);
            }
        }
        
        /**
         * Load a {@link SignedTextDescriptor} instance from the given signed or
         * unsigned text.
         * 
         * @param signedOrUnsignedText Either signed or unsigned text to be loaded.
         * @param onInvalidSingature is invoked if there's no valid signature. If null,
         *        the signature status will not be evaluated. To evaluate signature status
         *        without performing any action on failure, simply pass d->{}.
         * @return {@link SignedTextDescriptor} instance
         */
        public final SignedTextDescriptor load(String signedOrUnsignedText, Consumer<SignedTextDescriptor> onInvalidSingature) {
            if ( onInvalidSingature==null ) { return load(signedOrUnsignedText, false); }
            var descriptor = load(signedOrUnsignedText, true);
            if ( descriptor.getSignatureStatus()!=SignatureStatus.VALID_SIGNATURE ) {
                onInvalidSingature.accept(descriptor);
            }
            return descriptor;
        }

        private SignedTextDescriptor buildUnsignedDescriptor(String payload) {
            return SignedTextDescriptor.builder()
                    .payload(payload)
                    .signatureStatus(SignatureStatus.NO_SIGNATURE)
                    .build();
        }
        
        private SignedTextDescriptor buildSignedDescriptor(String payload, SignatureDescriptor signatureDescriptor, boolean evaluateSignatureStatus) {
            var signatureStatus = SignatureStatus.NOT_VERIFIED;
            if ( evaluateSignatureStatus ) {
                // TODO Load public key based on fingerprint in descriptor
                // TODO Verify payload against signature
                // var verifier = SignatureHelper.verifier(publicKey);
                // TODO Set proper signature status
            }
            return SignedTextDescriptor.builder()
                    .payload(payload)
                    .signatureDescriptor(signatureDescriptor)
                    .signatureStatus(signatureStatus)
                    .build();
        }

    }
    
    public static final class TextSigner {
        private final Signer signer;
        
        private TextSigner(byte[] privateKey, char[] passPhrase) {
            this.signer = new Signer(privateKey, passPhrase);
        }
        
        @SneakyThrows
        public final void signAndWrite(Path payloadPath, Path outputPath, ObjectNode extraInfo) {
            var content = sign(Files.readString(payloadPath), extraInfo);
            Files.writeString(outputPath, content, StandardOpenOption.CREATE_NEW);
        }
        
        @SneakyThrows
        public final String sign(Path payloadPath, ObjectNode extraInfo) {
            return sign(Files.readString(payloadPath), extraInfo);
        }
        
        @SneakyThrows
        public final String sign(String textToSign, ObjectNode extraInfo) {
            var payload = readBytes(textToSign);
            var fingerprint = signer.publicKeyFingerprint();
            var signature = signer.sign(payload, true);
            
            var signatureDescriptor = SignatureDescriptor.builder()
                    .signature(signature)
                    .publicKeyFingerprint(fingerprint)
                    .extraInfo(extraInfo)
                    .build();
            return generateOutput(payload, signatureDescriptor);
        }

        private byte[] readBytes(String payload) throws IOException {
            if ( payload.contains(String.valueOf(FILE_SEPARATOR)) ) {
                throw new IllegalStateException("Input file may not contain Unicode File Separator character \u001C");
            }
            return payload.getBytes(StandardCharsets.UTF_8);
        }

        @SneakyThrows
        private final String generateOutput(byte[] payload, SignatureDescriptor signatureDescriptor) {
            YAMLFactory factory = new YAMLFactory();
            try ( var os = new ByteArrayOutputStream();
                  var generator = (YAMLGenerator)factory.createGenerator(os) ) {
                os.write(payload);
                os.write(FILE_SEPARATOR);
                os.write('\n');
                generator.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
                        .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, true)
                        .setCodec(new ObjectMapper())
                        .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                        .useDefaultPrettyPrinter();
                generator.writeObject(signatureDescriptor);
                return os.toString(StandardCharsets.UTF_8);
            }
        }
    
        public final PublicKey publicKey() {
            return signer.publicKey();
        }
        
        public final String publicKeyFingerprint() {
            return signer.publicKeyFingerprint();
        }
        
        public final void writePublicKey(Path publicKeyPath) {
            // TODO
        }
    }
    
    @Reflectable @NoArgsConstructor
    @Data @AllArgsConstructor @Builder
    public static final class SignatureDescriptor {
        /** Actual signature */
        private String signature;
        /** Public key fingerprint */
        private String publicKeyFingerprint;
        /** Additional information about the signature or action */
        private ObjectNode extraInfo;
    }
    
    @Reflectable @NoArgsConstructor
    @Data @AllArgsConstructor @Builder
    public static final class SignedTextDescriptor {
        /** Original file contents */
        private String payload;
        /** Signature descriptor */
        private SignatureDescriptor signatureDescriptor; 
        /** Signature status */
        private SignatureStatus signatureStatus;
    }
    
    public static enum SignatureStatus {
        VALID_SIGNATURE, INVALID_SIGNATURE, NO_PUBLIC_KEY, NO_SIGNATURE, NOT_VERIFIED
    }
    
    @SneakyThrows
    public static void main(String[] args) {
        var priv = Paths.get("/home/rsenden/test-private.key");
        var pub = Paths.get("/home/rsenden/test-public.key");
        var output = Paths.get("/home/rsenden/signed.txt");
        Files.deleteIfExists(priv);
        Files.deleteIfExists(pub);
        Files.deleteIfExists(output);
        keyPairGenerator("test").writePem(priv, pub);
        textSigner(priv, "test".toCharArray()).signAndWrite(pub, output, null);
        var result = new SignedTextReader().load(Files.readString(output, StandardCharsets.UTF_8), true);
        System.out.println(result);
    }
}
