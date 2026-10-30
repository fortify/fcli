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
package com.fortify.cli.fod._common.scan.helper.compare;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;

import kong.unirest.RawResponse;
import kong.unirest.UnirestInstance;
import lombok.extern.slf4j.Slf4j;

/** SAST-specific scan comparison: scan summary settings and file changes based on scan manifests */
@Slf4j
public final class FoDScanCompareSastSupport implements IFoDScanCompareSupport {
    private static final String DETAILS = "/staticScanSummaryDetails/";
    private static final Pattern TIMESTAMP_COLUMN_PATTERN = Pattern.compile(
            "(?i).*\\d{4}-\\d{2}-\\d{2}.*|.*\\d{1,2}/\\d{1,2}/\\d{2,4}.*"
            + "|\\d{1,2}:\\d{2}(:\\d{2}([.,]\\d+)?)?(z|[+-]\\d{2}:?\\d{2})?|am|pm");
    private static final Pattern MANIFEST_LINE_PATTERN = Pattern.compile("^\"([^\"]*)\"\\s*(.*)$");
    private static final List<FoDScanCompareField> SETTING_FIELDS = List.of(
            new FoDScanCompareField("engineVersion", DETAILS + "engineVersion"),
            new FoDScanCompareField("rulePackVersion", DETAILS + "rulePackVersion"),
            new FoDScanCompareField("auditPreference", DETAILS + "auditPreferenceType"),
            new FoDScanCompareField("scanPolicy", DETAILS + "scanPolicy"),
            new FoDScanCompareField("fortifyAviatorRequested", DETAILS + "includeFortifyAviator"),
            new FoDScanCompareField("scanMethod", "/scanMethodTypeName"),
            new FoDScanCompareField("scanTool", "/scanTool"),
            new FoDScanCompareField("scanToolVersion", "/scanToolVersion"));
    private static final List<FoDScanCompareField> STATISTIC_FIELDS = List.of(
            new FoDScanCompareField("fileCount", DETAILS + "fileCount"),
            new FoDScanCompareField("totalLinesOfCode", DETAILS + "totalLinesOfCode"),
            new FoDScanCompareField("payloadSize", DETAILS + "payLoadSize"));
    private final int maxFileChanges;
    private final long maxManifestBytes;

    /**
     * @param maxFileChanges Maximum number of files to list for each of added, changed and removed files
     * @param maxManifestSizeMb Maximum manifest size in MB; manifest comparison is skipped for larger manifests
     */
    public FoDScanCompareSastSupport(int maxFileChanges, int maxManifestSizeMb) {
        this.maxFileChanges = maxFileChanges;
        this.maxManifestBytes = maxManifestSizeMb * FoDScanCompareHelper.BYTES_PER_MB;
    }

    @Override
    public FoDScanType getScanType() {
        return FoDScanType.Static;
    }

    @Override
    public Set<String> getIgnoredAssessmentTypes() {
        return Set.of("sarif (imported)");
    }

    @Override
    public List<FoDScanCompareField> getSettingFields() {
        return SETTING_FIELDS;
    }

    @Override
    public List<FoDScanCompareField> getStatisticFields() {
        return STATISTIC_FIELDS;
    }

    @Override
    public void addScanTypeDeltas(UnirestInstance unirest, FoDScanComparePair pair, ObjectNode result) {
        var current = loadManifest(unirest, pair.targetScanId());
        var previous = loadManifest(unirest, pair.previousScanId());
        result.put("currentManifestAvailable", current.available());
        result.put("previousManifestAvailable", previous.available());
        var skipReason = StringUtils.firstNonBlank(
                getSkipReason(current, pair.targetScanId()), getSkipReason(previous, pair.previousScanId()));
        result.put("manifestComparisonSkipped", skipReason != null);
        result.put("manifestComparisonSkippedReason", skipReason);
        if ( skipReason != null ) { return; }
        var currentEntries = parseManifest(current.text());
        var previousEntries = parseManifest(previous.text());
        var added = currentEntries.keySet().stream().filter(f -> !previousEntries.containsKey(f)).toList();
        var removed = previousEntries.keySet().stream().filter(f -> !currentEntries.containsKey(f)).toList();
        var changed = currentEntries.keySet().stream()
                .filter(f -> previousEntries.containsKey(f) && !previousEntries.get(f).equals(currentEntries.get(f)))
                .toList();
        FoDScanCompareHelper.addValueList(result, "addedFile", added, maxFileChanges);
        FoDScanCompareHelper.addValueList(result, "changedFile", changed, maxFileChanges);
        FoDScanCompareHelper.addValueList(result, "removedFile", removed, maxFileChanges);
    }

