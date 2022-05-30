package com.fortify.cli.tools.model;

import io.micronaut.aop.exceptions.UnimplementedAdviceException;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.Setter;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

@ReflectiveAccess
public class Checksum {
    @Getter @Setter ChecksumType checksumType;
    @Getter @Setter String checksum;

    public Checksum(String inChecksum){
        if(inChecksum == null){
            this.checksum = null;
            return;
        }
        this.checksumType = detectChecksumType(inChecksum);

        if(getChecksumType().equals(ChecksumType.MD5)    ||
           getChecksumType().equals(ChecksumType.SHA256) ||
           getChecksumType().equals(ChecksumType.SHA512)){
            this.checksum = inChecksum;
        } else {
            this.checksum = null;
        }
    }

    public Checksum(String checksum, ChecksumType checksumType){
        this.checksum = checksum;
        this.checksumType = checksumType;
    }

    @Override
    public String toString() {
        return checksum;
    }

    private ChecksumType detectChecksumType(String inChecksum){
        if(Pattern.matches("^[A-F0-9]{32}$", inChecksum.trim().toUpperCase()))
            return ChecksumType.MD5;

        // If SHA-3 is ever added, it will be the same length as SHA-256
        if(Pattern.matches("^[A-F0-9]{64}$", inChecksum.trim().toUpperCase()))
            return ChecksumType.SHA256;

        if(Pattern.matches("^[A-F0-9]{128}$", inChecksum.trim().toUpperCase()))
            return ChecksumType.SHA512;

        return null;
    }

    private String checksumTypeString(){
        switch (this.checksumType){
            case MD5:
                return "MD5";
            case SHA256:
                return "SHA-256";
            case SHA512:
                return "SHA-512";
            case SHA3:
                return "SHA-3";
        }
        return null;
    }

    public String generateFileChecksum(String filePath) {
        StringBuilder sbHash = new StringBuilder();
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            MessageDigest digest = MessageDigest.getInstance(checksumTypeString());
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }

            fis.close();
            byte[] bytes = digest.digest();
            for (int i = 0; i < bytes.length; i++) {
                sbHash.append(Integer
                        .toString((bytes[i] & 0xff) + 0x100, 16)
                        .substring(1));
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sbHash.toString();
    }

    public boolean match(String filePath, boolean printChecksumTestInfo){
        String fileChecksum = generateFileChecksum(filePath);
        if(printChecksumTestInfo){
            System.out.println("The provided checksum appears to be: " + checksumTypeString());
            System.out.println("Testing with checksum on file: " + fileChecksum);
        }
        return fileChecksum.toLowerCase().trim().equals(this.checksum.toLowerCase().trim());
    }

    /**
     * This method has not yet been implemented.
     * This method will reach out to a URL with the assumption that the http resource will only
     * contain a checksum/hash.
     * @return A string of the checksum located on some remote system assessable via http.
     */
    private String getRemoteChecksum(){
        throw new UnsupportedOperationException("This method has not yet been implemented.");
    }
}
