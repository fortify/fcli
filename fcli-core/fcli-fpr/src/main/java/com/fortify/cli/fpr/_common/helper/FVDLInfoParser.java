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
package com.fortify.cli.fpr._common.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliTechnicalException;

/**
 * Lightweight StAX-based parser that extracts Build and EngineData metadata
 * from the FVDL file without parsing the (potentially large) Vulnerabilities section.
 */
public final class FVDLInfoParser {

    private FVDLInfoParser() {}

    // ── Records ──────────────────────────────────────────────────────

    public record FVDLInfo(BuildInfo build, EngineInfo engine) {}

    public record BuildInfo(
            String project, String version, String buildID,
            Integer numberFiles, List<LocEntry> totalLoc,
            String sourceBasePath, Integer scanTimeSeconds,
            Integer buildDuration, List<SourceFileInfo> sourceFiles
    ) {}

    public record LocEntry(String type, int value) {}

    public record SourceFileInfo(
            String name, String type, String size,
            String encoding, Integer loc, List<LocEntry> locDetails
    ) {}

    public record EngineInfo(
            String engineVersion, MachineInfo machineInfo,
            List<String> commandLine, List<ErrorEntry> errors,
            List<RulePackInfo> rulePacks
    ) {}

    public record MachineInfo(String hostname, String username, String platform) {}

    public record ErrorEntry(String code, String message) {}

    public record RulePackInfo(String id, String name, String version, String sku) {}

    // ── Public API ───────────────────────────────────────────────────

    public static FVDLInfo parse(FprHandle fprHandle) {
        Path fvdlPath = fprHandle.getPath("/audit.fvdl");
        if (!Files.exists(fvdlPath)) {
            throw new FcliTechnicalException("audit.fvdl not found in FPR file");
        }
        var xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        try (InputStream is = Files.newInputStream(fvdlPath)) {
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(is);
            try {
                return doParse(reader);
            } finally {
                reader.close();
            }
        } catch (IOException | XMLStreamException e) {
            throw new FcliTechnicalException("Failed to parse FVDL metadata", e);
        }
    }

    // ── Top-level dispatcher ─────────────────────────────────────────