    private String getSkipReason(ManifestContent manifest, String scanId) {
        if ( manifest.tooLarge() ) {
            return String.format("Manifest for scan %s exceeds maximum size of %d MB; use --max-manifest-size to increase this limit",
                    scanId, maxManifestBytes / FoDScanCompareHelper.BYTES_PER_MB);
        }
        return manifest.available() ? null : String.format("Manifest not available for scan %s", scanId);
    }

    private ManifestContent loadManifest(UnirestInstance unirest, String scanId) {
        ManifestContent content;
        try {
            content = unirest.get(FoDUrls.SCAN_MANIFEST).routeParam("scanId", scanId)
                    .asObject(raw -> readManifest(raw, maxManifestBytes)).getBody();
        } catch ( UnexpectedHttpResponseException e ) {
            if ( !FoDScanCompareHelper.isUnavailable(e) ) { throw e; }
            log.debug("No manifest available for scan {} (HTTP status {})", scanId, e.getStatus());
            return ManifestContent.UNAVAILABLE;
        }
        if ( content == null ) { return ManifestContent.UNAVAILABLE; }
        if ( content.error() != null ) {
            throw new FcliTechnicalException(String.format("Error reading manifest for scan %s", scanId), content.error());
        }
        if ( content.tooLarge() ) { log.debug("Manifest for scan {} exceeds {} bytes", scanId, maxManifestBytes); }
        return content;
    }

    // Reads at most maxBytes+1 bytes, to avoid loading arbitrarily large manifests into memory
    private static ManifestContent readManifest(RawResponse raw, long maxBytes) {
        try ( InputStream in = raw.getContent() ) {
            // Error responses are handled by the fcli unexpected response interceptor
            if ( raw.getStatus() < 200 || raw.getStatus() >= 300 ) { return null; }
            var contentLength = NumberUtils.toLong(raw.getHeaders().getFirst("Content-Length"), -1);
            if ( contentLength > maxBytes ) { return ManifestContent.TOO_LARGE; }
            var bytes = in.readNBytes((int)Math.min(maxBytes + 1, Integer.MAX_VALUE - 8));
            if ( bytes.length > maxBytes ) { return ManifestContent.TOO_LARGE; }
            var text = new String(bytes, StandardCharsets.UTF_8);
            return StringUtils.isBlank(text) ? ManifestContent.UNAVAILABLE : new ManifestContent(true, text, false, null);
        } catch ( IOException e ) {
            return new ManifestContent(false, null, false, e);
        }
    }

    private record ManifestContent(boolean available, String text, boolean tooLarge, IOException error) {
        static final ManifestContent UNAVAILABLE = new ManifestContent(false, null, false, null);
        static final ManifestContent TOO_LARGE = new ManifestContent(true, null, true, null);
    }

    // Lines look like '"<path>"    <size>    <timestamp>'; maps path to non-timestamp columns to ignore timestamp-only changes
    static Map<String, String> parseManifest(String content) {
        var result = new LinkedHashMap<String, String>();
        if ( content == null ) { return result; }
        content.lines().map(String::trim).filter(StringUtils::isNotBlank).forEach(line -> {
            var matcher = MANIFEST_LINE_PATTERN.matcher(line);
            var path = matcher.matches() ? matcher.group(1) : line;
            var remainder = matcher.matches() ? matcher.group(2) : "";
            var fingerprint = Arrays.stream(remainder.split("\\s+"))
                    .filter(c -> !TIMESTAMP_COLUMN_PATTERN.matcher(c).matches())
                    .collect(Collectors.joining(" "));
            result.put(path, fingerprint);
        });
        return result;
    }
}
