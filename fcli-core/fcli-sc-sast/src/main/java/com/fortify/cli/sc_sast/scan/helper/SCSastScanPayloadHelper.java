/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.sc_sast.scan.helper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanPayloadDescriptor.SCSastScanPayloadDescriptorBuilder;

import io.micrometer.common.util.StringUtils;
import lombok.Builder;

@Builder
public class SCSastScanPayloadHelper {
    private static final Logger LOG = LoggerFactory.getLogger(SCSastScanPayloadHelper.class);
    private static final Pattern dotnetFlagFilePattern = Pattern.compile("^dotnet(-(?<version>\\d+\\.\\d+(\\.\\d+)?))?$");
    private static final Pattern onlyLinuxFilePattern = Pattern.compile("[:*?|<>\"]");
    private final File payloadFile;
    private final String overrideProductVersion;
    private final String overrideProductVersionOptionNames;
    
    public final SCSastScanPayloadDescriptor loadDescriptor() {
        if ( !payloadFile.exists() ) {
            throw new FcliSimpleException("Scan payload file "+payloadFile.getName()+" doesn't exist");
        }
        var builder = SCSastScanPayloadDescriptor.builder().payloadFile(payloadFile);
        loadDescriptor(builder);
        return builder.build();
    }

    private final void loadDescriptor(SCSastScanPayloadDescriptorBuilder builder) {
        try ( FileSystem fs = FileSystems.newFileSystem(payloadFile.toPath()) ) {
            Path mbsManifestPath = fs.getPath("MobileBuildSession.manifest");
            Path packageMetadataPath = fs.getPath("metadata");
            if ( Files.exists(mbsManifestPath) ) { 
                loadFromMbs(fs, mbsManifestPath, builder); 
            } else if ( Files.exists(packageMetadataPath) ) {
                loadFromPackage(fs, packageMetadataPath, builder);
            } else {
                throw new FcliSimpleException(payloadFile+" doesn't seem to be a valid MBS or ScanCentral package file");
            }
        } catch (IOException e) {
            throw new FcliSimpleException("Unable to proces scan payload file "+payloadFile, e);
        }
    }
    
    private void loadFromMbs(FileSystem fs, Path mbsManifestPath, SCSastScanPayloadDescriptorBuilder builder) throws IOException {
        if ( StringUtils.isNotBlank(overrideProductVersion) ) {
            throw new FcliSimpleException("Option "+overrideProductVersionOptionNames+" is not supported for MBS files");
        }
        Properties p = loadMbsManifestProperties(mbsManifestPath);
        builder
            .productVersion(p.getProperty("SCAVersion"))
            .buildId(p.getProperty("BuildID"))
            .dotNetRequired(false)
            .dotNetVersion(null)
            .jobType(SCSastScanJobType.SCAN_JOB)
            .requiredOs(SCSastOperatingSystem.ANY);
    }

    private Properties loadMbsManifestProperties(Path mbsManifestPath) throws IOException {
        try ( InputStream is = Files.newInputStream(mbsManifestPath) ) {
            Properties p = new Properties();
            p.load(is);
            return p;
        }
    }

    private void loadFromPackage(FileSystem fs, Path packageMetadataPath, SCSastScanPayloadDescriptorBuilder builder) throws IOException {
        var productVersion = getProductVersionFromPackageOrOverride(packageMetadataPath);
        var dotNetVersion = getDotNetVersionFromPackage(fs);
        var dotNetRequired = StringUtils.isNotBlank(dotNetVersion);
        var requiredOs = isLinuxRequired(fs) ? SCSastOperatingSystem.LINUX : SCSastOperatingSystem.ANY;
        builder
            .productVersion(productVersion)
            .buildId(null)
            .dotNetRequired(dotNetRequired)
            .dotNetVersion(dotNetVersion)
            .jobType(SCSastScanJobType.TRANSLATION_AND_SCAN_JOB)
            .requiredOs(requiredOs);
        
    }

    private boolean isLinuxRequired(FileSystem fs) {
        return StreamSupport.stream(fs.getFileStores().spliterator(), false)
                .map(FileStore::name)
                .map(onlyLinuxFilePattern::matcher)
                .filter(Matcher::matches)
                .findFirst()
                .isPresent();
    }

    private String getProductVersionFromPackageOrOverride(Path packageMetadataPath) throws IOException {
        var metadata = JsonHelper.getObjectMapper().readTree(Files.readAllBytes(packageMetadataPath));
        var clientVersionNode = metadata.get("client-version");
        // Older package versions don't include client-version, so node may be null
        var productVersion = clientVersionNode==null ? null : clientVersionNode.asText(); 
        var normalizedOverrideProductVersion = getNormalizedOverrideProductVersion();
        if ( StringUtils.isNotBlank(normalizedOverrideProductVersion) ) {
            if ( StringUtils.isNotBlank(productVersion) ) {
                LOG.warn(String.format("WARN: Detected product version %s, override with %s may cause unexpected results", productVersion, overrideProductVersion));
            }
            productVersion = normalizedOverrideProductVersion;
        }
        if ( StringUtils.isBlank(productVersion) ) {
            throw new FcliSimpleException("Can't detect product version from package, please specify "+overrideProductVersionOptionNames);
        }
        return productVersion;
    }

    private String getDotNetVersionFromPackage(FileSystem fs) {
        return StreamSupport.stream(fs.getFileStores().spliterator(), false)
            .map(FileStore::name)
            .map(dotnetFlagFilePattern::matcher)
            .filter(Matcher::matches)
            .findFirst()
            .map(m->m.group("version"))
            .orElse(null);
    }
    
    private final String getNormalizedOverrideProductVersion() {
        return overrideProductVersion==null ? null : overrideProductVersion.chars().filter(ch -> ch == '.').count()==1
                ? overrideProductVersion+".0"
                : overrideProductVersion;
    }
}