    private static FVDLInfo doParse(XMLStreamReader reader) throws XMLStreamException {
        BuildInfo buildInfo = null;
        EngineInfo engineInfo = null;

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (reader.getLocalName()) {
                    case "Build"           -> buildInfo = parseBuild(reader);
                    case "EngineData"      -> engineInfo = parseEngineData(reader);
                    case "Vulnerabilities" -> skipSection(reader, "Vulnerabilities");
                    default                -> { /* other top-level: UnifiedNodePool, etc. — skip implicitly */ }
                }
            }
        }
        return new FVDLInfo(
                buildInfo != null ? buildInfo : new BuildInfo(null, null, null, null, List.of(), null, null, null, List.of()),
                engineInfo != null ? engineInfo : new EngineInfo(null, null, List.of(), List.of(), List.of())
        );
    }

    // ── Build section ────────────────────────────────────────────────

    private static BuildInfo parseBuild(XMLStreamReader reader) throws XMLStreamException {
        String project = null, version = null, buildID = null, sourceBasePath = null;
        Integer numberFiles = null, scanTimeSeconds = null, buildDuration = null;
        var totalLoc = new ArrayList<LocEntry>();
        var sourceFiles = new ArrayList<SourceFileInfo>();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (reader.getLocalName()) {
                    case "Project"        -> project = readText(reader);
                    case "Version"        -> version = readText(reader);
                    case "BuildID"        -> buildID = readText(reader);
                    case "NumberFiles"    -> numberFiles = Integer.parseInt(readText(reader).trim());
                    case "BuildDuration"  -> buildDuration = Integer.parseInt(readText(reader).trim());
                    case "LOC"            -> totalLoc.add(parseLoc(reader));
                    case "SourceBasePath" -> sourceBasePath = readText(reader);
                    case "SourceFiles"    -> parseSourceFiles(reader, sourceFiles);
                    case "ScanTime"       -> {
                        var val = reader.getAttributeValue(null, "value");
                        if (val != null) { scanTimeSeconds = Integer.parseInt(val.trim()); }
                        skipSection(reader, "ScanTime");
                    }
                    default -> { /* JavaClasspath, Libdirs, Label — not needed */ }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "Build".equals(reader.getLocalName())) {
                break;
            }
        }
        return new BuildInfo(project, version, buildID, numberFiles, totalLoc,
                sourceBasePath, scanTimeSeconds, buildDuration, sourceFiles);
    }

    private static LocEntry parseLoc(XMLStreamReader reader) throws XMLStreamException {
        String type = reader.getAttributeValue(null, "type");
        String text = readText(reader);
        return new LocEntry(type, Integer.parseInt(text.trim()));
    }

    private static void parseSourceFiles(XMLStreamReader reader, List<SourceFileInfo> result) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "File".equals(reader.getLocalName())) {
                result.add(parseSourceFile(reader));
            } else if (event == XMLStreamConstants.END_ELEMENT && "SourceFiles".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    private static SourceFileInfo parseSourceFile(XMLStreamReader reader) throws XMLStreamException {
        String type = reader.getAttributeValue(null, "type");
        String size = reader.getAttributeValue(null, "size");
        String encoding = reader.getAttributeValue(null, "encoding");
        String locAttr = reader.getAttributeValue(null, "loc");
        Integer loc = locAttr != null ? Integer.parseInt(locAttr.trim()) : null;

        String name = null;
        var locDetails = new ArrayList<LocEntry>();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (reader.getLocalName()) {
                    case "Name" -> name = readText(reader);
                    case "LOC"  -> locDetails.add(parseLoc(reader));
                    default     -> skipSection(reader, reader.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "File".equals(reader.getLocalName())) {
                break;
            }
        }
        return new SourceFileInfo(name, type, size, encoding, loc, locDetails);
    }

    // ── EngineData section ───────────────────────────────────────────

    private static EngineInfo parseEngineData(XMLStreamReader reader) throws XMLStreamException {
        String engineVersion = null;
        MachineInfo machineInfo = null;
        var commandLine = new ArrayList<String>();
        var errors = new ArrayList<ErrorEntry>();
        var rulePacks = new ArrayList<RulePackInfo>();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (reader.getLocalName()) {
                    case "EngineVersion" -> engineVersion = readText(reader);
                    case "MachineInfo"   -> machineInfo = parseMachineInfo(reader);
                    case "CommandLine"   -> parseCommandLine(reader, commandLine);
                    case "Errors"        -> parseErrors(reader, errors);
                    case "RulePacks"     -> parseRulePacks(reader, rulePacks);
                    default              -> skipSection(reader, reader.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "EngineData".equals(reader.getLocalName())) {
                break;
            }
        }
        return new EngineInfo(engineVersion, machineInfo, commandLine, errors, rulePacks);
    }

    private static MachineInfo parseMachineInfo(XMLStreamReader reader) throws XMLStreamException {
        String hostname = null, username = null, platform = null;
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (reader.getLocalName()) {
                    case "Hostname" -> hostname = readText(reader);
                    case "Username" -> username = readText(reader);
                    case "Platform" -> platform = readText(reader);
                    default -> skipSection(reader, reader.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "MachineInfo".equals(reader.getLocalName())) {
                break;
            }
        }
        return new MachineInfo(hostname, username, platform);
    }

    private static void parseCommandLine(XMLStreamReader reader, List<String> result) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "Argument".equals(reader.getLocalName())) {
                result.add(readText(reader));
            } else if (event == XMLStreamConstants.END_ELEMENT && "CommandLine".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    private static void parseErrors(XMLStreamReader reader, List<ErrorEntry> result) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "Error".equals(reader.getLocalName())) {
                String code = reader.getAttributeValue(null, "code");
                String message = readText(reader);
                result.add(new ErrorEntry(code, message));
            } else if (event == XMLStreamConstants.END_ELEMENT && "Errors".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    private static void parseRulePacks(XMLStreamReader reader, List<RulePackInfo> result) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "RulePack".equals(reader.getLocalName())) {
                result.add(parseRulePack(reader));
            } else if (event == XMLStreamConstants.END_ELEMENT && "RulePacks".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    private static RulePackInfo parseRulePack(XMLStreamReader reader) throws XMLStreamException {
        String id = null, name = null, version = null, sku = null;
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (reader.getLocalName()) {
                    case "RulePackID" -> id = readText(reader);
                    case "Name"       -> name = readText(reader);
                    case "Version"    -> version = readText(reader);
                    case "SKU"        -> sku = readText(reader);
                    default           -> skipSection(reader, reader.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "RulePack".equals(reader.getLocalName())) {
                break;
            }
        }
        return new RulePackInfo(id, name, version, sku);
    }

    // ── XML utilities ────────────────────────────────────────────────

    private static String readText(XMLStreamReader reader) throws XMLStreamException {
        var sb = new StringBuilder();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                sb.append(reader.getText());
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return sb.toString().trim();
    }

    private static void skipSection(XMLStreamReader reader, String sectionName) throws XMLStreamException {
        int depth = 1;
        while (reader.hasNext() && depth > 0) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
            }
        }
    }
}
