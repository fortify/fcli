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
package com.fortify.cli.fod.aviator.helper;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.AbstractFcliException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.rest.unirest.HttpHeader;
import com.fortify.cli.common.rest.unirest.RestResponseBodyHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

/**
 * FoD remediations FPR download built on {@link RestResponseBodyHelper}, which downloads to a
 * temporary file, polls while FoD responds with HTTP 202, and only replaces the destination after
 * a complete, successful download. Existing destination files are left untouched on failure.
 *
 * <p>Polling policy: the request is sent at most {@value #MAX_ATTEMPTS} times, waiting
 * {@link #POLL_INTERVAL} between attempts; if FoD still responds with HTTP 202 after the last
 * attempt, the download fails.</p>
 *
 * <p>{@link RestResponseBodyHelper} only supports default-filesystem destinations. For other
 * filesystems (for example remediations cache zip entries), the FPR is first downloaded to a local
 * temporary file, and then copied into a temporary sibling entry that replaces the destination
 * only after the copy has completed.</p>
 */
public final class FoDRemediationsFprDownloadHelper {
    private static final Logger logger = LoggerFactory.getLogger(FoDRemediationsFprDownloadHelper.class);
    static final int MAX_ATTEMPTS = 10;
    static final Duration POLL_INTERVAL = Duration.ofSeconds(30);
    private static final String PART_SUFFIX = ".part";

    private FoDRemediationsFprDownloadHelper() {}

    public static void downloadStaticRemediationsFpr(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
            Path destination) {
        FoDScanDescriptor scanDescriptor = FoDScanHelper.getLatestScanDescriptor(unirest, releaseDescriptor.getReleaseId(),
                FoDScanType.Static, false);
        FoDScanHelper.validateScanDate(scanDescriptor, FoDScanHelper.MAX_RETENTION_PERIOD);
        GetRequest request = unirest.get("/api/v3/releases/{releaseId}/fpr")
                .routeParam("releaseId", releaseDescriptor.getReleaseId())
                .headerReplace(HttpHeader.ACCEPT, "application/octet-stream")
                .queryString("scanType", scanDescriptor.getScanType());
        download(request, destination, POLL_INTERVAL, MAX_ATTEMPTS);
    }

    static void download(GetRequest request, Path destination, Duration pollInterval, int maxAttempts) {
        try {
            if (destination.getFileSystem().equals(FileSystems.getDefault())) {
                RestResponseBodyHelper.saveToFileWhenReady(request, destination, null, pollInterval, Duration.ZERO, maxAttempts);
            } else {
                downloadViaLocalTempFile(request, destination, pollInterval, maxAttempts);
            }
        } catch (AbstractFcliException | UnexpectedHttpResponseException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new FcliTechnicalException("Error downloading FoD remediations FPR to " + destination, e);
        }
    }

    private static void downloadViaLocalTempFile(GetRequest request, Path destination, Duration pollInterval, int maxAttempts) {
        Path localTemp = null;
        Path partEntry = null;
        try {
            localTemp = Files.createTempFile("fcli-fod-fpr-", ".fpr");
            RestResponseBodyHelper.saveToFileWhenReady(request, localTemp, null, pollInterval, Duration.ZERO, maxAttempts);
            partEntry = destination.resolveSibling(destination.getFileName() + PART_SUFFIX);
            Files.copy(localTemp, partEntry, StandardCopyOption.REPLACE_EXISTING);
            Files.move(partEntry, destination, StandardCopyOption.REPLACE_EXISTING);
            partEntry = null;
        } catch (IOException e) {
            throw new FcliTechnicalException("Error saving FoD remediations FPR to " + destination, e);
        } finally {
            deleteQuietly(localTemp);
            deleteQuietly(partEntry);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warn("Failed to delete temporary FoD FPR download artifact: {}", path, e);
        }
    }
}
