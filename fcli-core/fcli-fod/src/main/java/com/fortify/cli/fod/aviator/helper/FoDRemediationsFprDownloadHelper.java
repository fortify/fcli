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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.AbstractFcliException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.rest.unirest.HttpHeader;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.RawResponse;
import kong.unirest.UnirestInstance;

/**
 * Shared FoD remediations FPR download with 202-retry handling. Supports any {@link Path},
 * including zip filesystem entry paths. Response body is written only when the download is
 * ready (successful non-202 status).
 */
public final class FoDRemediationsFprDownloadHelper {
    private static final Logger logger = LoggerFactory.getLogger(FoDRemediationsFprDownloadHelper.class);
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_SLEEP_MS = 30_000L;

    private FoDRemediationsFprDownloadHelper() {}

    public static void downloadStaticRemediationsFpr(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
            Path destination) {
        boolean completed = false;
        try {
            FoDScanDescriptor scanDescriptor = FoDScanHelper.getLatestScanDescriptor(unirest, releaseDescriptor.getReleaseId(),
                    FoDScanType.Static, false);
            FoDScanHelper.validateScanDate(scanDescriptor, FoDScanHelper.MAX_RETENTION_PERIOD);
            GetRequest request = unirest.get("/api/v3/releases/{releaseId}/fpr")
                    .routeParam("releaseId", releaseDescriptor.getReleaseId())
                    .headerReplace(HttpHeader.ACCEPT, "application/octet-stream")
                    .queryString("scanType", scanDescriptor.getScanType());

            int status = 202;
            int retries = 0;
            while (status == 202 && retries < MAX_RETRIES) {
                HttpResponse<Integer> response = request.asObject(raw -> copyBodyIfReady(raw, destination));
                status = response.getBody() != null ? response.getBody() : response.getStatus();
                if (status == 202) {
                    retries++;
                    sleepBeforeRetry(releaseDescriptor.getReleaseId());
                }
            }
            if (status == 202) {
                throw new FcliSimpleException("Timed out waiting for FoD remediations FPR download to complete after "
                        + MAX_RETRIES + " retries");
            }
            if (status < 200 || status >= 300) {
                throw new FcliSimpleException("FoD remediations FPR download failed with HTTP status " + status
                        + " for release " + releaseDescriptor.getReleaseId());
            }
            completed = true;
        } catch (AbstractFcliException e) {
            if (!completed) {
                deleteQuietly(destination);
            }
            throw e;
        } catch (RuntimeException e) {
            if (!completed) {
                deleteQuietly(destination);
            }
            throw new FcliTechnicalException("Error downloading FoD remediations FPR for release "
                    + releaseDescriptor.getReleaseId() + " to " + destination, e);
        }
    }

    /** Status first; write body only for ready 2xx (not 202). Drain otherwise so the connection can close. */
    private static int copyBodyIfReady(RawResponse raw, Path destination) {
        int status = raw.getStatus();
        try (InputStream in = raw.getContent()) {
            if (status < 200 || status >= 300 || status == 202) {
                in.transferTo(OutputStream.nullOutputStream());
                return status;
            }
            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            return status;
        } catch (IOException e) {
            throw new FcliTechnicalException("Error handling FoD download response for " + destination, e);
        }
    }

    private static void sleepBeforeRetry(String releaseId) {
        try {
            Thread.sleep(RETRY_SLEEP_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FcliTechnicalException(
                    "Interrupted while waiting for FoD remediations FPR download for release " + releaseId, e);
        }
    }

    private static void deleteQuietly(Path destination) {
        if (destination == null) {
            return;
        }
        try {
            Files.deleteIfExists(destination);
        } catch (IOException e) {
            logger.warn("Failed to delete incomplete FoD FPR download: {}", destination, e);
        }
    }
}
